package org.codealpha.gmsservice.services;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.codealpha.gmsservice.constants.KpiType;
import org.codealpha.gmsservice.constants.WorkflowObject;
import org.codealpha.gmsservice.entities.*;
import org.codealpha.gmsservice.models.SecureEntity;
import org.codealpha.gmsservice.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GrantService {

    private static final String SECRET = "bhjgsdf788778hsdfhgsdf777werghsbdfjhdsf88yw3r7t7yt^%^%%@#Ghj";
    @Autowired
    private GrantRepository grantRepository;
    @Autowired
    private GranterGrantSectionRepository granterGrantSectionRepository;
    @Autowired
    private GranterGrantSectionAttributeRepository granterGrantSectionAttributeRepository;
    @Autowired
    private GrantStringAttributeRepository grantStringAttributeRepository;
    @Autowired
    private GrantDocumentAttributesRepository grantDocumentAttributesRepository;
    @Autowired
    private GrantQuantitativeDataRepository grantQuantitativeDataRepository;
    @Autowired
    private GrantKpiRepository grantKpiRepository;
    @Autowired
    private GrantQualitativeDataRepository grantQualitativeDataRepository;
    @Autowired
    private GrantDocumentDataRepository grantDocumentDataRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private DocumentKpiNotesRepository documentKpiNotesRepository;
    @Autowired
    private DocKpiDataDocumentRepository docKpiDataDocumentRepository;
    @Autowired
    private QualKpiDocumentRepository qualKpiDocumentRepository;
    @Autowired
    private QualitativeKpiNotesRepository qualitativeKpiNotesRepository;
    @Autowired
    private QuantitativeKpiNotesRepository quantitativeKpiNotesRepository;
    @Autowired
    private QuantKpiDocumentRepository quantKpiDocumentRepository;
    @Autowired
    private GrantSpecificSectionAttributeRepository grantSpecificSectionAttributeRepository;
    @Autowired
    private GrantSpecificSectionRepository grantSpecificSectionRepository;
    @Autowired
    private GranterGrantTemplateRepository granterGrantTemplateRepository;
    @Autowired
    private GrantAssignmentRepository grantAssignmentRepository;
    @Autowired
    private GrantStringAttributeAttachmentRepository grantStringAttributeAttachmentRepository;
    @Autowired
    private GrantHistoryRepository grantHistoryRepository;
    @Autowired
    private WorkflowRepository workflowRepository;
    @Autowired
    private WorkflowStatusTransitionRepository workflowStatusTransitionRepository;
    @Autowired
    private WorkflowStatusRepository workflowStatusRepository;
    @Autowired
    private TemplateLibraryRepository templateLibraryRepository;

    public List<String> getGrantAlerts(Grant grant) {
        return null;
    }

    public Grant saveGrant(Grant grant) {
        return grantRepository.save(grant);
    }

    public Grant getById(Long id) {
        return grantRepository.findById(id).get();
    }

    public GrantSpecificSection getGrantSectionBySectionId(Long sectionId) {

        Optional<GrantSpecificSection> granterGrantSection = grantSpecificSectionRepository.findById(sectionId);
        if (granterGrantSection.isPresent()) {
            return granterGrantSection.get();
        }
        return null;
    }

    public GrantSpecificSectionAttribute getAttributeById(Long attributeId) {
        return grantSpecificSectionAttributeRepository.findById(attributeId).get();
    }

    public GrantSpecificSectionAttribute getSectionAttributeByAttributeIdAndType(
            Long attributeId, String type) {
        if (type.equalsIgnoreCase("text")) {
            Optional<GrantStringAttribute> grantStringAttribute = grantStringAttributeRepository.findById(attributeId);
            if (grantStringAttribute.isPresent()) {
                return grantStringAttribute.get().getSectionAttribute();
            }
        } else if (type.equalsIgnoreCase("multiline")) {
            Optional<GrantStringAttribute> grantStringAttribute = grantStringAttributeRepository.findById(attributeId);
            if (grantStringAttribute.isPresent()) {
                return grantStringAttribute.get().getSectionAttribute();
            }
        } else if (type.equalsIgnoreCase("document")) {
            Optional<GrantStringAttribute> grantStringAttribute = grantStringAttributeRepository.findById(attributeId);
            if (grantStringAttribute.isPresent()) {
                return grantStringAttribute.get().getSectionAttribute();
            }
        } else if (type.equalsIgnoreCase("kpi")) {
            Optional<GrantStringAttribute> grantStringAttribute = grantStringAttributeRepository.findById(attributeId);
            if (grantStringAttribute.isPresent()) {
                return grantStringAttribute.get().getSectionAttribute();
            }
        } else if (type.equalsIgnoreCase("table")) {
            Optional<GrantStringAttribute> grantStringAttribute = grantStringAttributeRepository.findById(attributeId);
            if (grantStringAttribute.isPresent()) {
                return grantStringAttribute.get().getSectionAttribute();
            }
        }

        return null;
    }

    public List<GrantStringAttribute> getStringAttributesByAttribute(
            GrantSpecificSectionAttribute grantSectionAttribute) {
        return grantStringAttributeRepository.findBySectionAttribute(grantSectionAttribute);
    }

    public GrantDocumentAttributes getDocumentAttributeById(Long docAttribId) {
        return grantDocumentAttributesRepository.findById(docAttribId).get();
    }

    public GrantStringAttribute saveStringAttribute(GrantStringAttribute grantStringAttribute) {
        return grantStringAttributeRepository.save(grantStringAttribute);
    }

    public GrantSpecificSectionAttribute saveSectionAttribute(
            GrantSpecificSectionAttribute sectionAttribute) {
        return grantSpecificSectionAttributeRepository.save(sectionAttribute);
    }

    public GrantSpecificSection saveSection(GrantSpecificSection newSection) {
        return grantSpecificSectionRepository.save(newSection);
    }

    public GrantQuantitativeKpiData getGrantQuantitativeKpiDataById(Long quntKpiDataId) {
        if (grantQuantitativeDataRepository.findById(quntKpiDataId).isPresent()) {
            return grantQuantitativeDataRepository.findById(quntKpiDataId).get();
        }
        return null;
    }

    public GrantQualitativeKpiData getGrantQualitativeKpiDataById(Long qualKpiDataId) {
        if (grantQualitativeDataRepository.findById(qualKpiDataId).isPresent()) {
            return grantQualitativeDataRepository.findById(qualKpiDataId).get();
        }
        return null;
    }

    public GrantQuantitativeKpiData saveGrantQunatitativeKpiData(GrantQuantitativeKpiData kpiData) {
        return grantQuantitativeDataRepository.save(kpiData);
    }

    public GrantQualitativeKpiData saveGrantQualitativeKpiData(GrantQualitativeKpiData kpiData) {
        return grantQualitativeDataRepository.save(kpiData);
    }

    public GrantKpi saveGrantKpi(GrantKpi grantKpi) {

        return grantKpiRepository.save(grantKpi);
    }

    public GrantKpi getGrantKpiById(Long id) {
        if (grantKpiRepository.findById(id).isPresent()) {
            return grantKpiRepository.findById(id).get();
        }
        return null;
    }

    public GrantKpi getGrantKpiByNameAndTypeAndGrant(String title, KpiType kpiType, Grant grant) {
        return grantKpiRepository.findByTitleAndKpiTypeAndGrant(title, kpiType, grant);
    }

    public GrantDocumentKpiData getGrantDocumentKpiDataById(Long id) {
        if (grantDocumentDataRepository.findById(id).isPresent()) {
            return grantDocumentDataRepository.findById(id).get();
        }
        return null;
    }

    public GrantDocumentKpiData saveGrantDocumentKpiData(GrantDocumentKpiData kpiData) {
        return grantDocumentDataRepository.save(kpiData);
    }

    public List<Template> getKpiTemplates(GrantKpi kpiId) {

        return templateRepository.findByKpi(kpiId);
    }

    public Template getKpiTemplateById(Long templateId) {
        if (templateRepository.findById(templateId).isPresent()) {
            return templateRepository.findById(templateId).get();
        }
        return null;
    }

    public GrantDocumentAttributes saveGrantDocumentAttribute(GrantDocumentAttributes grantDocumentAttributes) {
        return grantDocumentAttributesRepository.save(grantDocumentAttributes);
    }

    public DocumentKpiNotes getDocKpiNoteById(Long id) {
        return documentKpiNotesRepository.findById(id).get();
    }

    public DocumentKpiNotes saveDocumentKpiNote(DocumentKpiNotes documentKpiNote) {
        return documentKpiNotesRepository.save(documentKpiNote);
    }

    public QualitativeKpiNotes getQualKpiNoteById(Long id) {
        return qualitativeKpiNotesRepository.findById(id).get();
    }

    public QualitativeKpiNotes saveQualKpiNote(QualitativeKpiNotes qualKpiNote) {
        return qualitativeKpiNotesRepository.save(qualKpiNote);
    }

    public QuantitativeKpiNotes getQuantKpiNoteById(Long id) {
        return quantitativeKpiNotesRepository.findById(id).get();
    }

    public QuantitativeKpiNotes saveQuantKpiNote(QuantitativeKpiNotes quantKpiNote) {
        return quantitativeKpiNotesRepository.save(quantKpiNote);
    }

    public DocKpiDataDocument getDockpiDocById(Long id) {
        return docKpiDataDocumentRepository.findById(id).get();
    }

    public DocKpiDataDocument saveDocKpiDataDoc(DocKpiDataDocument dataDocument) {
        return docKpiDataDocumentRepository.save(dataDocument);
    }

    public QualKpiDataDocument getQualkpiDocById(Long id) {
        return qualKpiDocumentRepository.findById(id).get();
    }

    public QualKpiDataDocument saveQualKpiDataDoc(QualKpiDataDocument dataDocument) {
        return qualKpiDocumentRepository.save(dataDocument);
    }

    public QuantKpiDataDocument getQuantkpiDocById(Long id) {
        return quantKpiDocumentRepository.findById(id).get();
    }

    public QuantKpiDataDocument saveQuantKpiDataDoc(QuantKpiDataDocument dataDocument) {
        return quantKpiDocumentRepository.save(dataDocument);
    }

    public GrantStringAttribute findGrantStringBySectionAttribueAndGrant(GrantSpecificSection granterGrantSection, GrantSpecificSectionAttribute granterGrantSectionAttribute, Grant grant) {
        return grantStringAttributeRepository.findBySectionAndSectionAttributeAndGrant(granterGrantSection, granterGrantSectionAttribute, grant);
    }

    public GrantStringAttribute findGrantStringBySectionIdAttribueIdAndGrantId(Long granterGrantSectionId, Long granterGrantSectionAttributeId, Long grantId) {
        return grantStringAttributeRepository.findBySectionAndSectionIdAttributeIdAndGrantId(granterGrantSectionId, granterGrantSectionAttributeId, grantId);
    }

    public GrantStringAttribute findGrantStringAttributeById(Long grantStringAttributeId) {
        return grantStringAttributeRepository.findById(grantStringAttributeId).get();
    }

    public GrantDocumentAttributes findGrantDocumentBySectionAttribueAndGrant(GrantSpecificSection granterGrantSection, GrantSpecificSectionAttribute granterGrantSectionAttribute, Grant grant) {
        return grantDocumentAttributesRepository.findBySectionAndSectionAttributeAndGrant(granterGrantSection, granterGrantSectionAttribute, grant);
    }

    public GrantStringAttribute saveGrantStringAttribute(GrantStringAttribute stringAttribute) {
        return grantStringAttributeRepository.save(stringAttribute);
    }

    public Template saveKpiTemplate(Template storedTemplate) {
        return templateRepository.save(storedTemplate);
    }

    public GrantSpecificSectionAttribute findBySectionAndFieldName(GrantSpecificSection section, String fieldName) {
        return grantSpecificSectionAttributeRepository.findBySectionAndFieldName(section, fieldName);
    }

    public GrantSpecificSection findByGranterAndSectionName(Granter granter, String sectionName) {
        return grantSpecificSectionRepository.findByGranterAndSectionName(granter, sectionName);
    }

    public Grant findGrantByNameAndGranter(String name, Granter granter) {
        return grantRepository.findByNameAndGrantorOrganization(name, granter);
    }

    public String buildNotificationContent(Grant grant, WorkflowStatus status, String configValue) {
        return configValue.replace("%GRANT_NAME%",
                grant.getName())
                .replace("%GRANT_STATUS%", status.getVerb());
    }

    public List<GrantSpecificSection> getGrantSections(Grant grant) {
        return grantSpecificSectionRepository.findByGranterAndGrantId((Granter) grant.getGrantorOrganization(), grant.getId());
    }

    public List<GrantSpecificSectionAttribute> getAttributesBySection(GrantSpecificSection section) {
        return grantSpecificSectionAttributeRepository.findBySection(section);
    }


    public void deleteSections(List<GrantSpecificSection> sections) {
        grantSpecificSectionRepository.deleteAll(sections);
    }

    public void deleteSection(GrantSpecificSection section) {
        grantSpecificSectionRepository.delete(section);
    }

    public void deleteAtttribute(GrantSpecificSectionAttribute attrib) {
        grantSpecificSectionAttributeRepository.delete(attrib);
    }

    public void deleteSectionAttributes(List<GrantSpecificSectionAttribute> attributes) {
        grantSpecificSectionAttributeRepository.deleteAll(attributes);
    }

    public void deleteStringAttributes(List<GrantStringAttribute> stringAttributes) {
        grantStringAttributeRepository.deleteAll(stringAttributes);
    }

    public void deleteStringAttribute(GrantStringAttribute stringAttribute) {
        grantStringAttributeRepository.delete(stringAttribute);
    }

    public GranterGrantTemplate saveGrantTemplate(GranterGrantTemplate newTemplate) {
        return granterGrantTemplateRepository.save(newTemplate);
    }

    public GranterGrantSection saveGrantTemaplteSection(GranterGrantSection section) {
        return granterGrantSectionRepository.save(section);
    }

    public GranterGrantSectionAttribute saveGrantTemaplteSectionAttribute(GranterGrantSectionAttribute attribute) {
        return granterGrantSectionAttributeRepository.save(attribute);
    }


    public void deleteGrantTemplateSections(List<GranterGrantSection> sections) {
        granterGrantSectionRepository.deleteAll(sections);
    }

    public void deleteGrantTemplate(GranterGrantTemplate template) {
        granterGrantTemplateRepository.delete(template);
    }

    public int getNextAttributeOrder(Long granterId, Long sectionId) {
        return grantSpecificSectionAttributeRepository.getNextAttributeOrder(granterId, sectionId);
    }

    public int getNextSectionOrder(Long granterId, Long templateId) {
        return grantSpecificSectionRepository.getNextSectionOrder(granterId, templateId);
    }

    public void deleteGrant(Grant grant) {
        grantRepository.delete(grant);
    }

    public GrantAssignments saveAssignmentForGrant(GrantAssignments assignment) {
        return grantAssignmentRepository.save(assignment);
    }

    public List<GrantAssignments> getGrantCurrentAssignments(Grant grant) {
        return grantAssignmentRepository.findByGrantIdAndStateId(grant.getId(), grant.getGrantStatus().getId());
    }

    public List<GrantAssignments> getGrantWorkflowAssignments(Grant grant) {
        return grantAssignmentRepository.findByGrantId(grant.getId());
    }

    public GrantAssignments getGrantAssignmentById(Long assignmentId) {
        if (grantAssignmentRepository.findById(assignmentId).isPresent()) {
            return grantAssignmentRepository.findById(assignmentId).get();
        }
        return null;
    }

    public GrantStringAttributeAttachments saveGrantStringAttributeAttachment(GrantStringAttributeAttachments attachment) {
        return grantStringAttributeAttachmentRepository.save(attachment);
    }

    public List<GrantStringAttributeAttachments> getStringAttributeAttachmentsByStringAttribute(GrantStringAttribute grantStringAttribute) {
        return grantStringAttributeAttachmentRepository.findByGrantStringAttribute(grantStringAttribute);
    }

    public GrantStringAttributeAttachments getStringAttributeAttachmentsByAttachmentId(Long attachmentId) {
        return grantStringAttributeAttachmentRepository.findById(attachmentId).get();
    }

    public void deleteStringAttributeAttachmentsByAttachmentId(Long attachmentId) {
        grantStringAttributeAttachmentRepository.deleteById(attachmentId);
    }

    public void deleteStringAttributeAttachments(List<GrantStringAttributeAttachments> attachments) {
        grantStringAttributeAttachmentRepository.deleteAll(attachments);
    }

    public List<GrantHistory> getGrantHistory(Long grantId) {
        return grantHistoryRepository.findByGrantId(grantId);
    }

    public String buildHashCode(Grant grant) {
        SecureEntity secureEntity = new SecureEntity();
        secureEntity.setGrantId(grant.getId());
        secureEntity.setTemplateId(grant.getTemplateId());
        secureEntity.setSectionAndAtrribIds(new HashMap<>());
        secureEntity.setGranterId(grant.getGrantorOrganization().getId());
        Map<Long, List<Long>> map = new HashMap<>();
        grant.getGrantDetails().getSections().forEach(sec -> {
            List<Long> attribIds = new ArrayList<>();
            if(sec.getAttributes()!=null) {
                sec.getAttributes().forEach(a -> {
                    attribIds.add(a.getId());
                });
            }

            map.put(sec.getId(), attribIds);
        });
        secureEntity.setSectionAndAtrribIds(map);
        List<Long> templateIds = new ArrayList<>();
        granterGrantTemplateRepository.findByGranterId(grant.getGrantorOrganization().getId()).forEach(t ->{
            templateIds.add(t.getId());
        });
        secureEntity.setGrantTemplateIds(templateIds);

        List<Long> grantWorkflowIds = new ArrayList<>();
        Map<Long,List<Long>> grantWorkflowStatusIds = new HashMap<>();
        Map<Long,Long[][]> grantWorkflowTransitionIds = new HashMap<>();
        workflowRepository.findByGranterAndObject(grant.getGrantorOrganization(), WorkflowObject.GRANT).forEach(w -> {
            grantWorkflowIds.add(w.getId());
            List<Long> wfStatusIds = new ArrayList<>();
            workflowStatusRepository.findByWorkflow(w).forEach(ws -> {
                wfStatusIds.add(ws.getId());
            });
            grantWorkflowStatusIds.put(w.getId(),wfStatusIds);

            List<WorkflowStatusTransition> transitions = workflowStatusTransitionRepository.findByWorkflow(w);
            Long[][] stransitions = new Long[transitions.size()][2];
            final int[] counter = {0};
            workflowStatusTransitionRepository.findByWorkflow(w).forEach(st -> {
                stransitions[counter[0]][0]=st.getFromState().getId();
                stransitions[counter[0]][1]=st.getToState().getId();
                counter[0]++;
            });
            grantWorkflowTransitionIds.put(w.getId(),stransitions);
        });


        secureEntity.setGrantWorkflowIds(grantWorkflowIds);
        secureEntity.setWorkflowStatusIds(grantWorkflowStatusIds);
        secureEntity.setWorkflowStatusTransitionIds(grantWorkflowTransitionIds);
        secureEntity.setTenantCode(grant.getGrantorOrganization().getCode());

        List<Long> tLibraryIds = new ArrayList<>();
        templateLibraryRepository.findByGranterId(grant.getGrantorOrganization().getId()).forEach(tl -> {
            tLibraryIds.add(tl.getId());
        });
        secureEntity.setTemplateLibraryIds(tLibraryIds);

        try {
            String secureCode = Jwts.builder().setSubject(new ObjectMapper().writeValueAsString(secureEntity))
                    .signWith(SignatureAlgorithm.HS512, SECRET).compact();
            return secureCode;
        }catch (JsonProcessingException e){
            e.printStackTrace();
        }
        return "";
    }

    public SecureEntity unBuildGrantHashCode(Grant grant){
        String grantSecureCode = Jwts.parser().setSigningKey(SECRET)
                .parseClaimsJws(grant.getSecurityCode()).getBody().getSubject();
        SecureEntity secureHash = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            secureHash = mapper.readValue(grantSecureCode, SecureEntity.class);
        } catch (JsonParseException e) {

        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            return secureHash;
        }
    }
}
