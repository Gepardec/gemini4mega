package com.gepardec.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URL;

@ApplicationScoped
public class ZepConfig {

    @Inject
    @ConfigProperty(name = "quarkus.rest-client.zep.url")
    URL origin;

    @Inject
    @ConfigProperty(name = "zep.rest-token")
    String restToken;


    public String getRestBearerToken() {
        return restToken;
    }

    public String getUrlForFrontend() {
        return origin.toString();
    }
}
