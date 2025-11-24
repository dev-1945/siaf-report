package com.siaf.report;

import jakarta.ws.rs.Path;
import io.agroal.api.AgroalDataSource;
import jakarta.inject.Inject;

@Path("/report")
public class ReportResource {

    @Inject
    AgroalDataSource dataSource;

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
