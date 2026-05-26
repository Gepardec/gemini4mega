package com.gepardec.zep.service;


import com.gepardec.zep.model.Attendance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PseudonymizationService {
    Map<Integer, String> pseudonymizedUserIds = new HashMap<>();
    Random random = new Random();

    public List<Attendance> pseudonymize(List<Attendance> attendances) {

        List<Attendance> pseudonymizedAttendances = new ArrayList<>();

        attendances.forEach(attendance -> {
            Integer randomId;

            if (!pseudonymizedUserIds.containsValue(attendance.getEmployeeId())) {
                randomId = random.nextInt();
                pseudonymizedUserIds.put(randomId, attendance.getEmployeeId());

            } else {
                //TODO: Nächstes mal hier weitermachen
                //randomId = pseudonymizedUserIds.(attendance.employeeId());
                randomId = random.nextInt(); //temp solution
            }

            pseudonymizedAttendances.add(build(attendance, randomId));
        });

        return attendances;
    }

    public List<Attendance> unpseudonymize(List<Attendance> attendances) {
        return null;
    }

    private Attendance build(Attendance attendance, Integer randomId) {
        Attendance pseudonymizedAttendance = new Attendance();

        attendance.id(attendance.getId());
        attendance.date(attendance.getDate());
        attendance.from(attendance.getFrom());
        attendance.to(attendance.getTo());
        attendance.employeeId(randomId.toString());
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
