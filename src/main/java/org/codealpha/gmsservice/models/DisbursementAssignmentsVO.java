package org.codealpha.gmsservice.models;

import org.codealpha.gmsservice.entities.User;
import org.codealpha.gmsservice.entities.WorkflowStatus;


public class DisbursementAssignmentsVO {

    private Long id;
    private Long disbursementId;
    private Long stateId;
    private WorkflowStatus stateName;
    private Long assignmentId;
    private User assignmentUser;
    private boolean anchor = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDisbursementId() {
        return disbursementId;
    }

    public void setDisbursementId(Long disbursementId) {
        this.disbursementId = disbursementId;
    }

    public Long getStateId() {
        return stateId;
    }

    public void setStateId(Long stateId) {
        this.stateId = stateId;
    }

    public WorkflowStatus getStateName() {
        return stateName;
    }

    public void setStateName(WorkflowStatus stateName) {
        this.stateName = stateName;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public User getAssignmentUser() {
        return assignmentUser;
    }

    public void setAssignmentUser(User assignmentUser) {
        this.assignmentUser = assignmentUser;
    }

    public boolean isAnchor() {
        return anchor;
    }

    public void setAnchor(boolean anchor) {
        this.anchor = anchor;
    }
}
