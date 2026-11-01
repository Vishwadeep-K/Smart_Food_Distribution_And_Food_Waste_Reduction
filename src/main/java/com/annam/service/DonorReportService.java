package com.annam.service;

import com.annam.dto.DonorDonationDto;
import com.annam.dto.DonorReportDto;
import com.annam.model.ApprovalStatus;
import com.annam.model.Donor;
import com.annam.model.FoodBatch;
import com.annam.model.FoodCategory;
import com.annam.model.User;
import com.annam.repository.DonorRepository;
import com.annam.repository.FoodBatchRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DonorReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Corporate Brand Colors (White & Dark-Green theme)
    private static final Color BRAND_GREEN_DARK = new Color(15, 81, 50);    // #0f5132
    private static final Color BRAND_GREEN_PRIMARY = new Color(25, 135, 84); // #198754
    private static final Color BRAND_GREEN_LIGHT = new Color(230, 244, 234); // light mint
    private static final Color COLOR_GRAY_HEADER = new Color(248, 249, 250);
    private static final Color COLOR_TEXT_DARK = new Color(33, 37, 41);
    private static final Color COLOR_TEXT_MUTED = new Color(108, 117, 125);
    private static final Color COLOR_BORDER = new Color(222, 226, 230);

    private final DonorRepository donorRepository;
    private final FoodBatchRepository foodBatchRepository;
    private final DonorPortalService donorPortalService;

    public DonorReportService(DonorRepository donorRepository,
                              FoodBatchRepository foodBatchRepository,
                              DonorPortalService donorPortalService) {
        this.donorRepository = donorRepository;
        this.foodBatchRepository = foodBatchRepository;
        this.donorPortalService = donorPortalService;
    }

    /**
     * Generates a complete report DTO for the currently logged-in donor with database-backed calculations.
     */
    public DonorReportDto generateDonorReport(User user, String range, String customStart, String customEnd) {
        if (user == null || user.getDonorId() == null) {
            throw new IllegalArgumentException("Authenticated user has no associated Donor account.");
        }

        Donor donor = donorRepository.findById(user.getDonorId());
        if (donor == null) {
            throw new IllegalArgumentException("Donor record not found for ID: " + user.getDonorId());
        }

        // Determine date range boundaries
        LocalDate now = LocalDate.now();
        LocalDate startDate = null;
        LocalDate endDate = now;
        String rangeLabel;

        String safeRange = range != null ? range.trim().toLowerCase() : "all";
        switch (safeRange) {
            case "this_week":
                startDate = now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                rangeLabel = "This Week (" + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT) + ")";
                break;
            case "this_month":
                startDate = now.withDayOfMonth(1);
                rangeLabel = "This Month (" + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT) + ")";
                break;
            case "three_months":
                startDate = now.minusMonths(3);
                rangeLabel = "Last 3 Months (" + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT) + ")";
                break;
            case "custom":
                if (customStart != null && !customStart.trim().isEmpty()) {
                    try { startDate = LocalDate.parse(customStart.trim()); } catch (Exception ignored) {}
                }
                if (customEnd != null && !customEnd.trim().isEmpty()) {
                    try { endDate = LocalDate.parse(customEnd.trim()); } catch (Exception ignored) {}
                }
                rangeLabel = "Custom Range (" + (startDate != null ? startDate.format(DATE_FMT) : "Beginning")
                        + " to " + endDate.format(DATE_FMT) + ")";
                break;
            case "all":
            default:
                rangeLabel = "All Time (Complete Contribution History)";
                break;
        }

        // 1. Fetch batches strictly for this donor
        List<FoodBatch> allDonorBatches = foodBatchRepository.findByDonorId(donor.getDonorId());

        final LocalDate finalStart = startDate;
        final LocalDate finalEnd = endDate;

        // Filter batches strictly within the date range
        List<FoodBatch> filteredBatches = allDonorBatches.stream().filter(b -> {
            LocalDate batchDate = b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate() : now;
            if (finalStart != null && batchDate.isBefore(finalStart)) return false;
            if (finalEnd != null && batchDate.isAfter(finalEnd)) return false;
            return true;
        }).toList();

        // 2. Compute metrics
        int totalDonations = filteredBatches.size();
        int totalPortionsDonated = filteredBatches.stream().mapToInt(FoodBatch::getTotalPortions).sum();
        int totalPortionsDelivered = filteredBatches.stream().mapToInt(FoodBatch::getDeliveredPortions).sum();

        int pendingCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;
        int expiredCount = 0;

        Map<String, Integer> statusSummary = new LinkedHashMap<>();
        statusSummary.put("Delivered", 0);
        statusSummary.put("Partially Allocated", 0);
        statusSummary.put("Reserved", 0);
        statusSummary.put("Approved", 0);
        statusSummary.put("Pending Approval", 0);
        statusSummary.put("Rejected", 0);
        statusSummary.put("Expired", 0);

        // Category breakdown counters
        Map<FoodCategory, int[]> catMap = new LinkedHashMap<>();
        for (FoodCategory fc : FoodCategory.values()) {
            catMap.put(fc, new int[]{0, 0, 0}); // [batchCount, totalPortions, deliveredPortions]
        }

        // Date grouping map
        Map<String, int[]> dateMap = new TreeMap<>(Collections.reverseOrder());

        List<DonorDonationDto> dtoList = new ArrayList<>();

        for (FoodBatch b : filteredBatches) {
            DonorDonationDto dto = donorPortalService.toDonationDto(b);
            dtoList.add(dto);

            if (b.isExpired()) {
                expiredCount++;
            } else if (b.getApprovalStatus() == ApprovalStatus.REJECTED) {
                rejectedCount++;
            } else if (b.getApprovalStatus() == ApprovalStatus.NOT_APPROVED) {
                pendingCount++;
            } else if (b.getApprovalStatus() == ApprovalStatus.APPROVED) {
                approvedCount++;
            }

            String delStatus = dto.getDeliveryStatus();
            statusSummary.put(delStatus, statusSummary.getOrDefault(delStatus, 0) + 1);

            int[] catArr = catMap.computeIfAbsent(b.getCategory(), k -> new int[]{0, 0, 0});
            catArr[0]++;
            catArr[1] += b.getTotalPortions();
            catArr[2] += b.getDeliveredPortions();

            String dateKey = b.getCreatedAt() != null ? b.getCreatedAt().format(DATE_FMT) : now.format(DATE_FMT);
            int[] dateArr = dateMap.computeIfAbsent(dateKey, k -> new int[]{0, 0, 0});
            dateArr[0]++;
            dateArr[1] += b.getTotalPortions();
            dateArr[2] += b.getDeliveredPortions();
        }

        double deliverySuccessRate = totalPortionsDonated > 0
                ? Math.round((totalPortionsDelivered * 1000.0) / totalPortionsDonated) / 10.0
                : 0.0;

        // Build category breakdown list
        List<DonorReportDto.CategoryReportItem> catList = new ArrayList<>();
        for (var entry : catMap.entrySet()) {
            int count = entry.getValue()[0];
            int portions = entry.getValue()[1];
            int del = entry.getValue()[2];
            if (count > 0 || portions > 0) {
                double pct = totalPortionsDonated > 0
                        ? Math.round((portions * 1000.0) / totalPortionsDonated) / 10.0
                        : 0.0;
                catList.add(new DonorReportDto.CategoryReportItem(
                        entry.getKey().name(),
                        entry.getKey().getDisplayName(),
                        count,
                        portions,
                        del,
                        pct
                ));
            }
        }
        catList.sort((a, b) -> Integer.compare(b.getTotalPortions(), a.getTotalPortions()));

        // Build date grouped list
        List<DonorReportDto.DateGroupedItem> dateList = new ArrayList<>();
        for (var entry : dateMap.entrySet()) {
            dateList.add(new DonorReportDto.DateGroupedItem(
                    entry.getKey(),
                    entry.getValue()[0],
                    entry.getValue()[1],
                    entry.getValue()[2]
            ));
        }

        DonorReportDto report = new DonorReportDto();
        report.setReportTitle("ANNAM Donor Contribution Report");
        report.setGeneratedAt(LocalDateTime.now().format(DATETIME_FMT));
        report.setDateRangeLabel(rangeLabel);
        report.setStartDate(startDate != null ? startDate.format(DATE_FMT) : "");
        report.setEndDate(endDate.format(DATE_FMT));

        report.setDonorId(donor.getDonorId());
        report.setDonorName(donor.getName());
        report.setOrganization(donor.getOrganization());
        report.setDonorType(donor.getDonorType());
        report.setEmail(user.getEmail());
        report.setPhone(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : donor.getPhone());
        report.setPickupAddress(user.getPickupAddress() != null && !user.getPickupAddress().isEmpty() ? user.getPickupAddress() : donor.getLocation().getName());
        report.setLocationName(donor.getLocation().getName() + " (" + donor.getLocation().getLocationId() + ")");

        report.setTotalDonationsSubmitted(totalDonations);
        report.setTotalPortionsDonated(totalPortionsDonated);
        report.setTotalPortionsDelivered(totalPortionsDelivered);
        report.setPendingDonationsCount(pendingCount);
        report.setApprovedDonationsCount(approvedCount);
        report.setRejectedDonationsCount(rejectedCount);
        report.setExpiredDonationsCount(expiredCount);
        report.setDeliverySuccessRate(deliverySuccessRate);

        report.setStatusSummary(statusSummary);
        report.setCategoryBreakdown(catList);
        report.setDonationHistoryByDate(dateList);
        report.setDonations(dtoList);

        return report;
    }

    /**
     * Exports the report to CSV format with RFC 4180 compliance.
     */
    public byte[] generateCsvReport(DonorReportDto report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            // UTF-8 BOM for Excel compatibility
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            writer.println("\"" + report.getReportTitle() + "\"");
            writer.println("\"Generated At\",\"" + report.getGeneratedAt() + "\"");
            writer.println("\"Reporting Period\",\"" + report.getDateRangeLabel() + "\"");
            writer.println("\"Donor ID\",\"" + report.getDonorId() + "\"");
            writer.println("\"Donor Name\",\"" + escapeCsv(report.getDonorName()) + "\"");
            writer.println("\"Organization\",\"" + escapeCsv(report.getOrganization()) + "\"");
            writer.println("\"Donor Type\",\"" + escapeCsv(report.getDonorType()) + "\"");
            writer.println("\"Email\",\"" + escapeCsv(report.getEmail()) + "\"");
            writer.println("\"Phone\",\"" + escapeCsv(report.getPhone()) + "\"");
            writer.println("\"Pickup Address\",\"" + escapeCsv(report.getPickupAddress()) + "\"");
            writer.println();

            writer.println("\"--- CONTRIBUTION SUMMARY ---\"");
            writer.println("\"Metric\",\"Value\"");
            writer.println("\"Total Donations Submitted\"," + report.getTotalDonationsSubmitted());
            writer.println("\"Total Portions Donated\"," + report.getTotalPortionsDonated());
            writer.println("\"Total Portions Delivered\"," + report.getTotalPortionsDelivered());
            writer.println("\"Delivery Success Rate\",\"" + report.getDeliverySuccessRate() + "%\"");
            writer.println("\"Pending Approval Donations\"," + report.getPendingDonationsCount());
            writer.println("\"Approved Donations\"," + report.getApprovedDonationsCount());
            writer.println("\"Rejected Donations\"," + report.getRejectedDonationsCount());
            writer.println("\"Expired Donations\"," + report.getExpiredDonationsCount());
            writer.println();

            writer.println("\"--- FOOD CATEGORY BREAKDOWN ---\"");
            writer.println("\"Category\",\"Display Name\",\"Batch Count\",\"Total Portions\",\"Delivered Portions\",\"Portion Share (%)\"");
            for (var c : report.getCategoryBreakdown()) {
                writer.println(String.format("\"%s\",\"%s\",%d,%d,%d,\"%.1f%%\"",
                        escapeCsv(c.getCategory()), escapeCsv(c.getDisplayName()),
                        c.getBatchCount(), c.getTotalPortions(), c.getDeliveredPortions(), c.getPercentageOfTotal()));
            }
            writer.println();

            writer.println("\"--- DONATION RECORDS ---\"");
            writer.println("\"Batch ID\",\"Food Item\",\"Category\",\"Total Portions\",\"Available Portions\",\"Reserved Portions\",\"Delivered Portions\",\"Prep Time\",\"Use-By Time\",\"Pickup Zone\",\"Submitted On\",\"Approval Status\",\"Delivery Status\",\"Notes\",\"Rejection Reason\"");
            for (var d : report.getDonations()) {
                writer.println(String.format(
                        "\"%s\",\"%s\",\"%s\",%d,%d,%d,%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                        escapeCsv(d.getBatchId()),
                        escapeCsv(d.getFoodName()),
                        escapeCsv(d.getCategory()),
                        d.getTotalPortions(),
                        d.getAvailablePortions(),
                        d.getReservedPortions(),
                        d.getDeliveredPortions(),
                        escapeCsv(d.getPreparationTime()),
                        escapeCsv(d.getDeclaredUseByTime()),
                        escapeCsv(d.getPickupLocationName()),
                        escapeCsv(d.getCreatedAt()),
                        escapeCsv(d.getApprovalStatus()),
                        escapeCsv(d.getDeliveryStatus()),
                        escapeCsv(d.getNotes()),
                        escapeCsv(d.getRejectionReason() != null ? d.getRejectionReason() : "")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating CSV report: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    /**
     * Generates a professionally formatted PDF document using OpenPDF.
     */
    public byte[] generatePdfReport(DonorReportDto report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND_GREEN_DARK);
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BRAND_GREEN_PRIMARY);
            Font fontSectionHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BRAND_GREEN_DARK);
            Font fontTh = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font fontTb = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_DARK);
            Font fontTbBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_TEXT_DARK);
            Font fontMetaLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_TEXT_MUTED);
            Font fontMetaVal = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_DARK);
            Font fontKpiLabel = FontFactory.getFont(FontFactory.HELVETICA, 7, COLOR_TEXT_MUTED);
            Font fontKpiVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BRAND_GREEN_DARK);

            // 1. Header Banner
            Paragraph titlePara = new Paragraph("ANNAM SMART FOOD DISTRIBUTION", fontTitle);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(2f);
            document.add(titlePara);

            Paragraph subPara = new Paragraph(report.getReportTitle(), fontSubtitle);
            subPara.setAlignment(Element.ALIGN_CENTER);
            subPara.setSpacingAfter(12f);
            document.add(subPara);

            // 2. Metadata Box
            PdfPTable metaTable = new PdfPTable(4);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{1.5f, 2.5f, 1.5f, 2.5f});
            metaTable.setSpacingAfter(12f);

            addMetaCell(metaTable, "Donor Organization:", report.getOrganization() + " (" + report.getDonorId() + ")", fontMetaLabel, fontMetaVal);
            addMetaCell(metaTable, "Report Generated:", report.getGeneratedAt(), fontMetaLabel, fontMetaVal);
            addMetaCell(metaTable, "Contact Person:", report.getDonorName(), fontMetaLabel, fontMetaVal);
            addMetaCell(metaTable, "Reporting Period:", report.getDateRangeLabel(), fontMetaLabel, fontMetaVal);
            addMetaCell(metaTable, "Donor Type:", report.getDonorType(), fontMetaLabel, fontMetaVal);
            addMetaCell(metaTable, "Contact Phone:", report.getPhone(), fontMetaLabel, fontMetaVal);
            addMetaCell(metaTable, "Email Address:", report.getEmail(), fontMetaLabel, fontMetaVal);
            addMetaCell(metaTable, "Pickup Premise:", report.getPickupAddress(), fontMetaLabel, fontMetaVal);

            document.add(metaTable);

            // 3. KPI Summary Cards
            Paragraph kpiTitle = new Paragraph("KEY CONTRIBUTION METRICS", fontSectionHeader);
            kpiTitle.setSpacingAfter(6f);
            document.add(kpiTitle);

            PdfPTable kpiTable = new PdfPTable(5);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(14f);

            addKpiBox(kpiTable, "TOTAL DONATIONS", String.valueOf(report.getTotalDonationsSubmitted()), fontKpiLabel, fontKpiVal);
            addKpiBox(kpiTable, "PORTIONS DONATED", String.valueOf(report.getTotalPortionsDonated()), fontKpiLabel, fontKpiVal);
            addKpiBox(kpiTable, "DELIVERED PORTIONS", String.valueOf(report.getTotalPortionsDelivered()), fontKpiLabel, fontKpiVal);
            addKpiBox(kpiTable, "DELIVERY SUCCESS", String.format("%.1f%%", report.getDeliverySuccessRate()), fontKpiLabel, fontKpiVal);
            addKpiBox(kpiTable, "PENDING / REJECTED", report.getPendingDonationsCount() + " / " + report.getRejectedDonationsCount(), fontKpiLabel, fontKpiVal);

            document.add(kpiTable);

            // 4. Category Breakdown Table
            if (!report.getCategoryBreakdown().isEmpty()) {
                Paragraph catTitle = new Paragraph("FOOD CATEGORY BREAKDOWN", fontSectionHeader);
                catTitle.setSpacingAfter(6f);
                document.add(catTitle);

                PdfPTable catTable = new PdfPTable(5);
                catTable.setWidthPercentage(100);
                catTable.setWidths(new float[]{2f, 3f, 1.5f, 1.5f, 2f});
                catTable.setSpacingAfter(14f);

                addTh(catTable, "Category", fontTh);
                addTh(catTable, "Description", fontTh);
                addTh(catTable, "Batches", fontTh);
                addTh(catTable, "Total Portions", fontTh);
                addTh(catTable, "Contribution %", fontTh);

                for (var item : report.getCategoryBreakdown()) {
                    addTb(catTable, item.getCategory(), fontTbBold, Element.ALIGN_LEFT);
                    addTb(catTable, item.getDisplayName(), fontTb, Element.ALIGN_LEFT);
                    addTb(catTable, String.valueOf(item.getBatchCount()), fontTb, Element.ALIGN_CENTER);
                    addTb(catTable, String.valueOf(item.getTotalPortions()), fontTbBold, Element.ALIGN_CENTER);
                    addTb(catTable, String.format("%.1f%%", item.getPercentageOfTotal()), fontTb, Element.ALIGN_CENTER);
                }

                document.add(catTable);
            }

            // 5. Donation Records Table
            Paragraph recTitle = new Paragraph("FOOD DONATION RECORDS (" + report.getDonations().size() + " Total)", fontSectionHeader);
            recTitle.setSpacingAfter(6f);
            document.add(recTitle);

            if (report.getDonations().isEmpty()) {
                Paragraph emptyPara = new Paragraph("No food donation records found for the selected reporting period.", fontTb);
                emptyPara.setSpacingAfter(10f);
                document.add(emptyPara);
            } else {
                PdfPTable recTable = new PdfPTable(7);
                recTable.setWidthPercentage(100);
                recTable.setWidths(new float[]{1.2f, 2.5f, 1.8f, 1.5f, 1.3f, 1.8f, 2f});
                recTable.setSpacingAfter(15f);

                addTh(recTable, "Batch ID", fontTh);
                addTh(recTable, "Food Item", fontTh);
                addTh(recTable, "Category", fontTh);
                addTh(recTable, "Portions", fontTh);
                addTh(recTable, "Use-By", fontTh);
                addTh(recTable, "Submitted", fontTh);
                addTh(recTable, "Status", fontTh);

                for (var d : report.getDonations()) {
                    addTb(recTable, d.getBatchId(), fontTbBold, Element.ALIGN_CENTER);
                    addTb(recTable, d.getFoodName(), fontTb, Element.ALIGN_LEFT);
                    addTb(recTable, d.getCategory(), fontTb, Element.ALIGN_LEFT);
                    String portStr = d.getTotalPortions() + " (" + d.getDeliveredPortions() + " del)";
                    addTb(recTable, portStr, fontTb, Element.ALIGN_CENTER);
                    addTb(recTable, d.getDeclaredUseByTime(), fontTb, Element.ALIGN_CENTER);
                    addTb(recTable, d.getCreatedAt() != null ? d.getCreatedAt().substring(0, Math.min(10, d.getCreatedAt().length())) : "--", fontTb, Element.ALIGN_CENTER);
                    addTb(recTable, d.getDeliveryStatus(), fontTbBold, Element.ALIGN_CENTER);
                }

                document.add(recTable);
            }

            // 6. Footer Disclaimer
            Paragraph footer = new Paragraph("This is an official donor contribution statement generated automatically by the ANNAM Food Distribution System. Confidential & Verified Relational Database Record.", fontMetaLabel);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10f);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error rendering PDF report: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    // Helper methods for PDF formatting
    private void addMetaCell(PdfPTable table, String label, String value, Font fontLabel, Font fontVal) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(4f);
        cell.setBackgroundColor(COLOR_GRAY_HEADER);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", fontLabel));
        p.add(new Chunk(value != null ? value : "--", fontVal));
        cell.addElement(p);
        table.addCell(cell);
    }

    private void addKpiBox(PdfPTable table, String label, String value, Font fontLabel, Font fontVal) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BRAND_GREEN_PRIMARY);
        cell.setBackgroundColor(BRAND_GREEN_LIGHT);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pLabel = new Paragraph(label, fontLabel);
        pLabel.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pLabel);

        Paragraph pVal = new Paragraph(value, fontVal);
        pVal.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pVal);

        table.addCell(cell);
    }

    private void addTh(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BRAND_GREEN_DARK);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addTb(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        cell.setBorderColor(COLOR_BORDER);
        table.addCell(cell);
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }
}
