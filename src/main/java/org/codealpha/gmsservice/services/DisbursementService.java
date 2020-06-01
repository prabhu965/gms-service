package org.codealpha.gmsservice.services;

import org.codealpha.gmsservice.constants.AppConfiguration;
import org.codealpha.gmsservice.entities.*;
import org.codealpha.gmsservice.models.GrantVO;
import org.codealpha.gmsservice.repositories.DisbursementAssignmentRepository;
import org.codealpha.gmsservice.repositories.DisbursementRepository;
import org.codealpha.gmsservice.repositories.WorkflowPermissionRepository;
import org.codealpha.gmsservice.repositories.WorkflowStatusTransitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class DisbursementService {

    @Autowired
    private DisbursementRepository disbursementRepository;
    @Autowired
    private WorkflowStatusTransitionRepository workflowStatusTransitionRepository;
    @Autowired
    private DisbursementAssignmentRepository disbursementAssignmentRepository;
    @Autowired
    private WorkflowPermissionRepository workflowPermissionRepository;
    @Autowired
    private GrantService grantService;
    @Autowired
    private WorkflowPermissionService workflowPermissionService;
    @Autowired
    private UserService userService;
    @Autowired
    private AppConfigService appConfigService;

    public Disbursement saveDisbursement(Disbursement disbursement){
        return disbursementRepository.save(disbursement);
    }

    public Disbursement createAssignmentPlaceholders(Disbursement disbursementToSave, Long userId) {
        Workflow currentWorkflow = disbursementToSave.getStatus().getWorkflow();

        List<WorkflowStatus> statuses = new ArrayList<>();
        List<WorkflowStatusTransition> supportedTransitions = workflowStatusTransitionRepository.findByWorkflow(currentWorkflow);
        for (WorkflowStatusTransition supportedTransition : supportedTransitions) {
            if(!statuses.stream().filter(s -> Long.valueOf(s.getId())==Long.valueOf(supportedTransition.getFromState().getId())).findAny().isPresent()) {
                statuses.add(supportedTransition.getFromState());
            }
            if(!statuses.stream().filter(s -> Long.valueOf(s.getId())==Long.valueOf(supportedTransition.getToState().getId())).findAny().isPresent()) {
                statuses.add(supportedTransition.getToState());
            }
        }

        List<DisbursementAssignment> assignmentList = new ArrayList<>();
        DisbursementAssignment assignment = null;
        for (WorkflowStatus status : statuses) {
            if (!status.getTerminal()) {
                assignment = new DisbursementAssignment();
                if (status.isInitial()) {
                    assignment.setAnchor(true);
                    assignment.setOwner(userId);
                } else {
                    assignment.setAnchor(false);
                }
                assignment.setDisbursementId(disbursementToSave.getId());
                assignment.setStateId(status.getId());
                assignment = disbursementAssignmentRepository.save(assignment);
                assignmentList.add(assignment);
            }
        }

        disbursementToSave.setAssignments(assignmentList);
        disbursementToSave = disbursementToReturn(disbursementToSave,userId);
        return disbursementToSave;
    }

    public Disbursement disbursementToReturn(Disbursement disbursement,Long userId){
        List<WorkFlowPermission> permissions = workflowPermissionRepository.getPermissionsForDisbursementFlow(disbursement.getStatus().getId(),userId,disbursement.getId());
        
        disbursement.setFlowPermissions(permissions);
        disbursement.setAssignments(disbursementAssignmentRepository.findByDisbursementId(disbursement.getId()));

        GrantVO vo = new GrantVO().build(disbursement.getGrant(), grantService.getGrantSections(disbursement.getGrant()), workflowPermissionService, userService.getUserById(userId) , appConfigService.getAppConfigForGranterOrg(disbursement.getGrant().getGrantorOrganization().getId(),
        AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS), userService);

        disbursement.getGrant().setGrantDetails(vo.getGrantDetails());
        return disbursement;
    }

    public List<Disbursement> getDisbursementsForUserByStatus(User user,Organization org,String status) {
       List<Disbursement> disbursements = new ArrayList<>();
       if(status.equalsIgnoreCase("DRAFT")){
        disbursements = disbursementRepository.getInprogressDisbursementsForUser(user.getId(),org.getId());
       }else if(status.equalsIgnoreCase("ACTIVE")){
        disbursements = disbursementRepository.getActiveDisbursementsForUser(org.getId());
       }else if(status.equalsIgnoreCase("CLOSED")){
        disbursements = disbursementRepository.getClosedDisbursementsForUser(org.getId());
       }
       for(Disbursement d : disbursements){
           d = disbursementToReturn(d, user.getId());
       }
        return disbursements;
    }

	public Disbursement getDisbursementById(Long id) {
		return disbursementRepository.findById(id).get();
    }

    public List<DisbursementAssignment> getDisbursementAssignments(Disbursement disbursement){
        return disbursementAssignmentRepository.findByDisbursementId(disbursement.getId());
    }
    
    public void deleteAllAssignmentsForDisbursement(Disbursement disbursement){
        
        disbursementAssignmentRepository.deleteAll(getDisbursementAssignments(disbursement));
    }

    public void deleteDisbursement(Disbursement disbursement){
        disbursementRepository.delete(disbursement);
    }

    public DisbursementAssignment getDisbursementAssignmentById(Long id){
        return disbursementAssignmentRepository.findById(id).get();
    }

    public DisbursementAssignment saveAssignmentForDisbursement(DisbursementAssignment assignment){
        return disbursementAssignmentRepository.save(assignment);
    }

    public String[] buildEmailNotificationContent(Disbursement finalDisbursement, User user, String userName, String action, String date, String subConfigValue, String msgConfigValue, String currentState, String currentOwner, String previousState, String previousOwner, String previousAction, String hasChanges, String hasChangesComment, String hasNotes, String hasNotesComment, String link, User owner, Integer noOfDays) {

        String code = Base64.getEncoder().encodeToString(String.valueOf(finalDisbursement.getId()).getBytes());

        String host = "";
        String url = "";
        UriComponents uriComponents = null;
        try {
            uriComponents = ServletUriComponentsBuilder.fromCurrentContextPath().build();
            if (user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")) {
                host = uriComponents.getHost().substring(uriComponents.getHost().indexOf(".") + 1);

            } else {
                host = uriComponents.getHost();
            }
            UriComponentsBuilder uriBuilder =  UriComponentsBuilder.newInstance().scheme(uriComponents.getScheme()).host(host).port(uriComponents.getPort());
            url = uriBuilder.toUriString();
            url = url+"/home/?action=login&g=" + code+"&email=&type=disbursement";
        }catch (Exception e){
            url = link;

            url = url+"/home/?action=login&g=" + code+"&email=&type=disbursement";   
        }



        String message = msgConfigValue.replaceAll("%GRANT_NAME%", finalDisbursement.getGrant().getName()).replaceAll("%CURRENT_STATE%", currentState).replaceAll("%CURRENT_OWNER%", currentOwner).replaceAll("%PREVIOUS_STATE%", previousState).replaceAll("%PREVIOUS_OWNER%", previousOwner).replaceAll("%PREVIOUS_ACTION%", previousAction).replaceAll("%HAS_CHANGES%", hasChanges).replaceAll("%HAS_CHANGES_COMMENT%", hasChangesComment).replaceAll("%HAS_NOTES%",hasNotes).replaceAll("%HAS_NOTES_COMMENT%",hasNotesComment).replaceAll("%TENANT%",finalDisbursement.getGrant().getGrantorOrganization().getName()).replaceAll("%GRANT_LINK%",url).replaceAll("%OWNER_NAME%",owner==null?"":owner.getFirstName()+" "+owner.getLastName()).replaceAll("%OWNER_EMAIL%",owner==null?"":owner.getEmailId()).replaceAll("%NO_DAYS%",noOfDays==null?"":String.valueOf(noOfDays));
        String subject = subConfigValue.replaceAll("%GRANT_NAME%", finalDisbursement.getGrant().getName());

        return new String[]{subject, message};
    }
}
