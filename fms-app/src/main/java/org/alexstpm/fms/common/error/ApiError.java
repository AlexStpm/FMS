package org.alexstpm.fms.common.error;

public record ApiError(
        String error,
        String message
) {
}
