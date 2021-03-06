package org.codealpha.gmsservice.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codealpha.gmsservice.constants.AppConfiguration;
import org.codealpha.gmsservice.constants.Frequency;
import org.codealpha.gmsservice.entities.*;
import org.codealpha.gmsservice.models.*;
import org.codealpha.gmsservice.services.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/user/{userId}/report")
@Api(value = "Reports", description = "API end points for Reports", tags = { "Grants" })
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
    @Autowired
    private GranterReportTemplateService granterReportTemplateService;
    @Autowired
    private WorkflowPermissionService workflowPermissionService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private DisbursementService disbursementService;
    @Autowired
    private ReleaseService releaseService;
    @Value("${spring.timezone}")
    private String timezone;
    @Autowired
    private OrgTagService orgTagService;
    @Autowired
    private GrantTypeService grantTypeService;
    @Autowired
    private WorkflowValidationService workflowValidationService;
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/")
    public List<ReportCard> getAllReports(@PathVariable("userId") Long userId,
            @RequestHeader("X-TENANT-CODE") String tenantCode,
            @RequestParam(value = "q", required = false) String filterClause) {
        Organization org = null;
        User user = userService.getUserById(userId);

        List<ReportCard> reports = null;
        if (user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")) {
            org = user.getOrganization();
            if (filterClause != null && filterClause.equalsIgnoreCase("UPCOMING-DUE")) {
                reports = reportService.getAllAssignedReportCardsForGranteeUser(userId, org.getId(), "ACTIVE");
            } else if (filterClause != null && filterClause.equalsIgnoreCase("SUBMITTED")) {
                reports = reportService.getAllAssignedReportCardsForGranteeUser(userId, org.getId(), "REVIEW");
            } else if (filterClause != null && filterClause.equalsIgnoreCase("APPROVED")) {
                reports = reportService.getAllAssignedReportCardsForGranteeUser(userId, org.getId(), "CLOSED");
            }
        } else {
            org = organizationService.findOrganizationByTenantCode(tenantCode);
            Date start = DateTime.now().withTimeAtStartOfDay().toDate();
            Date end = new DateTime(start, DateTimeZone.forID(timezone)).plusDays(15).withTime(23, 59, 59, 999)
                    .toDate();
            boolean isAdmin = false;

            for (Role role : userRoleService.findRolesForUser(userService.getUserById(userId))) {
                if (role.getName().equalsIgnoreCase("ADMIN")) {
                    isAdmin = true;
                    break;
                }
            }
            if (filterClause != null && filterClause.equalsIgnoreCase("UPCOMING")) {
                if(!isAdmin) {
                    reports = reportService.getUpcomingReportCardsForGranterUserByDateRange(userId, org.getId(), start, end);
                }else{
                    reports = reportService.getUpcomingReportCardsForAdminGranterUserByDateRange(userId, org.getId(), start, end);

                }
                for (ReportCard report : reports) {
                    int futureReportsCount = reportService.getFutureReportCardsForGranterUserByDateRangeAndGrant(userId,
                            org.getId(), end, report.getGrant().getId()).size();
                    report.setFutureReportsCount(futureReportsCount);
                }
            } else if (filterClause != null && filterClause.equalsIgnoreCase("UPCOMING-FUTURE")) {
                if(!isAdmin) {
                    reports = reportService.getUpcomingFutureReportCardsForGranterUserByDate(userId, org.getId(), end);
                }else{
                    reports = reportService.getUpcomingFutureReportCardsForAdminGranterUserByDate(userId, org.getId(), end);
                }
                Map<Long, ReportCard> reportsHolder = new LinkedHashMap<Long, ReportCard>();
                for (ReportCard report : reports) {
                    if (!reportsHolder.keySet().contains(report.getGrant().getId())) {
                        reportsHolder.put(report.getGrant().getId(), report);
                    }
                }

                reports = new ArrayList<>();

                for (Long key : reportsHolder.keySet()) {
                    ReportCard r = reportsHolder.get(key);
                    List<ReportCard> otherReports = reportService.getReportCardsForGrant(r.getGrant());
                    otherReports.removeIf(a -> a.getId() == r.getId());
                    r.setFutureReportsCount(otherReports.size());
                    reports.add(r);
                }

                List<ReportCard> reportWithNullEndDate = new ArrayList<>();
                reportWithNullEndDate = reports.stream().filter(r -> r.getEndDate() == null)
                        .collect(Collectors.toList());
                reports.removeAll(reportWithNullEndDate);
                reports.sort(Comparator.comparing(ReportCard::getEndDate));
                reports.addAll(reportWithNullEndDate);
            } else if (filterClause != null && filterClause.equalsIgnoreCase("UPCOMING-DUE")) {
                if(!isAdmin) {
                    reports = reportService.getReadyToSubmitReportCardsForGranterUserByDateRange(userId, org.getId(), start,
                            end);
                }else{
                    reports = reportService.getReadyToSubmitReportCardsForAdminGranterUserByDateRange(userId, org.getId(), start,
                            end);
                }
            } else if (filterClause != null && filterClause.equalsIgnoreCase("SUBMITTED")) {
                if(!isAdmin){
                    reports = reportService.getSubmittedReportCardsForGranterUserByDateRange(userId, org.getId());
                }else{
                    reports = reportService.getSubmittedReportCardsForAdminGranterUserByDateRange(userId, org.getId());
                }
            } else if (filterClause != null && filterClause.equalsIgnoreCase("APPROVED")) {
                if(!isAdmin) {
                    reports = reportService.getApprovedReportCardsForGranterUserByDateRange(userId, org.getId());
                }else{
                    reports = reportService.getApprovedReportCardsForAdminGranterUserByDateRange(userId, org.getId());
                }
            }
            // reports = reportService.getAllAssignedReportsForGranterUser(userId,
            // org.getId());
        }

        /*if (reports != null) {
            for (Report report : reports) {

                report = _ReportToReturn(report, userId);

            }
        }*/

        if (user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE") && filterClause != null
                && filterClause.equalsIgnoreCase("SUBMITTED")) {
            for (ReportCard report : reports) {
                try {
                    ReportHistory historicReport = reportService.getSingleReportHistoryByStatusAndReportId("ACTIVE",
                            report.getId());
                    if (historicReport != null && historicReport.getReportDetail() != null) {
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        report.setReportDetails(
                                mapper.readValue(historicReport.getReportDetail(), ReportDetailVO.class));
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        for(ReportCard report:reports){
            List<ReportAssignment> assignments = reportService.getAssignmentsForReportById(report.getId());
            List<ReportAssignmentsVO> assignmentsVOs = new ArrayList<>();
            for(ReportAssignment assignment : assignments){
                ReportAssignmentsVO assignmentsVO = new ReportAssignmentsVO();
                assignmentsVO.setAssignmentId(assignment.getAssignment());
                assignmentsVO.setAnchor(assignment.isAnchor());
                if(assignment.getAssignment()!=null) {
                    assignmentsVO.setAssignmentUser(userService.getUserById(assignment.getAssignment()));
                }
                assignmentsVO.setReportId(assignment.getReportId());
                assignmentsVO.setStateId(assignment.getStateId());
                assignmentsVOs.add(assignmentsVO);
            }
            report.setWorkflowAssignments(assignmentsVOs);
        }
        return reports;
    }

    @GetMapping("/{reportId}")
    public Report getAllReports(@PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode,
            @PathVariable("reportId") Long reportId) {
        Organization tenantOrg = organizationService.findOrganizationByTenantCode(tenantCode);
        Report report = reportService.getReportById(reportId);

        report = _ReportToReturn(report, userId);
        _checkAndReturnHistoricalReport(userId, report);
        return report;
    }

    @GetMapping("/{reportId}/{grantId}")
    public List<ReportCard> getFutureReports(@PathVariable("userId") Long userId,
            @RequestHeader("X-TENANT-CODE") String tenantCode, @PathVariable("reportId") Long reportId,
            @PathVariable("grantId") Long grantId,@RequestParam(value = "type",required = false)String forType) {
        User user = userService.getUserById(userId);

        List<ReportCard> reports = null;
        if (user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTER")) {
            Organization org = organizationService.findOrganizationByTenantCode(tenantCode);
            Date start = DateTime.now().withTimeAtStartOfDay().toDate();
            Date end = new DateTime(start, DateTimeZone.forID(timezone)).plusDays(30).withTime(23, 59, 59, 999)
                    .toDate();
            if(forType.equalsIgnoreCase("upcoming")) {
                reports = reportService.futureReportForGranterUserByDateRangeAndGrant(userId, org.getId(), end, grantId);
            }else if(forType.equalsIgnoreCase("all")) {
                reports = reportService.getReportCardsForGrant(grantService.getById(grantId));
            }
        }

        reports.removeIf(r -> r.getId().longValue() == reportId.longValue());


        return reports;
    }

    @GetMapping("/{grantId}/approved")
    public List<Report> getApprovedReportsForGrant(@PathVariable("userId") Long userId,
            @RequestHeader("X-TENANT-CODE") String tenantCode, @PathVariable("grantId") Long grantId) {
        Optional<WorkflowStatus> reportApprovedStatus = workflowStatusService
                .getTenantWorkflowStatuses("REPORT",
                        organizationService.findOrganizationByTenantCode(tenantCode).getId())
                .stream().filter(s -> s.getInternalStatus().equalsIgnoreCase("CLOSED")).findFirst();
        List<Report> reports = new ArrayList<>();
        if (reportApprovedStatus.isPresent()) {
            Grant grant = grantService.getById(grantId);
            reports = reportService.findReportsByStatusForGrant(reportApprovedStatus.get(), grant);
            // Include approved reports of orgiginal grant if exist
            if (grant.getOrigGrantId() != null) {
                reports.addAll(reportService.findReportsByStatusForGrant(reportApprovedStatus.get(),
                        grantService.getById(grant.getOrigGrantId())));
            }
            // End
        }

        for (Report report : reports) {
            report = _ReportToReturn(report, userId);
        }

        return reports;
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

            reportService.setAssignmentHistory(assignmentsVO);

            workflowAssignments.add(assignmentsVO);
        }
        report.setWorkflowAssignments(workflowAssignments);
        List<ReportAssignment> reportAssignments = determineCanManage(report, userId);

        if (userService.getUserById(userId).getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")) {
            report.setForGranteeUse(true);
        } else {
            report.setForGranteeUse(false);
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

        ReportVO reportVO = new ReportVO().build(report, reportService.getReportSections(report), userService,
                reportService);
        report.setReportDetails(reportVO.getReportDetails());

        showDisbursementsForReport(report,userService.getUserById(userId));

        report.setNoteAddedBy(reportVO.getNoteAddedBy());
        report.setNoteAddedByUser(reportVO.getNoteAddedByUser());

        report.getWorkflowAssignments().sort((a, b) -> a.getId().compareTo(b.getId()));
        report.getReportDetails().getSections()
                .sort((a, b) -> Long.valueOf(a.getOrder()).compareTo(Long.valueOf(b.getOrder())));
        for (SectionVO section : report.getReportDetails().getSections()) {
            if (section.getAttributes() != null) {
                section.getAttributes().sort(
                        (a, b) -> Long.valueOf(a.getAttributeOrder()).compareTo(Long.valueOf(b.getAttributeOrder())));
            }
        }

        report.setGranteeUsers(userService.getAllGranteeUsers(report.getGrant().getOrganization()));

        GrantVO grantVO = new GrantVO().build(report.getGrant(), grantService.getGrantSections(report.getGrant()),
                workflowPermissionService, userService.getUserById(userId),
                appConfigService.getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(),
                        AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS),
                userService,grantService);

        ObjectMapper mapper = new ObjectMapper();
        report.getGrant().setGrantDetails(grantVO.getGrantDetails());

        List<Report> approvedReports = null;
        List<TableData> approvedDisbursements = new ArrayList<>();
        AtomicInteger installmentNumber = new AtomicInteger();

        /*
         * if (report.getLinkedApprovedReports() != null) { approvedReports =
         * reportService.getReportsByIds(report.getLinkedApprovedReports()); for (Report
         * approvedReport : approvedReports) {
         * reportService.getReportSections(approvedReport).forEach(sec -> { if
         * (sec.getAttributes() != null) { sec.getAttributes().forEach(attr -> { if
         * (attr.getFieldType().equalsIgnoreCase("disbursement")) {
         * 
         * try { List<TableData> tableDataList = mapper.readValue(
         * reportService.getReportStringByStringAttributeId(attr.getId()).getValue(),
         * new TypeReference<List<TableData>>() { }); tableDataList.forEach(td -> {
         * approvedDisbursements.add(td); installmentNumber.getAndIncrement(); }); }
         * catch (Exception e) { logger.error("Failed for report "+report.getId(),e); }
         * 
         * } }); } }); } }
         */

        report.getGrant().setApprovedReportsDisbursements(approvedDisbursements);

        report.getReportDetails().getSections().forEach(sec -> {
            if (sec.getAttributes() != null) {
                sec.getAttributes().forEach(attr -> {
                    if (attr.getFieldType().equalsIgnoreCase("disbursement") && attr.getFieldTableValue() != null) {
                        for (TableData data : attr.getFieldTableValue()) {
                            installmentNumber.getAndIncrement();
                            data.setName(String.valueOf(installmentNumber.get()));
                        }

                        try {
                            attr.setFieldValue(mapper.writeValueAsString(attr.getFieldTableValue()));
                        } catch (JsonProcessingException e) {
                            logger.error(e.getMessage(), e);
                        }

                    }
                });
            }
        });
        report.setSecurityCode(reportService.buildHashCode(report));
        report.setFlowAuthorities(reportService.getFlowAuthority(report, userId));

        List<GrantTag> grantTags = grantService.getTagsForGrant(report.getGrant().getId());
        /*List<GrantTagVO> grantTagsVoList = new ArrayList<>();
        for(GrantTag tag: grantTags){
            GrantTagVO vo =new GrantTagVO();
            vo.setGrantId(report.getGrant().getId());
            vo.setId(tag.getId());
            vo.setOrgTagId(tag.getOrgTagId());
            vo.setTagName(orgTagService.getOrgTagById(tag.getOrgTagId()).getName());
            grantTagsVoList.add(vo);
        }*/
        report.getGrant().setGrantTags(grantTags);

        return report;
    }

    private void showDisbursementsForReport(Report report, User currentUser) {
        List<WorkflowStatus> workflowStatuses = workflowStatusService.getTenantWorkflowStatuses("DISBURSEMENT",
                report.getGrant().getGrantorOrganization().getId());

        List<WorkflowStatus> closedStatuses = workflowStatuses.stream()
                .filter(ws -> ws.getInternalStatus().equalsIgnoreCase("CLOSED")).collect(Collectors.toList());
        List<Long> closedStatusIds = closedStatuses.stream().mapToLong(s -> s.getId()).boxed()
                .collect(Collectors.toList());

        List<WorkflowStatus> draftStatuses = workflowStatuses.stream()
                .filter(ws -> ws.getInternalStatus().equalsIgnoreCase("DRAFT")).collect(Collectors.toList());
        List<Long> draftStatusIds = draftStatuses.stream().mapToLong(s -> s.getId()).boxed()
                .collect(Collectors.toList());

        List<ActualDisbursement> finalActualDisbursements = new ArrayList();
        report.getReportDetails().getSections().forEach(s -> {
            if (s.getAttributes() != null && s.getAttributes().size() > 0) {
                s.getAttributes().forEach(a -> {
                    if (a.getFieldType().equalsIgnoreCase("disbursement")) {
                            List<Disbursement> closedDisbursements = getDisbursementsByStatusIds(report.getGrant(),closedStatusIds); //disbursementService
                                //getDibursementsForGrantByStatuses(report.getGrant().getId(), closedStatusIds);
                        List<Disbursement> draftDisbursements = getDisbursementsByStatusIds(report.getGrant(), draftStatusIds);
                        if (!report.getStatus().getInternalStatus().equalsIgnoreCase("CLOSED")) {
                            List<TableData> tableDataList = new ArrayList<>();
                            if (closedDisbursements != null) {
                                closedDisbursements.sort(Comparator.comparing(Disbursement::getCreatedAt));
                                AtomicInteger index = new AtomicInteger(1);
                                closedDisbursements.forEach(cd -> {
                                    List<ActualDisbursement> ads = disbursementService
                                            .getActualDisbursementsForDisbursement(cd);
                                    if (ads != null && ads.size() > 0) {
                                        finalActualDisbursements.addAll(ads);
                                    }

                                });
                            }


                                if (draftDisbursements != null && draftDisbursements.size() > 0) {
                                    if (!currentUser.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")) {
                                        draftDisbursements.removeIf(dd -> ((dd.getReportId() != null
                                                && dd.getReportId().longValue() != report.getId().longValue()  && dd.isGranteeEntry()) || (dd.getReportId() != null
                                                && dd.getReportId().longValue() == report.getId().longValue()  && dd.isGranteeEntry() && report.getStatus().getInternalStatus().equalsIgnoreCase("ACTIVE"))));
                                    }
                                    if (draftDisbursements != null) {
                                        draftDisbursements.sort(Comparator.comparing(Disbursement::getCreatedAt));
                                        AtomicInteger index = new AtomicInteger(1);
                                        draftDisbursements.forEach(cd -> {
                                            List<ActualDisbursement> ads = disbursementService
                                                    .getActualDisbursementsForDisbursement(cd);
                                            if (ads != null && ads.size() > 0) {
                                                finalActualDisbursements.addAll(ads);
                                            }

                                        });
                                    }
                                }



                            finalActualDisbursements.sort(Comparator.comparing(ActualDisbursement::getId));
                            if (finalActualDisbursements.size() > 0) {
                                AtomicInteger index = new AtomicInteger(1);
                                finalActualDisbursements.forEach(ad -> {
                                    TableData td = new TableData();
                                    ColumnData[] colDataList = new ColumnData[4];
                                    td.setName(String.valueOf(index.getAndIncrement()));
                                    td.setHeader("#");
                                    td.setStatus(ad.getStatus());
                                    td.setSaved(ad.getSaved());
                                    td.setActualDisbursementId(ad.getId());
                                    td.setDisbursementId(ad.getDisbursementId());
                                    Long repId = disbursementService.getDisbursementById(ad.getDisbursementId()).getReportId();
                                    td.setReportId(repId);
                                    if (disbursementService.getDisbursementById(ad.getDisbursementId())
                                            .isGranteeEntry()) {
                                        td.setEnteredByGrantee(true);
                                    }
                                    /*if(!currentUser.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE") && td.isEnteredByGrantee() && report.getId().longValue()!=repId.longValue() && !disbursementService.getDisbursementById(ad.getDisbursementId()).getStatus().getInternalStatus().equalsIgnoreCase("CLOSED")){
                                        td.setShowForGrantee(false);
                                    }*/

                                    if(currentUser.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE") && td.isEnteredByGrantee() && report.getId().longValue()!=repId.longValue() && !disbursementService.getDisbursementById(ad.getDisbursementId()).getStatus().getInternalStatus().equalsIgnoreCase("CLOSED")){
                                        td.setShowForGrantee(false);
                                    }


                                    ColumnData cdDate = new ColumnData();
                                    cdDate.setDataType("date");
                                    cdDate.setName("Disbursement Date");
                                    cdDate.setValue(ad.getDisbursementDate() != null
                                            ? new SimpleDateFormat("dd-MMM-yyyy").format(ad.getDisbursementDate())
                                            : null);

                                    ColumnData cdDA = new ColumnData();
                                    cdDA.setDataType("currency");
                                    cdDA.setName("Actual Disbursement");
                                    cdDA.setValue(
                                            ad.getActualAmount() != null ? String.valueOf(ad.getActualAmount()) : null);

                                    ColumnData cdFOS = new ColumnData();
                                    cdFOS.setDataType("currency");
                                    cdFOS.setName("Funds from Other Sources");
                                    cdFOS.setValue(
                                            ad.getOtherSources() != null ? String.valueOf(ad.getOtherSources()) : null);

                                    ColumnData cdN = new ColumnData();
                                    cdN.setName("Notes");
                                    cdN.setValue(ad.getNote());

                                    colDataList[0] = cdDate;
                                    colDataList[1] = cdDA;
                                    colDataList[2] = cdFOS;
                                    colDataList[3] = cdN;
                                    td.setColumns(colDataList);
                                    tableDataList.add(td);
                                });
                                a.setFieldTableValue(tableDataList);
                                try {
                                    a.setFieldValue(new ObjectMapper().writeValueAsString(tableDataList));
                                } catch (IOException e) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        } else {
                            List<TableData> tableDataList = new ArrayList<>();

                            if (closedDisbursements != null) {
                                AtomicInteger index = new AtomicInteger(1);
                                closedDisbursements.removeIf(
                                        cd -> new DateTime(cd.getMovedOn(), DateTimeZone.forID(timezone)).isAfter(
                                                new DateTime(report.getMovedOn(), DateTimeZone.forID(timezone))));
                                if (closedDisbursements != null) {
                                    closedDisbursements.forEach(cd -> {

                                        List<ActualDisbursement> ads = disbursementService
                                                .getActualDisbursementsForDisbursement(cd);
                                        if (ads != null && ads.size() > 0) {
                                            finalActualDisbursements.addAll(ads);
                                        }
                                    });
                                }
                            }

                            finalActualDisbursements.sort(Comparator.comparing(ActualDisbursement::getOrderPosition));
                            if (finalActualDisbursements.size() > 0) {
                                AtomicInteger index = new AtomicInteger(1);
                                finalActualDisbursements.forEach(ad -> {
                                    TableData td = new TableData();
                                    ColumnData[] colDataList = new ColumnData[4];
                                    td.setName(String.valueOf(index.getAndIncrement()));
                                    td.setHeader("#");
                                    td.setStatus(ad.getStatus());
                                    td.setSaved(ad.getStatus());
                                    td.setActualDisbursementId(ad.getId());
                                    td.setDisbursementId(ad.getDisbursementId());
                                    td.setReportId(disbursementService.getDisbursementById(ad.getDisbursementId()).getReportId());
                                    if (disbursementService.getDisbursementById(ad.getDisbursementId())
                                            .isGranteeEntry()) {
                                        td.setEnteredByGrantee(true);
                                    }
                                    ColumnData cdDate = new ColumnData();
                                    cdDate.setDataType("date");
                                    cdDate.setName("Disbursement Date");
                                    cdDate.setValue(ad.getDisbursementDate() != null
                                            ? new SimpleDateFormat("dd-MMM-yyyy").format(ad.getDisbursementDate())
                                            : null);

                                    ColumnData cdDA = new ColumnData();
                                    cdDA.setDataType("currency");
                                    cdDA.setName("Actual Disbursement");
                                    cdDA.setValue(String.valueOf(ad.getActualAmount()));

                                    ColumnData cdFOS = new ColumnData();
                                    cdFOS.setDataType("currency");
                                    cdFOS.setName("Funds from Other Sources");
                                    cdFOS.setValue(String.valueOf(ad.getOtherSources()));

                                    ColumnData cdN = new ColumnData();
                                    cdN.setName("Notes");
                                    cdN.setValue(ad.getNote());

                                    colDataList[0] = cdDate;
                                    colDataList[1] = cdDA;
                                    colDataList[2] = cdFOS;
                                    colDataList[3] = cdN;
                                    td.setColumns(colDataList);
                                    tableDataList.add(td);
                                });
                                a.setFieldTableValue(tableDataList);
                                try {
                                    a.setFieldValue(new ObjectMapper().writeValueAsString(tableDataList));
                                } catch (IOException e) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }

                    }
                });
            }
        });

    }

    private List<Disbursement> getDisbursementsByStatusIds(Grant grant, List<Long> statusIds) {
        List<Disbursement> closedDisbursements = new ArrayList<>();

        closedDisbursements = disbursementService.getDibursementsForGrantByStatuses(grant.getId(), statusIds);
        if(grant.getOrigGrantId()!=null){
            closedDisbursements.addAll(getDisbursementsByStatusIds(grantService.getById(grant.getOrigGrantId()),statusIds));
        }
        return closedDisbursements;
    }

    private List<ReportAssignment> determineCanManage(Report report, Long userId) {
        List<ReportAssignment> reportAssignments = reportService.getAssignmentsForReport(report);
        if ((reportAssignments.stream()
                .filter(ass -> (ass.getAssignment() == null ? 0L : ass.getAssignment().longValue()) == userId
                        .longValue() && ass.getStateId().longValue() == report.getStatus().getId().longValue())
                .findAny().isPresent())
                || (report.getStatus().getInternalStatus().equalsIgnoreCase("ACTIVE") && userService.getUserById(userId)
                        .getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE"))) {
            report.setCanManage(true);
        } else {
            report.setCanManage(false);
        }
        /*if (report.isDisabledByAmendment()) {
            report.setCanManage(false);
        }*/
        return reportAssignments;
    }

    @PutMapping("/{reportId}")
    @ApiOperation("Save report")
    public Report saveReport(
            @ApiParam(name = "grantId", value = "Unique identifier of report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "reportToSave", value = "Report to save in edit mode, passed in Body of request") @RequestBody Report reportToSave,
            @ApiParam(name = "userId", value = "Unique identifier of logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {

        // grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);

        Organization tenantOrg = organizationService.findOrganizationByTenantCode(tenantCode);
        User user = userService.getUserById(userId);
        Report report = null;
        Report savedReports = reportService.getReportById(reportId);
        determineCanManage(savedReports, userId);
        if (savedReports.getCanManage())
            report = _processReport(reportToSave, tenantOrg, user);

        report = _ReportToReturn(reportToSave, userId);
        return report;
    }

    private Report _processReport(Report reportToSave, Organization tenantOrg, User user) {
        Report report = reportService.getReportById(reportToSave.getId());

        report.setStartDate(reportToSave.getStartDate());
        report.setName(reportToSave.getName());
        report.setEndDate(reportToSave.getEndDate());
        report.setDueDate(reportToSave.getDueDate());
        report.setUpdatedAt(DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0).toDate());
        report.setUpdatedBy(user.getId());
        try {
            report.setReportDetail(new ObjectMapper().writeValueAsString(reportToSave.getReportDetails()));
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }

        List<Report> approvedReports = null;
        if (report.getLinkedApprovedReports() == null || report.getLinkedApprovedReports().isEmpty()) {
            approvedReports = reportService.findByGrantAndStatus(report.getGrant(),
                    workflowStatusService
                            .getTenantWorkflowStatuses("REPORT", report.getGrant().getGrantorOrganization().getId())
                            .stream().filter(s -> s.getInternalStatus().equalsIgnoreCase("CLOSED")).findFirst().get(),
                    report.getId());
            if (approvedReports == null || approvedReports.isEmpty()) {
                try {
                    report.setLinkedApprovedReports(
                            new ObjectMapper().writeValueAsString(Arrays.asList(new Long[] { 0l })));
                } catch (JsonProcessingException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                try {
                    report.setLinkedApprovedReports(new ObjectMapper().writeValueAsString(
                            approvedReports.stream().map(r -> new Long(r.getId())).collect(Collectors.toList())));
                } catch (JsonProcessingException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            report = reportService.saveReport(report);
        }

        _processStringAttributes(user, report, reportToSave, tenantOrg);

        report = reportService.saveReport(report);

        return report;
    }

    private void _processStringAttributes(User user, Report report, Report reportToSave, Organization tenant) {
        List<ReportStringAttribute> stringAttributes = new ArrayList<>();
        ReportSpecificSection reportSpecificSection = null;

        for (SectionVO sectionVO : reportToSave.getReportDetails().getSections()) {
            reportSpecificSection = reportService.getReportSpecificSectionById(sectionVO.getId());

            reportSpecificSection.setSectionName(sectionVO.getName());
            reportSpecificSection.setSectionOrder(sectionVO.getOrder());
            if ("ANUDAN".equalsIgnoreCase(tenant.getCode())) {
                reportSpecificSection.setGranter((Granter) report.getGrant().getGrantorOrganization());
            } else {
                reportSpecificSection.setGranter((Granter) tenant);
            }

            reportSpecificSection.setDeletable(true);

            reportSpecificSection = reportService.saveReportSpecificSection(reportSpecificSection);

            ReportSpecificSectionAttribute sectionAttribute = null;

            if (sectionVO.getAttributes() != null) {
                for (SectionAttributesVO sectionAttributesVO : sectionVO.getAttributes()) {

                    sectionAttribute = reportService.getReportStringByStringAttributeId(sectionAttributesVO.getId())
                            .getSectionAttribute();

                    sectionAttribute.setFieldName(sectionAttributesVO.getFieldName());
                    sectionAttribute.setFieldType(sectionAttributesVO.getFieldType());
                    if ("ANUDAN".equalsIgnoreCase(tenant.getCode())) {
                        sectionAttribute.setGranter((Granter) report.getGrant().getGrantorOrganization());
                    } else {
                        sectionAttribute.setGranter((Granter) tenant);
                    }

                    sectionAttribute.setAttributeOrder(sectionAttributesVO.getAttributeOrder());
                    sectionAttribute.setRequired(true);
                    sectionAttribute.setSection(reportSpecificSection);

                    sectionAttribute = reportService.saveReportSpecificSectionAttribute(sectionAttribute);

                    ReportStringAttribute reportStringAttribute = reportService
                            .getReportStringAttributeBySectionAttributeAndSection(sectionAttribute,
                                    reportSpecificSection);

                    reportStringAttribute.setTarget(sectionAttributesVO.getTarget());
                    reportStringAttribute.setFrequency(sectionAttributesVO.getFrequency());
                    if ((user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE") && !grantTypeService.findById(report.getGrant().getGrantTypeId()).isInternal()) || (user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTER") && grantTypeService.findById(report.getGrant().getGrantTypeId()).isInternal())) {
                        reportStringAttribute.setActualTarget(sectionAttributesVO.getActualTarget());
                    }
                    if (sectionAttribute.getFieldType().equalsIgnoreCase("table")
                            || sectionAttribute.getFieldType().equalsIgnoreCase("disbursement")) {
                        List<TableData> tableData = sectionAttributesVO.getFieldTableValue();
                        // Do the below only if field type is Disbursement
                        // The idea is to create a real disbursement if a new row is added
                        if (sectionAttribute.getFieldType().equalsIgnoreCase("disbursement")
                                && user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")) {
                            try {
                                List<TableData> newEntries = new ArrayList();
                                List<TableData> missingEntries = new ArrayList();

                                // Find out new entries
                                if (tableData != null) {
                                    for (TableData td : tableData) {
                                        if (td.isStatus() && !td.isSaved()) {
                                            newEntries.add(td);
                                        }
                                    }
                                }

                                if (tableData != null) {
                                    for (TableData et : tableData) {
                                        if (!et.isStatus()) {
                                            missingEntries.add(et);
                                        }
                                    }
                                }

                                if (tableData != null && tableData.size() > 0) {

                                    for (TableData nData : tableData) {

                                        if (!nData.isSaved()) {
                                            ActualDisbursement actualDisbursement = disbursementService
                                                    .getActualDisbursementById(nData.getActualDisbursementId());

                                            actualDisbursement.setOtherSources(
                                                    Double.valueOf(nData.getColumns()[2].getValue() == null ? "0d"
                                                            : nData.getColumns()[2].getValue()));
                                            actualDisbursement.setDisbursementDate(new SimpleDateFormat("dd-MMM-yyyy")
                                                    .parse(nData.getColumns()[0].getValue()));
                                            // actualDisbursement.setDisbursementId(newDisbursement.getId());
                                            actualDisbursement.setNote(nData.getColumns()[3].getValue());
                                            actualDisbursement.setActualAmount(0d);
                                            actualDisbursement.setCreatedAt(DateTime.now().toDate());
                                            actualDisbursement.setCreatedBy(user.getId());
                                            actualDisbursement.setStatus(nData.isStatus());
                                            actualDisbursement.setSaved(false);
                                            actualDisbursement.setOrderPosition(
                                                    disbursementService.getNewOrderPositionForActualDisbursementOfGrant(
                                                            report.getGrant().getId()));
                                            disbursementService.saveActualDisbursement(actualDisbursement);
                                        }
                                    }

                                }
                            } catch (ParseException e) {
                                logger.error(e.getMessage(), e);
                            }
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
    public ReportFieldInfo createFieldInSection(
            @ApiParam(name = "reportToSave", value = "Report to save if in edit mode passed in Body of request") @RequestBody Report reportToSave,
            @ApiParam(name = "reportId", value = "Unique identifier of the grant") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "sectionId", value = "Unique identifier of the section to which the field is being added") @PathVariable("sectionId") Long sectionId,
            @ApiParam(name = "userId", value = "Unique identifier of the logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        // grantService.saveGrant(grantToSave);
        /*
         * grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);
         * grantValidator.validateSectionExists(grantService,grantToSave,sectionId);
         */
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
        newSectionAttribute.setAttributeOrder(reportService.getNextAttributeOrder(
                organizationService.findOrganizationByTenantCode(tenantCode).getId(), sectionId));
        newSectionAttribute.setGranter((Granter) organizationService.findOrganizationByTenantCode(tenantCode));
        newSectionAttribute = reportService.saveReportSpecificSectionAttribute(newSectionAttribute);
        ReportStringAttribute stringAttribute = new ReportStringAttribute();
        stringAttribute.setValue("");
        stringAttribute.setSectionAttribute(newSectionAttribute);
        stringAttribute.setSection(reportSection);
        stringAttribute.setReport(report);

        stringAttribute = reportService.saveReportStringAttribute(stringAttribute);

        if (reportService._checkIfReportTemplateChanged(report, reportSection, newSectionAttribute, this)) {
            reportService._createNewReportTemplateFromExisiting(report);
        }

        report = _ReportToReturn(report, userId);
        return new ReportFieldInfo(newSectionAttribute.getId(), stringAttribute.getId(), report);
    }

    @PutMapping("/{reportId}/section/{sectionId}/field/{fieldId}")
    @ApiOperation("Update field information")
    public ReportFieldInfo updateField(
            @ApiParam(name = "sectionId", value = "Unique identifier of section") @PathVariable("sectionId") Long sectionId,
            @ApiParam(name = "attributeToSave", value = "Updated attribute to be saved") @RequestBody ReportAttributeToSaveVO attributeToSave,
            @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "fieldId", value = "Unique identifier of the field being updated") @PathVariable("fieldId") Long fieldId,
            @ApiParam(name = "userId", value = "Unique identifier of the logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        /*
         * grantValidator.validate(grantService,grantId,attributeToSave.getGrant(),
         * userId,tenantCode);
         * grantValidator.validateSectionExists(grantService,attributeToSave.getGrant(),
         * sectionId);
         * grantValidator.validateFieldExists(grantService,attributeToSave.getGrant(),
         * sectionId,fieldId);
         */
        Report report = saveReport(reportId, attributeToSave.getReport(), userId, tenantCode);
        ReportSpecificSectionAttribute currentAttribute = reportService.getReportStringByStringAttributeId(fieldId)
                .getSectionAttribute();
        currentAttribute.setFieldName(attributeToSave.getAttr().getFieldName());
        currentAttribute.setFieldType(attributeToSave.getAttr().getFieldType());
        currentAttribute = reportService.saveReportSpecificSectionAttribute(currentAttribute);
        ReportStringAttribute stringAttribute = reportService
                .getReportStringAttributeBySectionAttributeAndSection(currentAttribute, currentAttribute.getSection());
        // stringAttribute.setValue("");
        if (currentAttribute.getFieldType().equalsIgnoreCase("kpi")) {
            // stringAttribute.setFrequency(report.getType().toLowerCase());
            stringAttribute.setFrequency("adhoc");
        }
        stringAttribute = reportService.saveReportStringAttribute(stringAttribute);

        report = reportService.getReportById(reportId);
        if (reportService._checkIfReportTemplateChanged(report, currentAttribute.getSection(), currentAttribute,
                this)) {
            reportService._createNewReportTemplateFromExisiting(report);
        }

        report = _ReportToReturn(report, userId);
        return new ReportFieldInfo(currentAttribute.getId(), stringAttribute.getId(), report);
    }

    @PostMapping("/{reportId}/field/{fieldId}/template/{templateId}")
    @ApiOperation(value = "Attach document to field", notes = "Valid for Document field types only")
    public ReportDocInfo createDocumentForReportSectionField(
            @ApiParam(name = "reportToSave", value = "Report to save in edit mode, passed in Body of request") @RequestBody Report reportToSave,
            @ApiParam(name = "userId", value = "Unique identifier of logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "fieldId", value = "Unique identifier of the field to which document is being attached") @PathVariable("fieldId") Long fieldId,
            @ApiParam(name = "temaplteId", value = "Unique identified of the document template being attached") @PathVariable("templateId") Long templateId,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        saveReport(reportId, reportToSave, userId, tenantCode);
        TemplateLibrary libraryDoc = templateLibraryService.getTemplateLibraryDocumentById(templateId);

        ReportStringAttribute stringAttribute = reportService.getReportStringByStringAttributeId(fieldId);

        File file = null;
        String filePath = null;
        try {
            file = resourceLoader
                    .getResource("file:" + uploadLocation + URLDecoder.decode(libraryDoc.getLocation(), "UTF-8"))
                    .getFile();
            // filePath = uploadLocation + tenantCode + "/report-documents/" + reportId +
            // "/" + stringAttribute.getSection().getId() + "/" +
            // stringAttribute.getSectionAttribute().getId() + "/";

            User user = userService.getUserById(userId);

            if (user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")) {
                filePath = uploadLocation + user.getOrganization().getName().toUpperCase() + "/report-documents/"
                        + reportId + "/" + stringAttribute.getSection().getId() + "/"
                        + stringAttribute.getSectionAttribute().getId() + "/";
            } else {
                filePath = uploadLocation + tenantCode + "/report-documents/" + reportId + "/"
                        + stringAttribute.getSection().getId() + "/" + stringAttribute.getSectionAttribute().getId()
                        + "/";
            }

            File dir = new File(filePath);
            dir.mkdirs();
            File fileToCreate = new File(dir, libraryDoc.getName() + "." + libraryDoc.getType());
            FileCopyUtils.copy(file, fileToCreate);
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
            List<ReportStringAttributeAttachments> stringAttributeAttachments = reportService
                    .getStringAttributeAttachmentsByStringAttribute(stringAttribute);
            stringAttribute.setValue(mapper.writeValueAsString(stringAttributeAttachments));
            stringAttribute = reportService.saveReportStringAttribute(stringAttribute);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        Report report = reportService.getReportById(reportId);
        report = _ReportToReturn(report, userId);
        return new ReportDocInfo(attachment.getId(), report);
    }

    @PostMapping(value = "/{reportId}/section/{sectionId}/attribute/{attributeId}/upload", consumes = {
            "multipart/form-data" })
    @ApiOperation("Upload and attach files to Document field from disk")
    public ReportDocInfo saveUploadedFiles(
            @ApiParam(name = "sectionId", value = "Unique identifier of section") @PathVariable("sectionId") Long sectionId,
            @ApiParam(name = "userId", value = "Unique identifier of logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "attributeId", value = "Unique identifier of the document field") @PathVariable("attributeId") Long attributeId,
            @ApiParam(name = "reportData", value = "Report data") @RequestParam("reportToSave") String reportToSaveStr,
            @RequestParam("file") MultipartFile[] files,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Report reportToSave = null;
        try {
            reportToSave = mapper.readValue(reportToSaveStr, Report.class);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        /*
         * grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);
         * grantValidator.validateSectionExists(grantService,grantToSave,sectionId);
         * grantValidator.validateFieldExists(grantService,grantToSave,sectionId,
         * attributeId); grantValidator.validateFiles(files,supportedFileTypes);
         */

        Report report = reportService.getReportById(reportId);

        ReportStringAttribute attr = reportService.getReportStringByStringAttributeId(attributeId);
        User user = userService.getUserById(userId);

        String filePath = "";
        if (user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")) {
            filePath = uploadLocation + user.getOrganization().getName().toUpperCase() + "/report-documents/" + reportId
                    + "/" + attr.getSection().getId() + "/" + attr.getSectionAttribute().getId() + "/";
        } else {
            filePath = uploadLocation + tenantCode + "/report-documents/" + reportId + "/" + attr.getSection().getId()
                    + "/" + attr.getSectionAttribute().getId() + "/";
        }
        File dir = new File(filePath);
        dir.mkdirs();
        List<DocInfo> docInfos = new ArrayList<>();
        List<ReportStringAttributeAttachments> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String fileName = file.getOriginalFilename();

                File fileToCreate = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(fileToCreate);
                fos.write(file.getBytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ReportStringAttributeAttachments attachment = new ReportStringAttributeAttachments();
            attachment.setVersion(1);
            attachment.setType(FilenameUtils.getExtension(file.getOriginalFilename()));
            attachment.setTitle(file.getOriginalFilename()
                    .replace("." + FilenameUtils.getExtension(file.getOriginalFilename()), ""));
            attachment.setLocation(filePath);
            attachment.setName(file.getOriginalFilename()
                    .replace("." + FilenameUtils.getExtension(file.getOriginalFilename()), ""));
            attachment.setReportStringAttribute(attr);
            attachment.setDescription(file.getOriginalFilename()
                    .replace("." + FilenameUtils.getExtension(file.getOriginalFilename()), ""));
            attachment.setCreatedOn(new Date());
            attachment.setCreatedBy(userService.getUserById(userId).getEmailId());
            attachment = reportService.saveReportStringAttributeAttachment(attachment);
            attachments.add(attachment);
        }

        mapper = new ObjectMapper();
        try {
            if (attr.getValue() == null || attr.getValue().equalsIgnoreCase("")) {
                attr.setValue("[]");
            }
            List<ReportStringAttributeAttachments> currentAttachments = mapper.readValue(attr.getValue(),
                    new TypeReference<List<ReportStringAttributeAttachments>>() {
                    });
            if (currentAttachments == null) {
                currentAttachments = new ArrayList<>();
            }
            currentAttachments.addAll(attachments);

            attr.setValue(mapper.writeValueAsString(currentAttachments));
            attr = reportService.saveReportStringAttribute(attr);
            ReportStringAttribute finalAttr = attr;
            ReportStringAttribute finalAttr1 = finalAttr;
            finalAttr = report.getStringAttributes().stream().filter(g -> g.getId() == finalAttr1.getId()).findFirst()
                    .get();
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
    public ReportSectionInfo createSection(@RequestBody Report reportToSave,
            @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "templateId", value = "Unique identifier of the report template") @PathVariable("templateId") Long templateId,
            @ApiParam(name = "sectionName", value = "Name of the new section") @PathVariable("sectionName") String sectionName,
            @ApiParam(name = "userId", value = "Unique identifier of the logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        /*
         * grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);
         * grantValidator.validateTemplateExists(grantService,grantToSave,templateId);
         */
        Report report = saveReport(reportId, reportToSave, userId, tenantCode);

        ReportSpecificSection specificSection = new ReportSpecificSection();
        specificSection.setGranter((Granter) organizationService.findOrganizationByTenantCode(tenantCode));
        specificSection.setSectionName(sectionName);

        specificSection.setReportTemplateId(templateId);
        specificSection.setDeletable(true);
        specificSection.setReportId(reportId);
        specificSection.setSectionOrder(reportService
                .getNextSectionOrder(organizationService.findOrganizationByTenantCode(tenantCode).getId(), templateId));
        specificSection = reportService.saveSection(specificSection);

        if (reportService._checkIfReportTemplateChanged(report, specificSection, null, this)) {
            GranterReportTemplate newTemplate = reportService._createNewReportTemplateFromExisiting(report);
            templateId = newTemplate.getId();
        }

        report = _ReportToReturn(report, userId);
        return new ReportSectionInfo(specificSection.getId(), specificSection.getSectionName(), report);

    }

    @PutMapping("/{reportId}/template/{templateId}/section/{sectionId}")
    @ApiOperation("Delete existing section in report")
    public Report deleteSection(@RequestBody Report reportToSave,
            @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "templateId", value = "Unique identifier of the grant template") @PathVariable("templateId") Long templateId,
            @ApiParam(name = "sectionId", value = "Unique identifier of the section being deleted") @PathVariable("sectionId") Long sectionId,
            @ApiParam(name = "userId", value = "Unique identifier of the logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        /*
         * grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);
         * grantValidator.validateTemplateExists(grantService,grantToSave,templateId);
         * grantValidator.validateSectionExists(grantService,grantToSave,sectionId);
         */
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
        if (reportService._checkIfReportTemplateChanged(report, section, null, this)) {
            GranterReportTemplate newTemplate = reportService._createNewReportTemplateFromExisiting(report);
            templateId = newTemplate.getId();
        }
        report = _ReportToReturn(report, userId);
        return report;
    }

    @PostMapping("/{reportId}/assignment")
    @ApiOperation("Set owners for report workflow states")
    public Report saveReportAssignments(
            @ApiParam(name = "userId", value = "Unique identifier of logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "assignmentModel", value = "Set assignment for report per workflow state") @RequestBody ReportAssignmentModel assignmentModel,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        Report report = saveReport(reportId, assignmentModel.getReport(), userId, tenantCode);

        Map<Long, Long> currentAssignments = new LinkedHashMap();
        if (reportService.checkIfReportMovedThroughWFAtleastOnce(report.getId())) {

            reportService.getAssignmentsForReport(report).stream().forEach(a -> {
                currentAssignments.put(a.getStateId(), a.getAssignment());
            });
        }
        String customAss = null;
        UriComponents uriComponents = ServletUriComponentsBuilder.fromCurrentContextPath().build();
        String host = uriComponents.getHost().substring(uriComponents.getHost().indexOf(".") + 1);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance().scheme(uriComponents.getScheme())
                .host(host).port(uriComponents.getPort());
        String url = uriBuilder.toUriString();
        for (ReportAssignmentsVO assignmentsVO : assignmentModel.getAssignments()) {
            if (customAss == null && assignmentsVO.getCustomAssignments() != null) {
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
            assignment.setUpdatedBy(userId);
            assignment.setAssignedOn(DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0).toDate());

            if ((customAss != null && !"".equalsIgnoreCase(customAss.trim())) && workflowStatusService
                    .getById(assignmentsVO.getStateId()).getInternalStatus().equalsIgnoreCase("ACTIVE")) {
                String[] customAssignments = customAss.split(",");
                User granteeUser = null;
                User existingUser = userService.getUserByEmailAndOrg(customAss, report.getGrant().getOrganization());
                ObjectMapper mapper = new ObjectMapper();
                String code = null;

                code = Base64.getEncoder().encodeToString(String.valueOf(report.getId()).getBytes());

                if (existingUser != null && existingUser.isActive()) {
                    granteeUser = existingUser;
                    url = url + "/home/?action=login&org="
                            + URLEncoder.encode(report.getGrant().getOrganization().getName()) + "&r=" + code
                            + "&email=" + granteeUser.getEmailId() + "&type=report";
                } else if (existingUser != null && !existingUser.isActive()) {
                    granteeUser = existingUser;
                    url = url + "/home/?action=registration&org="
                            + URLEncoder.encode(report.getGrant().getOrganization().getName()) + "&r=" + code
                            + "&email=" + granteeUser.getEmailId() + "&type=report";

                } else {
                    granteeUser = new User();
                    Role newRole = roleService.findByOrganizationAndName(report.getGrant().getOrganization(), "Admin");

                    UserRole userRole = new UserRole();
                    userRole.setRole(newRole);
                    userRole.setUser(granteeUser);

                    List<UserRole> userRoles = new ArrayList<>();
                    userRoles.add(userRole);
                    granteeUser.setUserRoles(userRoles);
                    granteeUser.setFirstName("");
                    granteeUser.setLastName("");
                    granteeUser.setEmailId(customAss);
                    granteeUser.setOrganization(report.getGrant().getOrganization());
                    granteeUser.setActive(false);
                    granteeUser = userService.save(granteeUser);
                    userRole = userRoleService.saveUserRole(userRole);
                    url = url + "/home/?action=registration&org="
                            + URLEncoder.encode(report.getGrant().getOrganization().getName()) + "&r=" + code
                            + "&email=" + granteeUser.getEmailId() + "&type=report";
                }

                String[] notifications = reportService.buildReportInvitationContent(report,
                        userService.getUserById(userId),
                        appConfigService.getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(),
                                AppConfiguration.REPORT_INVITE_SUBJECT).getConfigValue(),
                        appConfigService.getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(),
                                AppConfiguration.REPORT_INVITE_MESSAGE).getConfigValue(),
                        url);
                commonEmailSevice.sendMail(new String[] { !granteeUser.isDeleted() ? granteeUser.getEmailId() : null },
                        null, notifications[0], notifications[1],
                        new String[] { appConfigService
                                .getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.PLATFORM_EMAIL_FOOTER)
                                .getConfigValue()
                                .replaceAll("%RELEASE_VERSION%", releaseService.getCurrentRelease().getVersion()) });

                assignment.setAssignment(granteeUser.getId());

                /*
                 * if (assignmentsVO.getAssignmentId() == 0) {
                 * assignment.setAssignment(granteeUser.getId()); } else { ReportAssignment ass
                 * = new ReportAssignment(); ass.setAssignment(granteeUser.getId());
                 * ass.setReportId(reportId); ass.setStateId(assignmentsVO.getStateId());
                 * ass.setAnchor(false); reportService.saveAssignmentForReport(ass); }
                 */
                /*
                 * for (String customAssignment : customAssignments) { User granteeUser = new
                 * User(); Role newRole = new Role(); newRole.setName("Admin");
                 * newRole.setOrganization(report.getGrant().getOrganization());
                 * newRole.setCreatedAt(DateTime.now().toDate());
                 * newRole.setCreatedBy("System");
                 * 
                 * newRole = roleService.saveRole(newRole); UserRole userRole = new UserRole();
                 * userRole.setRole(newRole); userRole.setUser(granteeUser); List<UserRole>
                 * userRoles = new ArrayList<>(); userRoles.add(userRole);
                 * granteeUser.setUserRoles(userRoles); granteeUser.setFirstName("To be set");
                 * granteeUser.setLastName("To be set"); granteeUser.setEmailId(customAss);
                 * granteeUser.setOrganization(report.getGrant().getOrganization()); granteeUser
                 * = userService.save(granteeUser);
                 * 
                 * if(assignmentsVO.getAssignmentId()==0){
                 * assignment.setAssignment(granteeUser.getId()); }else{ ReportAssignment ass =
                 * new ReportAssignment(); ass.setAssignment(granteeUser.getId());
                 * ass.setReportId(reportId); ass.setStateId(assignmentsVO.getStateId());
                 * ass.setAnchor(false); reportService.saveAssignmentForReport(ass); } }
                 */
            }

            assignment = reportService.saveAssignmentForReport(assignment);
        }

        if (currentAssignments.size() > 0) {

            List<ReportAssignment> newAssignments = reportService.getAssignmentsForReport(report);

            String[] notifications = reportService.buildEmailNotificationContent(report,
                    userService.getUserById(userId), null, null, null,
                    appConfigService.getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.OWNERSHIP_CHANGED_EMAIL_SUBJECT).getConfigValue(),
                    appConfigService.getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.OWNERSHIP_CHANGED_EMAIL_MESSAGE).getConfigValue(),
                    null, null, null, null, null, null, null, null, null, null, null, null, currentAssignments,
                    newAssignments);
            List<User> toUsers = newAssignments.stream().map(a -> a.getAssignment())
                    .map(uid -> userService.getUserById(uid)).collect(Collectors.toList());
            toUsers.removeIf(u -> u.isDeleted());
            List<User> ccUsers = currentAssignments.values().stream().map(uid -> userService.getUserById(uid))
                    .collect(Collectors.toList());
            ccUsers.removeIf(u -> u.isDeleted());

            commonEmailSevice
                    .sendMail(
                            toUsers.stream().map(u -> u.getEmailId()).collect(Collectors.toList())
                                    .toArray(new String[toUsers.size()]),
                            ccUsers.stream().map(u -> u.getEmailId()).collect(
                                    Collectors.toList()).toArray(new String[ccUsers.size()]),
                            notifications[0], notifications[1],
                            new String[] { appConfigService
                                    .getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(),
                                            AppConfiguration.PLATFORM_EMAIL_FOOTER)
                                    .getConfigValue().replaceAll("%RELEASE_VERSION%",
                                            releaseService.getCurrentRelease().getVersion()) });

            Map<Long, Long> cleanAsigneesList = new HashMap();
            for (Long ass : currentAssignments.values()) {
                cleanAsigneesList.put(ass, ass);
            }
            for (ReportAssignment ass : newAssignments) {
                cleanAsigneesList.put(ass.getAssignment(), ass.getAssignment());
            }
            notifications = reportService.buildEmailNotificationContent(report, userService.getUserById(userId), null,
                    null, null,
                    appConfigService.getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.OWNERSHIP_CHANGED_EMAIL_SUBJECT).getConfigValue(),
                    appConfigService.getAppConfigForGranterOrg(report.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.OWNERSHIP_CHANGED_EMAIL_MESSAGE).getConfigValue(),
                    null, null, null, null, null, null, null, null, null, null, null, null, currentAssignments,
                    newAssignments);

            final String[] finaNotifications = notifications;
            final Report finalGrant = report;

            cleanAsigneesList.keySet().stream().forEach(u -> {

                notificationsService.saveNotification(finaNotifications, u, finalGrant.getId(), "REPORT");
            });

        }

        report = _ReportToReturn(report, userId);
        return report;
    }

    @GetMapping("{reportId}/changeHistory")
    public PlainReport getReportHistory(@PathVariable("reportId") Long reportId,
            @PathVariable("userId") Long userId) throws IOException {

        Report report = reportService.getReportById(reportId);
        ReportSnapshot snapshot = reportSnapshotService.getMostRecentSnapshotByReportId(reportId);

        if(snapshot==null){
            return null;
        }

        report.setName(snapshot.getName());
        report.setStartDate(snapshot.getStartDate());
        report.setEndDate(snapshot.getEndDate());
        report.setStatus(workflowStatusService.findById(snapshot.getStatusId()));
        report.setDueDate(snapshot.getDueDate());
        ReportDetailVO details = new ObjectMapper().readValue(snapshot.getStringAttributes(),ReportDetailVO.class);
        report.setReportDetails(details);

        PlainReport reportToReturn = reportService.reportToPlain(report);
        return reportToReturn;
    }

    @PostMapping("/{reportId}/flow/{fromState}/{toState}")
    @ApiOperation("Move report through workflow")
    public Report MoveReportState(@RequestBody ReportWithNote reportWithNote,
            @ApiParam(name = "userId", value = "Unique identified of logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "fromStateId", value = "Unique identifier of the starting state of the report in the workflow") @PathVariable("fromState") Long fromStateId,
            @ApiParam(name = "toStateId", value = "Unique identifier of the ending state of the report in the workflow") @PathVariable("toState") Long toStateId,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {

        /*
         * grantValidator.validate(grantService,grantId,reportWithNote.getGrant(),userId
         * ,tenantCode);
         * grantValidator.validateFlow(grantService,reportWithNote.getGrant(),grantId,
         * userId,fromStateId,toStateId);
         */

        /*
         * for (SectionVO section :
         * reportWithNote.getReport().getReportDetails().getSections()) { if
         * (section.getAttributes() != null) { for (SectionAttributesVO attribute :
         * section.getAttributes()) { if
         * (attribute.getFieldType().equalsIgnoreCase("disbursement")) { List<String>
         * rowNames = new ArrayList<>(); if (attribute.getFieldTableValue().size() > 1)
         * { for (int i = 0; i < attribute.getFieldTableValue().size(); i++) { if
         * (attribute.getFieldTableValue().get(i).getColumns()[0].getValue().trim() ==
         * "" && attribute.getFieldTableValue().get(i).getColumns()[1].getValue().trim()
         * == "" &&
         * attribute.getFieldTableValue().get(i).getColumns()[2].getValue().trim() == ""
         * && attribute.getFieldTableValue().get(i).getColumns()[3].getValue() .trim()
         * == "") { rowNames.add(attribute.getFieldTableValue().get(i).getName()); } } }
         * 
         * for (String rowName : rowNames) { attribute.getFieldTableValue().removeIf(e
         * -> e.getName().equalsIgnoreCase(rowName)); }
         * 
         * for (int i = 0; i < attribute.getFieldTableValue().size(); i++) {
         * attribute.getFieldTableValue().get(i).setName(String.valueOf(i + 1)); try {
         * attribute.setFieldValue( new
         * ObjectMapper().writeValueAsString(attribute.getFieldTableValue())); } catch
         * (JsonProcessingException e) { logger.error(e.getMessage(), e); } }
         * 
         * } } } }
         */

        saveReport(reportId, reportWithNote.getReport(), userId, tenantCode);

        Report report = reportService.getReportById(reportId);
        Report finalReport = report;
        WorkflowStatus previousState = report.getStatus();

        User updatingUser = userService.getUserById(userId);
        if (updatingUser.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")
                && previousState.getInternalStatus().equalsIgnoreCase("ACTIVE")) {
            ReportAssignment changedAssignment = reportService.getAssignmentsForReport(report).stream()
                    .filter(ass -> ass.getReportId().longValue() == reportId.longValue()
                            && ass.getStateId().longValue() == finalReport.getStatus().getId().longValue())
                    .collect(Collectors.toList()).get(0);
            changedAssignment.setAssignment(userId);
            reportService.saveAssignmentForReport(changedAssignment);
        }
        ReportAssignment currentAssignment = reportService.getAssignmentsForReport(report).stream()
                .filter(ass -> ass.getReportId().longValue() == reportId.longValue()
                        && ass.getStateId().longValue() == finalReport.getStatus().getId().longValue())
                .collect(Collectors.toList()).get(0);
        User previousOwner = userService.getUserById(currentAssignment.getAssignment());

        report.setStatus(workflowStatusService.findById(toStateId));

        report.setNote((reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase(""))
                ? reportWithNote.getNote()
                : "No note added");
        report.setNoteAdded(new Date());
        report.setNoteAddedBy(userId);

        Date currentDateTime = DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0).toDate();
        report.setUpdatedAt(currentDateTime);
        report.setUpdatedBy(userId);
        report.setMovedOn(currentDateTime);
        report = reportService.saveReport(report);

        User user = userService.getUserById(userId);
        WorkflowStatus toStatus = workflowStatusService.findById(toStateId);

        List<User> usersToNotify = new ArrayList<>();// userService.usersToNotifyOnWorkflowSateChangeTo(toStateId);

        List<ReportAssignment> assigments = reportService.getAssignmentsForReport(report);
        assigments.forEach(ass -> {
            if (!usersToNotify.stream().filter(u -> u.getId() == ass.getAssignment()).findFirst().isPresent()) {
                usersToNotify.add(userService.getUserById(ass.getAssignment()));
            }
        });

        Optional<ReportAssignment> repAss = reportService.getAssignmentsForReport(report).stream()
                .filter(ass -> ass.getReportId().longValue() == reportId.longValue()
                        && ass.getStateId().longValue() == toStateId.longValue())
                .findAny();
        User currentOwner = null;
        String currentOwnerName = "";
        if (repAss.isPresent()) {
            currentOwner = userService.getUserById(repAss.get().getAssignment());
            currentOwnerName = currentOwner.getFirstName().concat(" ").concat(currentOwner.getLastName());
        }

        WorkflowStatusTransition transition = workflowStatusTransitionService.findByFromAndToStates(previousState,
                toStatus);

        WorkflowStatus currentState = workflowStatusService.findById(toStateId);
        if (!updatingUser.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")
                && !currentState.getInternalStatus().equalsIgnoreCase("ACTIVE")
                && !currentState.getInternalStatus().equalsIgnoreCase("CLOSED")) {
            usersToNotify.removeIf(u -> u.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE"));
        }

        String finalCurrentOwnerName = currentOwnerName;
        User finalCurrentOwner = currentOwner;
        if (toStatus.getInternalStatus().equalsIgnoreCase("ACTIVE")) {
            usersToNotify
                    .removeIf(u -> u.getId().longValue() == finalCurrentOwner.getId().longValue() || u.isDeleted());
            String emailNotificationContent[] = reportService.buildEmailNotificationContent(finalReport,
                    finalCurrentOwner, currentOwner.getFirstName().concat(" ").concat(currentOwner.getLastName()), null,
                    new SimpleDateFormat("dd-MMM-yyyy").format(DateTime.now().toDate()),
                    appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.REPORT_STATE_CHANGED_MAIL_SUBJECT).getConfigValue(),
                    appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.REPORT_STATE_CHANGED_MAIL_MESSAGE).getConfigValue(),
                    workflowStatusService.findById(toStateId).getName(), finalCurrentOwnerName, previousState.getName(),
                    previousOwner.getFirstName().concat(" ").concat(previousOwner.getLastName()),
                    transition.getAction(), "Yes", "Please review.",
                    reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("") ? "Yes"
                            : "No",
                    reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                            ? "Please review."
                            : "",
                    null, null, null, null, null);
            commonEmailSevice
                    .sendMail(new String[] { !currentOwner.isDeleted() ? currentOwner.getEmailId() : null },
                            usersToNotify.stream().map(mapper -> mapper.getEmailId()).collect(Collectors.toList())
                                    .toArray(new String[usersToNotify.size()]),
                            emailNotificationContent[0], emailNotificationContent[1],
                            new String[] { appConfigService
                                    .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                            AppConfiguration.PLATFORM_EMAIL_FOOTER)
                                    .getConfigValue().replaceAll("%RELEASE_VERSION%",
                                            releaseService.getCurrentRelease().getVersion()) });

            String notificationContent[] = reportService.buildEmailNotificationContent(finalReport, currentOwner,
                    currentOwner.getFirstName().concat(" ").concat(currentOwner.getLastName()), toStatus.getVerb(),
                    new SimpleDateFormat("dd-MMM-yyyy").format(DateTime.now().toDate()),
                    appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.REPORT_STATE_CHANGED_MAIL_SUBJECT).getConfigValue(),
                    appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.REPORT_STATE_CHANGED_MAIL_MESSAGE).getConfigValue(),
                    workflowStatusService.findById(toStateId).getName(), finalCurrentOwnerName, previousState.getName(),
                    previousOwner.getFirstName().concat(" ").concat(previousOwner.getLastName()),
                    transition.getAction(), "Yes", "Please review.",
                    reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("") ? "Yes"
                            : "No",
                    reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                            ? "Please review."
                            : "",
                    null, null, null, null, null);

            notificationsService.saveNotification(notificationContent, currentOwner.getId(), finalReport.getId(),
                    "REPORT");

            usersToNotify.stream().forEach(u -> {
                final String[] nc = reportService.buildEmailNotificationContent(finalReport, u,
                        u.getFirstName().concat(" ").concat(u.getLastName()), toStatus.getVerb(),
                        new SimpleDateFormat("dd-MMM-yyyy").format(DateTime.now().toDate()),
                        appConfigService
                                .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.REPORT_STATE_CHANGED_MAIL_SUBJECT)
                                .getConfigValue(),
                        appConfigService
                                .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.REPORT_STATE_CHANGED_MAIL_MESSAGE)
                                .getConfigValue(),
                        workflowStatusService.findById(toStateId).getName(), finalCurrentOwnerName,
                        previousState.getName(),
                        previousOwner.getFirstName().concat(" ").concat(previousOwner.getLastName()),
                        transition.getAction(), "Yes", "Please review.",
                        reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                                ? "Yes"
                                : "No",
                        reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                                ? "Please review."
                                : "",
                        null, null, null, null, null);

                notificationsService.saveNotification(nc, u.getId(), finalReport.getId(), "REPORT");
            });
        } else if (!toStatus.getInternalStatus().equalsIgnoreCase("CLOSED")) {
            usersToNotify
                    .removeIf(u -> u.getId().longValue() == finalCurrentOwner.getId().longValue() || u.isDeleted());
            if (!workflowStatusService.findById(fromStateId).getInternalStatus().equalsIgnoreCase("ACTIVE")) {
                usersToNotify.removeIf(u -> u.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE"));
            }

            String emailNotificationContent[] = reportService.buildEmailNotificationContent(finalReport,
                    finalCurrentOwner, currentOwner.getFirstName().concat(" ").concat(currentOwner.getLastName()), null,
                    new SimpleDateFormat("dd-MMM-yyyy").format(DateTime.now().toDate()),
                    appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.REPORT_STATE_CHANGED_MAIL_SUBJECT).getConfigValue(),
                    appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.REPORT_STATE_CHANGED_MAIL_MESSAGE).getConfigValue(),
                    workflowStatusService.findById(toStateId).getName(), finalCurrentOwnerName, previousState.getName(),
                    previousOwner.getFirstName().concat(" ").concat(previousOwner.getLastName()),
                    transition.getAction(), "Yes", "Please review.",
                    reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("") ? "Yes"
                            : "No",
                    reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                            ? "Please review."
                            : "",
                    null, null, null, null, null);
            commonEmailSevice
                    .sendMail(new String[] { !currentOwner.isDeleted() ? currentOwner.getEmailId() : null },
                            usersToNotify.stream().map(mapper -> mapper.getEmailId()).collect(Collectors.toList())
                                    .toArray(new String[usersToNotify.size()]),
                            emailNotificationContent[0], emailNotificationContent[1],
                            new String[] { appConfigService
                                    .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                            AppConfiguration.PLATFORM_EMAIL_FOOTER)
                                    .getConfigValue().replaceAll("%RELEASE_VERSION%",
                                            releaseService.getCurrentRelease().getVersion()) });

            String notificationContent[] = reportService.buildEmailNotificationContent(finalReport, currentOwner,
                    currentOwner.getFirstName().concat(" ").concat(currentOwner.getLastName()), toStatus.getVerb(),
                    new SimpleDateFormat("dd-MMM-yyyy").format(DateTime.now().toDate()),
                    appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.REPORT_STATE_CHANGED_MAIL_SUBJECT).getConfigValue(),
                    appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                            AppConfiguration.REPORT_STATE_CHANGED_MAIL_MESSAGE).getConfigValue(),
                    workflowStatusService.findById(toStateId).getName(), finalCurrentOwnerName, previousState.getName(),
                    previousOwner.getFirstName().concat(" ").concat(previousOwner.getLastName()),
                    transition.getAction(), "Yes", "Please review.",
                    reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("") ? "Yes"
                            : "No",
                    reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                            ? "Please review."
                            : "",
                    null, null, null, null, null);

            notificationsService.saveNotification(notificationContent, currentOwner.getId(), finalReport.getId(),
                    "REPORT");

            usersToNotify.stream().forEach(u -> {
                final String[] nc = reportService.buildEmailNotificationContent(finalReport, u,
                        u.getFirstName().concat(" ").concat(u.getLastName()), toStatus.getVerb(),
                        new SimpleDateFormat("dd-MMM-yyyy").format(DateTime.now().toDate()),
                        appConfigService
                                .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.REPORT_STATE_CHANGED_MAIL_SUBJECT)
                                .getConfigValue(),
                        appConfigService
                                .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.REPORT_STATE_CHANGED_MAIL_MESSAGE)
                                .getConfigValue(),
                        workflowStatusService.findById(toStateId).getName(), finalCurrentOwnerName,
                        previousState.getName(),
                        previousOwner.getFirstName().concat(" ").concat(previousOwner.getLastName()),
                        transition.getAction(), "Yes", "Please review.",
                        reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                                ? "Yes"
                                : "No",
                        reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                                ? "Please review."
                                : "",
                        null, null, null, null, null);

                notificationsService.saveNotification(nc, u.getId(), finalReport.getId(), "REPORT");
            });
        } else {

            Optional<User> granteeUsr =  usersToNotify.stream()
                    .filter(u -> u.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")).findFirst();
                    if(granteeUsr.isPresent()){
                        User granteeUser = granteeUsr.get();
                        usersToNotify.removeIf(u -> u.getId().longValue() == granteeUser.getId().longValue() || u.isDeleted());

                        String emailNotificationContent[] = reportService.buildEmailNotificationContent(finalReport, granteeUser,
                                granteeUser.getFirstName().concat(" ").concat(granteeUser.getLastName()), null,
                                new SimpleDateFormat("dd-MMM-yyyy").format(DateTime.now().toDate()),
                                appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.REPORT_STATE_CHANGED_MAIL_SUBJECT).getConfigValue(),
                                appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.REPORT_STATE_CHANGED_MAIL_MESSAGE).getConfigValue(),
                                workflowStatusService.findById(toStateId).getName(), finalCurrentOwnerName, previousState.getName(),
                                previousOwner.getFirstName().concat(" ").concat(previousOwner.getLastName()),
                                transition.getAction(), "Yes", "Please review.",
                                reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("") ? "Yes"
                                        : "No",
                                reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                                        ? "Please review."
                                        : "",
                                null, null, null, null, null);
                        commonEmailSevice
                                .sendMail(new String[] { !granteeUser.isDeleted() ? granteeUser.getEmailId() : null },
                                        usersToNotify.stream().map(mapper -> mapper.getEmailId()).collect(Collectors.toList())
                                                .toArray(new String[usersToNotify.size()]),
                                        emailNotificationContent[0], emailNotificationContent[1],
                                        new String[] { appConfigService
                                                .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                                        AppConfiguration.PLATFORM_EMAIL_FOOTER)
                                                .getConfigValue().replaceAll("%RELEASE_VERSION%",
                                                releaseService.getCurrentRelease().getVersion()) });

                        String notificationContent[] = reportService.buildEmailNotificationContent(finalReport, granteeUser,
                                granteeUser.getFirstName().concat(" ").concat(granteeUser.getLastName()), toStatus.getVerb(),
                                new SimpleDateFormat("dd-MMM-yyyy").format(DateTime.now().toDate()),
                                appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.REPORT_STATE_CHANGED_MAIL_SUBJECT).getConfigValue(),
                                appConfigService.getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.REPORT_STATE_CHANGED_MAIL_MESSAGE).getConfigValue(),
                                workflowStatusService.findById(toStateId).getName(), finalCurrentOwnerName, previousState.getName(),
                                previousOwner.getFirstName().concat(" ").concat(previousOwner.getLastName()),
                                transition.getAction(), "Yes", "Please review.",
                                reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("") ? "Yes"
                                        : "No",
                                reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                                        ? "Please review."
                                        : "",
                                null, null, null, null, null);

                        notificationsService.saveNotification(notificationContent, granteeUser.getId(), finalReport.getId(),
                                "REPORT");
                    }


            usersToNotify.stream().forEach(u -> {
                final String[] nc = reportService.buildEmailNotificationContent(finalReport, u,
                        u.getFirstName().concat(" ").concat(u.getLastName()), toStatus.getVerb(),
                        new SimpleDateFormat("dd-MMM-yyyy").format(DateTime.now().toDate()),
                        appConfigService
                                .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.REPORT_STATE_CHANGED_MAIL_SUBJECT)
                                .getConfigValue(),
                        appConfigService
                                .getAppConfigForGranterOrg(finalReport.getGrant().getGrantorOrganization().getId(),
                                        AppConfiguration.REPORT_STATE_CHANGED_MAIL_MESSAGE)
                                .getConfigValue(),
                        workflowStatusService.findById(toStateId).getName(), finalCurrentOwnerName,
                        previousState.getName(),
                        previousOwner.getFirstName().concat(" ").concat(previousOwner.getLastName()),
                        transition.getAction(), "Yes", "Please review.",
                        reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                                ? "Yes"
                                : "No",
                        reportWithNote.getNote() != null && !reportWithNote.getNote().trim().equalsIgnoreCase("")
                                ? "Please review."
                                : "",
                        null, null, null, null, null);

                notificationsService.saveNotification(nc, u.getId(), finalReport.getId(), "REPORT");
            });

        }

        report = _ReportToReturn(report, userId);
        _saveSnapShot(report, fromStateId, toStateId, currentOwner, previousOwner);

        if (toStatus.getInternalStatus().equalsIgnoreCase("CLOSED")) {
            List<WorkflowStatus> workflowStatuses = workflowStatusService.getTenantWorkflowStatuses("DISBURSEMENT",
                    report.getGrant().getGrantorOrganization().getId());
            final Report fReport = report;
            List<WorkflowStatus> draftStatuses = workflowStatuses.stream()
                    .filter(ws -> ws.getInternalStatus().equalsIgnoreCase("DRAFT")).collect(Collectors.toList());
            List<Long> draftStatusIds = draftStatuses.stream().mapToLong(s -> s.getId()).boxed()
                    .collect(Collectors.toList());
            List<Disbursement> draftDisbursements = disbursementService
                    .getDibursementsForGrantByStatuses(report.getGrant().getId(), draftStatusIds);


            WorkflowStatus closedtatus = workflowStatuses.stream()
                    .filter(ws -> ws.getInternalStatus().equalsIgnoreCase("CLOSED")).collect(Collectors.toList())
                    .get(0);

            if (draftDisbursements != null && draftDisbursements.size() > 0) {
                draftDisbursements
                        .removeIf(dd -> (dd.getReportId()==null || dd.getReportId().longValue() != fReport.getId().longValue() && dd.isGranteeEntry()));
                if (draftDisbursements != null && draftDisbursements.size() > 0) {
                    for (Disbursement d : draftDisbursements) {
                        d.setStatus(closedtatus);
                        d.setMovedOn(fReport.getMovedOn());
                        List<ActualDisbursement> ads = disbursementService.getActualDisbursementsForDisbursement(d);
                        if (ads != null && ads.size() > 0) {
                            for (ActualDisbursement ad : ads) {
                                 ad.setStatus(false);
                                ad.setSaved(true);
                            }
                        }
                        disbursementService.saveDisbursement(d);
                    }
                }
            }

        }

        // Temporary block to continue testing as Grantee has submitted the report
        /*
         * if (toStatus.getInternalStatus().equalsIgnoreCase("ACTIVE")) { ReportWithNote
         * withNote = new ReportWithNote();
         * 
         * report.getReportDetails().getSections().forEach(sec -> { if
         * (sec.getAttributes() != null) { sec.getAttributes().forEach(attr -> { if
         * (attr.getFieldType().equalsIgnoreCase("kpi")) {
         * attr.setActualTarget(String.valueOf(Math.round(Math.random() * 100))); } });
         * } });
         * 
         * report = saveReport(reportId, report, userId, tenantCode);
         * 
         * withNote.setNote("Grantee has submitted"); withNote.setReport(report); final
         * Report fReport = report; ReportAssignmentsVO ass =
         * report.getWorkflowAssignments().stream().filter(a ->
         * a.getStateId().longValue() ==
         * fReport.getStatus().getId().longValue()).findFirst().get();
         * 
         * MoveReportState(withNote,
         * userService.getUserById(ass.getAssignmentId()).getId(), reportId,
         * report.getStatus().getId(),
         * workflowStatusTransitionService.getByFromStatus(report.getStatus()).
         * getToState().getId(), tenantCode); }
         */

        return report;

    }

    private void _saveSnapShot(Report report, Long fromStatusId, Long toStatusId, User currentUser, User previousUser) {

        try {
            // for (AssignedTo assignment : report.getCurrentAssignment()) {
            ReportSnapshot snapshot = new ReportSnapshot();
            snapshot.setAssignedToId(currentUser!=null?currentUser.getId():null);
            snapshot.setEndDate(report.getEndDate());
            snapshot.setDueDate(report.getDueDate());
            snapshot.setReportId(report.getId());
            snapshot.setName(report.getName());
            snapshot.setStartDate(report.getStartDate());
            snapshot.setStatusId(fromStatusId);
            String stringAttribs = new ObjectMapper().writeValueAsString(report.getReportDetails());
            snapshot.setStringAttributes(stringAttribs);
            snapshot.setFromStringAttributes(stringAttribs);
            snapshot.setAssignedToId(currentUser!=null?currentUser.getId():null);
            snapshot.setMovedBy(previousUser.getId());
            snapshot.setFromNote(report.getNote());
            snapshot.setFromStateId(fromStatusId);
            snapshot.setToStateId(toStatusId);
            snapshot.setMovedOn(report.getMovedOn());
            reportSnapshotService.saveReportSnapshot(snapshot);
            // }
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @GetMapping("/{reportId}/history/")
    public List<ReportHistory> getReportHistory(@PathVariable("reportId") Long reportId,
            @PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode) {

        /*List<ReportHistory> history = null;
        User user = userService.getUserById(userId);
        if (user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTER")) {
            history = reportService.getReportHistory(reportId);

        } else if (user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")) {
            history = reportService.getReportHistoryForGrantee(reportId, user.getId());
        }
        for (ReportHistory historyEntry : history) {
            historyEntry.setNoteAddedByUser(userService.getUserById(historyEntry.getNoteAddedBy()));
        }
        return history;*/

        List<ReportHistory> history = new ArrayList();
        List<ReportSnapshot> reportSnapshotHistory = reportSnapshotService.getReportSnapshotForReport(reportId);
        if (reportSnapshotHistory == null
                || (reportSnapshotHistory != null && reportSnapshotHistory.get(0).getFromStateId() == null)) {
            history = reportService.getReportHistory(reportId);
            for (ReportHistory historyEntry : history) {
                historyEntry.setNoteAddedByUser(userService.getUserById(historyEntry.getNoteAddedBy()));
            }
        } else {
            for (ReportSnapshot snapShot : reportSnapshotHistory) {
                ReportHistory hist = new ReportHistory();
                hist.setName(snapShot.getName());
                hist.setId(snapShot.getReportId());
                hist.setNote(snapShot.getFromNote());
                hist.setNoteAdded(snapShot.getMovedOn());
                User assignedBy = userService.getUserById(snapShot.getMovedBy());
                hist.setNoteAddedBy(assignedBy.getId());
                hist.setNoteAddedByUser(assignedBy);
                hist.setStatus(workflowStatusService.findById(snapShot.getFromStateId()));
                history.add(hist);
            }
        }

        return history;

    }

    @PostMapping("/{reportId}/section/{sectionId}/field/{fieldId}")
    @ApiOperation("Delete field in a section")
    public Report deleteField(
            @ApiParam(name = "reportToSave", value = "Report to save if in edit mode, passed in Body of request") @RequestBody Report reportToSave,
            @ApiParam(name = "userId", value = "Unique identifier of the logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "sectionId", value = "Unique identifier of the section being modified") @PathVariable("sectionId") Long sectionId,
            @ApiParam(name = "fieldId", value = "Unique identifier of the field being deleted") @PathVariable("fieldId") Long fieldId,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        /*
         * grantValidator.validate(grantService,grantId,grantToSave,userId,tenantCode);
         * grantValidator.validateSectionExists(grantService,grantToSave,sectionId);
         * grantValidator.validateFieldExists(grantService,grantToSave,sectionId,fieldId
         * );
         */
        Report report = saveReport(reportId, reportToSave, userId, tenantCode);

        ReportStringAttribute stringAttrib = reportService.getReportStringByStringAttributeId(fieldId);
        ReportSpecificSectionAttribute attribute = stringAttrib.getSectionAttribute();

        if (stringAttrib.getSectionAttribute().getFieldType().equalsIgnoreCase("document")) {
            List<ReportStringAttributeAttachments> attachments = reportService
                    .getStringAttributeAttachmentsByStringAttribute(stringAttrib);
            reportService.deleteStringAttributeAttachments(attachments);
        }
        reportService.deleteStringAttribute(stringAttrib);
        reportService.deleteSectionAttribute(attribute);
        ReportStringAttribute rsa2Delete = report.getStringAttributes().stream()
                .filter(g -> g.getId() == stringAttrib.getId()).findFirst().get();
        report.getStringAttributes().remove(rsa2Delete);
        report = reportService.saveReport(report);

        if (reportService._checkIfReportTemplateChanged(report, attribute.getSection(), null, this)) {
            GranterReportTemplate newTemplate = reportService._createNewReportTemplateFromExisiting(report);
        }
        report = _ReportToReturn(report, userId);
        return report;
    }

    @GetMapping("/templates")
    @ApiOperation("Get all published grant templates for tenant")
    public List<GranterReportTemplate> getTenantPublishedReportTemplates(
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode,
            @PathVariable("userId") Long userId) {
        return reportService.findByGranterIdAndPublishedStatusAndPrivateStatus(
                organizationService.findOrganizationByTenantCode(tenantCode).getId(), true, false);
    }

    @PutMapping("/{reportId}/template/{templateId}/{templateName}")
    @ApiOperation("Save custom grant template with name and description")
    public Report updateTemplateName(
            @ApiParam(name = "userId", value = "Unique identifier of logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "templateId", value = "Unique identfier of the grant template") @PathVariable("templateId") Long templateId,
            @ApiParam(name = "templateName", value = "NName of the template to be saved") @PathVariable("templateName") String templateName,
            @ApiParam(name = "templateDate", value = "Additional information about the template such as descriptio, publish or save as private") @RequestBody TemplateMetaData templateData) {

        GranterReportTemplate template = granterReportTemplateService.findByTemplateId(templateId);
        template.setName(templateName);
        template.setDescription(templateData.getDescription());
        template.setPublished(templateData.isPublish());
        template.setPrivateToReport(templateData.isPrivateToGrant());
        template.setPublished(true);
        reportService.saveReportTemplate(template);

        Report report = reportService.getReportById(reportId);
        report = _ReportToReturn(report, userId);
        return report;

    }

    @GetMapping("/create/grant/{grantId}/template/{templateId}")
    @ApiOperation("Create new report with a template")
    public Report createReport(
            @ApiParam(name = "grantId", value = "Unique identifier for the selected grant") @PathVariable("grantId") Long grantId,
            @ApiParam(name = "templateId", value = "Unique identifier for the selected template") @PathVariable("templateId") Long templateId,
            @PathVariable("userId") Long userId,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode) {
        Report report = new Report();
        Grant reportForGrant = grantService.getById(grantId);
        GranterReportTemplate reportTemplate = reportService.findByTemplateId(templateId);

        report.setName("");
        report.setStartDate(null);
        report.setStDate("");
        report.setStatus(workflowStatusService.findInitialStatusByObjectAndGranterOrgId("REPORT",
                organizationService.findOrganizationByTenantCode(tenantCode).getId(),reportForGrant.getGrantTypeId()));
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
        List<WorkflowStatus> statuses = new ArrayList<>();
        List<WorkflowStatusTransition> supportedTransitions = workflowStatusTransitionService
                .getStatusTransitionsForWorkflow(
                        workflowService.findDefaultByGranterAndObjectAndType(granterOrg, "REPORT",reportForGrant.getGrantTypeId()));
        for (WorkflowStatusTransition supportedTransition : supportedTransitions) {
            if (!statuses.stream()
                    .filter(s -> s.getId().longValue() == supportedTransition.getFromState().getId().longValue())
                    .findFirst().isPresent()) {
                statuses.add(supportedTransition.getFromState());
            }
            if (!statuses.stream()
                    .filter(s -> s.getId().longValue() == supportedTransition.getToState().getId().longValue())
                    .findFirst().isPresent()) {
                statuses.add(supportedTransition.getToState());
            }
        }
        for (WorkflowStatus status : statuses) {

                assignment = new ReportAssignment();
                if (status.isInitial()) {
                    assignment.setAnchor(true);
                    assignment.setAssignment(userId);
                } else {
                    assignment.setAnchor(false);
                }
                assignment.setReportId(report.getId());
                assignment.setStateId(status.getId());

                if(status.getTerminal()){
                    final Report finalReport = report;
                    GrantAssignments activeStateOwner =  grantService.getGrantWorkflowAssignments(report.getGrant()).stream().filter(ass -> ass.getStateId().longValue()==finalReport.getGrant().getGrantStatus().getId().longValue()).findFirst().get();
                    assignment.setAssignment(activeStateOwner.getAssignments());
                }
                reportService.saveAssignmentForReport(assignment);

        }

        List<GranterReportSection> granterReportSections = reportTemplate.getSections();
        report.setStringAttributes(new ArrayList<>());
        AtomicBoolean reportTemplateHasDisbursement = new AtomicBoolean(false);
        AtomicReference<ReportStringAttribute> disbursementAttributeValue = new AtomicReference<>(new ReportStringAttribute());


        if(!granterReportSections.stream().filter(rs -> rs.getSectionName().equalsIgnoreCase("Project Indicators")).findFirst().isPresent()){
            GranterReportSection indicatorSection = new GranterReportSection();
            indicatorSection.setReportTemplate(reportTemplate);
            indicatorSection.setDeletable(true);
            indicatorSection.setGranter((Granter)granterOrg);
            indicatorSection.setSectionName("Project Indicators");
            indicatorSection.setSectionOrder(granterReportSections.size());
            granterReportSections.add(indicatorSection);
        }
        for (GranterReportSection reportSection : granterReportSections) {
            ReportSpecificSection specificSection = new ReportSpecificSection();
            specificSection.setDeletable(true);
            specificSection.setGranter((Granter) organizationService.findOrganizationByTenantCode(tenantCode));
            specificSection.setReportId(report.getId());
            specificSection.setReportTemplateId(reportTemplate.getId());
            specificSection.setSectionName(reportSection.getSectionName());
            specificSection.setSectionOrder(reportSection.getSectionOrder());

            specificSection = reportService.saveReportSpecificSection(specificSection);
            ReportSpecificSection finalSpecificSection = specificSection;
            Report finalReport = report;
            final AtomicInteger[] attribVOOrder = { new AtomicInteger(1) };
            Report finalReport1 = report;

            if (specificSection.getSectionName().equalsIgnoreCase("Project Indicators")) {
                for(Map<DatePeriod,PeriodAttribWithLabel> hold: getPeriodsWithAttributes(report.getGrant(),userId)){
                    hold.forEach((entry, val) -> {
                        val.getAttributes().forEach(attribVo -> {
                            ReportSpecificSectionAttribute sectionAttribute = new ReportSpecificSectionAttribute();
                            sectionAttribute.setAttributeOrder(attribVOOrder[0].getAndIncrement());
                            sectionAttribute.setDeletable(attribVo.isDeletable());
                            sectionAttribute.setFieldName(attribVo.getFieldName());
                            sectionAttribute.setFieldType(attribVo.getFieldType());
                            sectionAttribute.setGranter(finalSpecificSection.getGranter());
                            sectionAttribute.setRequired(attribVo.isRequired());
                            sectionAttribute.setSection(finalSpecificSection);
                            sectionAttribute.setCanEdit(false);
                            sectionAttribute = reportService.saveReportSpecificSectionAttribute(sectionAttribute);

                            ReportStringAttribute stringAttribute = new ReportStringAttribute();

                            stringAttribute.setSection(finalSpecificSection);
                            stringAttribute.setReport(finalReport);
                            stringAttribute.setSectionAttribute(sectionAttribute);
                            stringAttribute.setGrantLevelTarget(attribVo.getTarget());
                            stringAttribute.setFrequency(attribVo.getFrequency());

                            stringAttribute = reportService.saveReportStringAttribute(stringAttribute);
                        });
                    });
                }

            }

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
                if (sectionAttribute.getFieldType().equalsIgnoreCase("disbursement")) {
                    reportTemplateHasDisbursement.set(true);
                    disbursementAttributeValue.set(stringAttribute);
                }
            });
        }

        // Handle logic for setting dibursement type in reports
        for (GrantSpecificSection grantSection : grantService.getGrantSections(report.getGrant())) {
            for (GrantSpecificSectionAttribute specificSectionAttribute : grantService
                    .getAttributesBySection(grantSection)) {
                if (specificSectionAttribute.getFieldType().equalsIgnoreCase("disbursement")) {
                    if (reportTemplateHasDisbursement.get()) {
                        ObjectMapper mapper = new ObjectMapper();
                        String[] colHeaders = new String[] { "Disbursement Date", "Actual Disbursement",
                                "Funds from other Sources", "Notes" };
                        List<TableData> tableDataList = new ArrayList<>();
                        TableData tableData = new TableData();
                        tableData.setName("1");
                        tableData.setHeader("Planned Installment #");
                        tableData.setEnteredByGrantee(false);
                        tableData.setColumns(new ColumnData[4]);
                        for (int i = 0; i < tableData.getColumns().length; i++) {

                            tableData.getColumns()[i] = new ColumnData(colHeaders[i], "",
                                    (i == 1 || i == 2) ? "currency" : (i == 0) ? "date" : null);
                        }
                        tableDataList.add(tableData);

                        try {
                            disbursementAttributeValue.get().setValue(mapper.writeValueAsString(tableDataList));
                            reportService.saveReportStringAttribute(disbursementAttributeValue.get());
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    } else {
                        ReportSpecificSection specificSection = new ReportSpecificSection();
                        specificSection.setDeletable(true);
                        specificSection.setGranter((Granter) report.getGrant().getGrantorOrganization());
                        specificSection.setReportId(report.getId());
                        specificSection.setReportTemplateId(reportTemplate.getId());
                        specificSection.setSectionName("Project Funds");
                        List<ReportSpecificSection> reportSections = reportService.getReportSections(report);
                        specificSection.setSectionOrder(Collections.max(reportSections.stream()
                                .map(rs -> new Integer(rs.getSectionOrder())).collect(Collectors.toList())) + 1);
                        specificSection = reportService.saveReportSpecificSection(specificSection);

                        ReportSpecificSectionAttribute sectionAttribute = new ReportSpecificSectionAttribute();
                        sectionAttribute.setAttributeOrder(1);
                        sectionAttribute.setDeletable(true);
                        sectionAttribute.setFieldName("Disbursement Details");
                        sectionAttribute.setFieldType("disbursement");
                        sectionAttribute.setGranter((Granter) report.getGrant().getGrantorOrganization());
                        sectionAttribute.setRequired(false);
                        sectionAttribute.setSection(specificSection);
                        sectionAttribute.setCanEdit(true);
                        sectionAttribute.setExtras(null);
                        sectionAttribute = reportService.saveReportSpecificSectionAttribute(sectionAttribute);

                        ReportStringAttribute stringAttribute = new ReportStringAttribute();

                        stringAttribute.setSection(specificSection);
                        stringAttribute.setReport(report);
                        stringAttribute.setSectionAttribute(sectionAttribute);

                        stringAttribute = reportService.saveReportStringAttribute(stringAttribute);

                    }
                }
            }
        }

        report = _ReportToReturn(report, userId);
        return report;
    }

    private List<Map<DatePeriod,PeriodAttribWithLabel>> getPeriodsWithAttributes(Grant grant,Long userId){

        GrantVO grantVO = new GrantVO().build(grant, grantService.getGrantSections(grant),
                workflowPermissionService, userService.getUserById(userId),
                appConfigService.getAppConfigForGranterOrg(grant.getGrantorOrganization().getId(),
                        AppConfiguration.KPI_SUBMISSION_WINDOW_DAYS),
                userService,grantService);
        grant.setGrantDetails(grantVO.getGrantDetails());

        List<Map<DatePeriod,PeriodAttribWithLabel>> periodsWithAttributes = new ArrayList<>();
        Map<DatePeriod, PeriodAttribWithLabel> quarterlyPeriods = new HashMap<>();
        Map<DatePeriod, PeriodAttribWithLabel> halfyearlyPeriods = new HashMap<>();
        Map<DatePeriod, PeriodAttribWithLabel> monthlyPeriods = new HashMap<>();
        Map<DatePeriod, PeriodAttribWithLabel> yearlyPeriods = new HashMap<>();
        if (grant.getStartDate() != null && grant.getEndDate() != null) {
            grant.getGrantDetails().getSections().forEach(sec -> {
                if (sec.getAttributes() != null && sec.getAttributes().size() > 0) {
                    List<SectionAttributesVO> attribs = new ArrayList<>();
                    List order = ImmutableList.of("YEARLY", "HALF-YEARLY", "QUARTERLY", "MONTHLY");
                    final Ordering<String> colorOrdering = Ordering.explicit(order);
                    Comparator<SectionAttributesVO> attrComparator = Comparator
                            .comparing(c -> order.indexOf(c.getFrequency().toUpperCase()));
                    sec.getAttributes().removeIf(attr -> attr.getFrequency()==null);
                    sec.getAttributes().sort(attrComparator);

                    sec.getAttributes().forEach(attr -> {
                        if (attr.getFieldType().equalsIgnoreCase("KPI")) {

                            if (attr.getFrequency().equalsIgnoreCase("YEARLY")) {
                                DateTime st = new DateTime(grant.getMinEndEndate() != null
                                        ? new DateTime(grant.getMinEndEndate()).plusDays(1).toDate()
                                        : grant.getStartDate(), DateTimeZone.forID(timezone)).withTimeAtStartOfDay();
                                DateTime en = new DateTime(grant.getEnDate(), DateTimeZone.forID(timezone)).withTime(23,
                                        59, 59, 999);
                                List<DatePeriod> reportingFrequencies = getReportingFrequencies(st, en,
                                        Frequency.YEARLY);

                                reportingFrequencies.forEach(rf -> {

                                    List attrList = null;

                                    if (yearlyPeriods.containsKey(rf)) {
                                        attrList = yearlyPeriods.get(rf).getAttributes();
                                    } else {
                                        attrList = new ArrayList<SectionAttributesVO>();
                                    }
                                    attrList.add(attr);
                                    yearlyPeriods.put(rf, new PeriodAttribWithLabel(rf.getLabel(), attrList));

                                });
                            }

                            if (attr.getFrequency().equalsIgnoreCase("HALF-YEARLY")) {
                                DateTime st = new DateTime(grant.getMinEndEndate() != null
                                        ? new DateTime(grant.getMinEndEndate()).plusDays(1).toDate()
                                        : grant.getStartDate(), DateTimeZone.forID(timezone)).withTimeAtStartOfDay();
                                DateTime en = new DateTime(grant.getEnDate(), DateTimeZone.forID(timezone)).withTime(23,
                                        59, 59, 999);
                                List<DatePeriod> reportingFrequencies = getReportingFrequencies(st, en,
                                        Frequency.HALF_YEARLY);

                                reportingFrequencies.forEach(rf -> {

                                    List attrList = null;
                                    if (yearlyPeriods.containsKey(rf)) {
                                        yearlyPeriods.get(rf).getAttributes().add(attr);
                                    } else {

                                        if (halfyearlyPeriods.containsKey(rf)) {
                                            attrList = halfyearlyPeriods.get(rf).getAttributes();
                                        } else {
                                            attrList = new ArrayList<SectionAttributesVO>();
                                        }
                                        attrList.add(attr);
                                        halfyearlyPeriods.put(rf, new PeriodAttribWithLabel(rf.getLabel(), attrList));
                                    }
                                });
                            }

                            if (attr.getFrequency().equalsIgnoreCase("QUARTERLY")) {

                                DateTime st = new DateTime(grant.getMinEndEndate() != null
                                        ? new DateTime(grant.getMinEndEndate()).plusDays(1).toDate()
                                        : grant.getStartDate(), DateTimeZone.forID(timezone)).withTimeAtStartOfDay();
                                DateTime en = new DateTime(grant.getEnDate(), DateTimeZone.forID(timezone)).withTime(23,
                                        59, 59, 999);
                                List<DatePeriod> reportingFrequencies = getReportingFrequencies(st, en,
                                        Frequency.QUARTERLY);
                                reportingFrequencies.forEach(rf -> {

                                    List attrList = null;

                                    if (yearlyPeriods.containsKey(rf)) {
                                        yearlyPeriods.get(rf).getAttributes().add(attr);
                                    } else if (halfyearlyPeriods.containsKey(rf)) {
                                        halfyearlyPeriods.get(rf).getAttributes().add(attr);
                                    } else {
                                        if (quarterlyPeriods.containsKey(rf)) {
                                            attrList = quarterlyPeriods.get(rf).getAttributes();
                                        } else {
                                            attrList = new ArrayList<SectionAttributesVO>();
                                        }
                                        attrList.add(attr);
                                        quarterlyPeriods.put(rf, new PeriodAttribWithLabel(rf.getLabel(), attrList));
                                    }
                                });

                            }
                        }

                        if (attr.getFrequency().equalsIgnoreCase("MONTHLY")) {
                            DateTime st = new DateTime(grant.getMinEndEndate() != null
                                    ? new DateTime(grant.getMinEndEndate()).plusDays(1).toDate()
                                    : grant.getStartDate(), DateTimeZone.forID(timezone)).withTimeAtStartOfDay();
                            DateTime en = new DateTime(grant.getEnDate(), DateTimeZone.forID(timezone)).withTime(23, 59,
                                    59, 999);
                            List<DatePeriod> reportingFrequencies = getReportingFrequencies(st, en, Frequency.MONTHLY);

                            reportingFrequencies.forEach(rf -> {

                                List attrList = null;
                                if (yearlyPeriods.containsKey(rf)) {
                                    yearlyPeriods.get(rf).getAttributes().add(attr);
                                } else if (halfyearlyPeriods.containsKey(rf)) {
                                    halfyearlyPeriods.get(rf).getAttributes().add(attr);
                                } else if (quarterlyPeriods.containsKey(rf)) {
                                    quarterlyPeriods.get(rf).getAttributes().add(attr);
                                } else {

                                    if (monthlyPeriods.containsKey(rf)) {
                                        attrList = monthlyPeriods.get(rf).getAttributes();
                                    } else {
                                        attrList = new ArrayList<SectionAttributesVO>();
                                    }
                                    attrList.add(attr);
                                    monthlyPeriods.put(rf, new PeriodAttribWithLabel(rf.getLabel(), attrList));
                                }
                            });

                        }

                    });
                }
            });
        }


        periodsWithAttributes.add(monthlyPeriods);
        periodsWithAttributes.add(quarterlyPeriods);
        periodsWithAttributes.add(halfyearlyPeriods);
        periodsWithAttributes.add(yearlyPeriods);
        return periodsWithAttributes;
    }

    private List<DatePeriod> getReportingFrequencies(DateTime st, DateTime en, Frequency frequency) {

        List<DatePeriod> periods = new ArrayList<>();
        List<DatePeriod> periodsToReturn = new ArrayList<>(); // For adhoc reports just return one period
        if (frequency == Frequency.MONTHLY) {

            while (st.isBefore(en) && !st.withTime(23, 59, 59, 999).isEqual(en)) {
                DateTime tempEn = st.dayOfMonth().withMaximumValue().withTime(23, 59, 59, 999);
                if (tempEn.isAfter(en)) {
                    DatePeriod dp = new DatePeriod(st.toDate(), en.toDate());
                    dp.setLabel("Monthly Report");
                    periods.add(dp);
                    break;
                }
                DatePeriod p = new DatePeriod(st.toDate(), tempEn.toDate());
                p.setLabel("Monthly Report");
                periods.add(p);
                st = tempEn.plusDays(1).withTimeAtStartOfDay();
            }

        } else if (frequency == Frequency.QUARTERLY) {

            Month[] QUARTER_MONTH_ENDS = new Month[] { Month.MARCH, Month.JUNE, Month.SEPTEMBER, Month.DECEMBER };
            while (st.isBefore(en) && !st.withTime(23, 59, 59, 999).isEqual(en)) {
                DatePeriodLabel qrtrEnd = endOfQuarter(st);
                DateTime tempEn = qrtrEnd.getDateTime().dayOfMonth().withMaximumValue().withTime(23, 59, 59, 999);
                if (tempEn.isAfter(en)) {
                    DatePeriod dp = new DatePeriod(st.toDate(), en.toDate());
                    dp.setLabel(endOfQuarter(st).getPeriodLabel());
                    periods.add(dp);
                    break;
                }
                DatePeriod p = new DatePeriod(st.toDate(), tempEn.toDate());
                p.setLabel(qrtrEnd.getPeriodLabel());
                periods.add(p);
                st = tempEn.plusDays(1).withTimeAtStartOfDay();
            }
            // periods.add(new DatePeriod(st.toDate(),en.toDate()));
        } else if (frequency == Frequency.HALF_YEARLY) {

            while (st.isBefore(en) && !st.withTime(23, 59, 59, 999).isEqual(en)) {
                DatePeriodLabel halfYrEnd = endOfHalfYear(st);
                DateTime tempEn = halfYrEnd.getDateTime().dayOfMonth().withMaximumValue().withTime(23, 59, 59, 999);
                if (tempEn.isAfter(en)) {
                    DatePeriod dp = new DatePeriod(st.toDate(), en.toDate());
                    dp.setLabel(endOfHalfYear(st).getPeriodLabel());
                    periods.add(dp);
                    break;
                }
                DatePeriod p = new DatePeriod(st.toDate(), tempEn.toDate());
                p.setLabel(halfYrEnd.getPeriodLabel());
                periods.add(p);
                st = tempEn.plusDays(1).withTimeAtStartOfDay();
            }
            // periods.add(new DatePeriod(st.toDate(),en.toDate()));
        } else if (frequency == Frequency.YEARLY) {

            while (st.isBefore(en) && !st.withTime(23, 59, 59, 999).isEqual(en)) {
                DatePeriodLabel yrEnd = endOfYear(st);
                DateTime tempEn = yrEnd.getDateTime().dayOfMonth().withMaximumValue().withTime(23, 59, 59, 999);
                if (tempEn.isAfter(en)) {
                    DatePeriod dp = new DatePeriod(st.toDate(), en.toDate());
                    dp.setLabel(endOfYear(st).getPeriodLabel());
                    periods.add(dp);
                    break;
                }
                DatePeriod p = new DatePeriod(st.toDate(), tempEn.toDate());
                p.setLabel(yrEnd.getPeriodLabel());
                periods.add(p);
                st = tempEn.plusDays(1).withTimeAtStartOfDay();
            }
            // periods.add(new DatePeriod(st.toDate(),en.toDate()));
        }
        periodsToReturn.add(periods.get(0));
        return periodsToReturn;
    }

    private DatePeriodLabel endOfQuarter(DateTime st) {
        if (st.getMonthOfYear() >= Month.JANUARY.getValue() && st.getMonthOfYear() <= Month.MARCH.getValue()) {
            return new DatePeriodLabel(st.withMonthOfYear(Month.MARCH.getValue()),
                    "Quarterly Report - Q4 " + String.valueOf(st.getYear() - 1) + "/"
                            + String.valueOf(String.valueOf(st.getYear()).substring(2, 4)));
        } else if (st.getMonthOfYear() >= Month.APRIL.getValue() && st.getMonthOfYear() <= Month.JUNE.getValue()) {
            return new DatePeriodLabel(st.withMonthOfYear(Month.JUNE.getValue()),
                    "Quarterly Report - Q1 " + String.valueOf(st.getYear()) + "/"
                            + String.valueOf(String.valueOf(st.getYear() + 1).substring(2, 4)));
        } else if (st.getMonthOfYear() >= Month.JULY.getValue() && st.getMonthOfYear() <= Month.SEPTEMBER.getValue()) {
            return new DatePeriodLabel(st.withMonthOfYear(Month.SEPTEMBER.getValue()),
                    "Quarterly Report - Q2 " + String.valueOf(st.getYear()) + "/"
                            + String.valueOf(String.valueOf(st.getYear() + 1).substring(2, 4)));
        } else if (st.getMonthOfYear() >= Month.OCTOBER.getValue()
                && st.getMonthOfYear() <= Month.DECEMBER.getValue()) {
            return new DatePeriodLabel(st.withMonthOfYear(Month.DECEMBER.getValue()),
                    "Quarterly Report - Q3 " + String.valueOf(st.getYear()) + "/"
                            + String.valueOf(String.valueOf(st.getYear() + 1).substring(2, 4)));
        }
        return null;
    }

    private DatePeriodLabel endOfHalfYear(DateTime st) {
        if (st.getMonthOfYear() >= Month.APRIL.getValue() && st.getMonthOfYear() <= Month.SEPTEMBER.getValue()) {
            return new DatePeriodLabel(st.withMonthOfYear(Month.SEPTEMBER.getValue()),
                    "Half-Yearly Report - H1 " + String.valueOf(st.getYear()) + "/"
                            + String.valueOf(String.valueOf(st.getYear() + 1).substring(2, 4)));
        } else if (st.getMonthOfYear() >= Month.OCTOBER.getValue()
                && st.getMonthOfYear() <= Month.DECEMBER.getValue()) {
            return new DatePeriodLabel(st.plusYears(1).withMonthOfYear(Month.MARCH.getValue()),
                    "Half-Yearly Report - H2 " + String.valueOf(st.getYear()) + "/"
                            + String.valueOf(String.valueOf(st.getYear() + 1).substring(2, 4)));
        } else if (st.getMonthOfYear() >= Month.JANUARY.getValue() && st.getMonthOfYear() <= Month.MARCH.getValue()) {
            return new DatePeriodLabel(st.withMonthOfYear(Month.MARCH.getValue()),
                    "Half-Yearly Report - H2 " + String.valueOf(st.getYear() - 1) + "/"
                            + String.valueOf(String.valueOf(st.getYear()).substring(2, 4)));
        }
        return null;
    }

    private DatePeriodLabel endOfYear(DateTime st) {
        if (st.getMonthOfYear() >= Month.APRIL.getValue() && st.getMonthOfYear() <= Month.DECEMBER.getValue()) {
            return new DatePeriodLabel(st.plusYears(1).withMonthOfYear(Month.MARCH.getValue()),
                    "Yearly Report " + String.valueOf(st.getYear()) + "/"
                            + String.valueOf(String.valueOf(st.getYear() + 1).substring(2, 4)));
        } else if (st.getMonthOfYear() >= Month.JANUARY.getValue() && st.getMonthOfYear() <= Month.MARCH.getValue()) {
            return new DatePeriodLabel(st.withMonthOfYear(Month.MARCH.getValue()),
                    "Yearly Report " + String.valueOf(st.getYear() - 1) + "/"
                            + String.valueOf(String.valueOf(st.getYear()).substring(2, 4)));
        }
        return null;
    }

    @PostMapping(value = "/{reportId}/attachments", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] downloadSelectedAttachments(@PathVariable("userId") Long userId,
            @PathVariable("reportId") Long reportId, @RequestHeader("X-TENANT-CODE") String tenantCode,
            @RequestBody AttachmentDownloadRequest downloadRequest, HttpServletResponse response) throws IOException {

        ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
        // setting headers
        response.setContentType("application/zip");
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"test.zip\"");

        // creating byteArray stream, make it bufforable and passing this buffor to
        // ZipOutputStream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
        ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream);

        // simple file list, just for tests

        ArrayList<File> files = new ArrayList<>(2);
        files.add(new File("README.md"));

        User user = userService.getUserById(userId);

        // packing files
        for (Long attachmentId : downloadRequest.getAttachmentIds()) {
            ReportStringAttributeAttachments attachment = reportService
                    .getStringAttributeAttachmentsByAttachmentId(attachmentId);
            Long sectionId = attachment.getReportStringAttribute().getSectionAttribute().getSection().getId();
            Long attributeId = attachment.getReportStringAttribute().getSectionAttribute().getId();
            File file = null;
            if (user.getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")) {
                file = resourceLoader.getResource("file:" + uploadLocation
                        + user.getOrganization().getName().toUpperCase() + "/report-documents/" + reportId + "/"
                        + sectionId + "/" + attributeId + "/" + attachment.getName() + "." + attachment.getType())
                        .getFile();
                if (!file.exists()) {
                    file = resourceLoader
                            .getResource("file:" + uploadLocation
                                    + reportService.getReportById(reportId).getGrant().getGrantorOrganization()
                                            .getCode().toUpperCase()
                                    + "/report-documents/" + reportId + "/" + sectionId + "/" + attributeId + "/"
                                    + attachment.getName() + "." + attachment.getType())
                            .getFile();
                }
            } else {
                file = resourceLoader.getResource(
                        "file:" + uploadLocation + tenantCode + "/report-documents/" + reportId + "/" + sectionId + "/"
                                + attributeId + "/" + attachment.getName() + "." + attachment.getType())
                        .getFile();
                if (!file.exists()) {

                    file = resourceLoader
                            .getResource("file:" + uploadLocation
                                    + reportService.getReportById(reportId).getGrant().getOrganization().getName()
                                            .toUpperCase()
                                    + "/report-documents/" + reportId + "/" + sectionId + "/" + attributeId + "/"
                                    + attachment.getName() + "." + attachment.getType())
                            .getFile();
                }
            }
            // new zip entry and copying inputstream with file to zipOutputStream, after all
            // closing streams
            zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
            FileInputStream fileInputStream = new FileInputStream(file);

            IOUtils.copy(fileInputStream, zipOutputStream);

            fileInputStream.close();
            zipOutputStream.closeEntry();
        }

        if (zipOutputStream != null) {
            zipOutputStream.finish();
            zipOutputStream.flush();
            IOUtils.closeQuietly(zipOutputStream);
        }
        IOUtils.closeQuietly(bufferedOutputStream);
        IOUtils.closeQuietly(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    @GetMapping("/{reportId}/file/{fileId}")
    @ApiOperation(value = "Get file for download")
    public ResponseEntity<Resource> getFileForDownload(HttpServletResponse servletResponse,
            @RequestHeader("X-TENANT-CODE") String tenantCode, @PathVariable("reportId") Long reportId,
            @PathVariable("fileId") Long fileId) {

        ReportStringAttributeAttachments attachment = reportService.getStringAttributeAttachmentsByAttachmentId(fileId);
        String filePath = attachment.getLocation() + attachment.getName() + "." + attachment.getType();

        /*
         * servletResponse.setContentType(file.getcMediaType.IMAGE_PNG_VALUE);
         * servletResponse.setHeader("org-name",organizationService.
         * findOrganizationByTenantCode(tenant).getName());
         */
        try {
            File file = resourceLoader.getResource("file:" + filePath).getFile();
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=" + attachment.getName() + "." + attachment.getType());
            servletResponse.setHeader("filename", attachment.getName() + "." + attachment.getType());
            return ResponseEntity.ok().headers(headers).contentLength(file.length())
                    .contentType(MediaType.parseMediaType("application/octet-stream")).body(resource);
            // StreamUtils.copy(file.getInputStream(), servletResponse.getOutputStream());
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    @PostMapping("{reportId}/attribute/{attributeId}/attachment/{attachmentId}")
    @ApiOperation("Delete attachment from document field")
    public Report deleteReportStringAttributeAttachment(
            @ApiParam(name = "reportToSave", value = "Report to save in edit mode, pass in Body of request") @RequestBody Report reportToSave,
            @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "userId", value = "Unique identifier og logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "attachmentId", value = "Unique identifier of the document attachment being deleted") @PathVariable("attachmentId") Long attachmentId,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code") @RequestHeader("X-TENANT-CODE") String tenantCode,
            @ApiParam(name = "attributeId", value = "Unique identifier of the document field") @PathVariable("attributeId") Long attributeId) {
        saveReport(reportId, reportToSave, userId, tenantCode);
        ReportStringAttributeAttachments attch = reportService
                .getStringAttributeAttachmentsByAttachmentId(attachmentId);
        reportService.deleteStringAttributeAttachments(Arrays.asList(new ReportStringAttributeAttachments[] { attch }));

        File file = new File(attch.getLocation() + attch.getName() + "." + attch.getType());
        file.delete();
        ReportStringAttribute stringAttribute = reportService.findReportStringAttributeById(attributeId);
        List<ReportStringAttributeAttachments> stringAttributeAttachments = reportService
                .getStringAttributeAttachmentsByStringAttribute(stringAttribute);
        ObjectMapper mapper = new ObjectMapper();
        try {
            stringAttribute.setValue(mapper.writeValueAsString(stringAttributeAttachments));
            stringAttribute = reportService.saveReportStringAttribute(stringAttribute);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }

        Report report = reportService.getReportById(reportId);

        report = _ReportToReturn(report, userId);
        return report;
    }

    @GetMapping("/resolve")
    public Report resolveReport(@PathVariable("userId") Long userId, @RequestHeader("X-TENANT-CODE") String tenantCode,
            @RequestParam("r") String reportCode) {
        Long reportId = Long.valueOf(new String(Base64.getDecoder().decode(reportCode), StandardCharsets.UTF_8));
        Report report = reportService.getReportById(reportId);

        report = _ReportToReturn(report, userId);
        _checkAndReturnHistoricalReport(userId, report);
        return report;
    }

    private void _checkAndReturnHistoricalReport(@PathVariable("userId") Long userId, Report report) {
        if (userService.getUserById(userId).getOrganization().getOrganizationType().equalsIgnoreCase("GRANTEE")
                && report.getStatus().getInternalStatus().equalsIgnoreCase("REVIEW")) {
            try {
                ReportHistory historicReport = reportService.getSingleReportHistoryByStatusAndReportId("ACTIVE",
                        report.getId());
                if (historicReport != null && historicReport.getReportDetail() != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    report.setReportDetails(mapper.readValue(historicReport.getReportDetail(), ReportDetailVO.class));
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @DeleteMapping("/{reportId}")
    @ApiOperation("Delete report")
    public void deleteReport(
            @ApiParam(name = "reportId", value = "Unique identifier of the report") @PathVariable("reportId") Long reportId,
            @ApiParam(name = "userId", value = "Unique identifier of logged in user") @PathVariable("userId") Long userId,
            @ApiParam(name = "X-TENANT-CODE", value = "Tenant code ") @RequestHeader("X-TENANT-CODE") String tenantCode) {
            Report report = reportService.getReportById(reportId);

        reportService.deleteReport(report);
    }

    @GetMapping(value = "/compare/{currentReportId}/{origReportId}")
    public List<PlainReport> getReportsToCompare(@RequestHeader("X-TENANT-CODE")String tenantCode,
                                               @PathVariable("userId")Long userId,
                                               @PathVariable("currentGrantId")Long currentReportId,
                                               @PathVariable("origGrantId")Long origReportId){

        List<PlainReport> reportsToReturn = new ArrayList<>();

        Report currentReport = reportService.getReportById(currentReportId);
        currentReport = _ReportToReturn(currentReport,userId);

        Report origReport = reportService.getReportById(origReportId);
        origReport = _ReportToReturn(origReport,userId);

        try {
            reportsToReturn.add(reportService.reportToPlain(currentReport));
            reportsToReturn.add(reportService.reportToPlain(origReport));
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        return reportsToReturn;
    }

    @GetMapping(value = "/compare/{currentReportId}")
    public PlainReport getPlainGrantForCompare(@RequestHeader("X-TENANT-CODE")String tenantCode,
                                              @PathVariable("userId")Long userId,
                                              @PathVariable("currentReportId")Long currentReportId) throws IOException {
        Report currenReport = reportService.getReportById(currentReportId);
        currenReport = _ReportToReturn(currenReport,userId);



        return reportService.reportToPlain(currenReport);
    }
}
