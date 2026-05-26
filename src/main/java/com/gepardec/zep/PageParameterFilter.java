package com.gepardec.zep;

import com.gepardec.zep.service.PageContext;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@ApplicationScoped
@Unremovable
public class PageParameterFilter implements ClientRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PageParameterFilter.class);

    @Override
    public void filter(ClientRequestContext requestContext) {
        URI uri = requestContext.getUri();
        String query = uri.getQuery();

        log.debug("Original URI: {}", uri);

        // Get page number from ThreadLocal (default to 1 if not set)
        Integer pageNumber = PageContext.getPage();
        if (pageNumber == null) {
            pageNumber = 1;
        }

        // Add page parameter if not already present
        if (query != null && !query.contains("page=")) {
            String newQuery = query + "&page=" + pageNumber;
            URI newUri = URI.create(uri.toString().replace("?" + query, "?" + newQuery));
            requestContext.setUri(newUri);
            log.debug("Modified URI with page parameter {}: {}", pageNumber, newUri);
        } else if (query == null) {
            URI newUri = URI.create(uri + "?page=" + pageNumber);
            requestContext.setUri(newUri);
            log.debug("Added page parameter {} to URI without query: {}", pageNumber, newUri);
        } else {
            log.debug("URI already contains page parameter: {}", uri);
        }
    }
}
