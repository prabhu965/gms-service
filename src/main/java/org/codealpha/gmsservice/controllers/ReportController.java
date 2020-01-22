package org.codealpha.gmsservice.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FilenameUtils;
import org.codealpha.gmsservice.constants.AppConfiguration;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/{userId}/report")
@Api(value = "Reports", description = "API end points for Reports", tags = {"Grants"})
public class ReportController {

    private static Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private UserService userService;
    @Autowired
    private WorkflowStatusService workflowStatusService;
    @Autowired
    private GrantService grantService;
    @Autowired
    private TemplateLibraryService templateLibraryService;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    ReportSnapshotService reportSnapshotService;
    @Value("${spring.upload-file-location}")
    private String uploadLocation;
    @Value("${spring.supported-file-types}")
    private String[] supportedFileTypes;
    @Autowired
    private WorkflowStatusTransitionService workflowStatusTransitionService;
    @Autowired
    private AppConfigService appConfigService;
    @Autowired
    private CommonEmailSevice commonEmailSevice;
    @Autowired
    private NotificationsService notificationsService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserRoleService userRoleService;

    @GetMapping("/")
    public List<Report> getAllReports(@PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode) {
        Organization tenantOrg = organizationService.findOrganizationByTenantCode(tenantCode);
        List<Report> reports = reportService.getAllAssignedReportsForUser(userId, tenantOrg.getId());
        for (Report report : reports) {

            report = _ReportToReturn(report, userId);

        }
        return reports;
    }

    @GetMapping("/{reportId}")
    public Report getAllReports(@PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode, @PathVariable("reportId") Long reportId) {
        Organization tenantOrg = organizationService.findOrganizationByTenantCode(tenantCode);
        Report report = reportService.getReportById(reportId);

        return _ReportToReturn(report, userId);
    }

    private Report _ReportToReturn(Report report, Long userId) {

        report.setStringAttributes(reportService.getReportStringAttributesForReport(report));

        List<ReportAssignmentsVO> workflowAssignments = new ArrayList<>();
        for (ReportAssignment assignment : reportService.getAssignmentsForReport(report)) {
            ReportAssignmentsVO assignmentsVO = new ReportAssignmentsVO();
            assignmentsVO.setId(assignment.getId());
            assignmentsVO.setAnchor(assignment.isAnchor());
            assignmentsVO.setAssignmentId(assignment.getAssignment());
            if (assignment.getAssignment() != null && assignment.getAssignment() > 0) {
                assignmentsVO.setAssignmentUser(userService.getUserById(assignment.getAssignment()));
            }
            assignmentsVO.setReportId(assignment.getReportId());
            assignmentsVO.setStateId(assignment.getStateId());
            assignmentsVO.setStateName(workflowStatusService.findById(assignment.getStateId()));
            workflowAssignments.add(assignmentsVO);
        }
        report.setWorkflowAssignments(workflowAssignments);
        List<ReportAssignment> reportAssignments = reportService.getAssignmentsForReport(report);
        if (reportAssignments.stream().filter(ass -> ass.getAssignment() == userId && ass.getStateId() == report.getStatus().getId()).findAny().isPresent()) {
            report.setCanManage(true);
        } else {
            report.setCanManage(false);
        }
        if (reportAssignments != null) {
            for (ReportAssignment assignment : reportAssignments) {
                if (report.getCurrentAssignment() == null) {
                    List<AssignedTo> assignedToList = new ArrayList<>();
                    report.setCurrentAssignment(assignedToList);
                }
                AssignedTo newAssignedTo = new AssignedTo();
                if (assignment.getAssignment() != null && assignment.getAssignment() > 0) {
                    newAssignedTo.setUser(userService.getUserById(assignment.getAssignment()));
                }
                report.getCurrentAssignment().add(newAssignedTo);
            }
        }

        ReportVO reportVO = new ReportVO().build(report, reportService.getReportSections(report), userService, reportService);
        report.setReportDetails(reportVO.getReportDetails());

        report.setNoteAddedBy(reportVO.getNoteAddedBy());
        report.setNoteAddedByUser(reportVO.getNoteAddedByUser());

        report.getWorkflowAssignments().sort((a, b) -> a.getId().compareTo(b.getId()));
        report.getReportDetails().getSections().sort((a, b) -> Long.valueOf(a.getOrder()).compareTo(Long.valueOf(b.getOrder())));
        for (SectionVO section : report.getReportDetails().getSections()) {
            if (section.getAttributes() != null) {
                section.getAttributes().sort((a, b) -> Long.valueOf(a.getAttributeOrder()).compareTo(Long.valueOf(b.getAttributeOrder())));
            }
        }

        report.setGranteeUsers(userService.getAllGranteeUsers(report.getGrant().getOrganization()));

        report.setSecurityCode(reportService.buildHashCode(report));
        report.setFlowAuthorities(reportService.getFlowAuthority(report, userId));
        return report;
    }


