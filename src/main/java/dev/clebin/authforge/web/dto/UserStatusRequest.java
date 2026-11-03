package dev.clebin.authforge.web.dto;

import jakarta.validation.constraints.AssertTrue;

public record UserStatusRequest(Boolean enabled, Boolean locked) {

    @AssertTrue(message = "At least one status field must be provided.")
    public boolean isChangeSpecified() {
        return enabled != null || locked != null;
    }
}
