package org.codealpha.gmsservice.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class GrantDocumentAttributes {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column
  private String name;
  @Column
  private String fileType;
  @Column
  private String location;
  @Column
  private int version;
  @OneToOne
  @JoinColumn(referencedColumnName = "id")
  @JsonProperty("attributeDetails")
  private GranterGrantSectionAttribute sectionAttribute;
  @ManyToOne
  @JoinColumn(referencedColumnName = "id")
  @JsonProperty("sectionDetails")
  private GranterGrantSection section;
  @ManyToOne
  @JoinColumn(referencedColumnName = "id")
  @JsonIgnore
  private Grant grant;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public GranterGrantSectionAttribute getSectionAttribute() {
    return sectionAttribute;
  }

  public void setSectionAttribute(
      GranterGrantSectionAttribute sectionAttribute) {
    this.sectionAttribute = sectionAttribute;
  }

  public GranterGrantSection getSection() {
    return section;
  }

  public void setSection(GranterGrantSection section) {
    this.section = section;
  }

  public Grant getGrant() {
    return grant;
  }

  public void setGrant(Grant grant) {
    this.grant = grant;
  }
}
