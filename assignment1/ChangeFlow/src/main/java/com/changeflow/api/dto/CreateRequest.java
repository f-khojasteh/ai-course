package com.changeflow.api.dto;

import com.changeflow.api.validation.NormalizedText;

public record CreateRequest(@NormalizedText(max = 200) String title,
                            @NormalizedText(max = 5000) String description,
                            @NormalizedText(max = 2000) String reason) {
    public CreateRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
        reason = reason == null ? null : reason.strip();
    }
}
