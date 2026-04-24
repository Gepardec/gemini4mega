package com.gepardec.rest;

import com.gepardec.llm.service.PromptService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/checking")
public class CheckingController {
    private static final Logger log = LoggerFactory.getLogger(CheckingController.class);
    @Inject
    PromptService promptService;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public String checking(String entries){
        if (!entries.isEmpty()) {
            try {
                return promptService.prompt(entries);
            }
            catch (Exception e) {
                log.error(String.valueOf(e));
                return "Something went wrong";
            }
        } else {
            return "No entries provided";
        }
    }
}
