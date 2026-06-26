package com.gepardec.zep.service;

import com.gepardec.zep.api.MasterdataApi;
import com.gepardec.zep.model.MasterDataActivityResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    @Inject
    @RestClient
    MasterdataApi masterdataApi;

    public Map<String, String> getActivityNames(Set<String> activityIds) {
        Map<String, String> names = new HashMap<>();
        if (activityIds == null) {
            return names;
        }
        for (String activityId : activityIds) {
            if (activityId == null) {
                continue;
            }
            names.put(activityId, resolveActivityName(activityId));
        }
        return names;
    }

    private String resolveActivityName(String activityId) {
        try {
            MasterDataActivityResponse response = masterdataApi.activitiesIdGet(activityId);
            if (response != null && response.getData() != null
                    && response.getData().getName() != null
                    && !response.getData().getName().isBlank()) {
                return response.getData().getName();
            }
        } catch (Exception e) {
            log.warn("Could not resolve activity name for id={}", activityId, e);
        }
        return "activity#" + activityId;
    }
}
