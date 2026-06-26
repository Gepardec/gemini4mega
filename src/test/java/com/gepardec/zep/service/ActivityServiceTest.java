package com.gepardec.zep.service;

import com.gepardec.zep.api.MasterdataApi;
import com.gepardec.zep.model.MasterDataActivity;
import com.gepardec.zep.model.MasterDataActivityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivityServiceTest {

    MasterdataApi masterdataApi;
    ActivityService service;

    @BeforeEach
    void setUp() {
        masterdataApi = mock(MasterdataApi.class);
        service = new ActivityService();
        service.masterdataApi = masterdataApi;
    }

    @Test
    void resolvesNameForId() throws Exception {
        MasterDataActivity activity = new MasterDataActivity();
        activity.setName("Entwicklung");
        MasterDataActivityResponse response = new MasterDataActivityResponse();
        response.setData(activity);
        when(masterdataApi.activitiesIdGet("10")).thenReturn(response);

        Map<String, String> names = service.getActivityNames(Set.of("10"));

        assertEquals("Entwicklung", names.get("10"));
    }

    @Test
    void fallsBackToPlaceholderWhenLookupThrows() throws Exception {
        when(masterdataApi.activitiesIdGet("99")).thenThrow(new RuntimeException("boom"));

        Map<String, String> names = service.getActivityNames(Set.of("99"));

        assertEquals("activity#99", names.get("99"));
    }
}
