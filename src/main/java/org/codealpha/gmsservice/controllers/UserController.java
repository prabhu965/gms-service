package org.codealpha.gmsservice.controllers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.transaction.Transactional;
import javax.xml.ws.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.codealpha.gmsservice.entities.Grant;
import org.codealpha.gmsservice.entities.Grantee;
import org.codealpha.gmsservice.entities.Organization;
import org.codealpha.gmsservice.entities.Role;
import org.codealpha.gmsservice.entities.User;
import org.codealpha.gmsservice.entities.UserRole;
import org.codealpha.gmsservice.models.ErrorMessage;
import org.codealpha.gmsservice.models.UserVO;
import org.codealpha.gmsservice.services.DashboardService;
import org.codealpha.gmsservice.services.CommonEmailSevice;
import org.codealpha.gmsservice.services.GranteeService;
import org.codealpha.gmsservice.services.GranterService;
import org.codealpha.gmsservice.services.OrganizationService;
import org.codealpha.gmsservice.services.RoleService;
import org.codealpha.gmsservice.services.UserService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

/**
 * @author Developer <developer@enstratify.com>
 **/
@RestController
@RequestMapping("/users")
public class UserController {


  @Autowired
  private OrganizationService organizationService;
  @Autowired
  private CommonEmailSevice commonEmailSevice;
  @Autowired
  private UserService userService;
  @Autowired
  private GranteeService granteeService;
  @Autowired
  private GranterService granterService;
  @Autowired
  private RoleService roleService;
  @Autowired
  private DashboardService dashboardService;

  @GetMapping(value = "/{id}")
  public User get(@PathVariable(name = "id") Long id,
      @RequestHeader("X-TENANT-CODE") String tenantCode) {
    User user = userService.getUserById(id);

    return user;
  }

  @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
  @Transactional
  public User create(@RequestBody UserVO user) {

    User newUser = new User();

    //BCryptPasswordEncoder a  = new BCryptPasswordEncoder
    newUser.setCreatedAt(DateTime.now().toDate());
    newUser.setCreatedBy("Api");
    newUser.setEmailId(user.getEmailId());
    Organization newGranteeOrg = new Grantee();
    newGranteeOrg.setOrganizationType("GRANTEE");
    newGranteeOrg.setCreatedAt(DateTime.now().toDate());
    newGranteeOrg.setCreatedBy("System");
    newGranteeOrg.setName("To be set");

    newGranteeOrg = organizationService.save(newGranteeOrg);
    newUser.setOrganization(newGranteeOrg);
    newUser.setPassword(user.getPassword());

    Role newRole = new Role();
    newRole.setName("Admin");
    newRole.setOrganization(newGranteeOrg);
    newRole.setCreatedAt(DateTime.now().toDate());
    newRole.setCreatedBy("System");

    newRole = roleService.saveRole(newRole);
    UserRole userRole = new UserRole();
    userRole.setRole(newRole);
    userRole.setUser(newUser);
    List<UserRole> userRoles = new ArrayList<>();
    userRoles.add(userRole);
    newUser.setUserRoles(userRoles);
    newUser.setFirstName("To be set");
    newUser.setLastName("To be set");
    newUser = userService.save(newUser);

    UriComponents urlComponents = ServletUriComponentsBuilder.fromCurrentContextPath().build();

    String scheme = urlComponents.getScheme();
    String host = urlComponents.getHost();
    int port = urlComponents.getPort();

    String verificationLink =
        scheme + "://" + host + (port != -1 ? ":" + port : "") + "/grantee/verification?emailId="
            + user.getEmailId() + "&code=" + RandomStringUtils.randomAlphanumeric(127);

    System.out.println(verificationLink);
    commonEmailSevice
        .sendMail(user.getEmailId(), "Anudan.org - Verification Link", verificationLink);
    return newUser;
  }

  @PutMapping(value = "/", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public User update(@RequestBody UserVO user) {
    //BCryptPasswordEncoder a  = new BCryptPasswordEncoder
    User savedUser = userService.getUserById(user.getId());
    savedUser.setFirstName(user.getFirstName());
    savedUser.setLastName(user.getLastName());



    Organization userOrg = savedUser.getOrganization();
    userOrg.setName(user.getOrganization().getName());

    userOrg = organizationService.save(userOrg);
    savedUser.setOrganization(userOrg);
    savedUser = userService.save(savedUser);
    return savedUser;
  }


  @PostMapping("/activation")
  public HttpStatus verifyUser(@RequestParam("emailId") String email,
      @RequestParam("code") String code) {
    return HttpStatus.OK;
  }

  @GetMapping("/{id}/dashboard")
  public ResponseEntity<DashboardService> getDashbaord(
      @RequestHeader("X-TENANT-CODE") String tenantCode, @PathVariable("id") Long userId) {
    User user = userService.getUserById(userId);
    Organization userOrg = user.getOrganization();
    Organization tenantOrg = organizationService.findOrganizationByTenantCode(tenantCode);
    List<Grant> grants = null;
    switch (userOrg.getType()) {
      case "GRANTEE":
        grants = granteeService
            .getGrantsOfGranteeForGrantor(userOrg.getId(), tenantOrg, user.getUserRoles());
        return new ResponseEntity<>(dashboardService.build(user, grants,tenantOrg), HttpStatus.OK);
      case "GRANTER":
        grants = granterService.getGrantsOfGranterForGrantor(userOrg.getId(), tenantOrg, user.getId());
        return new ResponseEntity<>(dashboardService.build(user, grants,tenantOrg), HttpStatus.OK);
    }

    return new ResponseEntity<>(null, HttpStatus.OK);
  }

  @PostMapping("/{id}/validate-pwd")
  public ResponseEntity<ErrorMessage> validatePassword(@PathVariable("id") Long userId,
      @RequestBody String pwd) {
    User user = userService.getUserById(userId);
    if (user.getPassword().equalsIgnoreCase(pwd)) {
      return new ResponseEntity<>(new ErrorMessage(true,""), HttpStatus.OK);
    } else {
      return new ResponseEntity<>(new ErrorMessage(false,"You have entered an invalid previous password"), HttpStatus.OK);
    }
  }

  @PostMapping("/{id}/pwd")
  public ResponseEntity<User> changePassword(@PathVariable("id") Long userId,
      @RequestBody String pwd) {
    User user = userService.getUserById(userId);
    user.setPassword(pwd);
    user = userService.save(user);
    return new ResponseEntity<>(user, HttpStatus.OK);
  }
}
