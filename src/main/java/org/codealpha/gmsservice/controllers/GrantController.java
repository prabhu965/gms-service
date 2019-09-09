package org.codealpha.gmsservice.controllers;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.codealpha.gmsservice.constants.AppConfiguration;
import org.codealpha.gmsservice.constants.GrantStatus;
import org.codealpha.gmsservice.constants.KpiType;
import org.codealpha.gmsservice.entities.*;
import org.codealpha.gmsservice.models.*;
import org.codealpha.gmsservice.services.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/{userId}/grant")
public class GrantController {

    private static Logger logger = LoggerFactory.getLogger(GrantController.class);

    @Autowired
    private GrantQuantitativeDataService quantitativeDataService;
    @Autowired
    GrantQualitativeDataService qualitativeDataService;
    @Autowired
    private WorkflowPermissionService workflowPermissionService;
    @Autowired
    private AppConfigService appConfigService;
    @Autowired
    private UserService userService;
    @Autowired
    private WorkflowStatusService workflowStatusService;
    @Autowired
    private GrantService grantService;
    @Autowired
    private SubmissionService submissionService;
    @Autowired
    private GrantDocumentDataService grantDocumentDataService;
    @Autowired
    private QuantitativeKpiNotesService quantitativeKpiNotesService;
    @Autowired
    private QualitativeKpiNotesService qualitativeKpiNotesService;
    @Autowired
    private DocumentKpiNotesService documentKpiNotesService;
    @Autowired
    private DocKpiDataDocumentService docKpiDataDocumentService;
    @Autowired
    private QuantKpiDocumentService quantKpiDocumentService;
    @Autowired
    private SubmissionNoteService submissionNoteService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private GranteeService granteeService;
    @Autowired
    private NotificationsService notificationsService;
    @Autowired
    private GranterGrantTemplateService granterGrantTemplateService;

    @Value("${spring.upload-file-location}")
    private String uploadLocation;

    @Autowired
    private CommonEmailSevice commonEmailSevice;

    @GetMapping("/create/{templateId}")
    public Grant createGrant(@PathVariable("templateId") Long templateId, @PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode) {
        Grant grant = new Grant();

        grant.setName("");
        grant.setStartDate(null);
        grant.setStDate("");
        grant.setAmount(0D);
        grant.setDescription("");
        grant.setGrantStatus(workflowStatusService.findInitialStatusByObjectAndGranterOrgId("GRANT", organizationService.findOrganizationByTenantCode(tenantCode).getId()));
        grant.setStatusName(GrantStatus.DRAFT);
        grant.setEndDate(null);
        grant.setEnDate("");
        grant.setOrganization(null);
        grant.setCreatedAt(new Date());
        grant.setCreatedBy(userService.getUserById(userId).getEmailId());
        grant.setGrantorOrganization((Granter) organizationService.findOrganizationByTenantCode(tenantCode));
        grant.setRepresentative("");
        grant.setTemplateId(templateId);
        grant.setGrantTemplate(granterGrantTemplateService.findByTemplateId(templateId));

        grant = grantService.saveGrant(grant);

        GrantAssignments assignment = new GrantAssignments();
        assignment.setAnchor(true);
        assignment.setAssignments(userService.getUserById(userId).getId());
        assignment.setGrantId(grant.getId());
        assignment.setStateId(grant.getGrantStatus().getId());
        grantService.createAssignmentForGrant(assignment);

        GranterGrantTemplate grantTemplate = granterGrantTemplateService.findByTemplateId(templateId);

        List<GrantSpecificSection> grantSpecificSections = new ArrayList<>();
        List<GranterGrantSection> granterGrantSections = grantTemplate.getSections();
        grant.setStringAttributes(new ArrayList<>());
        for (GranterGrantSection grantSection : granterGrantSections) {
            GrantSpecificSection specificSection = new GrantSpecificSection();
            specificSection.setDeletable(true);
            specificSection.setGranter((Granter) organizationService.findOrganizationByTenantCode(tenantCode));
            specificSection.setGrantTemplateId(grantTemplate.getId());
            specificSection.setSectionName(grantSection.getSectionName());
            specificSection.setSectionOrder(grantSection.getSectionOrder());
            specificSection.setGrantId(grant.getId());

            specificSection = grantService.saveSection(specificSection);
            for (GranterGrantSectionAttribute sectionAttribute : grantSection.getAttributes()) {
                GrantSpecificSectionAttribute specificSectionAttribute = new GrantSpecificSectionAttribute();
                specificSectionAttribute.setDeletable(true);
                specificSectionAttribute.setFieldName(sectionAttribute.getFieldName());
                specificSectionAttribute.setFieldType(sectionAttribute.getFieldType());
                specificSectionAttribute.setGranter((Granter) organizationService.findOrganizationByTenantCode(tenantCode));
                specificSectionAttribute.setRequired(false);
                specificSectionAttribute.setAttributeOrder(sectionAttribute.getAttributeOrder());
                specificSectionAttribute.setSection(specificSection);
                specificSectionAttribute = grantService.saveSectionAttribute(specificSectionAttribute);


                GrantStringAttribute stringAttribute = new GrantStringAttribute();

                stringAttribute.setSection(specificSection);
                stringAttribute.setGrant(grant);
                stringAttribute.setSectionAttribute(specificSectionAttribute);
                stringAttribute.setValue("");
                stringAttribute.setFrequency("");
                stringAttribute.setTarget("");

                stringAttribute = grantService.saveStringAttribute(stringAttribute);
                grant.getStringAttributes().add(stringAttribute);
            }
            specificSection = grantService.saveSection(specificSection);
            grantSpecificSections.add(specificSection);
        }

        grant = _grantToReturn(userId, grant);
        return grant;
    }

    @DeleteMapping("/{grantId}")
    public void deleteGrant(@PathVariable("grantId") Long grantId, @PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode) {
        Grant grant = grantService.getById(grantId);
        for (GrantSpecificSection section : grantService.getGrantSections(grant)) {
            List<GrantSpecificSectionAttribute> attribs = grantService.getAttributesBySection(section);
            for (GrantSpecificSectionAttribute attribute : attribs) {
                List<GrantStringAttribute> strAttribs = grantService.getStringAttributesByAttribute(attribute);
                grantService.deleteStringAttributes(strAttribs);
            }
            grantService.deleteSectionAttributes(attribs);
            grantService.deleteSection(section);
        }
        grantService.deleteGrant(grant);

        GranterGrantTemplate template = granterGrantTemplateService.findByTemplateId(grant.getTemplateId());
        if (!template.isPublished()) {
            grantService.deleteGrantTemplate(template);
        }
    }

    private Grant _grantToReturn(@PathVariable("userId") Long userId, Grant grant) {
        User user = userService.getUserById(userId);

        grant.setActionAuthorities(workflowPermissionService
                .getGrantActionPermissions(grant.getGrantorOrganization().getId(),
                        user.getUserRoles(), grant.getGrantStatus().getId()));

        grant.setFlowAuthorities(workflowPermissionService
                .getGrantFlowPermissions(grant.getGrantorOrganization().getId(),
                        user.getUserRoles(), grant.getGrantStatus().getId()));

        grant.setGrantTemplate(granterGrantTemplateService.findByTemplateId(grant.getTemplateId()));

        GrantVO grantVO = new GrantVO();

        grantVO = grantVO.build(grant, grantService.getGrantSections(grant), workflowPermissionService, user, appConfigService
                .getAppConfigForGranterOrg(grant.getGrantorOrganization().getId(),
                        AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS));
        grant.setGrantDetails(grantVO.getGrantDetails());

        grant.getGrantDetails().getSections().sort((a, b) -> Long.valueOf(a.getOrder()).compareTo(Long.valueOf(b.getOrder())));
        for (SectionVO section : grant.getGrantDetails().getSections()) {
            if (section.getAttributes() != null) {
                section.getAttributes().sort((a, b) -> Long.valueOf(a.getAttributeOrder()).compareTo(Long.valueOf(b.getAttributeOrder())));
            }
        }
        grant = grantService.saveGrant(grant);
        return grant;
    }

