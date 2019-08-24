package org.codealpha.gmsservice.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "granter_grant_sections")
public class GranterGrantSection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column
  private String sectionName;
  @Column
  private int sectionOrder;
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

  public int getSectionOrder() {
    return sectionOrder;
  }

  public void setSectionOrder(int sectionOrder) {
    this.sectionOrder = sectionOrder;
  }
}
