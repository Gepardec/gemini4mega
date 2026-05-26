package com.gepardec.zep.service;

import com.gepardec.zep.api.EmployeesApi;
import com.gepardec.zep.model.EmployeeProject;
import com.gepardec.zep.model.EmployeeProjectsListResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    @Inject
    @RestClient
    EmployeesApi employeesApi;

    @Inject
    ListResponseService listResponseService;


    public List<EmployeeProject> getProjectsForUserAndMonth(String user, YearMonth payrollMonth) {

        LocalDate startDate = payrollMonth.atDay(1);
        LocalDate endDate = payrollMonth.atEndOfMonth();

        if (user != null && !user.isEmpty()) {
            try {
                log.info(String.format("Fetching all projects: Start: %s, End: %s, UserId: %s", startDate, endDate, user));
                return listResponseService.fetchAll(
                        () -> employeesApi.employeesUsernameProjectsGet(user, null, startDate, endDate),
                        EmployeeProjectsListResponse::getData,
                        response -> response.getMeta() != null ? response.getMeta().getTotal() : null);
            } catch (Exception e) {
                log.error("Error fetching projects", e);
                throw new BadRequestException(e);
            }
        } else {
            throw new BadRequestException("No user provided");
        }
    }

}