    @PostMapping("/{id}/section/{sectionId}/field")
    public FieldInfo createFieldInSection(@RequestBody Grant grantToSave,@PathVariable("id") Long grantId, @PathVariable("sectionId") Long sectionId, @PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode) {
        //grantService.saveGrant(grantToSave);
        saveGrant(grantToSave,userId,tenantCode);
        Grant grant = grantService.getById(grantId);
        GrantSpecificSection grantSection = grantService.getGrantSectionBySectionId(sectionId);

        GrantSpecificSectionAttribute newSectionAttribute = new GrantSpecificSectionAttribute();
        newSectionAttribute.setSection(grantSection);
        newSectionAttribute.setRequired(false);
        newSectionAttribute.setFieldType("multiline");
        newSectionAttribute.setFieldName("");
        newSectionAttribute.setDeletable(true);
        newSectionAttribute.setAttributeOrder(grantService.getNextAttributeOrder(organizationService.findOrganizationByTenantCode(tenantCode).getId(), sectionId));
        newSectionAttribute.setGranter((Granter) organizationService.findOrganizationByTenantCode(tenantCode));
        newSectionAttribute = grantService.saveSectionAttribute(newSectionAttribute);
        GrantStringAttribute stringAttribute = new GrantStringAttribute();
        stringAttribute.setValue("");
        stringAttribute.setSectionAttribute(newSectionAttribute);
        stringAttribute.setSection(grantSection);
        stringAttribute.setGrant(grant);



        stringAttribute = grantService.saveStringAttribute(stringAttribute);
        grant.getStringAttributes().add(stringAttribute);
        grant = grantService.saveGrant(grant);
        if (_checkIfGrantTemplateChanged(grant, grantSection, newSectionAttribute)) {
            _createNewGrantTemplateFromExisiting(grant);
        }

        grant = _grantToReturn(userId, grant);
        return new FieldInfo(newSectionAttribute.getId(), grant);
    }

    @PostMapping("/{id}/section/{sectionId}/field/{fieldId}")
    public Grant deleteField(@RequestBody Grant grantToSave,@PathVariable("userId") Long userId, @PathVariable("id") Long grantId, @PathVariable("sectionId") Long sectionId, @PathVariable("fieldId") Long fieldId,@RequestHeader("X-TENANT-CODE") String tenantCode) {
        //saveGrant(grantToSave,userId,tenantCode);
        Grant grant = grantService.getById(grantId);
        GrantSpecificSectionAttribute attribute = grantService.getAttributeById(fieldId);

        GrantStringAttribute stringAttrib = grantService.findGrantStringBySectionIdAttribueIdAndGrantId(attribute.getSection().getId(), attribute.getId(), grantId);

        grantService.deleteStringAttribute(stringAttrib);
        grantService.deleteAtttribute(attribute);

        if (_checkIfGrantTemplateChanged(grant, attribute.getSection(), null)) {
            GranterGrantTemplate newTemplate = _createNewGrantTemplateFromExisiting(grant);
        }
        grant = _grantToReturn(userService.getUserById(userId).getId(), grant);
        return grant;
    }

    @PostMapping("/{id}/field/{fieldId}")
    public FieldInfo updateField(@RequestBody SectionAttributesVO attributeToSave, @PathVariable("id") Long grantId, @PathVariable("fieldId") Long fieldId, @PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode) {
        GrantSpecificSectionAttribute currentAttribute = grantService.getAttributeById(fieldId);
        currentAttribute.setFieldName(attributeToSave.getFieldName());
        currentAttribute.setFieldType(attributeToSave.getFieldType());
        currentAttribute = grantService.saveSectionAttribute(currentAttribute);
        GrantStringAttribute stringAttribute = grantService.findGrantStringBySectionIdAttribueIdAndGrantId(currentAttribute.getSection().getId(), currentAttribute.getId(), grantId);
        stringAttribute.setValue("");
        stringAttribute = grantService.saveStringAttribute(stringAttribute);

        Grant grant = grantService.getById(grantId);
        if (_checkIfGrantTemplateChanged(grant, currentAttribute.getSection(), currentAttribute)) {
            _createNewGrantTemplateFromExisiting(grant);
        }


        grant = _grantToReturn(userId, grant);
        return new FieldInfo(currentAttribute.getId(), grant);
    }

    @GetMapping("/{id}/template/{templateId}/section/{sectionName}")
    public SectionInfo createSection(@PathVariable("id") Long grantId, @PathVariable("templateId") Long templateId, @PathVariable("sectionName") String sectionName, @PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode) {

        Grant grant = grantService.getById(grantId);

        GrantSpecificSection specificSection = new GrantSpecificSection();
        specificSection.setGranter((Granter) organizationService.findOrganizationByTenantCode(tenantCode));
        specificSection.setSectionName(sectionName);

        specificSection.setGrantTemplateId(templateId);
        specificSection.setDeletable(true);
        specificSection.setGrantId(grantId);
        specificSection.setSectionOrder(grantService.getNextSectionOrder(organizationService.findOrganizationByTenantCode(tenantCode).getId(), templateId));
        specificSection = grantService.saveSection(specificSection);

        if (_checkIfGrantTemplateChanged(grant, specificSection, null)) {
            GranterGrantTemplate newTemplate = _createNewGrantTemplateFromExisiting(grant);
            templateId = newTemplate.getId();
        }

        grant = _grantToReturn(userId, grant);
        return new SectionInfo(specificSection.getId(), specificSection.getSectionName(), grant);

    }

