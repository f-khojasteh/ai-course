CREATE TABLE user_access (
    user_id UUID PRIMARY KEY,
    requester BOOLEAN NOT NULL,
    reviewer BOOLEAN NOT NULL,
    updated_at TIMESTAMP(9) WITH TIME ZONE NOT NULL
);

CREATE TABLE credential (
    credential_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_access(user_id),
    secret_hash BINARY(32) NOT NULL UNIQUE,
    created_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP(9) WITH TIME ZONE
);
CREATE INDEX credential_user_idx ON credential(user_id);

CREATE TABLE change_request (
    id UUID PRIMARY KEY,
    title VARCHAR(400) NOT NULL,
    description CLOB NOT NULL,
    reason CLOB NOT NULL,
    requester_id UUID NOT NULL REFERENCES user_access(user_id),
    state VARCHAR(32) NOT NULL CHECK (state IN ('Draft', 'PendingReview', 'ChangesRequested', 'Approved')),
    latest_explanation CLOB,
    created_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL CHECK (version >= 0)
);
CREATE INDEX request_owner_order_idx ON change_request(requester_id, created_at, id);
CREATE INDEX request_state_order_idx ON change_request(state, created_at, id);

CREATE TABLE review_decision (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES change_request(id),
    reviewer_id UUID NOT NULL REFERENCES user_access(user_id),
    action VARCHAR(32) NOT NULL CHECK (action IN ('Approve', 'RequestChanges')),
    decided_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    resulting_request_version BIGINT NOT NULL CHECK (resulting_request_version >= 0),
    CONSTRAINT decision_request_version_unique UNIQUE (request_id, resulting_request_version)
);
CREATE INDEX decision_reviewer_idx ON review_decision(reviewer_id);