    @PutMapping("/{reportId}")
    @ApiOperation("Save report")
    public Report saveReport(@ApiParam(name = "grantId", value = "Unique identifier of report") @PathVariable("reportId") Long reportId, @ApiParam(name = "reportToSave", value = "Report to save in edit mode, passed in Body of request") @RequestBody Report reportToSave, @ApiParam(name = "userId", value = "Unique identifier of logged in user") @PathVariable("userId") Long userId,
                             @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {

        //grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);


        Organization tenantOrg = organizationService.findOrganizationByTenantCode(tenantCode);
        User user = userService.getUserById(userId);
        Report report = _processReport(reportToSave, tenantOrg, user);

        report = _ReportToReturn(reportToSave, userId);
        return report;
    }

    private Report _processReport(Report reportToSave, Organization tenantOrg, User user) {
        Report report = reportService.getReportById(reportToSave.getId());

        report.setStartDate(reportToSave.getStartDate());
        report.setName(reportToSave.getName());
        report.setEndDate(reportToSave.getEndDate());
        report.setDueDate(reportToSave.getDueDate());
        report.setUpdatedAt(DateTime.now().toDate());
        report.setUpdatedBy(user.getId());

        _processStringAttributes(report, reportToSave, tenantOrg);

        report = reportService.saveReport(report);

        return report;
    }

    private void _processStringAttributes(Report grant, Report reportToSave, Organization tenant) {
        List<ReportStringAttribute> stringAttributes = new ArrayList<>();
        ReportSpecificSection reportSpecificSection = null;

        for (SectionVO sectionVO : reportToSave.getReportDetails().getSections()) {
            reportSpecificSection = reportService.getReportSpecificSectionById(sectionVO.getId());

            reportSpecificSection.setSectionName(sectionVO.getName());
            reportSpecificSection.setSectionOrder(sectionVO.getOrder());
            reportSpecificSection.setGranter((Granter) tenant);
            reportSpecificSection.setDeletable(true);

            reportSpecificSection = reportService.saveReportSpecificSection(reportSpecificSection);

            ReportSpecificSectionAttribute sectionAttribute = null;

            if (sectionVO.getAttributes() != null) {
                for (SectionAttributesVO sectionAttributesVO : sectionVO.getAttributes()) {

                    sectionAttribute = reportService.getReportStringByStringAttributeId(sectionAttributesVO.getId()).getSectionAttribute();

                    sectionAttribute.setFieldName(sectionAttributesVO.getFieldName());
                    sectionAttribute.setFieldType(sectionAttributesVO.getFieldType());
                    sectionAttribute.setGranter((Granter) tenant);
                    sectionAttribute.setAttributeOrder(sectionAttributesVO.getAttributeOrder());
                    sectionAttribute.setRequired(true);
                    sectionAttribute.setSection(reportSpecificSection);

                    sectionAttribute = reportService.saveReportSpecificSectionAttribute(sectionAttribute);


                    ReportStringAttribute reportStringAttribute = reportService.getReportStringAttributeBySectionAttributeAndSection(sectionAttribute, reportSpecificSection);

                    reportStringAttribute.setTarget(sectionAttributesVO.getTarget());
                    reportStringAttribute.setFrequency(sectionAttributesVO.getFrequency());
                    reportStringAttribute.setActualTarget(sectionAttributesVO.getActualTarget());
                    if (sectionAttribute.getFieldType().equalsIgnoreCase("table")) {
                        List<TableData> tableData = sectionAttributesVO.getFieldTableValue();
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            reportStringAttribute.setValue(mapper.writeValueAsString(tableData));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                    } else {
                        reportStringAttribute.setValue(sectionAttributesVO.getFieldValue());
                    }
                    reportService.saveReportStringAttribute(reportStringAttribute);
                }
            }
        }
    }

    @PostMapping("/{reportId}/section/{sectionId}/field")
    @ApiOperation("Added new field to section")
    public ReportFieldInfo createFieldInSection(@ApiParam(name = "reportToSave", value = "Report to save if in edit mode passed in Body of request") @RequestBody Report reportToSave, @ApiParam(name = "reportId", value = "Unique identifier of the grant") @PathVariable("reportId") Long reportId, @ApiParam(name = "sectionId", value = "Unique identifier of the section to which the field is being added") @PathVariable("sectionId") Long sectionId, @ApiParam(name = "userId", value = "Unique identifier of the logged in user") @PathVariable("userId") Long userId, @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        //grantService.saveGrant(grantToSave);
        /*grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);
        grantValidator.validateSectionExists(grantService,grantToSave,sectionId);*/
        saveReport(reportId, reportToSave, userId, tenantCode);
        Report report = reportService.getReportById(reportId);
        ReportSpecificSection reportSection = reportService.getReportSpecificSectionById(sectionId);

        ReportSpecificSectionAttribute newSectionAttribute = new ReportSpecificSectionAttribute();
        newSectionAttribute.setSection(reportSection);
        newSectionAttribute.setRequired(false);
        newSectionAttribute.setFieldType("multiline");
        newSectionAttribute.setFieldName("");
        newSectionAttribute.setDeletable(true);
        newSectionAttribute.setCanEdit(true);
        newSectionAttribute.setAttributeOrder(reportService.getNextAttributeOrder(organizationService.findOrganizationByTenantCode(tenantCode).getId(), sectionId));
        newSectionAttribute.setGranter((Granter) organizationService.findOrganizationByTenantCode(tenantCode));
        newSectionAttribute = reportService.saveReportSpecificSectionAttribute(newSectionAttribute);
        ReportStringAttribute stringAttribute = new ReportStringAttribute();
        stringAttribute.setValue("");
        stringAttribute.setSectionAttribute(newSectionAttribute);
        stringAttribute.setSection(reportSection);
        stringAttribute.setReport(report);


        stringAttribute = reportService.saveReportStringAttribute(stringAttribute);

        if (_checkIfReportTemplateChanged(report, reportSection, newSectionAttribute)) {
            _createNewReportTemplateFromExisiting(report);
        }

        report = _ReportToReturn(report, userId);
        return new ReportFieldInfo(newSectionAttribute.getId(), stringAttribute.getId(), report);
    }

    private Boolean _checkIfReportTemplateChanged(Report report, ReportSpecificSection newSection, ReportSpecificSectionAttribute newAttribute) {
        GranterReportTemplate currentReportTemplate = reportService.findByTemplateId(report.getTemplate().getId());
        for (GranterReportSection reportSection : currentReportTemplate.getSections()) {
            if (!reportSection.getSectionName().equalsIgnoreCase(newSection.getSectionName())) {
                return true;
            }
            if (newAttribute != null) {
                for (GranterReportSectionAttribute sectionAttribute : reportSection.getAttributes()) {
                    if (!sectionAttribute.getFieldName().equalsIgnoreCase(newAttribute.getFieldName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private GranterReportTemplate _createNewReportTemplateFromExisiting(Report report) {
        GranterReportTemplate currentReportTemplate = reportService.findByTemplateId(report.getTemplate().getId());
        GranterReportTemplate newTemplate = null;
        if (!currentReportTemplate.isPublished()) {
            reportService.deleteReportTemplate(currentReportTemplate);
        }
        newTemplate = new GranterReportTemplate();
        newTemplate.setName("Custom Template");
        newTemplate.setGranterId(report.getGrant().getGrantorOrganization().getId());
        newTemplate.setPublished(false);
        newTemplate = reportService.saveReportTemplate(newTemplate);


        List<GranterReportSection> newSections = new ArrayList<>();
        for (ReportSpecificSection currentSection : reportService.getReportSections(report)) {
            GranterReportSection newSection = new GranterReportSection();
            newSection.setSectionOrder(currentSection.getSectionOrder());
            newSection.setSectionName(currentSection.getSectionName());
            newSection.setReportTemplate(newTemplate);
            newSection.setGranter((Granter) report.getGrant().getGrantorOrganization());
            newSection.setDeletable(currentSection.getDeletable());

            newSection = reportService.saveReportTemplateSection(newSection);
            newSections.add(newSection);

            currentSection.setReportTemplateId(newTemplate.getId());
            currentSection = reportService.saveSection(currentSection);

            for (ReportSpecificSectionAttribute currentAttribute : reportService.getSpecificSectionAttributesBySection(currentSection)) {
                GranterReportSectionAttribute newAttribute = new GranterReportSectionAttribute();
                newAttribute.setDeletable(currentAttribute.getDeletable());
                newAttribute.setFieldName(currentAttribute.getFieldName());
                newAttribute.setFieldType(currentAttribute.getFieldType());
                newAttribute.setGranter((Granter) currentAttribute.getGranter());
                newAttribute.setRequired(currentAttribute.getRequired());
                newAttribute.setAttributeOrder(currentAttribute.getAttributeOrder());
                newAttribute.setSection(newSection);
                if (currentAttribute.getFieldType().equalsIgnoreCase("table")) {
                    ReportStringAttribute stringAttribute = reportService.getReportStringAttributeBySectionAttributeAndSection(currentAttribute, currentSection);

                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        List<TableData> tableData = mapper.readValue(stringAttribute.getValue(), new TypeReference<List<TableData>>() {
                        });
                        for (TableData data : tableData) {
                            for (ColumnData columnData : data.getColumns()) {
                                columnData.setValue("");
                            }
                        }
                        newAttribute.setExtras(mapper.writeValueAsString(tableData));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                newAttribute = reportService.saveReportTemplateSectionAttribute(newAttribute);

            }
        }

        newTemplate.setSections(newSections);
        newTemplate = reportService.saveReportTemplate(newTemplate);

        //grant = grantService.getById(grant.getId());
        report.setTemplate(newTemplate);
        reportService.saveReport(report);
        return newTemplate;
    }

    @PutMapping("/{reportId}/section/{sectionId}/field/{fieldId}")
    @ApiOperation("Update field information")
    public ReportFieldInfo updateField(@ApiParam(name = "sectionId", value = "Unique identifier of section") @PathVariable("sectionId") Long sectionId, @ApiParam(name = "attributeToSave", value = "Updated attribute to be saved") @RequestBody ReportAttributeToSaveVO attributeToSave, @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId, @ApiParam(name = "fieldId", value = "Unique identifier of the field being updated") @PathVariable("fieldId") Long fieldId, @ApiParam(name = "userId", value = "Unique identifier of the logged in user") @PathVariable("userId") Long userId, @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        /*grantValidator.validate(grantService,grantId,attributeToSave.getGrant(),userId,tenantCode);
        grantValidator.validateSectionExists(grantService,attributeToSave.getGrant(),sectionId);
        grantValidator.validateFieldExists(grantService,attributeToSave.getGrant(),sectionId,fieldId);*/
        Report report = saveReport(reportId, attributeToSave.getReport(), userId, tenantCode);
        ReportSpecificSectionAttribute currentAttribute = reportService.getReportStringByStringAttributeId(fieldId).getSectionAttribute();
        currentAttribute.setFieldName(attributeToSave.getAttr().getFieldName());
        currentAttribute.setFieldType(attributeToSave.getAttr().getFieldType());
        currentAttribute = reportService.saveReportSpecificSectionAttribute(currentAttribute);
        ReportStringAttribute stringAttribute = reportService.getReportStringAttributeBySectionAttributeAndSection(currentAttribute, currentAttribute.getSection());
        stringAttribute.setValue("");
        if (currentAttribute.getFieldType().equalsIgnoreCase("kpi")) {
            stringAttribute.setFrequency(report.getType().toLowerCase());
        }
        stringAttribute = reportService.saveReportStringAttribute(stringAttribute);

        report = reportService.getReportById(reportId);
        if (_checkIfReportTemplateChanged(report, currentAttribute.getSection(), currentAttribute)) {
            _createNewReportTemplateFromExisiting(report);
        }


        report = _ReportToReturn(report, userId);
        return new ReportFieldInfo(currentAttribute.getId(), stringAttribute.getId(), report);
    }

    @PostMapping("/{reportId}/field/{fieldId}/template/{templateId}")
    @ApiOperation(value = "Attach document to field", notes = "Valid for Document field types only")
    public ReportDocInfo createDocumentForReportSectionField(@ApiParam(name = "reportToSave", value = "Report to save in edit mode, passed in Body of request") @RequestBody Report reportToSave, @ApiParam(name = "userId", value = "Unique identifier of logged in user") @PathVariable("userId") Long userId, @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId, @ApiParam(name = "fieldId", value = "Unique identifier of the field to which document is being attached") @PathVariable("fieldId") Long fieldId, @ApiParam(name = "temaplteId", value = "Unique identified of the document template being attached") @PathVariable("templateId") Long templateId, @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        saveReport(reportId, reportToSave, userId, tenantCode);
        TemplateLibrary libraryDoc = templateLibraryService.getTemplateLibraryDocumentById(templateId);

        ReportStringAttribute stringAttribute = reportService.getReportStringByStringAttributeId(fieldId);

        Resource file = null;
        String filePath = null;
        try {
            file = resourceLoader
                    .getResource("file:" + uploadLocation + URLDecoder.decode(libraryDoc.getLocation(), "UTF-8"));
            filePath = uploadLocation + tenantCode + "/report-documents/" + reportId + "/" + stringAttribute.getSection().getId() + "/" + stringAttribute.getSectionAttribute().getId() + "/";

            File dir = new File(filePath);
            dir.mkdirs();
            File fileToCreate = new File(dir, libraryDoc.getName() + "." + libraryDoc.getType());
            FileWriter newJsp = new FileWriter(fileToCreate);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ReportStringAttributeAttachments attachment = new ReportStringAttributeAttachments();
        attachment.setCreatedBy(userService.getUserById(userId).getEmailId());
        attachment.setCreatedOn(new Date());
        attachment.setDescription(libraryDoc.getDescription());
        attachment.setReportStringAttribute(stringAttribute);
        attachment.setLocation(filePath);
        attachment.setName(libraryDoc.getName());
        attachment.setTitle("");
        attachment.setType(libraryDoc.getType());
        attachment.setVersion(1);
        attachment = reportService.saveReportStringAttributeAttachment(attachment);

        ObjectMapper mapper = new ObjectMapper();
        try {
            List<ReportStringAttributeAttachments> stringAttributeAttachments = reportService.getStringAttributeAttachmentsByStringAttribute(stringAttribute);
            stringAttribute.setValue(mapper.writeValueAsString(stringAttributeAttachments));
            stringAttribute = reportService.saveReportStringAttribute(stringAttribute);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        Report report = reportService.getReportById(reportId);
        report = _ReportToReturn(report, userId);
        return new ReportDocInfo(attachment.getId(), report);
    }

    @PostMapping(value = "/{reportId}/section/{sectionId}/attribute/{attributeId}/upload", consumes = {"multipart/form-data"})
    @ApiOperation("Upload and attach files to Document field from disk")
    public ReportDocInfo saveUploadedFiles(@ApiParam(name = "sectionId", value = "Unique identifier of section") @PathVariable("sectionId") Long sectionId, @ApiParam(name = "userId", value = "Unique identifier of logged in user") @PathVariable("userId") Long userId, @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId, @ApiParam(name = "attributeId", value = "Unique identifier of the document field") @PathVariable("attributeId") Long attributeId, @ApiParam(name = "reportData", value = "Report data") @RequestParam("reportToSave") String reportToSaveStr, @RequestParam("file") MultipartFile[] files, @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Report reportToSave = null;
        try {
            reportToSave = mapper.readValue(reportToSaveStr, Report.class);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        /*grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);
        grantValidator.validateSectionExists(grantService,grantToSave,sectionId);
        grantValidator.validateFieldExists(grantService,grantToSave,sectionId,attributeId);
        grantValidator.validateFiles(files,supportedFileTypes);*/

        Report report = reportService.getReportById(reportId);

        ReportStringAttribute attr = reportService.getReportStringByStringAttributeId(attributeId);
        String filePath = uploadLocation + tenantCode + "/report-documents/" + reportId + "/" + attr.getSection().getId() + "/" + attr.getSectionAttribute().getId() + "/";
        File dir = new File(filePath);
        dir.mkdirs();
        List<DocInfo> docInfos = new ArrayList<>();
        List<ReportStringAttributeAttachments> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                File fileToCreate = new File(dir, file.getOriginalFilename());
                file.transferTo(fileToCreate);
                //FileWriter newJsp = new FileWriter(fileToCreate);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ReportStringAttributeAttachments attachment = new ReportStringAttributeAttachments();
            attachment.setVersion(1);
            attachment.setType(FilenameUtils.getExtension(file.getOriginalFilename()));
            attachment.setTitle(file.getOriginalFilename().replace("." + FilenameUtils.getExtension(file.getOriginalFilename()), ""));
            attachment.setLocation(filePath);
            attachment.setName(file.getOriginalFilename().replace("." + FilenameUtils.getExtension(file.getOriginalFilename()), ""));
            attachment.setReportStringAttribute(attr);
            attachment.setDescription(file.getOriginalFilename().replace("." + FilenameUtils.getExtension(file.getOriginalFilename()), ""));
            attachment.setCreatedOn(new Date());
            attachment.setCreatedBy(userService.getUserById(userId).getEmailId());
            attachment = reportService.saveReportStringAttributeAttachment(attachment);
            attachments.add(attachment);
        }

        mapper = new ObjectMapper();
        try {
            if (attr.getValue().equalsIgnoreCase("")) {
                attr.setValue("[]");
            }
            List<ReportStringAttributeAttachments> currentAttachments = mapper.readValue(attr.getValue(), new TypeReference<List<ReportStringAttributeAttachments>>() {
            });
            if (currentAttachments == null) {
                currentAttachments = new ArrayList<>();
            }
            currentAttachments.addAll(attachments);

            attr.setValue(mapper.writeValueAsString(currentAttachments));
            attr = reportService.saveReportStringAttribute(attr);
            ReportStringAttribute finalAttr = attr;
            ReportStringAttribute finalAttr1 = finalAttr;
            finalAttr = report.getStringAttributes().stream().filter(g -> g.getId() == finalAttr1.getId()).findFirst().get();
            finalAttr.setValue(mapper.writeValueAsString(currentAttachments));
            reportService.saveReport(report);

        } catch (IOException e) {
            e.printStackTrace();
        }


        report = reportService.getReportById(reportId);
        report = _ReportToReturn(report, userId);

        return new ReportDocInfo(attachments.get(attachments.size() - 1).getId(), report);
    }


    @PostMapping("/{reportId}/template/{templateId}/section/{sectionName}")
    @ApiOperation("Create new section in grant")
    public ReportSectionInfo createSection(@RequestBody Report reportToSave, @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId, @ApiParam(name = "templateId", value = "Unique identifier of the report template") @PathVariable("templateId") Long templateId, @ApiParam(name = "sectionName", value = "Name of the new section") @PathVariable("sectionName") String sectionName, @ApiParam(name = "userId", value = "Unique identifier of the logged in user") @PathVariable("userId") Long userId, @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
       /* grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);
        grantValidator.validateTemplateExists(grantService,grantToSave,templateId);*/
        Report report = saveReport(reportId, reportToSave, userId, tenantCode);

        ReportSpecificSection specificSection = new ReportSpecificSection();
        specificSection.setGranter((Granter) organizationService.findOrganizationByTenantCode(tenantCode));
        specificSection.setSectionName(sectionName);

        specificSection.setReportTemplateId(templateId);
        specificSection.setDeletable(true);
        specificSection.setReportId(reportId);
        specificSection.setSectionOrder(reportService.getNextSectionOrder(organizationService.findOrganizationByTenantCode(tenantCode).getId(), templateId));
        specificSection = reportService.saveSection(specificSection);

        if (_checkIfReportTemplateChanged(report, specificSection, null)) {
            GranterReportTemplate newTemplate = _createNewReportTemplateFromExisiting(report);
            templateId = newTemplate.getId();
        }

        report = _ReportToReturn(report, userId);
        return new ReportSectionInfo(specificSection.getId(), specificSection.getSectionName(), report);

    }

    @PutMapping("/{reportId}/template/{templateId}/section/{sectionId}")
    @ApiOperation("Delete existing section in report")
    public Report deleteSection(@RequestBody Report reportToSave, @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId, @ApiParam(name = "templateId", value = "Unique identifier of the grant template") @PathVariable("templateId") Long templateId, @ApiParam(name = "sectionId", value = "Unique identifier of the section being deleted") @PathVariable("sectionId") Long sectionId, @ApiParam(name = "userId", value = "Unique identifier of the logged in user") @PathVariable("userId") Long userId, @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        /*grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);
        grantValidator.validateTemplateExists(grantService,grantToSave,templateId);
        grantValidator.validateSectionExists(grantService,grantToSave,sectionId);*/
        ReportSpecificSection section = reportService.getReportSpecificSectionById(sectionId);
        Report report = reportService.getReportById(reportId);

        List<ReportStringAttribute> newStringAttribsList = new ArrayList<>();
        for (ReportSpecificSectionAttribute attrib : reportService.getSpecificSectionAttributesBySection(section)) {
            for (ReportStringAttribute stringAttrib : reportService.getReportStringAttributesByAttribute(attrib)) {
                if (stringAttrib != null) {
                    reportService.deleteStringAttribute(stringAttrib);

                    report.getStringAttributes().removeIf(e -> e.getId() == stringAttrib.getId());
                }
            }
        }

        reportService.deleteSectionAttributes(reportService.getSpecificSectionAttributesBySection(section));
        reportService.deleteSection(section);

        report = reportService.getReportById(reportId);
        if (_checkIfReportTemplateChanged(report, section, null)) {
            GranterReportTemplate newTemplate = _createNewReportTemplateFromExisiting(report);
            templateId = newTemplate.getId();
        }
        report = _ReportToReturn(report, userId);
        return report;
    }

    @PostMapping("/{reportId}/assignment")
    @ApiOperation("Set owners for report workflow states")
    public Report saveReportAssignments(@ApiParam(name = "userId", value = "Unique identifier of logged in user") @PathVariable("userId") Long userId, @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId, @ApiParam(name = "assignmentModel", value = "Set assignment for report per workflow state") @RequestBody ReportAssignmentModel assignmentModel, @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        Report report = saveReport(reportId, assignmentModel.getReport(), userId, tenantCode);
        String customAss = null;
        UriComponents uriComponents = ServletUriComponentsBuilder.fromCurrentContextPath().build();
        String host = uriComponents.getHost().substring(uriComponents.getHost().indexOf(".")+1);
        UriComponentsBuilder uriBuilder =  UriComponentsBuilder.newInstance().scheme(uriComponents.getScheme()).host(host).port(uriComponents.getPort());
        String url = uriBuilder.toUriString();
        for (ReportAssignmentsVO assignmentsVO : assignmentModel.getAssignments()) {
            if(customAss==null && assignmentsVO.getCustomAssignments()!=null){
                customAss = assignmentsVO.getCustomAssignments();
            }
            ReportAssignment assignment = null;
            if (assignmentsVO.getId() == null) {
                assignment = new ReportAssignment();
                assignment.setStateId(assignmentsVO.getStateId());
                assignment.setReportId(assignmentsVO.getReportId());
            } else {
                assignment = reportService.getReportAssignmentById(assignmentsVO.getId());
            }

            assignment.setAssignment(assignmentsVO.getAssignmentId());

            if((customAss!=null && !"".equalsIgnoreCase(customAss.trim()))  && workflowStatusService.getById(assignmentsVO.getStateId()).getInternalStatus().equalsIgnoreCase("ACTIVE")){
                String[] customAssignments = customAss.split(",");
                User granteeUser = null;
                User existingUser = userService.getUserByEmailAndOrg(customAss,report.getGrant().getOrganization());
                ObjectMapper mapper = new ObjectMapper();
                String code = null;

                    code = Base64.getEncoder().encodeToString(new byte[]{report.getId().byteValue()});

                if(existingUser != null && existingUser.isActive()){
                    granteeUser = existingUser;
                    url = url+"/home/?action=login&org="+URLEncoder.encode(report.getGrant().getOrganization().getName())+"&r=" + code+"&email="+granteeUser.getEmailId()+"&type=report";
                }else if(existingUser!=null && !existingUser.isActive()){
                    granteeUser = existingUser;
                    url = url+"/home/?action=registration&org="+ URLEncoder.encode(report.getGrant().getOrganization().getName())+"&r=" + code+"&email="+granteeUser.getEmailId()+"&type=report";

                } else {
                    granteeUser = new User();
                    Role newRole = roleService.findByOrganizationAndName(report.getGrant().getOrganization(), "Admin");


                    UserRole userRole = new UserRole();
                    userRole.setRole(newRole);
                    userRole.setUser(granteeUser);

                    List<UserRole> userRoles = new ArrayList<>();
                    userRoles.add(userRole);
                    granteeUser.setUserRoles(userRoles);
                    granteeUser.setFirstName("To be set");
                    granteeUser.setLastName("To be set");
                    granteeUser.setEmailId(customAss);
                    granteeUser.setOrganization(report.getGrant().getOrganization());
                    granteeUser.setActive(false);
                    granteeUser = userService.save(granteeUser);
                    userRole = userRoleService.saveUserRole(userRole);
                    url = url+"/home/?action=registration&org="+URLEncoder.encode(report.getGrant().getOrganization().getName())+"&r=" + code+"&email="+granteeUser.getEmailId()+"&type=report";
                }

                String[] notifications = reportService.buildReportInvitationContent(report,
                        userService.getUserById(userId),
                        appConfigService.getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(), AppConfiguration.REPORT_INVITE_SUBJECT).getConfigValue(),
                        appConfigService.getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(), AppConfiguration.REPORT_INVITE_MESSAGE).getConfigValue(),
                        url);
                commonEmailSevice.sendMail(granteeUser.getEmailId(),notifications[0],notifications[1],new String[]{appConfigService.getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(),AppConfiguration.PLATFORM_EMAIL_FOOTER).getConfigValue()});

                if(assignmentsVO.getAssignmentId()==0){
                    assignment.setAssignment(granteeUser.getId());
                }else{
                    ReportAssignment ass = new ReportAssignment();
                    ass.setAssignment(granteeUser.getId());
                    ass.setReportId(reportId);
                    ass.setStateId(assignmentsVO.getStateId());
                    ass.setAnchor(false);
                    reportService.saveAssignmentForReport(ass);
                }
                /*for (String customAssignment : customAssignments) {
                    User granteeUser = new User();
                    Role newRole = new Role();
                    newRole.setName("Admin");
                    newRole.setOrganization(report.getGrant().getOrganization());
                    newRole.setCreatedAt(DateTime.now().toDate());
                    newRole.setCreatedBy("System");

                    newRole = roleService.saveRole(newRole);
                    UserRole userRole = new UserRole();
                    userRole.setRole(newRole);
                    userRole.setUser(granteeUser);
                    List<UserRole> userRoles = new ArrayList<>();
                    userRoles.add(userRole);
                    granteeUser.setUserRoles(userRoles);
                    granteeUser.setFirstName("To be set");
                    granteeUser.setLastName("To be set");
                    granteeUser.setEmailId(customAss);
                    granteeUser.setOrganization(report.getGrant().getOrganization());
                    granteeUser = userService.save(granteeUser);

                    if(assignmentsVO.getAssignmentId()==0){
                        assignment.setAssignment(granteeUser.getId());
                    }else{
                        ReportAssignment ass = new ReportAssignment();
                        ass.setAssignment(granteeUser.getId());
                        ass.setReportId(reportId);
                        ass.setStateId(assignmentsVO.getStateId());
                        ass.setAnchor(false);
                        reportService.saveAssignmentForReport(ass);
                    }
                }*/
            }

            assignment = reportService.saveAssignmentForReport(assignment);
        }



        report = _ReportToReturn(report, userId);
        return report;
    }

    @GetMapping("{reportId}/changeHistory")
    public ReportSnapshot getReportHistory(@PathVariable("reportId") Long reportId, @PathVariable("userId") Long userId) {
        Report report = reportService.getReportById(reportId);

        return reportSnapshotService.getSnapshotByReportIdAndAssignedToIdAndStatusId(reportId, userId, report.getStatus().getId());
    }

    @PostMapping("/{reportId}/flow/{fromState}/{toState}")
    @ApiOperation("Move report through workflow")
    public Report MoveReportState(@RequestBody ReportWithNote reportWithNote, @ApiParam(name = "userId", value = "Unique identified of logged in user") @PathVariable("userId") Long userId,
                                  @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId, @ApiParam(name = "fromStateId", value = "Unique identifier of the starting state of the report in the workflow") @PathVariable("fromState") Long fromStateId,
                                  @ApiParam(name = "toStateId", value = "Unique identifier of the ending state of the report in the workflow") @PathVariable("toState") Long toStateId,
                                  @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {

        /*grantValidator.validate(grantService,grantId,reportWithNote.getGrant(),userId,tenantCode);
        grantValidator.validateFlow(grantService,reportWithNote.getGrant(),grantId,userId,fromStateId,toStateId);*/

        Report report = reportService.getReportById(reportId);
        Report finalReport = report;
        WorkflowStatus previousState = report.getStatus();

        User previousOwner = userService.getUserById(reportService.getAssignmentsForReport(report).stream().filter(ass -> ass.getReportId().longValue() == reportId.longValue() && ass.getStateId().longValue() == finalReport.getStatus().getId().longValue()).collect(Collectors.toList()).get(0).getAssignment());
        report.setStatus(workflowStatusService.findById(toStateId));
        if (reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")) {
            report.setNote(reportWithNote.getNote());
            report.setNoteAdded(new Date());
            report.setNoteAddedBy(userId);
        }
        report.setUpdatedAt(DateTime.now().toDate());
        report.setUpdatedBy(userId);
        report = reportService.saveReport(report);

        User user = userService.getUserById(userId);
        WorkflowStatus toStatus = workflowStatusService.findById(toStateId);

        List<User> usersToNotify = new ArrayList<>();//userService.usersToNotifyOnWorkflowSateChangeTo(toStateId);

        List<ReportAssignment> assigments = reportService.getAssignmentsForReport(report);
        assigments.forEach(ass -> usersToNotify.add(userService.getUserById(ass.getAssignment())));

        Optional<ReportAssignment> repAss = reportService.getAssignmentsForReport(report).stream().filter(ass -> ass.getReportId().longValue() == reportId.longValue() && ass.getStateId().longValue() == toStateId.longValue()).findAny();
        User currentOwner = null;
        String currentOwnerName = "";
        if (repAss.isPresent()) {
            currentOwner = userService.getUserById(repAss.get().getAssignment());
            currentOwnerName = currentOwner.getFirstName().concat(" ").concat(currentOwner.getLastName());
        }

        WorkflowStatusTransition transition = workflowStatusTransitionService.findByFromAndToStates(previousState, toStatus);

        String notificationContent[] = reportService.buildNotificationContent(finalReport, user.getFirstName().concat(" ").concat(user.getLastName()), toStatus.getVerb(), new SimpleDateFormat("dd-MMM-yyyy").format(DateTime.now().toDate()), appConfigService
                        .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                AppConfiguration.REPORT_STATE_CHANGED_MAIL_SUBJECT).getConfigValue(), appConfigService
                        .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                AppConfiguration.REPORT_STATE_CHANGED_MAIL_MESSAGE).getConfigValue(),
                workflowStatusService.findById(toStateId).getName(), currentOwnerName,
                previousState.getName(),
                previousOwner.getFirstName().concat(" ").concat(previousOwner.getLastName()),
                transition.getAction(), "Yes",
                "Please review.",
                reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("") ? "Yes" : "No",
                reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("") ? "Please review." : "");
        usersToNotify.stream().forEach(u -> {


            commonEmailSevice.sendMail(u.getEmailId(), notificationContent[0], notificationContent[1], new String[]{appConfigService
                    .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.PLATFORM_EMAIL_FOOTER).getConfigValue()});
        });
        usersToNotify.stream().forEach(u -> notificationsService.saveNotification(notificationContent, u.getId(), finalReport.getId()));

        //}

        report = _ReportToReturn(report, userId);
        _saveSnapShot(report);

        //Temporary block to continue testing as Grantee has submitted the report
        /*if (toStatus.getInternalStatus().equalsIgnoreCase("ACTIVE")) {
            ReportWithNote withNote = new ReportWithNote();

            report.getReportDetails().getSections().forEach(sec -> {
                if (sec.getAttributes() != null) {
                    sec.getAttributes().forEach(attr -> {
                        if (attr.getFieldType().equalsIgnoreCase("kpi")) {
                            attr.setActualTarget(String.valueOf(Math.round(Math.random() * 100)));
                        }
                    });
                }
            });

            report = saveReport(reportId, report, userId, tenantCode);

            withNote.setNote("Grantee has submitted");
            withNote.setReport(report);
            final Report fReport = report;
            ReportAssignmentsVO ass = report.getWorkflowAssignments().stream().filter(a -> a.getStateId().longValue() == fReport.getStatus().getId().longValue()).findFirst().get();

            MoveReportState(withNote, userService.getUserById(ass.getAssignmentId()).getId(), reportId, report.getStatus().getId(), workflowStatusTransitionService.getByFromStatus(report.getStatus()).getToState().getId(), tenantCode);
        }*/

        return report;

    }

    private void _saveSnapShot(Report report) {

        try {
            for (AssignedTo assignment : report.getCurrentAssignment()) {
                ReportSnapshot snapshot = new ReportSnapshot();
                snapshot.setAssignedToId(assignment.getUser().getId());
                snapshot.setEndDate(report.getEndDate());
                snapshot.setDueDate(report.getDueDate());
                snapshot.setReportId(report.getId());
                snapshot.setName(report.getName());
                snapshot.setStartDate(report.getStartDate());
                snapshot.setStatusId(report.getStatus().getId());
                snapshot.setStringAttributes(new ObjectMapper().writeValueAsString(report.getReportDetails()));
                reportSnapshotService.saveReportSnapshot(snapshot);
            }
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @GetMapping("/{reportId}/history/")
    public List<ReportHistory> getReportHistory(@PathVariable("reportId") Long reportId, @PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode) {

        List<ReportHistory> history = reportService.getReportHistory(reportId);
        for (ReportHistory historyEntry : history) {
            historyEntry.setNoteAddedByUser(userService.getUserById(historyEntry.getNoteAddedBy()));
        }
        return history;
    }

    @PostMapping("/{reportId}/section/{sectionId}/field/{fieldId}")
    @ApiOperation("Delete field in a section")
    public Report deleteField(@ApiParam(name = "reportToSave", value = "Report to save if in edit mode, passed in Body of request") @RequestBody Report reportToSave, @ApiParam(name = "userId", value = "Unique identifier of the logged in user") @PathVariable("userId") Long userId, @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId, @ApiParam(name = "sectionId", value = "Unique identifier of the section being modified") @PathVariable("sectionId") Long sectionId, @ApiParam(name = "fieldId", value = "Unique identifier of the field being deleted") @PathVariable("fieldId") Long fieldId, @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        /*grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);
        grantValidator.validateSectionExists(grantService,grantToSave,sectionId);
        grantValidator.validateFieldExists(grantService,grantToSave,sectionId,fieldId);*/
        Report report = saveReport(reportId, reportToSave, userId, tenantCode);


        ReportStringAttribute stringAttrib = reportService.getReportStringByStringAttributeId(fieldId);
        ReportSpecificSectionAttribute attribute = stringAttrib.getSectionAttribute();

        if (stringAttrib.getSectionAttribute().getFieldType().equalsIgnoreCase("document")) {
            List<ReportStringAttributeAttachments> attachments = reportService.getStringAttributeAttachmentsByStringAttribute(stringAttrib);
            reportService.deleteStringAttributeAttachments(attachments);
        }
        reportService.deleteStringAttribute(stringAttrib);
        reportService.deleteSectionAttribute(attribute);
        ReportStringAttribute rsa2Delete = report.getStringAttributes().stream().filter(g -> g.getId() == stringAttrib.getId()).findFirst().get();
        report.getStringAttributes().remove(rsa2Delete);
        report = reportService.saveReport(report);


        if (_checkIfReportTemplateChanged(report, attribute.getSection(), null)) {
            GranterReportTemplate newTemplate = _createNewReportTemplateFromExisiting(report);
        }
        report = _ReportToReturn(report, userId);
        return report;
    }

    @GetMapping("/templates")
    @ApiOperation("Get all published grant templates for tenant")
    public List<GranterReportTemplate> getTenantPublishedReportTemplates(@ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode, @PathVariable("userId") Long userId) {
        return reportService.findByGranterIdAndPublishedStatus(organizationService.findOrganizationByTenantCode(tenantCode).getId(), true);
    }

    @GetMapping("/create/grant/{grantId}/template/{templateId}")
    @ApiOperation("Create new report with a template")
    public Report createReport(@ApiParam(name = "grantId", value = "Unique identifier for the selected grant") @PathVariable("grantId") Long grantId,@ApiParam(name = "templateId", value = "Unique identifier for the selected template") @PathVariable("templateId") Long templateId, @PathVariable("userId") Long userId, @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        Report report = new Report();
        Grant reportForGrant = grantService.getById(grantId);
        GranterReportTemplate reportTemplate = reportService.findByTemplateId(templateId);

        report.setName("");
        report.setStartDate(null);
        report.setStDate("");
        report.setStatus(workflowStatusService.findInitialStatusByObjectAndGranterOrgId("REPORT", organizationService.findOrganizationByTenantCode(tenantCode).getId()));
        report.setEndDate(null);
        report.setEnDate("");
        report.setGrant(reportForGrant);
        report.setType("adhoc");
        report.setCreatedAt(new Date());
        report.setTemplate(reportTemplate);
        report.setCreatedBy(userService.getUserById(userId).getId());

        report = reportService.saveReport(report);

        ReportAssignment assignment = null;

        Organization granterOrg = organizationService.findOrganizationByTenantCode(tenantCode);
        List<WorkflowStatus> statuses = workflowStatusService.getTenantWorkflowStatuses("REPORT", granterOrg.getId());
        for (WorkflowStatus status : statuses) {
            if (!status.getTerminal()) {
                assignment = new ReportAssignment();
                if (status.isInitial()) {
                    assignment.setAnchor(true);
                    assignment.setAssignment(userId);
                } else {
                    assignment.setAnchor(false);
                }
                assignment.setReportId(report.getId());
                assignment.setStateId(status.getId());
                reportService.saveAssignmentForReport(assignment);
            }
        }



        List<GranterReportSection> granterReportSections = reportTemplate.getSections();
        report.setStringAttributes(new ArrayList<>());

        for (GranterReportSection reportSection : granterReportSections) {
            ReportSpecificSection specificSection = new ReportSpecificSection();
            specificSection.setDeletable(true);
            specificSection.setGranter((Granter)organizationService.findOrganizationByTenantCode(tenantCode));
            specificSection.setReportId(report.getId());
            specificSection.setReportTemplateId(reportTemplate.getId());
            specificSection.setSectionName(reportSection.getSectionName());
            specificSection.setSectionOrder(reportSection.getSectionOrder());

            specificSection = reportService.saveReportSpecificSection(specificSection);
            ReportSpecificSection finalSpecificSection = specificSection;
            Report finalReport = report;
            final AtomicInteger[] attribVOOrder = {new AtomicInteger(1)};
            Report finalReport1 = report;
            reportSection.getAttributes().forEach(a -> {
                ReportSpecificSectionAttribute sectionAttribute = new ReportSpecificSectionAttribute();
                sectionAttribute.setAttributeOrder(attribVOOrder[0].getAndIncrement());
                sectionAttribute.setDeletable(a.getDeletable());
                sectionAttribute.setFieldName(a.getFieldName());
                sectionAttribute.setFieldType(a.getFieldType());
                sectionAttribute.setGranter(finalSpecificSection.getGranter());
                sectionAttribute.setRequired(a.getRequired());
                sectionAttribute.setSection(finalSpecificSection);
                sectionAttribute.setCanEdit(true);
                sectionAttribute.setExtras(a.getExtras());
                sectionAttribute = reportService.saveReportSpecificSectionAttribute(sectionAttribute);

                ReportStringAttribute stringAttribute = new ReportStringAttribute();

                stringAttribute.setSection(finalSpecificSection);
                stringAttribute.setReport(finalReport);
                stringAttribute.setSectionAttribute(sectionAttribute);
                if (sectionAttribute.getFieldType().equalsIgnoreCase("kpi")) {
                    stringAttribute.setGrantLevelTarget(null);
                    stringAttribute.setFrequency(finalReport1.getType().toLowerCase());
                } else if (sectionAttribute.getFieldType().equalsIgnoreCase("table")) {
                    stringAttribute.setValue(a.getExtras());
                }
                stringAttribute = reportService.saveReportStringAttribute(stringAttribute);
            });
        }

        report = _ReportToReturn(report,userId);
        return report;
    }
}
