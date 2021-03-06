package org.codealpha.gmsservice.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.codealpha.gmsservice.constants.AppConfiguration;
import org.codealpha.gmsservice.constants.KpiType;
import org.codealpha.gmsservice.entities.*;
import org.codealpha.gmsservice.models.*;
import org.codealpha.gmsservice.repositories.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GrantService {

    private static final String SECRET = "bhjgsdf788778hsdfhgsdf777werghsbdfjhdsf88yw3r7t7yt^%^%%@#Ghj";
    @Autowired
    private GrantRepository grantRepository;
    @Autowired
    private GrantCardRepository grantCardRepository;
    @Autowired
    private GranterGrantSectionRepository granterGrantSectionRepository;
    @Autowired
    private GranterReportSectionRepository granterReportSectionRepository;
    @Autowired
    private GranterGrantSectionAttributeRepository granterGrantSectionAttributeRepository;
    @Autowired
    private GranterReportSectionAttributeRepository granterReportSectionAttributeRepository;
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
    @Autowired
    private UserService userService;
    @Autowired
    private WorkflowPermissionService workflowPermissionService;
    @Autowired
    private GranterGrantTemplateService granterGrantTemplateService;
    @Autowired
    private WorkflowStatusService workflowStatusService;
    @Autowired
    private AppConfigService appConfigService;
    @Autowired
    private GrantAssignmentHistoryRepository assignmentHistoryRepository;
    @Autowired
    private GrantDocumentRepository grantDocumentRepository;
    @Autowired
    private DisbursementRepository disbursementRepository;
    @Autowired
    private ActualDisbursementRepository actualDisbursementRepository;
    @Autowired
    private ReportService reportService;
    @Autowired
    private DisbursementService disbursementService;
    @Autowired
    private DisabledUsersEntityRepository disabledUsersEntityRepository;
    @Autowired
    private GrantTypeRepository grantTypeRepository;
    @Autowired
    private  GrantTagRepository grantTagRepository;
    @Autowired
    private OrgTagService orgTagService;
    @Autowired
    private OrgTagRepository orgTagRepository;
    @Autowired
    private DataExportSummaryRepository dataExportSummaryRepository;
    @Value("${spring.timezone}")
    private String timezone;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ReportCardRepository reportCardRepository;

    public GrantService() {
    }

    public List<String> getGrantAlerts(Grant grant) {
        return null;
    }

    public Grant saveGrant(Grant grant) {
        return grantRepository.save(grant);
    }

    public Grant getById(Long id) {
        return grantRepository.getById(id);
    }

    public GrantCard getGrantCardById(Long id) {
        return grantCardRepository.getById(id);
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

    public GrantSpecificSectionAttribute getSectionAttributeByAttributeIdAndType(Long attributeId, String type) {
        if (type.equalsIgnoreCase("text")) {
            Optional<GrantStringAttribute> grantStringAttribute = grantStringAttributeRepository.findById(attributeId);
            if (grantStringAttribute.isPresent()) {
                return grantStringAttribute.get().getSectionAttribute();
            }
        } else if (type.equalsIgnoreCase("multiline")) {
           GrantStringAttribute grantStringAttribute = grantStringAttributeRepository.getGrantStringAttributeById(attributeId);
                return grantStringAttribute.getSectionAttribute();

        } else if (type.equalsIgnoreCase("document")) {
            GrantStringAttribute grantStringAttribute = grantStringAttributeRepository.getGrantStringAttributeById(attributeId);
                return grantStringAttribute.getSectionAttribute();

        } else if (type.equalsIgnoreCase("kpi")) {
            GrantStringAttribute grantStringAttribute = grantStringAttributeRepository.getGrantStringAttributeById(attributeId);
                return grantStringAttribute.getSectionAttribute();

        } else if (type.equalsIgnoreCase("table") || type.equalsIgnoreCase("disbursement")) {
            GrantStringAttribute grantStringAttribute = grantStringAttributeRepository.getGrantStringAttributeById(attributeId);
                return grantStringAttribute.getSectionAttribute();
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

    public GrantSpecificSectionAttribute saveSectionAttribute(GrantSpecificSectionAttribute sectionAttribute) {
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

    public DocKpiDataDocument saveDocKpiDataDoc(DocKpiDataDocument dataDocument) {
        return docKpiDataDocumentRepository.save(dataDocument);
    }

    public QualKpiDataDocument getQualkpiDocById(Long id) {
        return qualKpiDocumentRepository.findById(id).get();
    }

    public QuantKpiDataDocument saveQuantKpiDataDoc(QuantKpiDataDocument dataDocument) {
        return quantKpiDocumentRepository.save(dataDocument);
    }

    public GrantStringAttribute findGrantStringBySectionAttribueAndGrant(GrantSpecificSection granterGrantSection,
            GrantSpecificSectionAttribute granterGrantSectionAttribute, Grant grant) {
        return grantStringAttributeRepository.findBySectionAndSectionAttributeAndGrant(granterGrantSection,
                granterGrantSectionAttribute, grant);
    }

    public GrantStringAttribute findGrantStringBySectionIdAttribueIdAndGrantId(Long granterGrantSectionId,
            Long granterGrantSectionAttributeId, Long grantId) {
        return grantStringAttributeRepository.findBySectionAndSectionIdAttributeIdAndGrantId(granterGrantSectionId,
                granterGrantSectionAttributeId, grantId);
    }

    public GrantStringAttribute findGrantStringAttributeById(Long grantStringAttributeId) {
        return grantStringAttributeRepository.findById(grantStringAttributeId).get();
    }

    public GrantDocumentAttributes findGrantDocumentBySectionAttribueAndGrant(GrantSpecificSection granterGrantSection,
            GrantSpecificSectionAttribute granterGrantSectionAttribute, Grant grant) {
        return grantDocumentAttributesRepository.findBySectionAndSectionAttributeAndGrant(granterGrantSection,
                granterGrantSectionAttribute, grant);
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
        return grantRepository.findByNameAndGrantorOrganization(name, granter.getId());
    }

    public String buildNotificationContent(Grant grant, WorkflowStatus status, String configValue) {
        return configValue.replace("%GRANT_NAME%", grant.getName()).replace("%GRANT_STATUS%", status.getVerb());
    }

    public List<GrantSpecificSection> getGrantSections(Grant grant) {
        return grantSpecificSectionRepository.findByGranterAndGrantId((Granter) grant.getGrantorOrganization(),
                grant.getId());
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

    public GranterReportSection saveReportTemplateSection(GranterReportSection section) {
        return granterReportSectionRepository.save(section);
    }

    public GranterGrantSectionAttribute saveGrantTemaplteSectionAttribute(GranterGrantSectionAttribute attribute) {
        return granterGrantSectionAttributeRepository.save(attribute);
    }

    public GranterReportSectionAttribute saveReportTemplateSectionAttribute(GranterReportSectionAttribute attribute) {
        return granterReportSectionAttributeRepository.save(attribute);
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

    public GrantAssignments getGrantAssignmentForGrantStateAndUser(Grant grant, WorkflowStatus status, User user) {
        return grantAssignmentRepository.findByGrantIdAndStateIdAndAssignments(grant.getId(), status.getId(),
                user.getId());
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

    public GrantStringAttributeAttachments saveGrantStringAttributeAttachment(
            GrantStringAttributeAttachments attachment) {
        return grantStringAttributeAttachmentRepository.save(attachment);
    }

    public List<GrantStringAttributeAttachments> getStringAttributeAttachmentsByStringAttribute(
            GrantStringAttribute grantStringAttribute) {
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

    public String[] buildEmailNotificationContent(Grant finalGrant, User user, String userName, String action,
            String date, String subConfigValue, String msgConfigValue, String currentState, String currentOwner,
            String previousState, String previousOwner, String previousAction, String hasChanges,
            String hasChangesComment, String hasNotes, String hasNotesComment, String link, User owner,
            Integer noOfDays, Map<Long, Long> previousApprover, List<GrantAssignments> newApprover) {

        String code = Base64.getEncoder().encodeToString(String.valueOf(finalGrant.getId()).getBytes());

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
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance().scheme(uriComponents.getScheme())
                    .host(host).port(uriComponents.getPort());
            url = uriBuilder.toUriString();
            url = url + "/home/?action=login&g=" + code + "&email=&type=grant";
        } catch (Exception e) {
            url = link;

            url = url + "/home/?action=login&g=" + code + "&email=&type=grant";
        }

        String grantName = "";
        if((finalGrant.getGrantStatus().getInternalStatus().equalsIgnoreCase("DRAFT") || finalGrant.getGrantStatus().getInternalStatus().equalsIgnoreCase("REVIEW")) && finalGrant.getOrigGrantId()!=null){
            grantName = "Amendment in-progress ["+getById(finalGrant.getOrigGrantId()).getReferenceNo()+"] "+ finalGrant.getName();
        }else{
            grantName = finalGrant.getReferenceNo()!=null?"[".concat(finalGrant.getReferenceNo()).concat("] ").concat(finalGrant.getName()):finalGrant.getName();
        }
        String message = msgConfigValue.replaceAll("%GRANT_NAME%", grantName)
                .replaceAll("%CURRENT_STATE%", currentState).replaceAll("%CURRENT_OWNER%", currentOwner)
                .replaceAll("%PREVIOUS_STATE%", previousState).replaceAll("%PREVIOUS_OWNER%", previousOwner)
                .replaceAll("%PREVIOUS_ACTION%", previousAction).replaceAll("%HAS_CHANGES%", hasChanges)
                .replaceAll("%HAS_CHANGES_COMMENT%", hasChangesComment).replaceAll("%HAS_NOTES%", hasNotes)
                .replaceAll("%HAS_NOTES_COMMENT%", hasNotesComment)
                .replaceAll("%TENANT%", finalGrant.getGrantorOrganization().getName()).replaceAll("%GRANT_LINK%", url)
                .replaceAll("%OWNER_NAME%", owner == null ? "" : owner.getFirstName() + " " + owner.getLastName())
                .replaceAll("%OWNER_EMAIL%", owner == null ? "" : owner.getEmailId())
                .replaceAll("%NO_DAYS%", noOfDays == null ? "" : String.valueOf(noOfDays))
                .replaceAll("%GRANTEE%",
                        finalGrant.getOrganization() != null ? finalGrant.getOrganization().getName() : "")
                .replaceAll("%APPROVER_TYPE%", "Approver").replaceAll("%ENTITY_TYPE%", "grant")
                .replaceAll("%PREVIOUS_ASSIGNMENTS%", getAssignmentsTable(previousApprover, newApprover))
                .replaceAll("%ENTITY_NAME%", finalGrant.getName());
        String subject = subConfigValue.replaceAll("%GRANT_NAME%", grantName);

        return new String[] { subject, message };
    }

    private String getAssignmentsTable(Map<Long, Long> assignments, List<GrantAssignments> newAssignments) {
        if (assignments == null) {
            return "";
        }

        newAssignments.sort(Comparator.comparing(GrantAssignments::getId, (a, b) -> {
            return a.compareTo(b);
        }));

        String[] table = {
                "<table width='100%' border='1' cellpadding='2' cellspacing='0'><tr><td><b>Review State</b></td><td><b>Current State Owners</b></td><td><b>Previous State Owners</b></td></tr>" };
        newAssignments.forEach(a -> {
            Long prevAss = assignments.keySet().stream().filter(b -> b == a.getStateId()).findFirst().get();

            table[0] = table[0].concat("<tr>").concat("<td width='30%'>")
                    .concat(workflowStatusRepository.findById(a.getStateId()).get().getName()).concat("</td>")
                    .concat("<td>")
                    .concat(userService.getUserById(a.getAssignments()).getFirstName().concat(" ")
                            .concat(userService.getUserById(a.getAssignments()).getLastName()))
                    .concat("</td>")

                    .concat("<td>")
                    .concat(userService.getUserById(assignments.get(prevAss)).getFirstName().concat(" ")
                            .concat(userService.getUserById(assignments.get(prevAss)).getLastName()).concat("</td>")
                            .concat("</tr>"));
        });

        table[0] = table[0].concat("</table>");
        return table[0];

    }

    public String[] buildGrantInvitationContent(Grant grant, User user, String sub, String msg, String url) {
        sub = sub.replace("%GRANT_NAME%", grant.getName());
        msg = msg.replace("%GRANT_NAME%", grant.getName())
                .replace("%TENANT_NAME%", grant.getGrantorOrganization().getName()).replace("%LINK%", url);
        return new String[] { sub, msg };
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
            if (sec.getAttributes() != null) {
                sec.getAttributes().forEach(a -> {
                    attribIds.add(a.getId());
                });
            }

            map.put(sec.getId(), attribIds);
        });
        secureEntity.setSectionAndAtrribIds(map);
        List<Long> templateIds = new ArrayList<>();
        granterGrantTemplateRepository.findByGranterId(grant.getGrantorOrganization().getId()).forEach(t -> {
            templateIds.add(t.getId());
        });
        secureEntity.setGrantTemplateIds(templateIds);

        List<Long> grantWorkflowIds = new ArrayList<>();
        Map<Long, List<Long>> grantWorkflowStatusIds = new HashMap<>();
        Map<Long, Long[][]> grantWorkflowTransitionIds = new HashMap<>();
        workflowRepository.findByGranterAndObjectAndType(grant.getGrantorOrganization().getId(), "GRANT",grant.getGrantTypeId()).forEach(w -> {
            grantWorkflowIds.add(w.getId());
            List<Long> wfStatusIds = new ArrayList<>();
            workflowStatusRepository.findByWorkflow(w).forEach(ws -> {
                wfStatusIds.add(ws.getId());
            });
            grantWorkflowStatusIds.put(w.getId(), wfStatusIds);

            List<WorkflowStatusTransition> transitions = workflowStatusTransitionRepository.findByWorkflow(w);
            Long[][] stransitions = new Long[transitions.size()][2];
            final int[] counter = { 0 };
            workflowStatusTransitionRepository.findByWorkflow(w).forEach(st -> {
                stransitions[counter[0]][0] = st.getFromState().getId();
                stransitions[counter[0]][1] = st.getToState().getId();
                counter[0]++;
            });
            grantWorkflowTransitionIds.put(w.getId(), stransitions);
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
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public SecureEntity unBuildGrantHashCode(Grant grant) {
        String grantSecureCode = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(grant.getSecurityCode()).getBody()
                .getSubject();
        SecureEntity secureHash = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            secureHash = mapper.readValue(grantSecureCode, SecureEntity.class);
        } catch (JsonParseException e) {

        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return secureHash;
        }
    }

    public List<Grant> getActiveGrantsForTenant(Organization organization) {
        return grantRepository.findActiveGrants(organization.getId());
    }

    public List<GrantAssignments> getActionDueGrantsForPlatform(List<Long> granterIds) {
        return grantAssignmentRepository.getActionDueGrantsForPlatform(granterIds);
    }

    public List<GrantAssignments> getActionDueGrantsForGranterOrg(Long granterId) {
        return grantAssignmentRepository.getActionDueGrantsForGranterOrg(granterId);
    }

    public Long getCountOfOtherGrantsWithStartDateAndStatus(Date startDate, Long granterId, Long statusId) {
        return grantRepository.getCountOfOtherGrantsWithStartDateAndStatus(startDate, granterId, statusId);
    }

    public List<Grant> getGrantsOwnedByUserByStatus(Long userId, String status) {
        return grantRepository.findGrantsOwnedByUserByStatus(userId, status);
    }

    public Grant _grantToReturn(@PathVariable("userId") Long userId, Grant grant) {
        User user = userService.getUserById(userId);

        grant.setActionAuthorities(
                workflowPermissionService.getGrantActionPermissions(grant.getGrantorOrganization().getId(),
                        user.getUserRoles(), grant.getGrantStatus().getId(), userId, grant.getId()));

        grant.setFlowAuthorities(workflowPermissionService.getGrantFlowPermissions(grant.getGrantStatus().getId(),
                userId, grant.getId()));

        grant.setGrantTemplate(granterGrantTemplateService.findByTemplateId(grant.getTemplateId()));

        GrantVO grantVO = new GrantVO();

        grantVO = grantVO.build(grant, getGrantSections(grant), workflowPermissionService, user,
                appConfigService.getAppConfigForGranterOrg(grant.getGrantorOrganization().getId(),
                        AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS),
                userService,this);
        grant.setGrantDetails(grantVO.getGrantDetails());
        grant.setNoteAddedBy(grantVO.getNoteAddedBy());
        grant.setNoteAddedByUser(grantVO.getNoteAddedByUser());

        List<GrantAssignmentsVO> workflowAssignments = new ArrayList<>();
        grant.setWorkflowAssignment(getGrantWorkflowAssignments(grant));
        for (GrantAssignments assignment : grant.getWorkflowAssignment()) {
            GrantAssignmentsVO assignmentsVO = new GrantAssignmentsVO();
            assignmentsVO.setId(assignment.getId());
            assignmentsVO.setAnchor(assignment.isAnchor());
            assignmentsVO.setAssignments(assignment.getAssignments());
            if (assignment.getAssignments() != null && assignment.getAssignments() > 0) {
                assignmentsVO.setAssignmentUser(userService.getUserById(assignment.getAssignments()));
            }
            assignmentsVO.setGrantId(assignment.getGrant().getId());
            assignmentsVO.setStateId(assignment.getStateId());
            assignmentsVO.setStateName(workflowStatusService.findById(assignment.getStateId()));

            setAssignmentHistory(grant, assignmentsVO);
            workflowAssignments.add(assignmentsVO);
        }
        grant.setWorkflowAssignments(workflowAssignments);
        List<GrantAssignments> grantAssignments = getGrantCurrentAssignments(grant);
        if (grantAssignments != null) {
            for (GrantAssignments assignment : grantAssignments) {
                /*if (grant.getCurrentAssignment() == null) {
                    Long assignedToList = new ArrayList<>();
                    grant.setCurrentAssignment(assignedToList);
                }*/
                /*AssignedTo newAssignedTo = new AssignedTo();
                if (assignment.getAssignments() != null && assignment.getAssignments() > 0) {
                    newAssignedTo.setUser(userService.getUserById(assignment.getAssignments()));
                }*/
                grant.setCurrentAssignment(assignment.getAssignments());
            }
        }
        grant = saveGrant(grant);

        grant.getWorkflowAssignment().sort((a, b) -> a.getId().compareTo(b.getId()));
        grant.getGrantDetails().getSections()
                .sort((a, b) -> Long.valueOf(a.getOrder()).compareTo(Long.valueOf(b.getOrder())));
        for (SectionVO section : grant.getGrantDetails().getSections()) {
            if (section.getAttributes() != null) {
                section.getAttributes().sort(
                        (a, b) -> Long.valueOf(a.getAttributeOrder()).compareTo(Long.valueOf(b.getAttributeOrder())));
            }
        }

        grant.setSecurityCode(buildHashCode(grant));
        List<WorkflowStatus> workflowStatuses = workflowStatusRepository.getAllTenantStatuses("DISBURSEMENT",
                grant.getGrantorOrganization().getId());

        List<WorkflowStatus> activeAndClosedStatuses = workflowStatuses.stream()
                .filter(ws -> ws.getInternalStatus().equalsIgnoreCase("ACTIVE")
                        || ws.getInternalStatus().equalsIgnoreCase("CLOSED"))
                .collect(Collectors.toList());
        List<Long> statusIds = activeAndClosedStatuses.stream().mapToLong(s -> s.getId()).boxed()
                .collect(Collectors.toList());
        List<Disbursement> approvedDisbursements = disbursementRepository
                .getDisbursementByGrantAndStatuses(grant.getId(), statusIds);
        List<ActualDisbursement> approvedActualDisbursements = new ArrayList<>();
        if (approvedDisbursements != null) {

            for (Disbursement approved : approvedDisbursements) {
                List<ActualDisbursement> approvedActuals = actualDisbursementRepository
                        .findByDisbursementId(approved.getId());
                approvedActualDisbursements.addAll(approvedActuals);
            }
        }

        if(grant.getOrigGrantId()!=null) {
            approvedActualDisbursements.addAll(getPastApprovedActualDisbursementsForGrant(getById(grant.getOrigGrantId()),true));
        }
        HashSet<Object> seen=new HashSet<>();
        approvedActualDisbursements.removeIf(e->!seen.add(e.getId()));

        grant.setProjectDocumentsCount(getGrantsDocuments(grant.getId()).size());
        approvedActualDisbursements.removeIf(ad -> ad.getActualAmount() == null);
        grant.setApprovedDisbursementsTotal(
                approvedActualDisbursements.stream().mapToDouble(ActualDisbursement::getActualAmount).sum());

        Optional<WorkflowStatus> reportApprovedStatus = workflowStatusService
                .getTenantWorkflowStatuses("REPORT", grant.getGrantorOrganization().getId()).stream()
                .filter(s -> s.getInternalStatus().equalsIgnoreCase("CLOSED")).findFirst();
        List<ReportCard> reports = new ArrayList<>();
        int noOfReports = 0;
        if (reportApprovedStatus.isPresent()) {
            reports = reportService.findReportCardsByStatusForGrant(reportApprovedStatus.get(), grant);
            noOfReports = reports.size();
            // Include approved reports of orgiginal grant if exist
            if (grant.getOrigGrantId() != null) {
                reports = reportService.findReportCardsByStatusForGrant(reportApprovedStatus.get(),
                        getById(grant.getOrigGrantId()));
                noOfReports += reports.size();
            }
            // End
            grant.setApprovedReportsForGrant(noOfReports);
        }

        // Set old grant ref no if current amendment grant is still in porogress
        if (grant.getOrigGrantId() != null && !grant.getGrantStatus().getInternalStatus().equalsIgnoreCase("ACTIVE")
                && !grant.getGrantStatus().getInternalStatus().equalsIgnoreCase("CLOSED")) {
            grant.setOrigGrantRefNo(getById(grant.getOrigGrantId()).getReferenceNo());
        }

        // Set Minimum End Date for Amendment grant
        if (grant.getOrigGrantId() != null) {
            List<Report> existingReports = reportService.getReportsForGrant(getById(grant.getOrigGrantId()));
                if (existingReports != null && existingReports.size() > 0) {
                //existingReports.removeIf(r -> r.getStatus().getInternalStatus().equalsIgnoreCase("DRAFT"));
                existingReports.removeIf(r -> r.getEndDate()==null);
                if (existingReports != null && existingReports.size() > 0) {

                    Comparator<Report> endDateComparator = Comparator.comparing(c -> c.getEndDate());
                    existingReports.sort(endDateComparator);
                    Report lastReport = existingReports.get(existingReports.size() - 1);
                    grant.setMinEndEndate(lastReport.getEndDate());
                }else{
                    grant.setMinEndEndate(grant.getStartDate());
                }
            }

            List<Disbursement> existingDisbursements = disbursementService
                    .getAllDisbursementsForGrant(grant.getOrigGrantId());
            if (existingDisbursements != null && existingDisbursements.size() > 0) {
                existingDisbursements.removeIf(d -> !d.getStatus().getInternalStatus().equalsIgnoreCase("DRAFT"));
                if (existingDisbursements != null && existingDisbursements.size() > 0) {

                    Comparator<Disbursement> endDateComparator = Comparator.comparing(d -> d.getMovedOn());
                    existingDisbursements.sort(endDateComparator);
                    Disbursement lastDisbursement = existingDisbursements.get(existingDisbursements.size() - 1);
                    if (grant.getMinEndEndate() != null && new DateTime(lastDisbursement.getMovedOn())
                            .isAfter(new DateTime(grant.getMinEndEndate()))) {
                        grant.setMinEndEndate(lastDisbursement.getMovedOn());
                    }else{
                        grant.setMinEndEndate(grant.getStartDate());
                    }

                }
            }
        }

        List<GrantTag> grantTags = getTagsForGrant(grant.getId());
        List<GrantTagVO> grantTagsVoList = new ArrayList<>();
        for(GrantTag tag: grantTags){
            GrantTagVO vo =new GrantTagVO();
            vo.setGrantId(grant.getId());
            vo.setId(tag.getId());
            vo.setOrgTagId(tag.getOrgTagId());
            vo.setTagName(orgTagService.getOrgTagById(tag.getOrgTagId()).getName());
            grantTagsVoList.add(vo);
        }
        grant.setTags(grantTagsVoList);
        return grant;
    }

    private List<ActualDisbursement> getPastApprovedActualDisbursementsForGrant(Grant grant,boolean includeCurrent) {
        List<Disbursement> disbs = disbursementService.getAllDisbursementsForGrant(grant.getId());
        List<ActualDisbursement> approvedActualDisbursements = new ArrayList<>();
        List<WorkflowStatus> workflowStatuses = workflowStatusRepository.getAllTenantStatuses("DISBURSEMENT",
                grant.getGrantorOrganization().getId());

        List<WorkflowStatus> activeAndClosedStatuses = workflowStatuses.stream()
                .filter(ws -> ws.getInternalStatus().equalsIgnoreCase("ACTIVE")
                        || ws.getInternalStatus().equalsIgnoreCase("CLOSED"))
                .collect(Collectors.toList());
        List<Long> statusIds = activeAndClosedStatuses.stream().mapToLong(s -> s.getId()).boxed()
                .collect(Collectors.toList());

        for(Disbursement disbursement:disbs){
            List<Disbursement> approvedDisbursements = disbursementService.getDibursementsForGrantByStatuses(disbursement.getGrant().getId(),
                    statusIds);

            if (approvedDisbursements != null) {
                if(!includeCurrent){
                    approvedDisbursements.removeIf(d -> d.getId().longValue() == disbursement.getId().longValue());
                }
                approvedDisbursements.removeIf(d -> new DateTime(d.getMovedOn(), DateTimeZone.forID(timezone))
                        .isAfter(new DateTime(disbursement.getMovedOn(), DateTimeZone.forID(timezone))));
                for (Disbursement approved : approvedDisbursements) {
                    List<ActualDisbursement> approvedActuals = disbursementService.getActualDisbursementsForDisbursement(approved);
                    approvedActualDisbursements.addAll(approvedActuals);
                }
            }
            //Get previous actual disbursements if grant is amended
            if(grant.getOrigGrantId()!=null){
                List<Disbursement> disburs = disbursementService.getAllDisbursementsForGrant(disbursement.getGrant().getOrigGrantId());
                for(Disbursement d : disburs){
                    approvedActualDisbursements.addAll(getPastApprovedActualDisbursementsForGrant(d.getGrant(),true));
                }
            }
        }

        approvedActualDisbursements.sort(Comparator.comparing(ActualDisbursement::getOrderPosition));
        return approvedActualDisbursements;
    }

    public void setAssignmentHistory(Grant grant, GrantAssignmentsVO assignmentsVO) {
        if (grantRepository.findGrantsThatMovedAtleastOnce(grant.getId()).size() > 0) {
            List<GrantAssignmentHistory> assignmentHistories = assignmentHistoryRepository
                    .findByGrantIdAndStateIdOrderByUpdatedOnDesc(grant.getId(), assignmentsVO.getStateId());
            for (GrantAssignmentHistory grantAss : assignmentHistories) {
                if (grantAss.getAssignments() != null && grantAss.getAssignments() != 0) {
                    grantAss.setAssignmentUser(userService.getUserById(grantAss.getAssignments()));
                }
                if (grantAss.getUpdatedBy() != null && grantAss.getUpdatedBy() != 0) {
                    grantAss.setUpdatedByUser(userService.getUserById(grantAss.getUpdatedBy()));
                }
            }
            assignmentsVO.setHistory(assignmentHistories);
        }
    }

    public boolean checkIfGrantMovedThroughWFAtleastOnce(Long grantId) {
        return grantRepository.findGrantsThatMovedAtleastOnce(grantId).size() > 0;
    }

    public List<GrantDocument> getGrantsDocuments(Long grantId) {
        return grantDocumentRepository.findByGrantId(grantId);
    }

    public GrantDocument saveGrantDocument(GrantDocument attachment) {
        return grantDocumentRepository.save(attachment);
    }

    public GrantDocument getGrantDocumentById(Long attachmentId) {
        return grantDocumentRepository.findById(attachmentId).get();
    }

    public void deleteGrantDocument(GrantDocument doc) {
        grantDocumentRepository.delete(doc);
    }

    public List<DisabledUsersEntity> getGrantsWithDisabledUsers(){
        return disabledUsersEntityRepository.getGrants();
    }

    public List<GrantType> getGrantTypesForTenantOrg(Long orgId){
        return grantTypeRepository.findGrantTypesForTenant(orgId);
    }

    public GrantType getGrantypeById(Long grantTypeId){
        return grantTypeRepository.findById(grantTypeId).get();
    }

    public List<Grant> getAllGrantsForGranter(Long granterId){
        return grantRepository.getAllGrantsForGranter(granterId);
    }

    public List<GrantTag> getTagsForGrant(Long grantId){
        return grantTagRepository.getTagsForGrant(grantId);
    }

    public GrantTag attachTagToGrant(GrantTag tag){
        return grantTagRepository.save(tag);
    }

    public void detachTagToGrant(GrantTag tag){
        grantTagRepository.delete(tag);
    }

    public GrantTag getGrantTagById(Long id){
        return grantTagRepository.getTagById(id);
    }

    public boolean isTagInUse(Long orgTagId){
        return grantTagRepository.isTagInUse(orgTagId);
    }

    public DataExportSummary saveExportSummary(DataExportSummary summary){
        return dataExportSummaryRepository.save(summary);
    }

    public List<Long> getAllGrantIdsForProject(Long grantId){
        List<Long> dGrantIds = getDownstreamGrantIds(grantId);
        List<Long> uGrantIds = getUpstreamGrantIds(grantId);

        Set<Long> finalGrantIds = new HashSet<>();
        finalGrantIds.addAll(dGrantIds);
        finalGrantIds.addAll(uGrantIds);
        finalGrantIds.removeIf(g -> g.longValue()==grantId.longValue());
        return finalGrantIds.stream().collect(Collectors.toList());
    }

    private List<Long> getDownstreamGrantIds(Long grantId) {
        List<Long> ids = new ArrayList<>();
        ids.add(grantId);
        Grant grant = getById(grantId);
        if(grant.getOrigGrantId()!=null){
            ids.addAll(getDownstreamGrantIds(grant.getOrigGrantId()));
        }
        return ids;
    }

    private List<Long> getUpstreamGrantIds(Long grantId) {
        List<Long> ids = new ArrayList<>();

        Grant grant = getByOrigGrantId(grantId);

        if(grant!=null){
            ids.add(grant.getId());
            ids.addAll(getUpstreamGrantIds(grant.getId()));
        }
        return ids;
    }

    public Grant getByOrigGrantId(Long grantId){
        return grantRepository.getByOrigGrantId(grantId);
    }


    public Long getActionDueGrantsForUser(Long userId) {
        return grantRepository.getActionDueGrantsForUser(userId);
    }

    public List<GrantCard> getDetailedActionDueGrantsForUser(Long userId) {
        return grantCardRepository.getDetailedActionDueGrantsForUser(userId);
    }

    public Long getActionDueReportsForUser(Long userId) {
        return reportRepository.getActionDueReportsForUser(userId);
    }

    public List<ReportCard> getDetailedActionDueReportsForUser(Long userId) {
        return reportCardRepository.getDetailedActionDueReportsForUser(userId);
    }

    public Long getUpComingDraftGrants(Long userId) {
        return grantRepository.getUpComingDraftGrants(userId);
    }

    public List<GrantCard> getDetailedUpComingDraftGrants(Long userId) {
        return grantCardRepository.getDetailedUpComingDraftGrants(userId);
    }

    public Long getGrantsInWorkflow(Long userId) {
        return grantRepository.getGrantsInWorkflow(userId);
    }

    public Long getUpcomingGrantsDisbursementAmount(Long userId) {
        return grantRepository.getUpcomingGrantsDisbursementAmount(userId);
    }

    public Long getGrantsTotalForUserByStatus(Long userId,String status) {
        return grantRepository.getGrantsTotalForUserByStatus(userId,status);
    }

    public Long getCommittedAmountByUserAndStatus(Long userId, String status) {
        return grantRepository.getCommittedAmountByUserAndStatus(userId,status);
    }

    public Long getDisbursedAmountByUserAndStatus(Long userId, String status) {
        return grantRepository.getDisbursedAmountByUserAndStatus(userId,status);
    }

    public Long getGranteeOrgsCountByUserAndStatus(Long userId, String status) {
        return grantRepository.getGranteeOrgsCountByUserAndStatus(userId,status);
    }

    public Long getGrantsWithNoApprovedReportsByUserAndStatus(Long userId, String status) {
        return grantRepository.getGrantsWithNoApprovedReportsByUserAndStatus(userId,status);
    }

    public Long getGrantsWithNoAKPIsByUserAndStatus(Long userId, String status) {
        return grantRepository.getGrantsWithNoAKPIsByUserAndStatus(userId,status);
    }

    public boolean isUserPartOfActiveWorkflow(Long userId){
        return grantRepository.isUserPartOfActiveWorkflow(userId);
    }

    public Long getUpcomingReportsDisbursementAmount(Long userId) {
        return reportRepository.getUpcomingReportsDisbursementAmount(userId);
    }

    public List<Grant> getgrantsByStatusForUser(Long userId, String status) {
        return grantRepository.getgrantsByStatusForUser(userId,status);
    }

    public PlainGrant grantToPlain(Grant grant) throws IOException {
        SimpleDateFormat sd = new SimpleDateFormat("dd-MMM-yyyy");
        PlainGrant plainGrant = new PlainGrant();
        plainGrant.setName(grant.getName());
        plainGrant.setAmount(grant.getAmount());
        Date end = grant.getEndDate();
        plainGrant.setEndDate(end!=null?sd.format(end):"");
        Date start = grant.getStartDate();
        plainGrant.setStartDate(start!=null?sd.format(start):"");
        plainGrant.setImplementingOrganizationName(grant.getOrganization()!=null?grant.getOrganization().getName():"");
        plainGrant.setImplementingOrganizationRepresentative(grant.getRepresentative()!=null?grant.getRepresentative():"");
        plainGrant.setReferenceNo(grant.getReferenceNo());
        plainGrant.setCurrentInternalStatus(grant.getGrantStatus().getInternalStatus());
        plainGrant.setCurrentStatus(grant.getGrantStatus().getName());
        Optional<GrantAssignments> assignment =  grant.getWorkflowAssignment().stream().filter(ass -> ass.getStateId().longValue()==grant.getGrantStatus().getId()).findFirst();
        if(assignment.isPresent()){
            User owner = userService.getUserById(assignment.get().getAssignments());
            plainGrant.setCurrentOwner(owner.getFirstName()+" "+owner.getLastName());
        }
        if(grant.getGrantDetails().getSections()!=null && grant.getGrantDetails().getSections().size()>0){
            List<PlainSection> plainSections = new ArrayList<>();
            grant.getGrantDetails().getSections().sort((a,b) ->Long.valueOf(a.getOrder()).compareTo(Long.valueOf(b.getOrder())));
            for(SectionVO section : grant.getGrantDetails().getSections()){
                List<PlainAttribute> plainAttributes = new ArrayList<>();
                if(section.getAttributes()!=null && section.getAttributes().size()>0) {
                    section.getAttributes().sort((a,b) -> Long.valueOf(a.getAttributeOrder()).compareTo(Long.valueOf(b.getAttributeOrder())));
                    ObjectMapper mapper = new ObjectMapper();
                    for (SectionAttributesVO attribute : section.getAttributes()) {
                        PlainAttribute plainAttribute = new PlainAttribute();
                        plainAttribute.setId(attribute.getId());
                        plainAttribute.setName(attribute.getFieldName());
                        plainAttribute.setType(attribute.getFieldType());
                        plainAttribute.setValue(attribute.getFieldValue());
                        plainAttribute.setOrder(attribute.getAttributeOrder());
                        switch (attribute.getFieldType()) {

                            case "kpi":
                                plainAttribute.setFrequency(attribute.getFrequency());
                                plainAttribute.setTarget(Long.valueOf(attribute.getTarget()));
                                break;
                            case "disbursement":
                            case "table":

                                plainAttribute.setTableValue(mapper.readValue(attribute.getFieldValue(), new TypeReference<List<TableData>>() {}));
                                break;
                            case "document":
                                if(attribute.getFieldValue()!=null && !attribute.getFieldValue().equalsIgnoreCase("")){
                                    plainAttribute.setAttachments(mapper.readValue(attribute.getFieldValue(), new TypeReference<List<GrantStringAttributeAttachments>>() {}));
                                }

                                break;
                        }

                        plainAttributes.add(plainAttribute);
                    }

                }
                plainSections.add(new PlainSection(section.getId(),section.getName(),section.getOrder(), plainAttributes));
            }
            plainGrant.setSections(plainSections);
        }

        return plainGrant;
    }
}
