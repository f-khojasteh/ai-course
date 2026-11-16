package com.changeflow.api.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonPropertyOrder({"items", "offset", "limit", "hasMore"})
public record ChangeRequestListResponse(List<ChangeRequestResponse> items, int offset, int limit, boolean hasMore) {
    public ChangeRequestListResponse { items = List.copyOf(items); }
}
