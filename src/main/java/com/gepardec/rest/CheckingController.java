package com.gepardec.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/checking")
public class CheckingController {
    @GET
    @Produces(MediaType.TEXT_PLAIN)

    public String checking(String message){
        return "";
    }
}