    @DeleteMapping("/{id}/template/{templateId}/section/{sectionId}")
    public Grant deleteSection(@PathVariable("id") Long grantId, @PathVariable("templateId") Long templateId, @PathVariable("sectionId") Long sectionId, @PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode) {
        GrantSpecificSection section = grantService.getGrantSectionBySectionId(sectionId);
        Grant grant = grantService.getById(grantId);

        grantService.deleteSection(section);

        if (_checkIfGrantTemplateChanged(grant, section, null)) {
            GranterGrantTemplate newTemplate = _createNewGrantTemplateFromExisiting(grant);
            templateId = newTemplate.getId();
        }
        grant = _grantToReturn(userId, grant);
        return grant;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    private GranterGrantTemplate _createNewGrantTemplateFromExisiting(Grant grant) {
        GranterGrantTemplate currentGrantTemplate = granterGrantTemplateService.findByTemplateId(grant.getTemplateId());
        GranterGrantTemplate newTemplate = null;
        if (!currentGrantTemplate.isPublished()) {
            grantService.deleteGrantTemplate(currentGrantTemplate);
        }
        newTemplate = new GranterGrantTemplate();
        newTemplate.setName("Custom Template");
        newTemplate.setGranterId(grant.getGrantorOrganization().getId());
        newTemplate.setPublished(false);
        newTemplate = grantService.saveGrantTemplate(newTemplate);


        List<GranterGrantSection> newSections = new ArrayList<>();
        for (GrantSpecificSection currentSection : grantService.getGrantSections(grant)) {
            GranterGrantSection newSection = new GranterGrantSection();
            newSection.setSectionOrder(currentSection.getSectionOrder());
            newSection.setSectionName(currentSection.getSectionName());
            newSection.setGrantTemplate(newTemplate);
            newSection.setGranter((Granter) grant.getGrantorOrganization());
            newSection.setDeletable(currentSection.getDeletable());

            newSection = grantService.saveGrantTemaplteSection(newSection);
            newSections.add(newSection);

            currentSection.setGrantTemplateId(newTemplate.getId());
            currentSection = grantService.saveSection(currentSection);

            for (GrantSpecificSectionAttribute currentAttribute : grantService.getAttributesBySection(currentSection)) {
                GranterGrantSectionAttribute newAttribute = new GranterGrantSectionAttribute();
                newAttribute.setDeletable(currentAttribute.getDeletable());
                newAttribute.setFieldName(currentAttribute.getFieldName());
                newAttribute.setFieldType(currentAttribute.getFieldType());
                newAttribute.setGranter((Granter) currentAttribute.getGranter());
                newAttribute.setRequired(currentAttribute.getRequired());
                newAttribute.setAttributeOrder(currentAttribute.getAttributeOrder());
                newAttribute.setSection(newSection);

                newAttribute = grantService.saveGrantTemaplteSectionAttribute(newAttribute);

            }
        }

        newTemplate.setSections(newSections);
        newTemplate = grantService.saveGrantTemplate(newTemplate);

        grant.setTemplateId(newTemplate.getId());
        grantService.saveGrant(grant);
        return newTemplate;
    }

    private Boolean _checkIfGrantTemplateChanged(Grant grant, GrantSpecificSection newSection, GrantSpecificSectionAttribute newAttribute) {
        GranterGrantTemplate currentGrantTemplate = granterGrantTemplateService.findByTemplateId(grant.getTemplateId());
        for (GranterGrantSection grantSection : currentGrantTemplate.getSections()) {
            if (!grantSection.getSectionName().equalsIgnoreCase(newSection.getSectionName())) {
                return true;
            }
            if (newAttribute != null) {
                for (GranterGrantSectionAttribute sectionAttribute : grantSection.getAttributes()) {
                    if (!sectionAttribute.getFieldName().equalsIgnoreCase(newAttribute.getFieldName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @GetMapping("/{id}")
    public GrantVO getGrant(@PathVariable("id") Long grantId, @PathVariable("userId") Long userId) {

        User user = userService.getUserById(userId);
        Grant grant = grantService.getById(grantId);
        return new GrantVO()
                .build(grant, grantService.getGrantSections(grant), workflowPermissionService, user,
                        appConfigService.getAppConfigForGranterOrg(grant.getGrantorOrganization().getId(),
                                AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS));
    }

    @PutMapping(value = "/kpi")
    @Transactional
    public GrantVO saveKpiSubmissions(@RequestBody SubmissionData submissionData,
                                      @PathVariable("userId") Long userId) {

        User user = userService.getUserById(userId);
        for (KpiSubmissionData data : submissionData.getKpiSubmissionData()) {
            switch (data.getType()) {
                case "QUANTITATIVE":
                    GrantQuantitativeKpiData quantitativeKpiData = quantitativeDataService
                            .findById(data.getKpiDataId());
                    quantitativeKpiData.setActuals(Integer.valueOf(data.getValue()));
                    quantitativeKpiData.setUpdatedAt(DateTime.now().toDate());
                    quantitativeKpiData.setUpdatedBy(user.getEmailId());

                    if (data.getNotes() != null) {
                        for (String note : data.getNotes()) {
                            QuantitativeKpiNotes kpiNote = new QuantitativeKpiNotes();
                            kpiNote.setMessage(note);
                            kpiNote.setPostedBy(user);
                            kpiNote.setPostedOn(DateTime.now().toDate());
                            kpiNote.setKpiData(quantitativeKpiData);
                            kpiNote = quantitativeKpiNotesService.saveQuantitativeKpiNotes(kpiNote);
                            quantitativeKpiData.getNotesHistory().add(kpiNote);
                        }
                    }

                    List<QuantKpiDataDocument> kpiDocs = new ArrayList<>();
                    if ((data.getFiles() != null && !data
                            .getFiles().isEmpty())) {
                        for (UploadFile uploadedFile : data.getFiles()) {
                            String fileName = uploadLocation + uploadedFile.getFileName();
                            QuantKpiDataDocument quantKpiDataDocument = new QuantKpiDataDocument();
                            quantKpiDataDocument.setFileName(uploadedFile.getFileName());
                            if (quantitativeKpiData.getSubmissionDocs().contains(quantKpiDataDocument)) {
                                quantKpiDataDocument = quantitativeKpiData.getSubmissionDocs()
                                        .get(quantitativeKpiData.getSubmissionDocs().indexOf(quantKpiDataDocument));
                                quantKpiDataDocument.setVersion(quantKpiDataDocument.getVersion() + 1);
                            } else {
                                quantKpiDataDocument.setFileType(uploadedFile.getFileType());
                                quantKpiDataDocument.setQuantKpiData(quantitativeKpiData);
                            }

                            try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {

                                byte[] dataBytes = Base64.getDecoder().decode(uploadedFile.getValue());
                                fileOutputStream.write(dataBytes);
                            } catch (IOException e) {
                                logger.error(e.getMessage(), e);
                            }

                            quantKpiDataDocument = quantKpiDocumentService.saveFile(quantKpiDataDocument);
                            kpiDocs.add(quantKpiDataDocument);
                        }

                        quantitativeKpiData.setSubmissionDocs(kpiDocs);
                        quantitativeKpiData = quantitativeDataService.saveData(quantitativeKpiData);

                    }
                    break;
                case "QUALITATIVE":
                    GrantQualitativeKpiData qualitativeKpiData = qualitativeDataService
                            .findById(data.getKpiDataId());
                    qualitativeKpiData.setActuals(data.getValue());
                    qualitativeKpiData.setCreatedAt(DateTime.now().toDate());
                    qualitativeKpiData.setUpdatedBy(user.getEmailId());

                    qualitativeDataService.saveData(qualitativeKpiData);
                    if (data.getNotes() != null) {
                        for (String note : data.getNotes()) {
                            QualitativeKpiNotes kpiNote = new QualitativeKpiNotes();
                            kpiNote.setMessage(note);
                            kpiNote.setPostedBy(user);
                            kpiNote.setPostedOn(DateTime.now().toDate());
                            kpiNote.setKpiData(qualitativeKpiData);
                            kpiNote = qualitativeKpiNotesService.saveQualitativeKpiNotes(kpiNote);
                            qualitativeKpiData.getNotesHistory().add(kpiNote);
                        }
                    }
                    break;
                case "DOCUMENT":
                    GrantDocumentKpiData documentKpiData = grantDocumentDataService
                            .findById(data.getKpiDataId());

                    List<DocKpiDataDocument> docKpiDataDocuments = documentKpiData.getSubmissionDocs();
                    List<DocKpiDataDocument> submissionDocs = new ArrayList<>();

                    if (documentKpiData.getActuals() != null && (data.getFiles() == null || data
                            .getFiles().isEmpty())) {
                    } else {
                        for (UploadFile uploadedFile : data.getFiles()) {
                            String fileName = uploadLocation + uploadedFile.getFileName();
                            DocKpiDataDocument docKpiDataDocument = new DocKpiDataDocument();
                            docKpiDataDocument.setFileName(uploadedFile.getFileName());
                            if (docKpiDataDocuments.contains(docKpiDataDocument)) {
                                docKpiDataDocument = docKpiDataDocuments
                                        .get(docKpiDataDocuments.indexOf(docKpiDataDocument));
                                docKpiDataDocument.setVersion(docKpiDataDocument.getVersion() + 1);
                            } else {
                                docKpiDataDocument.setFileType(uploadedFile.getFileType());
                                docKpiDataDocument.setDocKpiData(documentKpiData);
                            }

                            try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {

                                byte[] dataBytes = Base64.getDecoder().decode(uploadedFile.getValue());
                                fileOutputStream.write(dataBytes);
                            } catch (IOException e) {
                                logger.error(e.getMessage(), e);
                            }

                            docKpiDataDocument = docKpiDataDocumentService.saveKpiDoc(docKpiDataDocument);
                            submissionDocs.add(docKpiDataDocument);
                        }

                    }
                    if (data.getNotes() != null) {
                        for (String note : data.getNotes()) {
                            DocumentKpiNotes kpiNote = new DocumentKpiNotes();
                            kpiNote.setMessage(note);
                            kpiNote.setPostedBy(user);
                            kpiNote.setPostedOn(DateTime.now().toDate());
                            kpiNote.setKpiData(documentKpiData);
                            kpiNote = documentKpiNotesService.saveDocumentKpiNotes(kpiNote);
                            documentKpiData.getNotesHistory().add(kpiNote);
                        }
                    }
                    documentKpiData.setSubmissionDocs(submissionDocs);
                    grantDocumentDataService.saveDocumentKpi(documentKpiData);
                    break;

                default:
                    logger.info("Nothing to do");
            }
        }

        Submission submission = submissionService.getById(submissionData.getId());

        for (String msg : submissionData.getNotes()) {
            SubmissionNote note = new SubmissionNote();
            note.setMessage(msg);
            note.setSubmission(submission);
            note.setPostedBy(user);
            note.setPostedOn(DateTime.now().toDate());
            submissionNoteService.saveSubmissionNote(note);
        }
        submission.setSubmittedOn(DateTime.now().toDate());
        submission
                .setSubmissionStatus(workflowStatusService
                        .findById(submissionData.getKpiSubmissionData().get(0).getToStatusId()));

        submission = submissionService.saveSubmission(submission);

        List<User> usersToNotify = userService
                .usersToNotifyOnWorkflowSateChangeTo(submission.getSubmissionStatus().getId());

        for (User userToNotify : usersToNotify) {
            commonEmailSevice.sendMail(userToNotify.getEmailId(), appConfigService
                            .getAppConfigForGranterOrg(submission.getGrant().getGrantorOrganization().getId(),
                                    AppConfiguration.SUBMISSION_ALTER_MAIL_SUBJECT).getConfigValue(),
                    submissionService.buildMailContent(submission, appConfigService
                            .getAppConfigForGranterOrg(submission.getGrant().getGrantorOrganization().getId(),
                                    AppConfiguration.SUBMISSION_ALTER_MAIL_CONTENT).getConfigValue()));
        }

        Grant grant = submission.getGrant();
        grant.setSubstatus(submission.getSubmissionStatus());
        grant = grantService.saveGrant(grant);
        grant = grantService.getById(grant.getId());
        return new GrantVO()
                .build(grant, grantService.getGrantSections(grant), workflowPermissionService, user,
                        appConfigService.getAppConfigForGranterOrg(grant.getGrantorOrganization().getId(),
                                AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS));
    }


    @PutMapping("/")
    public Grant saveGrant(@RequestBody Grant grantToSave, @PathVariable("userId") Long userId,
                           @RequestHeader("X-TENANT-CODE") String tenantCode) {


        Organization tenantOrg = organizationService.findOrganizationByTenantCode(tenantCode);
        User user = userService.getUserById(userId);
        grantToSave.setOrganization(_processNewGranteeOrgIfPresent(grantToSave));
        Grant grant = _processGrant(grantToSave, tenantOrg, user);

        //grant = _processGrantKpis(grantToSave, tenantOrg, user);

        //grantToSave = grantService.saveGrant(grantToSave);

        grant.setActionAuthorities(workflowPermissionService
                .getGrantActionPermissions(grant.getGrantorOrganization().getId(),
                        user.getUserRoles(), grant.getGrantStatus().getId()));

        grant.setFlowAuthorities(workflowPermissionService
                .getGrantFlowPermissions(grant.getGrantorOrganization().getId(),
                        user.getUserRoles(), grant.getGrantStatus().getId()));

        for (Submission submission : grant.getSubmissions()) {
            submission.setActionAuthorities(workflowPermissionService
                    .getSubmissionActionPermission(grant.getGrantorOrganization().getId(),
                            user.getUserRoles()));

            AppConfig submissionWindow = appConfigService
                    .getAppConfigForGranterOrg(submission.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS);
            Date submissionWindowStart = new DateTime(submission.getSubmitBy())
                    .minusDays(Integer.valueOf(submissionWindow.getConfigValue()) + 1).toDate();

            List<WorkFlowPermission> flowPermissions = workflowPermissionService
                    .getSubmissionFlowPermissions(grant.getGrantorOrganization().getId(),
                            user.getUserRoles(), submission.getSubmissionStatus().getId());

            if (!flowPermissions.isEmpty() && DateTime.now().toDate()
                    .after(submissionWindowStart)) {
                submission.setFlowAuthorities(flowPermissions);
            }
            if (DateTime.now().toDate()
                    .after(submissionWindowStart)) {
                submission.setOpenForReporting(true);
            } else {
                submission.setOpenForReporting(false);
            }
        }

        GrantVO grantVO = new GrantVO();

        grantVO = grantVO.build(grant, grantService.getGrantSections(grant), workflowPermissionService, user, appConfigService
                .getAppConfigForGranterOrg(grant.getGrantorOrganization().getId(),
                        AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS));
        grant.setGrantDetails(grantVO.getGrantDetails());
        GranterGrantTemplate templ = granterGrantTemplateService.findByTemplateId(grant.getTemplateId());
        if(templ!=null) {
            grant.setGrantTemplate(templ);
        }

        //submissionService.saveSubmissions(grantToSave.getSubmissions());

        grant.getSubmissions().sort((a, b) -> a.getSubmitBy().compareTo(b.getSubmitBy()));
        grant.getGrantDetails().getSections().sort((a, b) -> Long.valueOf(a.getOrder()).compareTo(Long.valueOf(b.getOrder())));
        for (SectionVO sec : grant.getGrantDetails().getSections()) {
            if (sec.getAttributes() != null) {
                sec.getAttributes().sort((a, b) -> Long.valueOf(a.getAttributeOrder()).compareTo(Long.valueOf(b.getAttributeOrder())));
            }
        }
        return grant;
    }

    private Grantee _processNewGranteeOrgIfPresent(Grant grantToSave) {
        Grantee newGrantee = null;
        if(grantToSave.getOrganization()!=null) {
            if (grantToSave.getOrganization().getId() < 0) {
                newGrantee = (Grantee) grantToSave.getOrganization();
                newGrantee = granteeService.saveGrantee((Grantee) newGrantee);
            } else {
                newGrantee = (Grantee) grantToSave.getOrganization();
            }
        }
        return newGrantee;
    }

    private Grant _processGrant(Grant grantToSave, Organization tenant, User user) {
        Grant grant = null;
        //grant = grantService.findGrantByNameAndGranter(grantToSave.getName(),(Granter)tenant);
        if (grantToSave.getId() < 0) {
            grant = new Grant();
            grant.setGrantStatus(workflowStatusService.findInitialStatusByObjectAndGranterOrgId("GRANT", tenant.getId()));
            grant.setSubstatus(workflowStatusService.findInitialStatusByObjectAndGranterOrgId("SUBMISSION", tenant.getId()));
            grant.setOrganization((Grantee) grantToSave.getOrganization());
            List<GrantStringAttribute> stringAttributes = new ArrayList<>();
            List<GrantDocumentAttributes> docAttributes = new ArrayList<>();
            grant.setStringAttributes(stringAttributes);
            grant.setDocumentAttributes(docAttributes);
        } else {
            grant = grantService.getById(grantToSave.getId());
        }
        grant.setAmount(grantToSave.getAmount());
        grant.setDescription(grantToSave.getDescription());
        grant.setRepresentative(grantToSave.getRepresentative());
        if (grantToSave.getEndDate() != null) {
            grant.setEnDate(grantToSave.getEnDate());
            grant.setEndDate(grantToSave.getEndDate());
        }
        if (grantToSave.getStartDate() != null) {
            grant.setStDate(grantToSave.getStDate());
            grant.setStartDate(grantToSave.getStartDate());
        }
        grant.setGrantorOrganization((Granter) tenant);
        grant.setGrantStatus(grantToSave.getGrantStatus());
        grant.setName(grantToSave.getName());


        grantToSave.setOrganization((Grantee)grantToSave.getOrganization());


        grant.setOrganization((Grantee) grantToSave.getOrganization());

        grant.setStatusName(grantToSave.getStatusName());
        if (grantToSave.getEndDate() != null) {
            grant.setStartDate(grantToSave.getStartDate());
            grant.setStDate(grantToSave.getStDate());
        }
        grant.setSubstatus(grantToSave.getSubstatus());
        grant.setUpdatedAt(DateTime.now().toDate());
        grant.setUpdatedBy(user.getEmailId());
        grant.setSubstatus(grantToSave.getSubstatus());
        grant = grantService.saveGrant(grant);

        //_processDocumentAttributes(grant, grantToSave, tenant);
        //grant.setKpis(_processGrantKpis(grant, grantToSave, tenant, user));
        _processStringAttributes(grant, grantToSave, tenant);
        //grant.setSubmissions(_processGrantSubmissions(grant, grantToSave, tenant, user));

        grant = grantService.saveGrant(grant);

        return grant;
    }

    private void _processDocumentAttributes(Grant grant, Grant grantToSave, Organization tenant) {
        List<GrantDocumentAttributes> documentAttributes = new ArrayList<>();
        GrantSpecificSection granterGrantSection = null;
        for (SectionVO sectionVO : grantToSave.getGrantDetails().getSections()) {
            if (sectionVO.getId() < 0) {
                granterGrantSection = new GrantSpecificSection();
            } else {
                granterGrantSection = grantService.getGrantSectionBySectionId(sectionVO.getId());
            }
            granterGrantSection.setSectionName(sectionVO.getName());
            granterGrantSection.setGranter((Granter) tenant);
            granterGrantSection.setDeletable(true);

            granterGrantSection = grantService.saveSection(granterGrantSection);

            GrantSpecificSectionAttribute sectionAttribute = null;
            for (SectionAttributesVO sectionAttributesVO : sectionVO.getAttributes()) {
                if (sectionAttributesVO.getId() < 0) {
                    sectionAttribute = new GrantSpecificSectionAttribute();
                } else {
                    sectionAttribute = grantService.getSectionAttributeByAttributeIdAndType(sectionAttributesVO.getId(), sectionAttributesVO.getFieldType());
                }
                sectionAttribute.setDeletable(true);
                sectionAttribute.setFieldName(sectionAttributesVO.getFieldName());
                sectionAttribute.setFieldType(sectionAttributesVO.getFieldType());
                sectionAttribute.setGranter((Granter) tenant);
                sectionAttribute.setRequired(true);
                sectionAttribute.setSection(granterGrantSection);
                sectionAttribute = grantService.saveSectionAttribute(sectionAttribute);


                GrantDocumentAttributes grantDocAttribute = grantService.findGrantDocumentBySectionAttribueAndGrant(granterGrantSection, sectionAttribute, grant);
                if (grantDocAttribute == null) {
                    grantDocAttribute = new GrantDocumentAttributes();
                    grantDocAttribute.setSectionAttribute(sectionAttribute);
                    grantDocAttribute.setSection(granterGrantSection);
                    grantDocAttribute.setGrant(grant);
                }
                grantService.saveGrantDocumentAttribute(grantDocAttribute);
                grant.getDocumentAttributes().add(grantDocAttribute);
            }
            grant = grantService.saveGrant(grant);
        }
    }

    private void _processStringAttributes(Grant grant, Grant grantToSave, Organization tenant) {
        List<GrantStringAttribute> stringAttributes = new ArrayList<>();
        GrantSpecificSection grantSpecificSection = null;
        List<GrantSpecificSection> sectionsToDelete = new ArrayList<>();
        if (grantService.getGrantSections(grant) != null) {
            for (GrantSpecificSection grantSection : grantService.getGrantSections(grant)) {
                boolean found = false;
                for (SectionVO secVO : grantToSave.getGrantDetails().getSections()) {
                    if (secVO.getId().longValue() == grantSection.getId().longValue()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    sectionsToDelete.add(grantSection);
                }
            }
        }
        for (SectionVO sectionVO : grantToSave.getGrantDetails().getSections()) {
            //grantSpecificSection = grantService.findByGranterAndSectionName((Granter) grant.getGrantorOrganization(), sectionVO.getName());
            if (sectionVO.getId() < 0 && grantSpecificSection == null) {
                grantSpecificSection = new GrantSpecificSection();
            } else if (sectionVO.getId() > 0) {
                grantSpecificSection = grantService.getGrantSectionBySectionId(sectionVO.getId());
            }
            grantSpecificSection.setSectionName(sectionVO.getName());
            grantSpecificSection.setGranter((Granter) tenant);
            grantSpecificSection.setDeletable(true);

            grantSpecificSection = grantService.saveSection(grantSpecificSection);

            GrantSpecificSectionAttribute sectionAttribute = null;

            if (sectionVO.getAttributes() != null) {
                List<GrantSpecificSectionAttribute> sectionAttributesToDelete = new ArrayList<>();
                for (SectionAttributesVO sectionAttributesVO : sectionVO.getAttributes()) {
                    /*if (sectionAttributesVO.getFieldName().trim().equalsIgnoreCase("")) {
                        continue;
                    }*/

                    sectionAttribute = grantService.getSectionAttributeByAttributeIdAndType(sectionAttributesVO.getId(), sectionAttributesVO.getFieldType());


                    if (sectionAttribute == null) {
                        sectionAttributesToDelete.add(sectionAttribute);
                        continue;
                    }
                    //sectionAttribute.setDeletable(true);
                    sectionAttribute.setFieldName(sectionAttributesVO.getFieldName());
                    sectionAttribute.setFieldType(sectionAttributesVO.getFieldType());
                    sectionAttribute.setGranter((Granter) tenant);
                    sectionAttribute.setRequired(true);
                    sectionAttribute.setSection(grantSpecificSection);
                    sectionAttribute = grantService.saveSectionAttribute(sectionAttribute);


                    GrantStringAttribute grantStringAttribute = grantService.findGrantStringBySectionAttribueAndGrant(grantSpecificSection, sectionAttribute, grant);
                    if (grantStringAttribute == null) {
                        grantStringAttribute = new GrantStringAttribute();
                        grantStringAttribute.setSectionAttribute(sectionAttribute);
                        grantStringAttribute.setSection(grantSpecificSection);
                        grantStringAttribute.setGrant(grant);
                    }
                    grantStringAttribute.setTarget(sectionAttributesVO.getTarget());
                    grantStringAttribute.setFrequency(sectionAttributesVO.getFrequency());
                    if (sectionAttribute.getFieldType().equalsIgnoreCase("table")) {
                        List<TableData> tableData = sectionAttributesVO.getFieldTableValue();
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            grantStringAttribute.setValue(mapper.writeValueAsString(tableData));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                    } else {
                        grantStringAttribute.setValue(sectionAttributesVO.getFieldValue());
                    }
                    grantService.saveGrantStringAttribute(grantStringAttribute);
                    grant.getStringAttributes().add(grantStringAttribute);
                }
            }
            grant = grantService.saveGrant(grant);
        }

        if (sectionsToDelete != null && sectionsToDelete.size() > 0) {
            for (GrantSpecificSection section : sectionsToDelete) {
                GrantSpecificSection sec = grantService.getGrantSectionBySectionId(section.getId());
                List<GrantSpecificSectionAttribute> attribs = grantService.getAttributesBySection(sec);
                for (GrantSpecificSectionAttribute attribute : attribs) {
                    List<GrantStringAttribute> strAttribs = grantService.getStringAttributesByAttribute(attribute);
                    grantService.deleteStringAttributes(strAttribs);
                }
                grantService.deleteSectionAttributes(attribs);
            }
            grantService.deleteSections(sectionsToDelete);
            grant = grantService.saveGrant(grant);
        }

    }

    private List<GrantKpi> _processGrantKpis(Grant grant, Grant grantToSave, Organization tenant, User user) {
        GrantKpi grantKpi = null;
        List<GrantKpi> kpisList = new ArrayList<>();
        for (GrantKpi kpi : grantToSave.getKpis()) {

            if (kpi.getId() < 0) {
                grantKpi = new GrantKpi();
            } else {
                grantKpi = grantService.getGrantKpiById(kpi.getId());
            }
            grantKpi.setTitle(kpi.getTitle());
            grantKpi.setScheduled(kpi.isScheduled());
            grantKpi.setPeriodicity(kpi.getPeriodicity());
            grantKpi.setKpiType(kpi.getKpiType());
            grantKpi.setDescription(kpi.getDescription());
            grantKpi.setFrequency(kpi.getFrequency());
            grantKpi.setUpdatedAt(DateTime.now().toDate());
            grantKpi.setUpdatedBy(user.getEmailId());
            grantKpi.setKpiReportingType(kpi.getKpiReportingType());
            grantKpi.setGrant(grant);

            grantKpi = grantService.saveGrantKpi(grantKpi);

            grantKpi.setTemplates(_processKpiTemplates(grantKpi, kpi, tenant));

            kpisList.add(grantKpi);
        }
        return kpisList;
    }


    private List<Template> _processKpiTemplates(GrantKpi kpi, GrantKpi kpitoSave, Organization tenant) {
        List<Template> kpiTemplates = new ArrayList<>();
        try {

            Template kpiTemplate = null;
            for (Template template : kpitoSave.getTemplates()) {
                if (template.getId() < 0) {
                    kpiTemplate = new Template();
                } else {
                    kpiTemplate = grantService.getKpiTemplateById(template.getId());
                }
                kpiTemplate.setDescription(template.getDescription());
                kpiTemplate.setFileType(template.getFileType());
                kpiTemplate.setKpi(kpi);
                kpiTemplate.setLocation(template.getLocation());
                kpiTemplate.setName(template.getName());
                kpiTemplate.setType(template.getType());
                kpiTemplate.setVersion(template.getVersion());
                kpiTemplate = grantService.saveKpiTemplate(kpiTemplate);
                kpiTemplates.add(kpiTemplate);

                if (template.getData() != null) {
                    String uploadFolder = uploadLocation + tenant.getCode() + "/grants/" + kpi.getGrant().getId() + "/kpi-templates";


                    Files.createDirectories(Paths.get(uploadFolder));

                    FileOutputStream fileOutputStream = new FileOutputStream(uploadFolder + "/" + template.getName());
                    byte[] dataBytes = Base64.getDecoder().decode(template.getData().substring(template.getData().indexOf(",") + 1));
                    fileOutputStream.write(dataBytes);
                    fileOutputStream.close();


                }
            }
        } catch (IOException e) {
            logger.error("Could not process the uploaded File. Please try again.");
        }
        return kpiTemplates;
    }

    private List<Submission> _processGrantSubmissions(Grant grant, Grant grantToSave, Organization tenant, User user) {
        Submission grantSubmission = null;
        List<Submission> grantSubmissions = new ArrayList<>();

        for (Submission submission : grantToSave.getSubmissions()) {
            if (submission.getId() < 0) {
                grantSubmission = new Submission();
                grantSubmission.setSubmissionStatus(workflowStatusService.findInitialStatusByObjectAndGranterOrgId("SUBMISSION", tenant.getId()));
            } else {
                grantSubmission = submissionService.getById(submission.getId());
            }

            grantSubmission.setGrant(grant);

            grantSubmission.setSubmissionStatus(workflowStatusService.findById(submission.getSubmissionStatus().getId()));
            if (submission.getFlowAuthorities() != null) {
                grant.setSubstatus(workflowStatusService.findById(submission.getSubmissionStatus().getId()));
                grant = grantService.saveGrant(grant);
            }
            grantSubmission.setSubmitBy(DateTime.parse(submission.getSubmitDateStr()).toDate());
            grantSubmission.setSubmitDateStr(submission.getSubmitDateStr());
            grantSubmission.setSubmittedOn(submission.getSubmittedOn());
            grantSubmission.setTitle(submission.getTitle());
            grantSubmission = submissionService.saveSubmission(grantSubmission);

            grantSubmission.setDocumentKpiSubmissions(_processDocumentKpis(grantSubmission, submission, tenant, user));
            grantSubmission.setQualitativeKpiSubmissions(_processQualitativeKpis(grantSubmission, submission, tenant, user));
            grantSubmission.setQuantitiaveKpisubmissions(_processQuantitativeKpis(grantSubmission, submission, tenant, user));
            //grantSubmission.setSubmissionNotes(_processSubmissionNote(grantSubmission, submission, tenant, user));

            grantSubmission = submissionService.saveSubmission(grantSubmission);
            grantSubmissions.add(grantSubmission);
        }
        return grantSubmissions;
    }

    private List<GrantQuantitativeKpiData> _processQuantitativeKpis(Submission submission, Submission submission2Save, Organization tenant, User user) {
        GrantQuantitativeKpiData quantKpiData = null;
        List<GrantQuantitativeKpiData> quantKpiDataList = new ArrayList<>();
        for (GrantQuantitativeKpiData docKpi : submission2Save.getQuantitiaveKpisubmissions()) {
            for (GrantKpi kpi : submission.getGrant().getKpis()) {
                if (kpi.getKpiType() == KpiType.QUANTITATIVE && (kpi.getId() == docKpi.getGrantKpi().getId() || kpi.getTitle().equalsIgnoreCase(docKpi.getGrantKpi().getTitle()))) {
                    if (docKpi.getId() < 0) {
                        quantKpiData = new GrantQuantitativeKpiData();
                    } else {
                        quantKpiData = grantService.getGrantQuantitativeKpiDataById(docKpi.getId());
                    }
                    quantKpiData.setActuals(docKpi.getActuals());
                    quantKpiData.setGoal(docKpi.getGoal());
                    // TODO documentKpiData.setGrantKpi();

                    quantKpiData.setGrantKpi(kpi);
                    quantKpiData.setSubmission(submission);
                    quantKpiData.setToReport(docKpi.getToReport());


                    quantKpiData = grantService.saveGrantQunatitativeKpiData(quantKpiData);

                    quantKpiData.setNotesHistory(_processQuantNotesHistory(quantKpiData, docKpi, tenant, user));
                    quantKpiData.setSubmissionDocs(_processQuantSubmissionDocs(quantKpiData, docKpi, tenant, user));

                    quantKpiData = grantService.saveGrantQunatitativeKpiData(quantKpiData);
                    quantKpiDataList.add(quantKpiData);
                }
            }
        }
        return quantKpiDataList;
    }

    private List<GrantQualitativeKpiData> _processQualitativeKpis(Submission submission, Submission submission2Save, Organization tenant, User user) {
        GrantQualitativeKpiData qualKpiData = null;
        List<GrantQualitativeKpiData> qualKpiDataList = new ArrayList<>();
        for (GrantQualitativeKpiData docKpi : submission2Save.getQualitativeKpiSubmissions()) {
            for (GrantKpi kpi : submission.getGrant().getKpis()) {
                if (kpi.getKpiType() == KpiType.QUALITATIVE && (kpi.getId() == docKpi.getGrantKpi().getId() || kpi.getTitle().equalsIgnoreCase(docKpi.getGrantKpi().getTitle()))) {
                    if (docKpi.getId() < 0) {
                        qualKpiData = new GrantQualitativeKpiData();
                    } else {
                        qualKpiData = grantService.getGrantQualitativeKpiDataById(docKpi.getId());
                    }
                    qualKpiData.setActuals(docKpi.getActuals());
                    qualKpiData.setGoal(docKpi.getGoal());
                    // TODO documentKpiData.setGrantKpi();
                    qualKpiData.setGrantKpi(kpi);
                    qualKpiData.setSubmission(submission);
                    qualKpiData.setToReport(docKpi.getToReport());


                    qualKpiData = grantService.saveGrantQualitativeKpiData(qualKpiData);

                    qualKpiData.setNotesHistory(_processQualNotesHistory(qualKpiData, docKpi, tenant, user));
                    qualKpiData.setSubmissionDocs(_processQualSubmissionDocs(qualKpiData, docKpi, tenant, user));

                    qualKpiData = grantService.saveGrantQualitativeKpiData(qualKpiData);
                    qualKpiDataList.add(qualKpiData);
                }
            }
        }
        return qualKpiDataList;
    }

    private List<GrantDocumentKpiData> _processDocumentKpis(Submission submission, Submission submission2Save, Organization tenant, User user) {
        GrantDocumentKpiData documentKpiData = null;
        List<GrantDocumentKpiData> documentKpiDataList = new ArrayList<>();
        for (GrantDocumentKpiData docKpi : submission2Save.getDocumentKpiSubmissions()) {
            for (GrantKpi kpi : submission.getGrant().getKpis()) {
                if (kpi.getKpiType() == KpiType.DOCUMENT && (kpi.getId() == docKpi.getGrantKpi().getId() || kpi.getTitle().equalsIgnoreCase(docKpi.getGrantKpi().getTitle()))) {
                    if (docKpi.getId() < 0) {
                        documentKpiData = new GrantDocumentKpiData();
                    } else {
                        documentKpiData = grantService.getGrantDocumentKpiDataById(docKpi.getId());
                    }
                    documentKpiData.setActuals(docKpi.getActuals());
                    documentKpiData.setGoal(docKpi.getGoal());
                    // TODO documentKpiData.setGrantKpi();

                    documentKpiData.setSubmission(submission);
                    documentKpiData.setGrantKpi(kpi);
                    documentKpiData.setToReport(docKpi.getToReport());
                    documentKpiData.setType(docKpi.getType());

                    documentKpiData = grantService.saveGrantDocumentKpiData(documentKpiData);

                    documentKpiData.setNotesHistory(_processDocNotesHistory(documentKpiData, docKpi, tenant, user));
                    documentKpiData.setSubmissionDocs(_processDocSubmissionDocs(documentKpiData, docKpi, tenant, user));

                    documentKpiData = grantService.saveGrantDocumentKpiData(documentKpiData);
                    documentKpiDataList.add(documentKpiData);
                }
            }
        }
        return documentKpiDataList;
    }

    public List<QuantKpiDataDocument> _processQuantSubmissionDocs(GrantQuantitativeKpiData documentKpiData, GrantQuantitativeKpiData docKpi2Save, Organization tenant, User user) {
        QuantKpiDataDocument kpiDoc = null;
        List<QuantKpiDataDocument> quantKpiDataDocuments = new ArrayList<>();
        if (docKpi2Save.getSubmissionDocs() != null) {
            for (QuantKpiDataDocument doc : docKpi2Save.getSubmissionDocs()) {
                if (doc.getId() < 0) {
                    kpiDoc = new QuantKpiDataDocument();
                } else {
                    kpiDoc = grantService.getQuantkpiDocById(doc.getId());
                }

                kpiDoc.setFileName(doc.getFileName());
                kpiDoc.setFileType(doc.getFileType());
                kpiDoc.setVersion(doc.getVersion());

                if (doc.getData() != null) {
                    String uploadFolder = uploadLocation + tenant.getCode() + "/grants/" + documentKpiData.getGrantKpi().getGrant().getId() + "/kpi-documents";
                    try {

                        Files.createDirectories(Paths.get(uploadFolder));

                        FileOutputStream fileOutputStream = new FileOutputStream(uploadFolder + "/" + doc.getFileName());
                        byte[] dataBytes = Base64.getDecoder().decode(doc.getData().substring(doc.getData().indexOf(",") + 1));
                        fileOutputStream.write(dataBytes);
                        fileOutputStream.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    kpiDoc.setQuantKpiData(documentKpiData);
                    kpiDoc = grantService.saveQuantKpiDataDoc(kpiDoc);
                    quantKpiDataDocuments.add(kpiDoc);
                }
            }
        }
        return quantKpiDataDocuments;
    }

    public List<QuantitativeKpiNotes> _processQuantNotesHistory(GrantQuantitativeKpiData documentKpiData, GrantQuantitativeKpiData docKpi2Save, Organization tenant, User user) {
        QuantitativeKpiNotes kpiNote = null;
        List<QuantitativeKpiNotes> quantKpiNote = new ArrayList<>();
        if (docKpi2Save.getNotesHistory() != null) {
            for (QuantitativeKpiNotes docNote : docKpi2Save.getNotesHistory()) {
                if (docNote.getId() < 0) {
                    kpiNote = new QuantitativeKpiNotes();
                } else {
                    kpiNote = grantService.getQuantKpiNoteById(docNote.getId());
                }

                kpiNote.setPostedOn(DateTime.now().toDate());
                kpiNote.setPostedBy(user);
                kpiNote.setMessage(docNote.getMessage());
                kpiNote.setKpiData(documentKpiData);
                kpiNote = grantService.saveQuantKpiNote(kpiNote);
                quantKpiNote.add(kpiNote);
            }
        }
        return quantKpiNote;
    }

    public List<QualKpiDataDocument> _processQualSubmissionDocs(GrantQualitativeKpiData documentKpiData, GrantQualitativeKpiData docKpi2Save, Organization tenant, User user) {
        QualKpiDataDocument kpiDoc = null;
        List<QualKpiDataDocument> qualKpiDataDocuments = new ArrayList<>();
        if (docKpi2Save.getSubmissionDocs() != null) {
            for (QualKpiDataDocument doc : docKpi2Save.getSubmissionDocs()) {
                if (doc.getId() < 0) {
                    kpiDoc = new QualKpiDataDocument();
                } else {
                    kpiDoc = grantService.getQualkpiDocById(doc.getId());
                }

                kpiDoc.setFileName(doc.getFileName());
                kpiDoc.setFileType(doc.getFileType());
                kpiDoc.setVersion(doc.getVersion());
                kpiDoc.setQualKpiData(documentKpiData);
                kpiDoc = grantService.saveQualKpiDataDoc(kpiDoc);
                qualKpiDataDocuments.add(kpiDoc);
            }
        }
        return qualKpiDataDocuments;
    }

    public List<QualitativeKpiNotes> _processQualNotesHistory(GrantQualitativeKpiData documentKpiData, GrantQualitativeKpiData docKpi2Save, Organization tenant, User user) {
        QualitativeKpiNotes kpiNote = null;
        List<QualitativeKpiNotes> qualKpiNote = new ArrayList<>();
        if (docKpi2Save.getNotesHistory() != null) {
            for (QualitativeKpiNotes docNote : docKpi2Save.getNotesHistory()) {
                if (docNote.getId() < 0) {
                    kpiNote = new QualitativeKpiNotes();
                } else {
                    kpiNote = grantService.getQualKpiNoteById(docNote.getId());
                }

                kpiNote.setPostedOn(DateTime.now().toDate());
                kpiNote.setPostedBy(user);
                kpiNote.setMessage(docNote.getMessage());
                kpiNote.setKpiData(documentKpiData);
                kpiNote = grantService.saveQualKpiNote(kpiNote);
                qualKpiNote.add(kpiNote);
            }
        }
        return qualKpiNote;
    }

    public List<DocKpiDataDocument> _processDocSubmissionDocs(GrantDocumentKpiData documentKpiData, GrantDocumentKpiData docKpi2Save, Organization tenant, User user) {
        DocKpiDataDocument kpiDoc = null;
        List<DocKpiDataDocument> docKpiDataDocuments = new ArrayList<>();
        if (docKpi2Save.getSubmissionDocs() != null) {
            for (DocKpiDataDocument doc : docKpi2Save.getSubmissionDocs()) {
                if (doc.getId() < 0) {
                    kpiDoc = new DocKpiDataDocument();
                } else {
                    kpiDoc = grantService.getDockpiDocById(doc.getId());
                }

                kpiDoc.setDocKpiData(documentKpiData);
                kpiDoc.setFileName(doc.getFileName());
                kpiDoc.setFileType(doc.getFileType());
                kpiDoc.setVersion(doc.getVersion());
                kpiDoc = grantService.saveDocKpiDataDoc(kpiDoc);
                docKpiDataDocuments.add(kpiDoc);
            }
        }
        return docKpiDataDocuments;
    }

    public List<DocumentKpiNotes> _processDocNotesHistory(GrantDocumentKpiData documentKpiData, GrantDocumentKpiData docKpi2Save, Organization tenant, User user) {
        DocumentKpiNotes kpiNote = null;
        List<DocumentKpiNotes> docKpiNotes = null;
        if (docKpi2Save.getNotesHistory() != null) {
            for (DocumentKpiNotes docNote : docKpi2Save.getNotesHistory()) {
                if (docNote.getId() < 0) {
                    kpiNote = new DocumentKpiNotes();
                } else {
                    kpiNote = grantService.getDocKpiNoteById(docNote.getId());
                }

                kpiNote.setPostedOn(DateTime.now().toDate());
                kpiNote.setPostedBy(user);
                kpiNote.setMessage(docNote.getMessage());
                kpiNote.setKpiData(documentKpiData);
                kpiNote = grantService.saveDocumentKpiNote(kpiNote);
                if (docKpiNotes == null) {
                    docKpiNotes = new ArrayList<>();
                }
                docKpiNotes.add(kpiNote);
            }
        }
        return docKpiNotes;
    }

    @PostMapping("/{grantId}/flow/{fromState}/{toState}")
    public Grant MoveGrantState(@PathVariable("userId") Long userId,
                                @PathVariable("grantId") Long grantId, @PathVariable("fromState") Long fromStateId,
                                @PathVariable("toState") Long toStateId) {

        Grant grant = grantService.getById(grantId);
        grant.setGrantStatus(workflowStatusService.findById(toStateId));
        grant.setUpdatedAt(DateTime.now().toDate());
        grant.setUpdatedBy(userService.getUserById(userId).getEmailId());
        grant = grantService.saveGrant(grant);

        User user = userService.getUserById(userId);
        WorkflowStatus toStatus = workflowStatusService.findById(toStateId);

        //List<WorkFlowPermission> permissions = workflowPermissionService.getFlowPermisionsOfRoleForStateTransition(grant.getId(),user.getUserRoles(),toStateId);
        //if(!permissions.isEmpty()){
        List<User> usersToNotify = userService.usersToNotifyOnWorkflowSateChangeTo(toStateId);
        String notificationMessageTemplate = appConfigService
                .getAppConfigForGranterOrg(grant.getGrantorOrganization().getId(),
                        AppConfiguration.GRANT_ALERT_NOTIFICATION_MESSAGE).getConfigValue();

        String message = grantService.buildNotificationContent(grant, toStatus, notificationMessageTemplate);

        usersToNotify.stream().forEach(u -> notificationsService.saveNotification(message, u.getId()));

        //}

        grant.setActionAuthorities(workflowPermissionService
                .getGrantActionPermissions(grant.getGrantorOrganization().getId(),
                        user.getUserRoles(), grant.getGrantStatus().getId()));

        grant.setFlowAuthorities(workflowPermissionService
                .getGrantFlowPermissions(grant.getGrantorOrganization().getId(),
                        user.getUserRoles(), grant.getGrantStatus().getId()));
        GrantVO grantVO = new GrantVO().build(grant, grantService.getGrantSections(grant), workflowPermissionService, user,
                appConfigService.getAppConfigForGranterOrg(grant.getGrantorOrganization().getId(),
                        AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS));

        grant.setGrantDetails(grantVO.getGrantDetails());
        grant.setGrantTemplate(granterGrantTemplateService.findByTemplateId(grant.getTemplateId()));

        grant.getSubmissions().sort((a, b) -> a.getSubmitBy().compareTo(b.getSubmitBy()));
        grant.getGrantDetails().getSections().sort((a, b) -> Long.valueOf(a.getOrder()).compareTo(Long.valueOf(b.getOrder())));
        for (SectionVO sec : grant.getGrantDetails().getSections()) {
            if (sec.getAttributes() != null) {
                sec.getAttributes().sort((a, b) -> Long.valueOf(a.getAttributeOrder()).compareTo(Long.valueOf(b.getAttributeOrder())));
            }
        }
        return grant;

    }

    @PutMapping("/{grantId}/template/{templateId}/{templateName}")
    public Grant updateTemplateName(@PathVariable("userId") Long userId,
                                    @PathVariable("grantId") Long grantId,
                                    @PathVariable("templateId") Long templateId,
                                    @PathVariable("templateName") String templateName) {

        GranterGrantTemplate template = granterGrantTemplateService.findByTemplateId(templateId);
        template.setName(templateName);
        template.setPublished(true);
        grantService.saveGrantTemplate(template);

        Grant grant = grantService.getById(grantId);
        grant = _grantToReturn(userId, grant);
        return grant;

    }

    @PutMapping("/{grantId}/submission/flow/{fromState}/{toState}")
    public SubmissionVO saveAndMoveSubmissionState(@RequestBody SubmissionVO submissionVO,
                                                   @PathVariable("userId") Long userId,
                                                   @PathVariable("grantId") Long grantId, @PathVariable("fromState") Long fromStateId,
                                                   @PathVariable("toState") Long toStateId) {

        Submission submission = submissionService.getById(submissionVO.getId());

        for (GrantQuantitativeKpiData grantQuantitativeKpiData : submissionVO
                .getQuantitiaveKpisubmissions()) {
            GrantQuantitativeKpiData quantitativeKpiData = grantService
                    .getGrantQuantitativeKpiDataById(grantQuantitativeKpiData.getId());
            if (quantitativeKpiData.getActuals() != grantQuantitativeKpiData.getActuals()) {
                quantitativeKpiData.setActuals(grantQuantitativeKpiData.getActuals());
                grantService.saveGrantQunatitativeKpiData(quantitativeKpiData);
            }
        }
        for (GrantQualitativeKpiData grantQualitativeKpiData : submissionVO
                .getQualitativeKpiSubmissions()) {
            GrantQualitativeKpiData qualititativeKpiData = grantService
                    .getGrantQualitativeKpiDataById(grantQualitativeKpiData.getId());
            if (qualititativeKpiData.getActuals() != grantQualitativeKpiData.getActuals()) {
                qualititativeKpiData.setActuals(grantQualitativeKpiData.getActuals());
                grantService.saveGrantQualitativeKpiData(qualititativeKpiData);
            }
        }

        if (submission.getSubmissionStatus().getId() != toStateId) {
            submission.setSubmissionStatus(
                    workflowStatusService.findById(toStateId));
            Date now = DateTime.now().toDate();
            submission.setSubmittedOn(now);
            submission.setUpdatedBy(userService.getUserById(userId).getEmailId());
            submission.setUpdatedAt(now);

            submissionService.saveSubmission(submission);
        }

        Grant grant = grantService.getById(submission.getGrant().getId());
        grant.setSubstatus(workflowStatusService.findById(toStateId));
        grantService.saveGrant(grant);
        return new SubmissionVO()
                .build(submission, workflowPermissionService, userService.getUserById(userId),
                        appConfigService
                                .getAppConfigForGranterOrg(submission.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS));

    }

    /*private void saveSectionAndFieldsChanges(
            @RequestBody GrantVO grantToSave, Grant grant) {
        for (SectionVO sectionVO : grantToSave.getGrantDetails().getSections()) {
            GranterGrantSection grantSection = grantService.getGrantSectionBySectionId(sectionVO.getId());

            if (grantSection == null) {
                GranterGrantSection newSection = new GranterGrantSection();
                newSection.setDeletable(true);
                newSection.setGranter((Granter) grant.getGrantorOrganization());
                newSection.setSectionName(sectionVO.getName());
                newSection = grantService.saveSection(newSection);
                grantSection = newSection;
            }

            for (SectionAttributesVO sectionAttributesVO : sectionVO.getAttributes()) {
                GranterGrantSectionAttribute sectionAttribute = grantService
                        .getSectionAttributeByAttributeIdAndType(sectionAttributesVO.getId(),
                                sectionAttributesVO.getFieldType());

                if (sectionAttribute == null) {
                    GranterGrantSectionAttribute newSectionAttrib = new GranterGrantSectionAttribute();
                    newSectionAttrib.setDeletable(true);
                    newSectionAttrib.setFieldName(sectionAttributesVO.getFieldName());
                    newSectionAttrib.setFieldType(sectionAttributesVO.getFieldType());
                    newSectionAttrib.setGranter(grantSection.getGranter());
                    newSectionAttrib.setRequired(false);
                    newSectionAttrib.setSection(grantSection);

                    newSectionAttrib = grantService.saveSectionAttribute(newSectionAttrib);
                    sectionAttribute = newSectionAttrib;
                }

                switch (sectionAttributesVO.getFieldType()) {
                    case "string":
                        GrantStringAttribute grantStringAttribute = grantService
                                .findGrantStringBySectionAttribueAndGrant(sectionAttribute.getSection(),sectionAttribute,grant);

                        if (grantStringAttribute == null) {
                            GrantStringAttribute newGrantStringAttribute = new GrantStringAttribute();
                            newGrantStringAttribute.setValue(sectionAttributesVO.getFieldValue());
                            newGrantStringAttribute.setGrant(grant);
                            newGrantStringAttribute.setSection(grantSection);
                            newGrantStringAttribute.setSectionAttribute(sectionAttribute);

                            newGrantStringAttribute = grantService.saveStringAttribute(newGrantStringAttribute);
                            grantStringAttribute = newGrantStringAttribute;
                        }

                        if (!grantStringAttribute.getValue()
                                .equalsIgnoreCase(sectionAttributesVO.getFieldValue())) {
                            grantStringAttribute.setValue(sectionAttributesVO.getFieldValue());
                            grantService.saveStringAttribute(grantStringAttribute);
                        }
                        break;
                    case "document":
                        break;
                }
            }
        }
    }
*/

    @PostMapping(value = "/{grantId}/pdf")
    public PdfDocument getPDFExport(@PathVariable("userId") Long userId, @PathVariable("grantId") Long grantId, @RequestBody String htmlContent, HttpServletRequest request, HttpServletResponse response) {

        Grant grant = grantService.getById(grantId);
        String fileName = grant.getName().replaceAll("[^A-Za-z0-9]", "_") + ".pdf";

        PDDocument report = new PDDocument();
        File tempFile = null;
        User user = userService.getUserById(userId);

        try {

            //Add Document Properties
            PDDocumentInformation info = new PDDocumentInformation();
            info.setAuthor(user.getFirstName() + " " + user.getLastName());
            info.setTitle(grant.getName());
            info.setSubject("Grant Summary");

            //Add Page 1
            PDPage page = new PDPage();
            report.addPage(page);

            //Add content to page 1
            PDPageContentStream contentStream = new PDPageContentStream(report, page);
            contentStream.beginText();
            contentStream.newLineAtOffset(20, 700);
            contentStream.setFont(PDType1Font.HELVETICA, 10F);
            contentStream.setLeading(14.5f);

            _writeContent(contentStream, "Title", grant.getName());
            _writeContent(contentStream, "Generic", grant.getDescription());
            _writeContent(contentStream, "Label", "Grant Start:");
            _writeContent(contentStream, "Generic", new SimpleDateFormat("dd-MMM-yyyy").format(grant.getStartDate()));
            _writeContent(contentStream, "Label", "Grant End:");
            _writeContent(contentStream, "Generic", new SimpleDateFormat("dd-MMM-yyyy").format(grant.getEndDate()));

            GrantVO grantVO = new GrantVO();
            grantVO = grantVO.build(grant, grantService.getGrantSections(grant), workflowPermissionService, user, appConfigService
                    .getAppConfigForGranterOrg(grant.getGrantorOrganization().getId(),
                            AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS));
            grant.setGrantDetails(grantVO.getGrantDetails());
            for (SectionVO section : grant.getGrantDetails().getSections()) {
                _writeContent(contentStream, "Header", section.getName());

                for (SectionAttributesVO attributesVO : section.getAttributes()) {
                    _writeContent(contentStream, "Label", attributesVO.getFieldName());
                    _writeContent(contentStream, "Generic", attributesVO.getFieldValue());
                }
            }

            contentStream.endText();
            contentStream.close();

            PDPage submissionPage = new PDPage();
            report.addPage(submissionPage);

            contentStream = new PDPageContentStream(report, submissionPage);
            contentStream.beginText();
            contentStream.newLineAtOffset(20, 700);
            contentStream.setFont(PDType1Font.HELVETICA, 10F);
            contentStream.setLeading(14.5f);
            _writeContent(contentStream, "Header", "Submission Details");
            for (Submission submission : grant.getSubmissions()) {
                _writeContent(contentStream, "Generic", submission.getTitle() + " [" + submission.getSubmitBy() + "] [" + submission.getSubmissionStatus().getDisplayName() + "]");
            }

            contentStream.endText();
            contentStream.close();

            report.save(fileName);
            tempFile = new File(fileName);
            Resource file = resourceLoader.getResource("file:" + tempFile.getAbsolutePath());
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response
                    .setHeader("Content-Disposition", "attachment; filename=" + grant.getName() + ".pdf");
            byte[] imageBytes = new byte[(int) tempFile.length()];
            file.getInputStream().read(imageBytes, 0, imageBytes.length);
            file.getInputStream().close();
            String imageStr = Base64.getEncoder().encodeToString(imageBytes);
            PdfDocument pdfDocument = new PdfDocument();
            pdfDocument.setData(imageStr);

            return pdfDocument;
        } catch (IOException e) {
            logger.error(e.getMessage());
        } finally {
            try {
                report.close();
                tempFile.delete();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        /*File tempFile = null;
        FileOutputStream fos = null;
        try {
            tempFile = new File(grant.getName().replaceAll("[^A-Za-z0-9]", "_")+ ".pdf");
            fos = new FileOutputStream(tempFile);
            PdfRendererBuilder builder = new PdfRendererBuilder();

            Document doc = Jsoup.parse(htmlContent);
            doc.getElementsByClass("sidebar").remove();

            builder
                    .useFastMode()
                    .withW3cDocument(new W3CDom().fromJsoup(doc), request.getServletPath())
                    .toStream(fos)
                    .run();

            Resource file = resourceLoader.getResource("file:"+tempFile.getAbsolutePath());
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response
                    .setHeader("Content-Disposition", "attachment; filename=" + grant.getName()+".pdf");
            byte[] imageBytes = new byte[(int)tempFile.length()];
            file.getInputStream().read(imageBytes, 0, imageBytes.length);
            file.getInputStream().close();
            String imageStr = Base64.getEncoder().encodeToString(imageBytes);
            PdfDocument pdfDocument = new PdfDocument();
            pdfDocument.setData(imageStr);

            return pdfDocument;



        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }*/

        return null;
    }

    private void _writeContent(PDPageContentStream contentStream, String type, String content) {
        try {
            switch (type) {
                case "Title":
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16F);
                    contentStream.setNonStrokingColor(Color.BLACK);
                    break;
                case "Header":
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14F);
                    contentStream.setNonStrokingColor(Color.BLACK);
                    break;
                case "Label":
                    contentStream.setFont(PDType1Font.HELVETICA, 10F);
                    contentStream.setNonStrokingColor(Color.GRAY);
                    break;
                case "Generic":
                    contentStream.setFont(PDType1Font.HELVETICA, 10F);
                    contentStream.setNonStrokingColor(Color.BLACK);
                    break;
                default:
                    contentStream.setNonStrokingColor(Color.BLACK);
            }

            content = WordUtils.wrap(content, 120);
            String[] splitContent = content.split("\n");
            for (int i = 0; i < splitContent.length; i++) {
                contentStream.showText(splitContent[i]);
                contentStream.newLine();
            }

            switch (type) {
                case "Header":
                    contentStream.newLine();
                    break;
                case "Generic":
                    contentStream.newLine();
                    break;
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }


    @GetMapping("/templates")
    public List<GranterGrantTemplate> getTenantPublishedGrantTemplates(@RequestHeader("X-TENANT-CODE") String tenantCode) {
        return granterGrantTemplateService.findByGranterIdAndPublishedStatus(organizationService.findOrganizationByTenantCode(tenantCode).getId(), true);
    }
}
