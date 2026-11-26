CREATE TABLE collected_evidence (
                                    id            UUID                     PRIMARY KEY,
                                    session_id    UUID                     NOT NULL
                                        REFERENCES game_session(id) ON DELETE CASCADE,
                                    title         VARCHAR(200)             NOT NULL,
                                    description   VARCHAR(2000)            NOT NULL,
                                    discovered_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_collected_evidence_session
    ON collected_evidence (session_id, discovered_at);