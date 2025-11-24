package com.siaf.report;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import io.agroal.api.AgroalDataSource;
import jakarta.inject.Inject;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JREmptyDataSource;

@Path("/report")
public class ReportResource {

/*
    @Inject
    AgroalDataSource dataSource;
*/
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from SIAF Report Service";
    }

    @GET
    @Path("/hello")
    @Produces("application/pdf")
    public Response generateHelloReport() {
        try {
            // Cargar el archivo .jrxml desde resources
            InputStream reportStream = getClass().getResourceAsStream("/reports/hello_world.jrxml");
            if (reportStream == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Report template not found").build();
            }

            // Compilar el reporte
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            // Parámetros (vacío por ahora)
            Map<String, Object> parameters = new HashMap<>();

            // Llenar el reporte (usando un datasource vacío ya que no consultamos BD en este ejemplo)
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            // Exportar a PDF
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            return Response.ok(pdfBytes)
                    .header("Content-Disposition", "inline; filename=hello_world.pdf")
                    .build();

        } catch (JRException e) {
            e.printStackTrace();
            return Response.serverError().entity("Error generating report: " + e.getMessage()).build();
        }
    }

/*
    @GET
    @Path("/test-db")
    @Produces(MediaType.TEXT_PLAIN)
    public String testDb() {
        try (Connection connection = dataSource.getConnection()) {
            return "Database connection successful: " + connection.getMetaData().getURL();
        } catch (SQLException e) {
            return "Database connection failed: " + e.getMessage();
        }
    }
*/
}
