package com.gepardec.zep.service;

import com.gepardec.zep.api.TicketsApi;
import com.gepardec.zep.model.TicketSubtask;
import com.gepardec.zep.model.TicketSubtaskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubtaskServiceTest {

    TicketsApi ticketsApi;
    SubtaskService service;

    @BeforeEach
    void setUp() {
        ticketsApi = mock(TicketsApi.class);
        service = new SubtaskService();
        service.ticketsApi = ticketsApi;
    }

    @Test
    void resolvesNameForTicketAndSubtask() throws Exception {
        TicketSubtask subtask = new TicketSubtask();
        subtask.setName("Login-Maske");
        TicketSubtaskResponse response = new TicketSubtaskResponse();
        response.setData(subtask);
        when(ticketsApi.ticketsIdSubtasksSubtaskIdGet(5, 12)).thenReturn(response);

        Map<TicketSubtaskKey, String> names =
                service.getSubtaskNames(Set.of(new TicketSubtaskKey(5, 12)));

        assertEquals("Login-Maske", names.get(new TicketSubtaskKey(5, 12)));
    }

    @Test
    void fallsBackToPlaceholderWhenTicketIdIsNull() throws Exception {
        Map<TicketSubtaskKey, String> names =
                service.getSubtaskNames(Set.of(new TicketSubtaskKey(null, 12)));

        assertEquals("subtask#12", names.get(new TicketSubtaskKey(null, 12)));
    }

    @Test
    void fallsBackToPlaceholderWhenLookupThrows() throws Exception {
        when(ticketsApi.ticketsIdSubtasksSubtaskIdGet(5, 12)).thenThrow(new RuntimeException("boom"));

        Map<TicketSubtaskKey, String> names =
                service.getSubtaskNames(Set.of(new TicketSubtaskKey(5, 12)));

        assertEquals("subtask#12", names.get(new TicketSubtaskKey(5, 12)));
    }
}
