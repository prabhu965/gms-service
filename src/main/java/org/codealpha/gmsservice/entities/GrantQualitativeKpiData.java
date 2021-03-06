package org.codealpha.gmsservice.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class GrantQualitativeKpiData extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column
  private String goal;
  @Column(nullable = true)
  private String actuals;
  @Column(nullable = true)
  private String note;
  @Column
  private Boolean toReport;

  @ManyToOne
  @JoinColumn(referencedColumnName = "id")
  @JsonProperty(access = Access.WRITE_ONLY)
  @JsonBackReference
  private Submission submission;

  @ManyToOne
  @JoinColumn(referencedColumnName = "id")
  private GrantKpi grantKpi;

  @OneToMany(mappedBy = "kpiData", fetch = FetchType.LAZY,cascade = CascadeType.ALL)
  private List<QualitativeKpiNotes> notesHistory;

  @OneToMany(mappedBy = "qualKpiData",cascade = CascadeType.ALL)
  List<QualKpiDataDocument> submissionDocs;

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public void setId(Long id) {
    this.id = id;
  }

  public String getGoal() {
    return goal;
  }

  public void setGoal(String goal) {
    this.goal = goal;
  }

  public Submission getSubmission() {
    return submission;
  }

  public void setSubmission(Submission submission) {
    this.submission = submission;
  }

  public GrantKpi getGrantKpi() {
    return grantKpi;
  }

  public void setGrantKpi(GrantKpi grantKpi) {
    this.grantKpi = grantKpi;
  }

  public String getActuals() {
    return actuals;
  }

  public void setActuals(String actuals) {
    this.actuals = actuals;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public List<QualitativeKpiNotes> getNotesHistory() {
    return notesHistory;
  }

  public void setNotesHistory(
      List<QualitativeKpiNotes> notesHistory) {
    this.notesHistory = notesHistory;
  }

  public List<QualKpiDataDocument> getSubmissionDocs() {
    return submissionDocs;
  }

  public void setSubmissionDocs(
      List<QualKpiDataDocument> submissionDocs) {
    this.submissionDocs = submissionDocs;
  }

  public Boolean getToReport() {
    return toReport;
  }

  public void setToReport(Boolean toReport) {
    this.toReport = toReport;
  }
}
