package com.gepardec.zep.service;

import com.gepardec.zep.client.ZepAttendanceRestClient;
import com.gepardec.zep.dto.ZepAttendance;
import com.gepardec.zep.dto.ZepResponse;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;

import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class AttendanceService {
    @RestClient
    private ZepAttendanceRestClient zepAttendanceRestClient;

//    @Inject
//    Logger logger;

    public List<ZepAttendance> getBillableAttendancesForUserAndMonth(String username, YearMonth payrollMonth) {
        return getAttendanceForUserAndMonth(username, payrollMonth).stream()
                .filter(ZepAttendance::billable)
                .toList();
    }

    //Return the attendances for a user for a given month. The month in which the date is located determines the month to be queried.
    public List<ZepAttendance> getAttendanceForUserAndMonth(String username, YearMonth payrollMonth) {
        String startDate = payrollMonth.atDay(1).toString();
        String endDate = payrollMonth.atEndOfMonth().toString();

        return Multi.createBy().repeating()
                .uni(AtomicInteger::new, page ->
                        zepAttendanceRestClient.getAttendance(startDate, endDate, username, page.incrementAndGet())
                                //.onFailure().invoke(ex -> logger.warn("Error retrieving attendances from ZEP", ex))
                )
                .whilst(ZepResponse::hasNext)
                .map(ZepResponse::data)
                .onItem().<ZepAttendance>disjoint()
                .collect().asList()
                .await().indefinitely();
    }
}

