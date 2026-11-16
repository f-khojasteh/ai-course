package com.changeflow.api;

import com.changeflow.api.dto.*;
import com.changeflow.application.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJacksonInputMessage;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import java.net.URI;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import static com.changeflow.application.ApplicationFailure.Code.*;

@RestController
@RequestMapping(value = "/api/change-requests", produces = "application/json;charset=UTF-8")
public class ChangeRequestController {
    private final ChangeRequestService service;
    private final ChangeRequestDtoMapper mapper;
    private final ObjectMapper json;

    public ChangeRequestController(ChangeRequestService service, ChangeRequestDtoMapper mapper, ObjectMapper json) {
        this.service = service; this.mapper = mapper; this.json = json;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChangeRequestResponse> create(@AuthenticationPrincipal AuthenticatedCaller caller,
                                                       @Valid @RequestBody CreateRequest body) {
        var result = service.create(caller, body.title(), body.description(), body.reason());
        return ResponseEntity.created(URI.create("/api/change-requests/" + result.request().id()))
                .cacheControl(CacheControl.noStore()).eTag(RequestEtags.value(result)).body(mapper.response(result));
    }

    @GetMapping
    public ResponseEntity<ChangeRequestListResponse> list(@AuthenticationPrincipal AuthenticatedCaller caller,
                                                         @RequestParam MultiValueMap<String, String> parameters) {
        for (var parameter : parameters.entrySet()) {
            if (!Set.of("offset", "limit").contains(parameter.getKey()) || parameter.getValue().size() != 1) throw new ApplicationFailure(INVALID_PARAMETER);
        }
        var page = service.list(caller, number(parameters, "offset", 0), number(parameters, "limit", 50));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ChangeRequestListResponse(
                page.items().stream().map(mapper::response).toList(), page.offset(), page.limit(), page.hasMore()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChangeRequestResponse> read(@AuthenticationPrincipal AuthenticatedCaller caller, @PathVariable UUID id) {
        return item(service.read(caller, id));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChangeRequestResponse> edit(@AuthenticationPrincipal AuthenticatedCaller caller, @PathVariable UUID id,
                                                     @RequestHeader HttpHeaders headers,
                                                     @Valid @RequestBody EditRequest body) {
        return item(service.edit(caller, id, RequestEtags.expected(id, headers.get(HttpHeaders.IF_MATCH)), mapper.edit(body)));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ChangeRequestResponse> submit(@AuthenticationPrincipal AuthenticatedCaller caller, @PathVariable UUID id,
                                                       @RequestHeader HttpHeaders headers,
                                                       @RequestBody(required = false) byte[] body) throws HttpMediaTypeNotSupportedException, IOException {
        emptyActionBody(body, headers);
        return item(service.submit(caller, id, RequestEtags.expected(id, headers.get(HttpHeaders.IF_MATCH))));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ChangeRequestResponse> approve(@AuthenticationPrincipal AuthenticatedCaller caller, @PathVariable UUID id,
                                                        @RequestHeader HttpHeaders headers,
                                                        @RequestBody(required = false) byte[] body) throws HttpMediaTypeNotSupportedException, IOException {
        emptyActionBody(body, headers);
        return item(service.approve(caller, id, RequestEtags.expected(id, headers.get(HttpHeaders.IF_MATCH))));
    }

    @PostMapping(value = "/{id}/request-changes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChangeRequestResponse> requestChanges(@AuthenticationPrincipal AuthenticatedCaller caller, @PathVariable UUID id,
                                                               @RequestHeader HttpHeaders headers,
                                                               @Valid @RequestBody RequestChangesRequest body) {
        return item(service.requestChanges(caller, id, RequestEtags.expected(id, headers.get(HttpHeaders.IF_MATCH)), body.explanation()));
    }

    private ResponseEntity<ChangeRequestResponse> item(RequestSnapshot result) {
        // The public service proxy has committed before a success body/ETag is constructed.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(RequestEtags.value(result)).body(mapper.response(result));
    }
    private int number(MultiValueMap<String, String> parameters, String name, int fallback) {
        if (!parameters.containsKey(name)) return fallback;
        try { return Integer.parseInt(parameters.getFirst(name)); }
        catch (NumberFormatException invalid) { throw new ApplicationFailure(INVALID_PARAMETER); }
    }
    private void emptyActionBody(byte[] bytes, HttpHeaders headers) throws HttpMediaTypeNotSupportedException, IOException {
        if (bytes == null || bytes.length == 0) return;
        MediaType media = headers.getContentType();
        if (media == null || !MediaType.APPLICATION_JSON.equalsTypeAndSubtype(media)) {
            throw new HttpMediaTypeNotSupportedException(media == null ? MediaType.APPLICATION_OCTET_STREAM : media, List.of(MediaType.APPLICATION_JSON));
        }
        JsonNode body;
        try { body = json.readerFor(JsonNode.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(bytes); }
        catch (JsonProcessingException invalid) {
            throw new HttpMessageNotReadableException("Invalid action JSON.", invalid,
                    new MappingJacksonInputMessage(new ByteArrayInputStream(bytes), headers));
        }
        if (body == null || !body.isObject()) throw new ApplicationFailure(INVALID_CONTENT);
        var fields = body.fieldNames();
        if (fields.hasNext()) {
            String field = fields.next();
            boolean controlled = Set.of("requesterId", "requesterIdentity", "roles", "state", "id", "revision", "createdAt", "updatedAt", "latestExplanation").contains(field);
            throw new ApplicationFailure(controlled ? FORBIDDEN_FIELD : UNKNOWN_FIELD);
        }
    }
}
