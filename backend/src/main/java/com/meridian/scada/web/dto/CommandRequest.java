package com.meridian.scada.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CommandRequest(
        @NotBlank
        @Pattern(regexp = "START|STOP|OPEN|CLOSE|RESET", message = "Unsupported command")
        String action
) {
}
