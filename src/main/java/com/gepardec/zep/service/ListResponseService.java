package com.gepardec.zep.service;

import com.gepardec.zep.api.ApiException;
import com.gepardec.zep.model.Attendance;
import com.gepardec.zep.model.AttendancesListResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Generic service to fetch all elements from paginated ZEP API responses.
 * Works with any ListResponse type (AttendancesListResponse, CustomersListResponse, etc.)
 */
@ApplicationScoped
public class ListResponseService {

    private static final Logger log = LoggerFactory.getLogger(ListResponseService.class);
    private static final int MAX_LIMIT = 100;

    /**
     * Fetches all elements from a paginated API endpoint.
     *
     * @param apiCall        Function that makes the API call and returns a ListResponse
     * @param dataExtractor  Function that extracts the data list from the response
     * @param totalExtractor Function that extracts the total count from the response metadata
     * @param <T>            The element type
     * @param <R>            The response type
     * @return List of all elements across all pages
     * @throws ApiException if the API call fails
     */
    public <T, R> List<T> fetchAll(
            ApiCallFunction<R> apiCall,
            Function<R, List<T>> dataExtractor,
            Function<R, Integer> totalExtractor) throws ApiException {

        List<T> allElements = new ArrayList<>();

        // Set page number in ThreadLocal for PageParameterFilter
        PageContext.setPage(1);

        try {
            // Fetch first page
            R response = apiCall.call();

            List<T> firstPageData = dataExtractor.apply(response);
            if (firstPageData != null) {
                allElements.addAll(firstPageData);
                log.info("Fetched page 1: {} elements", firstPageData.size());
                logAttendanceProjectIdStatsIfApplicable(response, 1, firstPageData.size());
            }

            // Calculate total pages
            Integer total = totalExtractor.apply(response);
            if (total != null && total > MAX_LIMIT) {
                int totalPages = (int) Math.ceil((double) total / MAX_LIMIT);
                log.info("Total elements: {}, Total pages: {}", total, totalPages);

                // Fetch remaining pages
                for (int page = 2; page <= totalPages; page++) {
                    PageContext.setPage(page);
                    response = apiCall.call();

                    List<T> pageData = dataExtractor.apply(response);
                    if (pageData != null) {
                        allElements.addAll(pageData);
                        log.info("Fetched page {}: {} elements", page, pageData.size());
                        logAttendanceProjectIdStatsIfApplicable(response, page, pageData.size());
                    }
                }
            }

            log.info("Total elements fetched: {}", allElements.size());
            return allElements;

        } finally {
            PageContext.clear();
        }
    }

    /**
     * Functional interface for API calls that may throw ApiException
     */
    @FunctionalInterface
    public interface ApiCallFunction<R> {
        R call() throws ApiException;
    }

    private <R> void logAttendanceProjectIdStatsIfApplicable(R response, int page, int pageSize) {
        if (!(response instanceof AttendancesListResponse attendancesResponse)) {
            return;
        }

        List<Attendance> attendances = attendancesResponse.getData();
        if (attendances == null || attendances.isEmpty()) {
            log.warn("Attendance page {} has no data payload", page);
            return;
        }

        long nullProjectIds = attendances.stream()
                .filter(attendance -> attendance.getProjectId() == null)
                .count();
        long nonNullProjectIds = attendances.size() - nullProjectIds;

        log.info("Attendance page {} mapping stats: size={}, projectId non-null={}, projectId null={}",
                page, pageSize, nonNullProjectIds, nullProjectIds);

        if (log.isDebugEnabled()) {
            String sample = attendances.stream()
                    .limit(5)
                    .map(attendance -> "id=" + attendance.getId()
                            + ",projectId=" + attendance.getProjectId()
                            + ",projectTaskId=" + attendance.getProjectTaskId()
                            + ",directionOfTravel=" + attendance.getDirectionOfTravel())
                    .toList()
                    .toString();
            log.debug("Attendance page {} sample mapping: {}", page, sample);
        }
    }
}
