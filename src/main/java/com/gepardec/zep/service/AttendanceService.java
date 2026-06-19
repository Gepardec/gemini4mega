package com.gepardec.zep.service;

import com.gepardec.zep.api.AttendancesApi;
import com.gepardec.zep.model.Attendance;
import com.gepardec.zep.model.AttendancesListResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;


@ApplicationScoped
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private static final int MAX_LIMIT = 100;

    @Inject
    @RestClient
    AttendancesApi attendancesApi;

    @Inject
    ListResponseService listResponseService;


    public List<Attendance> getAttendanceForUserAndMonth(String user, YearMonth payrollMonth) {

        LocalDate startDate = payrollMonth.atDay(1);
        LocalDate endDate = payrollMonth.atEndOfMonth();

        if (user != null && !user.isEmpty()) {
            try {
                log.info(String.format("Fetching all attendances: Start: %s, End: %s, UserId: %s", startDate, endDate, user));
                List<Attendance> attendances = listResponseService.fetchAll(
                        () -> attendancesApi.attendancesGet(startDate, endDate, user, MAX_LIMIT),
                        AttendancesListResponse::getData,
                        response -> response.getMeta() != null ? response.getMeta().getTotal() : null);
                attendances.forEach(a -> a.employeeId(user));
                return attendances;
            } catch (Exception e) {
                log.error("Error fetching attendances", e);
                throw new BadRequestException(e);
            }
        } else {
            throw new BadRequestException("No user provided");
        }
    }

}
