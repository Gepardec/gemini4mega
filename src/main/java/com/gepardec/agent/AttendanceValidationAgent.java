package com.gepardec.agent;

import com.gepardec.llm.service.PromptService;
import com.gepardec.zep.model.Attendance;
import com.gepardec.zep.service.AttendanceService;
import com.gepardec.zep.service.PseudonymizationService;
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

    @Inject
    PseudonymizationService pseudonymizationService;

    private static final Logger log = LoggerFactory.getLogger(AttendanceValidationAgent.class);

    public String checkSingleMonth(String username, YearMonth payrollMonth) {
        List<Attendance> attendancesOfUser = attendanceService.getAttendanceForUserAndMonth(username, payrollMonth);
        System.out.println(attendancesOfUser.toString());


        return promptService.prompt(pseudonymizationService.pseudonymize(attendancesOfUser, Attendance::getEmployeeId, this::createAttendance).toString());
    }

    private Attendance createAttendance(Attendance attendance, String id) {
        Attendance pseudonymizedAttendance = new Attendance();

        attendance.id(attendance.getId());
        attendance.date(attendance.getDate());
        attendance.from(attendance.getFrom());
        attendance.to(attendance.getTo());
        attendance.employeeId(id);
        attendance.projectId(attendance.getProjectId());
        attendance.projectTaskId(attendance.getProjectTaskId());
        attendance.duration(attendance.getDuration());
        attendance.note(attendance.getNote());
        attendance.billable(attendance.getBillable());
        attendance.workLocationId(attendance.getWorkLocationId());
        attendance.workLocationIsProjectRelevant(attendance.getWorkLocationIsProjectRelevant());
        attendance.activityId(attendance.getActivityId());
        attendance.vehicleId(attendance.getVehicleId());

        return pseudonymizedAttendance;
    }
}
