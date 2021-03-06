package org.codealpha.gmsservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.*;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codealpha.gmsservice.entities.User;
import org.codealpha.gmsservice.exceptions.InvalidCredentialsException;
import org.codealpha.gmsservice.exceptions.InvalidHeadersException;
import org.codealpha.gmsservice.repositories.OrganizationRepository;
import org.codealpha.gmsservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class JWTLoginFilter extends AbstractAuthenticationProcessingFilter {

  @Autowired
  private UserRepository userRepository;
  private OrganizationRepository organizationRepository;
  AccountCredentials creds;

  public JWTLoginFilter(String url, AuthenticationManager authManager,
                        UserRepository userRepository, OrganizationRepository organizationRepository) {
    super(new AntPathRequestMatcher(url));
    setAuthenticationManager(authManager);
    this.userRepository = userRepository;
    this.organizationRepository = organizationRepository;
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request,
      HttpServletResponse response) throws  AuthenticationException, IOException, ServletException {



    creds =
        new ObjectMapper().readValue(request.getInputStream(), AccountCredentials.class);
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new GrantedAuthority() {

      @Override
      public String getAuthority() {
        // TODO Auto-generated method stub
        return creds.getProvider();
      }
    });



    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
        creds.getUsername(), creds.getPassword(), authorities);
    Map<String,String> detailsMap = new HashMap<>();
    detailsMap.put("TOKEN",request.getHeader("X-TENANT-CODE"));
    detailsMap.put("CAPTCHA",creds.getRecaptchaToken());
    authToken.setDetails(detailsMap);
    return getAuthenticationManager().authenticate(authToken);
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res,
      FilterChain chain, Authentication auth) throws IOException, ServletException {

    User user = null;
    String token = ((Map<String,String>) auth.getDetails()).get("TOKEN");
    if(!"ANUDAN".equalsIgnoreCase(token)){
      user = userRepository.findByEmailIdAndOrganization(auth.getName(),organizationRepository.findByCode(token));
    }else if("ANUDAN".equalsIgnoreCase(token)){
      List<User> users = userRepository.findByEmailId(auth.getName());
      for (User user1 : users) {
        if(user1.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE") || user1.getOrganization().getOrganizationType().equalsIgnoreCase("PLATFORM")){
          user = user1;
          break;
        }
      }
    }

    Long userId = user.getId();


    ObjectMapper mapper = new ObjectMapper();
    String userJSON = mapper.writeValueAsString(user);

    JsonNode userNode = mapper.readTree(userJSON);

    String tenant = req.getHeader("X-TENANT-CODE");

    TokenAuthenticationService.addAuthentication(res, auth.getName(),userNode,tenant);

  }

  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request,
      HttpServletResponse response, AuthenticationException failed)
      throws IOException, ServletException {
    throw new InvalidCredentialsException(failed.getMessage());
  }
}
