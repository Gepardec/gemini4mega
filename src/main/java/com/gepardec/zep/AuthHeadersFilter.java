package com.gepardec.zep;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Unremovable
public class AuthHeadersFilter implements ClientRequestFilter {

    @ConfigProperty(name = "zep.rest-token")
    String token;

    @Override
    public void filter(ClientRequestContext requestContext) {
        requestContext.getHeaders().add("Authorization", "Bearer " + token);
    }
}
