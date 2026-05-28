package com.gepardec.zep.service;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@ApplicationScoped
public class PseudonymizationService {
    private static final Logger LOG = Logger.getLogger(PseudonymizationService.class);

    private final BiMap<Integer, String> pseudonymizedUserIds = Maps.synchronizedBiMap(HashBiMap.create());

    private final AtomicInteger idCounter = new AtomicInteger(0);

    public <T> List<T> pseudonymize(List<T> entries, Function<T, String> idGetter, PseudonymizerFunction<T> pseudonymizeFunction) {
        if (entries == null || idGetter == null || pseudonymizeFunction == null) {
            throw new IllegalArgumentException("Parameters must not be null");
        }

        return entries.stream()
                .map(entry -> {
                    String id = idGetter.apply(entry);
                    if (id == null) {
                        LOG.error("ID must not be null. Entry will be skipped.");
                        return null;
                    }
                    if (!pseudonymizedUserIds.containsValue(id)) {
                        pseudonymizedUserIds.put(idCounter.getAndIncrement(), id);
                    }

                    Integer pseudonymizedId = pseudonymizedUserIds.inverse().get(id);
                    return pseudonymizeFunction.apply(entry, String.valueOf(pseudonymizedId));
                })
                .toList();
    }

    public <T> List<T> unpseudonymize(List<T> entries, Function<T, String> idGetter, PseudonymizerFunction<T> unpseudonymizeFunction) {
        if (entries == null || idGetter == null || unpseudonymizeFunction == null) {
            throw new IllegalArgumentException("Parameters must not be null");
        }

        return entries.stream()
                .map(entry -> {
                    try {
                        int pseudonymizedId = Integer.parseInt(idGetter.apply(entry));
                        if (pseudonymizedUserIds.containsKey(pseudonymizedId)) {
                            String originalId = pseudonymizedUserIds.get(pseudonymizedId);
                            return unpseudonymizeFunction.apply(entry, originalId);
                        } else {
                            LOG.warnf("Unable to unpseudonymize entry with pseudonymized ID: %d. Entry will be skipped.", pseudonymizedId);
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        LOG.error("Invalid pseudonymized ID format for entry. Entry will be skipped.", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public interface PseudonymizerFunction<T> {
        T apply(T t, String employeeId);
    }
}
