package com.gepardec.rest;

import com.gepardec.agent.AttendanceValidationAgent;
import com.gepardec.llm.service.PromptService;
import com.gepardec.zep.service.AttendanceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;

@Path("/checking")
public class CheckingController {

    private static final Logger log = LoggerFactory.getLogger(CheckingController.class);

    @Inject
    AttendanceValidationAgent attendanceValidationAgent;

    /*@POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public String checking(String entries){
       if (!entries.isEmpty()) {
            try {
                return attendanceValidationAgent.check(entries);
            }
            catch (Exception e) {
                log.error(String.valueOf(e));
                return "Something went wrong";
            }
        } else {
            return "No entries provided";
        }
    }

     */

    @GET
    @Path("/{user}/{year}/{month}")
    @Produces(MediaType.APPLICATION_JSON)
    public String validate(@PathParam("user") String user, @PathParam("year") Integer year, @PathParam("month") Integer month){

        YearMonth yearMonth = YearMonth.of(year, month);

        if (user != null && !user.isEmpty()) {
            try {
                return attendanceValidationAgent.checkSingleMonth(user, yearMonth);
            }
            catch (Exception e) {
                log.error(String.valueOf(e));
                return "Something went wrong";
            }
        } else {
            return "No user provided";
        }
    }


}