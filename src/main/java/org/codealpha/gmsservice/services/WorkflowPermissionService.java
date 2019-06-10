package org.codealpha.gmsservice.services;

import java.util.List;
import java.util.stream.Collectors;
import org.codealpha.gmsservice.entities.Organization;
import org.codealpha.gmsservice.entities.Role;
import org.codealpha.gmsservice.entities.UserRole;
import org.codealpha.gmsservice.entities.WorkFlowPermission;
import org.codealpha.gmsservice.entities.WorkflowActionPermission;
import org.codealpha.gmsservice.repositories.WorkflowActionPermissionRepository;
import org.codealpha.gmsservice.repositories.WorkflowPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowPermissionService {

  @Autowired
  private WorkflowPermissionRepository workflowPermissionRepository;
  @Autowired
  private WorkflowActionPermissionRepository workflowActionPermissionRepository;

  public List<WorkFlowPermission> getGrantFlowPermissions(Long granterOrgId,
      List<UserRole> userRoles) {

    List<Long> userRoleIds = userRoles.stream().map(e -> new Long(e.getRole().getId())).collect(
        Collectors.toList());

    return workflowPermissionRepository.getPermissionsForGrantFlow(granterOrgId, userRoleIds);
  }

  public List<WorkFlowPermission> getSubmissionFlowPermissions(Long granterOrgId, List<UserRole> userRoles,
      Long statusId) {

    List<Long> userRoleIds = userRoles.stream().map(e->new Long(e.getRole().getId())).collect(
        Collectors.toList());

    return workflowPermissionRepository
        .getPermissionsForSubmissionFlow(granterOrgId, userRoleIds, statusId);
  }


  public WorkflowActionPermission getGrantActionPermissions(Long granterOrgId,
      List<UserRole> userRoles, Long grantStatusId) {

    List<Long> userRoleIds = userRoles.stream().map(e -> new Long(e.getRole().getId())).collect(
        Collectors.toList());
    return workflowActionPermissionRepository
        .getActionPermissionsForGrant(granterOrgId, userRoleIds, grantStatusId);
  }

  public WorkflowActionPermission getSubmissionActionPermission(Long granterOrgId,
      List<UserRole> userRoles) {

    List<Long> userRoleIds = userRoles.stream().map(e -> new Long(e.getRole().getId())).collect(
        Collectors.toList());

    return workflowActionPermissionRepository
        .getActionPermissionsForSubmission(granterOrgId, userRoleIds);
  }


}
