package com.bitalep.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public final class InstantQuery {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    private InstantQuery() {}

    public static Instant start(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.length() <= 10) {
            return LocalDate.parse(value).atStartOfDay(ZONE).toInstant();
        }
        return Instant.parse(value);
    }

    public static Instant endInclusive(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.length() <= 10) {
            return LocalDate.parse(value).atTime(LocalTime.MAX).atZone(ZONE).toInstant();
        }
        return Instant.parse(value);
    }
}
