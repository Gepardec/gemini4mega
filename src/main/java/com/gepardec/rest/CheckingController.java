package com.gepardec.rest;

import com.gepardec.agent.AttendanceValidationAgent;
import com.gepardec.model.ValidationResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.util.Collections;
import java.util.Map;

@Path("/checking")
public class CheckingController {

    private static final Logger log = LoggerFactory.getLogger(CheckingController.class);

    @Inject
    AttendanceValidationAgent attendanceValidationAgent;

    @GET
    @Path("/{user}/{year}/{month}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validate(@PathParam("user") String user, @PathParam("year") Integer year, @PathParam("month") Integer month) {

        if (user == null || user.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "No user provided"))
                    .build();
        }

        try {
            YearMonth yearMonth = YearMonth.of(year, month);
            log.info("Starting validation for user={}, year={}, month={}", user, year, month);

            String result = attendanceValidationAgent.checkSingleMonth(user, yearMonth);

//            log.info("Validation completed for user={}, valid={}", user, result.getValid());

            return Response.ok(result).build();

        } catch (Exception e) {
            log.error("Validation failed for user={}, year={}, month={}", user, year, month, e);

            // Return error as ValidationResult for consistent response format
            ValidationResult.ValidationError error = new ValidationResult.ValidationError(
                    -1,
                    "SYSTEM-ERROR",
                    "SYSTEM",
                    "likely",
                    "System error during validation: " + e.getMessage()
            );

            ValidationResult errorResult = new ValidationResult(false, Collections.singletonList(error));

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResult)
                    .build();
        }
    }


}