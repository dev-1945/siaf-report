package com.siaf.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPageEventHelper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import io.agroal.api.AgroalDataSource;
import jakarta.inject.Inject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

@Path("/report")
public class AssetReportResource {

    @Inject
    AgroalDataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @POST
    @Path("/asset")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateReport(String jsonPayload) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonPayload);
            String reportType = rootNode.has("reportType") ? rootNode.get("reportType").asText() : "preview_pdf";

            if ("download_xls".equals(reportType)) {
                return generateExcel(jsonPayload);
            } else {
                return generatePdf(jsonPayload, reportType);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Error generating report: " + e.getMessage()).build();
        }
    }

    private Response generateExcel(String jsonPayload) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Inventario");
            
            // Styles
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle groupStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font groupFont = workbook.createFont();
            groupFont.setBold(true);
            groupStyle.setFont(groupFont);
            groupStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            groupStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Title Row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("PLANILLA DE INVENTARIO");
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 14));

            // Header Row
            Row headerRow = sheet.createRow(2);
            String[] headers = {
                "Bien", "Datos Adic.", "Conserv.", "Inscrip.", "Año", 
                "Rol Av.", "Motor", "Fojas", "Carro", "Placa", 
                "FUB A", "FUB N", "Vou A", "Vou N", "Oficina"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 3;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM read_asset_report(?::json)")) {
                
                ps.setString(1, jsonPayload);
                
                try (ResultSet rs = ps.executeQuery()) {
                    String currentAsset = "";
                    
                    while (rs.next()) {
                        String assetName = rs.getString("assetName");
                        long count = rs.getLong("assetCount");
                        
                        // Group Header
                        if (!assetName.equals(currentAsset)) {
                            currentAsset = assetName;
                            Row groupRow = sheet.createRow(rowNum++);
                            Cell groupCell = groupRow.createCell(0);
                            groupCell.setCellValue(assetName + ": " + count + " unidades");
                            groupCell.setCellStyle(groupStyle);
                            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 14));
                        }

                        // Detail Row
                        Row row = sheet.createRow(rowNum++);
                        createCell(row, 0, rs.getString("code"), dataStyle);
                        createCell(row, 1, rs.getString("inventoryName"), dataStyle);
                        createCell(row, 2, rs.getString("registryOfficer"), dataStyle);
                        createCell(row, 3, rs.getString("registration"), dataStyle);
                        createCell(row, 4, String.valueOf(rs.getInt("regYear")), dataStyle);
                        createCell(row, 5, rs.getString("taxId"), dataStyle);
                        createCell(row, 6, rs.getString("engine"), dataStyle);
                        createCell(row, 7, rs.getString("folio"), dataStyle);
                        createCell(row, 8, rs.getString("chassis"), dataStyle);
                        createCell(row, 9, rs.getString("plate"), dataStyle);
                        createCell(row, 10, String.valueOf(rs.getInt("fubYear")), dataStyle);
                        createCell(row, 11, String.valueOf(rs.getInt("fubNumber")), dataStyle);
                        createCell(row, 12, String.valueOf(rs.getInt("voucherYear")), dataStyle);
                        createCell(row, 13, rs.getString("voucherNumber"), dataStyle);
                        createCell(row, 14, String.valueOf(rs.getInt("officeNumber")), dataStyle);
                    }
                }
            }

            // Autosize columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return Response.ok(out.toByteArray())
                    .header("Content-Disposition", "attachment; filename=inventory_report.xlsx")
                    .type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .build();
        }
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private Response generatePdf(String jsonPayload, String reportType) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 20);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            // Add Header Event
            writer.setPageEvent(new HeaderFooterPageEvent());
            
            document.open();

            // --- Title ---
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph title = new Paragraph("PLANILLA DE INVENTARIO", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            // --- Subtitle (Region & Unit) ---
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            String subtitleText = "";
            
            // We need to fetch at least one row to get region/unit names
            // Since we iterate later, we'll use a scrollable ResultSet or just fetch first row separately?
            // Better approach: The query returns regionName/unitName in every row.
            // We can read the first row, print the subtitle, and then process the rows.
            // However, ResultSet is forward-only by default.
            // Let's use a scrollable ResultSet.

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM read_asset_report(?::json)", 
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                
                ps.setString(1, jsonPayload);
                
                try (ResultSet rs = ps.executeQuery()) {
                    
                    if (rs.next()) {
                        String regionName = rs.getString("regionName");
                        String unitName = rs.getString("unitName");
                        
                        subtitleText = "REGIÓN: " + (regionName != null ? regionName : "TODAS") + 
                                     " - UNIDAD: " + (unitName != null ? unitName : "TODAS");
                        
                        Paragraph subtitle = new Paragraph(subtitleText, subtitleFont);
                        subtitle.setAlignment(Element.ALIGN_CENTER);
                        subtitle.setSpacingAfter(10);
                        document.add(subtitle);
                        
                        // Reset cursor to before first row so while(rs.next()) works correctly
                        rs.beforeFirst();
                    } else {
                         // No data found case
                        Paragraph subtitle = new Paragraph("NO SE ENCONTRARON DATOS", subtitleFont);
                        subtitle.setAlignment(Element.ALIGN_CENTER);
                        subtitle.setSpacingAfter(10);
                        document.add(subtitle);
                    }

                    // --- Table Setup ---
                    float[] columnWidths = {
                        6f, // Bien (Increased)
                        8f, // Datos Adicionales
                        5f, // Conservador
                        5f, // Inscripcion
                        3f, // Año
                        4f, // Rol Avaluo
                        5f, // Motor
                        3f, // Fojas
                        5f, // Carro
                        4f, // Placa
                        3f, // Fub Ano
                        3f, // Fub Num
                        3f, // Vou Ano
                        3f, // Vou Num
                        3f  // Ofi Num
                    };
                    
                    PdfPTable table = new PdfPTable(columnWidths);
                    table.setWidthPercentage(100);

                    // --- Header ---
                    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);
                    String[] headers = {
                        "Bien", "Datos Adic.", "Conserv.", "Inscrip.", "Año", 
                        "Rol Av.", "Motor", "Fojas", "Carro", "Placa", 
                        "FUB A", "FUB N", "Vou A", "Vou N", "Oficina"
                    };

                    for (String header : headers) {
                        addHeaderCell(table, header, headerFont, 1, 1);
                    }
                    table.setHeaderRows(1);

                    // --- Data Rows ---
                    Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);
                    Font groupFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);

                    String currentAsset = "";
                    
                    while (rs.next()) {
                        String assetName = rs.getString("assetName");
                        long count = rs.getLong("assetCount");
                        
                        // Group Header
                        if (assetName != null && !assetName.equals(currentAsset)) {
                            currentAsset = assetName;
                            PdfPCell groupCell = new PdfPCell(new Phrase(assetName + ": " + count + " unidades", groupFont));
                            groupCell.setColspan(columnWidths.length);
                            groupCell.setBackgroundColor(new Color(240, 240, 240)); // Light gray
                            groupCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                            groupCell.setPadding(4);
                            table.addCell(groupCell);
                        }

                        // Detail Row
                        addDataCell(table, rs.getString("code"), dataFont);
                        addDataCell(table, rs.getString("inventoryName"), dataFont);
                        addDataCell(table, rs.getString("registryOfficer"), dataFont);
                        addDataCell(table, rs.getString("registration"), dataFont);
                        addDataCell(table, String.valueOf(rs.getInt("regYear")), dataFont);
                        addDataCell(table, rs.getString("taxId"), dataFont);
                        addDataCell(table, rs.getString("engine"), dataFont);
                        addDataCell(table, rs.getString("folio"), dataFont);
                        addDataCell(table, rs.getString("chassis"), dataFont);
                        addDataCell(table, rs.getString("plate"), dataFont);
                        addDataCell(table, String.valueOf(rs.getInt("fubYear")), dataFont);
                        addDataCell(table, String.valueOf(rs.getInt("fubNumber")), dataFont);
                        addDataCell(table, String.valueOf(rs.getInt("voucherYear")), dataFont);
                        addDataCell(table, rs.getString("voucherNumber"), dataFont);
                        addDataCell(table, String.valueOf(rs.getInt("officeNumber")), dataFont);
                    }
                    document.add(table);
                }
            }

            document.close();

            String contentDisposition = "download_pdf".equals(reportType) ? "attachment" : "inline";
            return Response.ok(out.toByteArray())
                    .header("Content-Disposition", contentDisposition + "; filename=inventory_report.pdf")
                    .type("application/pdf")
                    .build();
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setPadding(3);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2);
        table.addCell(cell);
    }

    // Inner class for Header/Footer
    class HeaderFooterPageEvent extends PdfPageEventHelper {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
            headerTable.setLockedWidth(true);
            headerTable.getDefaultCell().setBorder(0);

            // Left: Service Name
            PdfPCell leftCell = new PdfPCell(new Phrase("SERVICIO AGRICOLA Y GANADERO", headerFont));
            leftCell.setBorder(0);
            leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            headerTable.addCell(leftCell);

            // Right: Page, Date, Time
            String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            String pageInfo = "Página " + writer.getPageNumber() + " - " + date;
            PdfPCell rightCell = new PdfPCell(new Phrase(pageInfo, headerFont));
            rightCell.setBorder(0);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            headerTable.addCell(rightCell);

            headerTable.writeSelectedRows(0, -1, document.leftMargin(), document.getPageSize().getHeight() - 10, writer.getDirectContent());
        }
    }
}
