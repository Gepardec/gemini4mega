package com.gepardec.agent;

import com.gepardec.llm.service.PromptService;
import com.gepardec.rest.CheckingController;
import com.gepardec.zep.dto.ZepAttendance;
import com.gepardec.zep.service.AttendanceService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.util.List;

@ApplicationScoped
public class AttendanceValidationAgent {
    @Inject
    PromptService promptService;
    @Inject
    AttendanceService attendanceService;
    private static final Logger log = LoggerFactory.getLogger(CheckingController.class);

    public String checkSingleMonth(String username, YearMonth payrollMonth){
        List<ZepAttendance> attendancesOfUser = attendanceService.getAttendanceForUserAndMonth(username,payrollMonth);
        System.out.println(attendancesOfUser.toString());

        return promptService.prompt(attendancesOfUser.toString());
    }


}
