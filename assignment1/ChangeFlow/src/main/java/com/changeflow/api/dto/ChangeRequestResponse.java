package com.changeflow.api.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.UUID;

@JsonPropertyOrder({"id", "title", "description", "reason", "requesterId", "state", "latestExplanation", "createdAt", "updatedAt", "revision"})
public record ChangeRequestResponse(UUID id, String title, String description, String reason, UUID requesterId,
                                    String state, String latestExplanation, Instant createdAt, Instant updatedAt, long revision) { }
