package org.codealpha.gmsservice.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codealpha.gmsservice.entities.*;
import org.codealpha.gmsservice.services.ReportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ReportDetailVO {

  private List<SectionVO> sections;

  public List<SectionVO> getSections() {
    return sections;
  }

  public void setSections(List<SectionVO> sections) {
    Collections.sort(sections);
    this.sections = sections;
  }

  public ReportDetailVO buildStringAttributes(List<ReportSpecificSection> grantSections, List<ReportStringAttribute> value, ReportService reportService, Long grantId) {

    SectionVO sectionVO = null;
    sections = new ArrayList<>();

    for(ReportSpecificSection sec: grantSections){
      sectionVO = new SectionVO();
      sectionVO.setId(sec.getId());
      sectionVO.setName(sec.getSectionName());
      sectionVO.setOrder(sec.getSectionOrder());

      if (!sections.contains(sectionVO)) {
        sections.add(sectionVO);
      }
    }
    if(value!=null) {
      for (ReportStringAttribute stringAttribute : value) {

        Optional<SectionVO> so = sections.stream().filter(a -> a.getId()==stringAttribute.getSection().getId()).findFirst();
        if(so.isPresent()) {
          sectionVO = so.get();
        }else{
          continue;
        }
        List<SectionAttributesVO> sectionAttributes = sectionVO.getAttributes();
        SectionAttributesVO sectionAttribute = new SectionAttributesVO();
        sectionAttribute.setId(stringAttribute.getId());
        sectionAttribute.setFieldName(stringAttribute.getSectionAttribute().getFieldName());
        sectionAttribute
            .setFieldType(stringAttribute.getSectionAttribute().getFieldType());
        //sectionAttribute.setDeletable(stringAttribute.getSectionAttribute().getDeletable());
        sectionAttribute.setRequired(stringAttribute.getSectionAttribute().getRequired());
        sectionAttribute.setAttributeOrder(stringAttribute.getSectionAttribute().getAttributeOrder());
        sectionAttribute.setCanEdit(stringAttribute.getSectionAttribute().getCanEdit());
        sectionAttribute.setGrantLevelTarget(stringAttribute.getGrantLevelTarget());
        sectionAttribute.setFieldValue(stringAttribute.getValue());
        if(grantId!=0) {
          sectionAttribute.setCumulativeActuals(reportService.getApprovedReportsActualSumForGrant(grantId, sectionAttribute.getFieldName()));
        }
        if(sectionAttribute.getFieldType().equalsIgnoreCase("table")){
            ObjectMapper mapper = new ObjectMapper();
          if(sectionAttribute.getFieldValue()==null || sectionAttribute.getFieldValue().trim().equalsIgnoreCase("") ){
            List<TableData> tableDataList = new ArrayList<>();
            TableData tableData = new TableData();
            tableData.setName("");
            tableData.setColumns(new ColumnData[5]);
            for(int i=0;i<tableData.getColumns().length;i++){

              tableData.getColumns()[i] = new ColumnData("","");
            }
            tableDataList.add(tableData);

            try {
              sectionAttribute.setFieldValue( mapper.writeValueAsString(tableDataList));
            } catch (JsonProcessingException e) {
              e.printStackTrace();
            }
          }
          List<TableData> tableData = null;
          try {
            tableData = mapper.readValue(sectionAttribute.getFieldValue(), new TypeReference<List<TableData>>() {});
          } catch (IOException e) {
            e.printStackTrace();
          }
          sectionAttribute.setFieldTableValue(tableData);
        } /*else if(sectionAttribute.getFieldType().equalsIgnoreCase("disbursement")){
          ObjectMapper mapper = new ObjectMapper();
          String[] colHeaders = new String[]{"Disbursement Date","Actual Disbursement","Funds from other Sources","Notes"};
          if(sectionAttribute.getFieldValue()==null || sectionAttribute.getFieldValue().trim().equalsIgnoreCase("") ){
            List<TableData> tableDataList = new ArrayList<>();
            TableData tableData = new TableData();
            tableData.setName("1");
            tableData.setHeader("#");
            tableData.setEnteredByGrantee(false);
            tableData.setColumns(new ColumnData[4]);
            for(int i=0;i<tableData.getColumns().length;i++){

              tableData.getColumns()[i] = new ColumnData(colHeaders[i],"",(i==1 || i==2)?"currency":i==0?"date":null);
            }
            tableDataList.add(tableData);

            try {
              sectionAttribute.setFieldValue( mapper.writeValueAsString(tableDataList));
            } catch (JsonProcessingException e) {
              e.printStackTrace();
            }
          }
          List<TableData> tableData = null;
          try {
            tableData = mapper.readValue(sectionAttribute.getFieldValue(), new TypeReference<List<TableData>>() {});
          } catch (IOException e) {
            e.printStackTrace();
          }
          sectionAttribute.setFieldTableValue(tableData);
        }*/ else if(sectionAttribute.getFieldType().equalsIgnoreCase("document")){

          ObjectMapper mapper = new ObjectMapper();
          if(sectionAttribute.getFieldValue()==null || sectionAttribute.getFieldValue().trim().equalsIgnoreCase("") ) {
            sectionAttribute.setAttachments(new ArrayList<>());
          }else{
            try {
              List<GrantStringAttributeAttachments> assignedTemplates = mapper.readValue(sectionAttribute.getFieldValue(),new TypeReference<List<GrantStringAttributeAttachments>>(){});
              sectionAttribute.setAttachments(assignedTemplates);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
        sectionAttribute.setTarget(stringAttribute.getTarget());
        sectionAttribute.setFrequency(stringAttribute.getFrequency());
        sectionAttribute.setActualTarget(stringAttribute.getActualTarget());
        
        if (sectionAttributes == null) {
          sectionAttributes = new ArrayList<>();
        }
        if (!sectionAttributes.contains(sectionAttribute)) {
          sectionAttributes.add(sectionAttribute);
        }
        sectionVO.setAttributes(sectionAttributes);

      }
    }
    return this;
  }


  public ReportDetailVO buildDocumentAttributes(List<GrantDocumentAttributes> value) {
    sections = new ArrayList<>();
    SectionVO sectionVO = null;
    if(value!=null) {
      for (GrantDocumentAttributes documentAttribute : value) {
        sectionVO = new SectionVO();
        sectionVO.setId(documentAttribute.getSection().getId());
        sectionVO.setName(documentAttribute.getSection().getSectionName());

        if (!sections.contains(sectionVO)) {
          sections.add(sectionVO);
        }

        sectionVO = sections.get(sections.indexOf(sectionVO));
        List<SectionAttributesVO> sectionAttributes = sectionVO.getAttributes();
        SectionAttributesVO sectionAttribute = new SectionAttributesVO();
        sectionAttribute.setId(documentAttribute.getId());
        sectionAttribute.setFieldName(documentAttribute.getName());
        sectionAttribute
            .setFieldType(documentAttribute.getSectionAttribute().getFieldType());

        sectionAttribute.setDeletable(documentAttribute.getSectionAttribute().getDeletable());
        sectionAttribute.setRequired(documentAttribute.getSectionAttribute().getRequired());

        sectionAttribute.setFieldValue(documentAttribute.getLocation());
        if (sectionAttributes == null) {
          sectionAttributes = new ArrayList<>();
        }
        if (sectionAttributes.size()>0 && !sectionAttributes.contains(sectionAttribute)) {
          sectionAttributes.add(sectionAttribute);
        }
        sectionVO.setAttributes(sectionAttributes);

      }
    }
    return this;
  }
}
