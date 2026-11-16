package com.changeflow.api.dto;

import com.changeflow.api.validation.NormalizedText;

public record RequestChangesRequest(@NormalizedText(max = 2000) String explanation) {
    public RequestChangesRequest { explanation = explanation == null ? null : explanation.strip(); }
}
