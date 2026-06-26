package com.gepardec.zep.service;

import com.gepardec.zep.api.MasterdataApi;
import com.gepardec.zep.model.Location;
import com.gepardec.zep.model.LocationDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkLocationServiceTest {

    MasterdataApi masterdataApi;
    WorkLocationService service;

    @BeforeEach
    void setUp() {
        masterdataApi = mock(MasterdataApi.class);
        service = new WorkLocationService();
        service.masterdataApi = masterdataApi;
    }

    @Test
    void resolvesNameForId() throws Exception {
        Location location = new Location();
        location.setName("Homeoffice");
        LocationDetailResponse response = new LocationDetailResponse();
        response.setData(location);
        when(masterdataApi.locationsIdGet("3")).thenReturn(response);

        Map<String, String> names = service.getWorkLocationNames(Set.of("3"));

        assertEquals("Homeoffice", names.get("3"));
    }

    @Test
    void fallsBackToPlaceholderWhenLookupThrows() throws Exception {
        when(masterdataApi.locationsIdGet("7")).thenThrow(new RuntimeException("boom"));

        Map<String, String> names = service.getWorkLocationNames(Set.of("7"));

        assertEquals("location#7", names.get("7"));
    }
}
