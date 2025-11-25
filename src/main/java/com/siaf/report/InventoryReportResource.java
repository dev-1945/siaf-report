package com.siaf.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPageEventHelper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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
public class InventoryReportResource {

    @Inject
    AgroalDataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    // Variables para el cabezado del reporte
    private String headerRegionName = "";
    private String headerUnitName = "";
    private String headerSector = "";
    private String headerBuilding = "";
    private String headerFromAsset = "";
    private String headerToAsset = "";

    @POST
    @Path("/inventory")
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
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM read_inventory_report(?::json)")) {
                
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
        // Extraer parámetros del JSON para el cabezado
        JsonNode params = objectMapper.readTree(jsonPayload);
        headerSector = params.has("sector") ? params.get("sector").asText("") : "";
        headerBuilding = params.has("building") ? params.get("building").asText("") : "";
        headerFromAsset = params.has("fromAsset") ? params.get("fromAsset").asText("") : "";
        headerToAsset = params.has("toAsset") ? params.get("toAsset").asText("") : "";

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 70, 20);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            // Add Header Event
            writer.setPageEvent(new HeaderFooterPageEvent());
            
            document.open();

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM read_inventory_report(?::json)", 
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                
                ps.setString(1, jsonPayload);
                
                try (ResultSet rs = ps.executeQuery()) {
                    
                    if (rs.next()) {
                        // Obtener nombres para el cabezado
                        headerRegionName = rs.getString("regionName");
                        headerUnitName = rs.getString("unitName");
                        if (headerRegionName == null) headerRegionName = "TODAS";
                        if (headerUnitName == null) headerUnitName = "TODAS";
                        
                        // Reset cursor to before first row so while(rs.next()) works correctly
                        rs.beforeFirst();
                    } else {
                        headerRegionName = "TODAS";
                        headerUnitName = "TODAS";
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
        cell.setMinimumHeight(20f); // Doble alto para evitar palabras cortadas
        table.addCell(cell);
    }

    // Inner class for Header/Footer
    class HeaderFooterPageEvent extends PdfPageEventHelper {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font filterFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float pageWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
            float yPosition = document.getPageSize().getHeight() - 15;
            
            // --- Línea 1: SERVICIO AGRICOLA Y GANADERO | Fecha Hora ---
            PdfPTable line1 = new PdfPTable(2);
            line1.setTotalWidth(pageWidth);
            line1.setLockedWidth(true);
            line1.getDefaultCell().setBorder(0);

            PdfPCell leftCell = new PdfPCell(new Phrase("SERVICIO AGRICOLA Y GANADERO", headerFont));
            leftCell.setBorder(0);
            leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            line1.addCell(leftCell);

            String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            PdfPCell rightCell = new PdfPCell(new Phrase(dateTime, headerFont));
            rightCell.setBorder(0);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            line1.addCell(rightCell);

            line1.writeSelectedRows(0, -1, document.leftMargin(), yPosition, writer.getDirectContent());
            yPosition -= 12;

            // --- Línea 2: PLANILLA DE INVENTARIO (centro) | Página (derecha) ---
            PdfPTable line2 = new PdfPTable(3);
            line2.setTotalWidth(pageWidth);
            line2.setLockedWidth(true);
            line2.getDefaultCell().setBorder(0);
            float[] widths2 = {1f, 2f, 1f};
            try {
                line2.setWidths(widths2);
            } catch (Exception e) { }

            PdfPCell emptyCell = new PdfPCell(new Phrase(""));
            emptyCell.setBorder(0);
            line2.addCell(emptyCell);

            PdfPCell titleCell = new PdfPCell(new Phrase("PLANILLA DE INVENTARIO", titleFont));
            titleCell.setBorder(0);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            line2.addCell(titleCell);

            PdfPCell pageCell = new PdfPCell(new Phrase("Página " + writer.getPageNumber(), headerFont));
            pageCell.setBorder(0);
            pageCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            line2.addCell(pageCell);

            line2.writeSelectedRows(0, -1, document.leftMargin(), yPosition, writer.getDirectContent());
            yPosition -= 14;

            // --- Línea 3: Filtros (Región, Unidad, Sector, Edificio, Especies) ---
            StringBuilder filters = new StringBuilder();
            filters.append("Región: ").append(headerRegionName != null && !headerRegionName.isEmpty() ? headerRegionName : "TODAS");
            filters.append(" - Unidad: ").append(headerUnitName != null && !headerUnitName.isEmpty() ? headerUnitName : "TODAS");
            filters.append(" - Sec: ").append(headerSector != null && !headerSector.isEmpty() ? headerSector : "TODOS");
            filters.append(" - Edif: ").append(headerBuilding != null && !headerBuilding.isEmpty() ? headerBuilding : "TODOS");
            
            // Especies
            if ((headerFromAsset != null && !headerFromAsset.isEmpty()) || (headerToAsset != null && !headerToAsset.isEmpty())) {
                filters.append(" - Especies: ");
                if (headerFromAsset != null && !headerFromAsset.isEmpty()) {
                    filters.append(headerFromAsset);
                }
                if (headerToAsset != null && !headerToAsset.isEmpty()) {
                    if (headerFromAsset != null && !headerFromAsset.isEmpty()) {
                        filters.append(" a ");
                    }
                    filters.append(headerToAsset);
                }
            }

            PdfPTable line3 = new PdfPTable(1);
            line3.setTotalWidth(pageWidth);
            line3.setLockedWidth(true);
            line3.getDefaultCell().setBorder(0);

            PdfPCell filterCell = new PdfPCell(new Phrase(filters.toString(), filterFont));
            filterCell.setBorder(0);
            filterCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            line3.addCell(filterCell);

            line3.writeSelectedRows(0, -1, document.leftMargin(), yPosition, writer.getDirectContent());
        }
    }
}
