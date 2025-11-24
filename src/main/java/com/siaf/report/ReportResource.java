package com.siaf.report;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.agroal.api.AgroalDataSource;
import jakarta.inject.Inject;

@Path("/report")
public class ReportResource {

    @Inject
    AgroalDataSource dataSource;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from SIAF Report Service";
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
