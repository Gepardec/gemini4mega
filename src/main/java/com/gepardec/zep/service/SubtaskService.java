package com.gepardec.zep.service;

import com.gepardec.zep.api.TicketsApi;
import com.gepardec.zep.model.TicketSubtaskResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class SubtaskService {

    private static final Logger log = LoggerFactory.getLogger(SubtaskService.class);

    @Inject
    @RestClient
    TicketsApi ticketsApi;

    public Map<TicketSubtaskKey, String> getSubtaskNames(Set<TicketSubtaskKey> keys) {
        Map<TicketSubtaskKey, String> names = new HashMap<>();
        if (keys == null) {
            return names;
        }
        for (TicketSubtaskKey key : keys) {
            if (key == null || key.subtaskId() == null) {
                continue;
            }
            names.put(key, resolveSubtaskName(key));
        }
        return names;
    }

    private String resolveSubtaskName(TicketSubtaskKey key) {
        if (key.ticketId() != null) {
            try {
                TicketSubtaskResponse response =
                        ticketsApi.ticketsIdSubtasksSubtaskIdGet(key.ticketId(), key.subtaskId());
                if (response != null && response.getData() != null
                        && response.getData().getName() != null
                        && !response.getData().getName().isBlank()) {
                    return response.getData().getName();
                }
            } catch (Exception e) {
                log.warn("Could not resolve subtask name for ticketId={}, subtaskId={}",
                        key.ticketId(), key.subtaskId(), e);
            }
        }
        return "subtask#" + key.subtaskId();
    }
}
