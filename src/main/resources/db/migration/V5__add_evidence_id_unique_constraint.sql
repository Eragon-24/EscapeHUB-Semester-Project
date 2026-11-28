ALTER TABLE collected_evidence
    ADD COLUMN evidence_id VARCHAR(100);

ALTER TABLE collected_evidence
    ADD CONSTRAINT uq_collected_evidence_session_evidence_id
        UNIQUE (session_id, evidence_id);