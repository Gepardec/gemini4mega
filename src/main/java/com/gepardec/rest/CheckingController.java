package com.gepardec.rest;

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

//    @Inject
//    PromptService promptService;

    @Inject
    AttendanceService attendanceService;

//    @POST
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.TEXT_PLAIN)
//    public String checking(String entries){
//        if (!entries.isEmpty()) {
//            try {
//                return promptService.prompt(entries);
//            }
//            catch (Exception e) {
//                log.error(String.valueOf(e));
//                return "Something went wrong";
//            }
//        } else {
//            return "No entries provided";
//        }
//    }

    @GET
    @Path("/{user}/{year}/{month}")
    @Produces(MediaType.APPLICATION_JSON)
    public String testZEP(@PathParam("user") String user, @PathParam("year") Integer year, @PathParam("month") Integer month){

        YearMonth yearMonth = YearMonth.of(year, month);

        if (user != null && !user.isEmpty()) {
            try {
                return attendanceService.getAttendanceForUserAndMonth(user, yearMonth).toString();
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
