package com.gepardec.zep.service;


import com.gepardec.zep.dto.ZepAttendance;

import java.util.*;

public class PseudonymizationService {
    Map<Integer, String> pseudonymizedUserIds = new HashMap<>();
    Random random = new Random();

    public List<ZepAttendance> pseudonymize(List<ZepAttendance> attendances) {

        List<ZepAttendance> pseudonymizedAttendances = new ArrayList<>();

        attendances.forEach(attendance -> {
            Integer randomId;

            if (!pseudonymizedUserIds.containsValue(attendance.employeeId())) {
                randomId = random.nextInt();
                pseudonymizedUserIds.put(randomId, attendance.employeeId());

            }
            else {
                //TODO: Nächstes mal hier weitermachen
                //randomId = pseudonymizedUserIds.(attendance.employeeId());
                randomId = random.nextInt(); //temp solution
            }

            pseudonymizedAttendances.add(ZepAttendance.builder()
                    .id(attendance.id())
                    .date(attendance.date())
                    .from(attendance.from())
                    .to(attendance.to())
                    .employeeId(randomId.toString())
                    .projectId(attendance.projectId())
                    .projectTaskId(attendance.projectTaskId())
                    .duration(attendance.duration())
                    .note(attendance.note())
                    .billable(attendance.billable())
                    .workLocation(attendance.workLocation())
                    .workLocationIsProjectRelevant(attendance.workLocationIsProjectRelevant())
                    .workLocation(attendance.activity())
                    .vehicle(attendance.vehicle())
                    .build());

        });

        return attendances;
    }

    public List<ZepAttendance> unpseudonymize(List<ZepAttendance> attendances) {
        return null;
    }
}
