package fr.univ.billetterie.dto;

import java.time.Instant;

public record ErreurResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
}

