package com.gepardec.zep.service;

import com.gepardec.zep.api.MasterdataApi;
import com.gepardec.zep.model.LocationDetailResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class WorkLocationService {

    private static final Logger log = LoggerFactory.getLogger(WorkLocationService.class);

    @Inject
    @RestClient
    MasterdataApi masterdataApi;

    public Map<String, String> getWorkLocationNames(Set<String> workLocationIds) {
        Map<String, String> names = new HashMap<>();
        if (workLocationIds == null) {
            return names;
        }
        for (String workLocationId : workLocationIds) {
            if (workLocationId == null) {
                continue;
            }
            names.put(workLocationId, resolveWorkLocationName(workLocationId));
        }
        return names;
    }

    private String resolveWorkLocationName(String workLocationId) {
        try {
            LocationDetailResponse response = masterdataApi.locationsIdGet(workLocationId);
            if (response != null && response.getData() != null
                    && response.getData().getName() != null
                    && !response.getData().getName().isBlank()) {
                return response.getData().getName();
            }
        } catch (Exception e) {
            log.warn("Could not resolve work location name for id={}", workLocationId, e);
        }
        return "location#" + workLocationId;
    }
}
