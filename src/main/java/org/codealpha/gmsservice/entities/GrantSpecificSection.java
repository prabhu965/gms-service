package org.codealpha.gmsservice.entities;

import javax.persistence.*;

@Entity(name = "grant_specific_sections")
public class GrantSpecificSection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column
  private String sectionName;
  @Column
  private Integer sectionOrder;
  @Column
  private Boolean deletable;
  @ManyToOne
  @JoinColumn(referencedColumnName = "id")
  private Granter granter;
  @Column
  private Long grantTemplateId;



  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSectionName() {
    return sectionName;
  }

  public void setSectionName(String sectionName) {
    this.sectionName = sectionName;
  }

  public Granter getGranter() {
    return granter;
  }

  public void setGranter(Granter granter) {
    this.granter = granter;
  }

  public Boolean getDeletable() {
    return deletable;
  }

  public void setDeletable(Boolean deletable) {
    this.deletable = deletable;
  }

  public Long getGrantTemplateId() {
    return grantTemplateId;
  }

  public void setGrantTemplateId(Long grantTemplateId) {
    this.grantTemplateId = grantTemplateId;
  }

  public Integer getSectionOrder() {
    return sectionOrder;
  }

  public void setSectionOrder(Integer sectionOrder) {
    this.sectionOrder = sectionOrder;
  }
}
