package com.changeflow.api;

import com.changeflow.api.dto.*;
import com.changeflow.application.RequestSnapshot;
import com.changeflow.domain.ChangeRequest;
import org.springframework.stereotype.Component;

@Component
public class ChangeRequestDtoMapper {
    public ChangeRequestResponse response(RequestSnapshot snapshot) {
        var value = snapshot.request();
        return new ChangeRequestResponse(value.id(), value.title(), value.description(), value.reason(), value.requesterId(),
                value.state().name(), value.latestExplanation(), value.createdAt(), value.updatedAt(), snapshot.revision());
    }
    public ChangeRequest.Edit edit(EditRequest dto) {
        return new ChangeRequest.Edit(dto.titleSupplied(), dto.getTitle(), dto.descriptionSupplied(), dto.getDescription(), dto.reasonSupplied(), dto.getReason());
    }
}
