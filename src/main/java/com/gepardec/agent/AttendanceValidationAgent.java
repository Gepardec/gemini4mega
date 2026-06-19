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
        System.out.println( "Alle Bcuchungen"+ attendancesOfUser.size());
        System.out.println( "Alle Bcuchungen"+ attendancesOfUser.getFirst().getEmployeeId());
        System.out.println( "Alle Bcuchungen"+ attendancesOfUser.getFirst().getId());
        System.out.println( "Alle Bcuchungen"+ attendancesOfUser.getFirst().getFrom());
        System.out.println( "Alle Bcuchungen"+ attendancesOfUser.getFirst().getNote());
        System.out.println( "Alle Bcuchungen"+ attendancesOfUser.get(10).getNote());


        return promptService.prompt(pseudonymizationService.pseudonymize(attendancesOfUser, Attendance::getEmployeeId, this::createAttendance).toString());
    }

    private Attendance createAttendance(Attendance attendance, String id) {
        Attendance pseudonymizedAttendance = new Attendance();

        pseudonymizedAttendance.id(attendance.getId());
        pseudonymizedAttendance.date(attendance.getDate());
        pseudonymizedAttendance.from(attendance.getFrom());
        pseudonymizedAttendance.to(attendance.getTo());
        pseudonymizedAttendance.employeeId(id);
        pseudonymizedAttendance.projectId(attendance.getProjectId());
        pseudonymizedAttendance.projectTaskId(attendance.getProjectTaskId());
        pseudonymizedAttendance.duration(attendance.getDuration());
        pseudonymizedAttendance.note(attendance.getNote());
        pseudonymizedAttendance.billable(attendance.getBillable());
        pseudonymizedAttendance.workLocationId(attendance.getWorkLocationId());
        pseudonymizedAttendance.workLocationIsProjectRelevant(attendance.getWorkLocationIsProjectRelevant());
        pseudonymizedAttendance.activityId(attendance.getActivityId());
        pseudonymizedAttendance.vehicleId(attendance.getVehicleId());

        return pseudonymizedAttendance;
    }
}
