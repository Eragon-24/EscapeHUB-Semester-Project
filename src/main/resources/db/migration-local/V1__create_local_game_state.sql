CREATE TABLE game_states (
    session_id UUID PRIMARY KEY,
    current_room_id VARCHAR(120) NOT NULL,
    game_status VARCHAR(32) NOT NULL,
    exit_unlocked BOOLEAN NOT NULL,
    state_version BIGINT NOT NULL
);

CREATE TABLE game_state_attributes (
    session_id UUID NOT NULL REFERENCES game_states (session_id) ON DELETE CASCADE,
    attribute_key VARCHAR(64) NOT NULL,
    attribute_value VARCHAR(1000) NOT NULL,
    PRIMARY KEY (session_id, attribute_key)
);

CREATE TABLE game_state_completed_puzzles (
    session_id UUID NOT NULL REFERENCES game_states (session_id) ON DELETE CASCADE,
    puzzle_id VARCHAR(120) NOT NULL,
    PRIMARY KEY (session_id, puzzle_id)
);

CREATE TABLE game_state_collected_items (
    session_id UUID NOT NULL REFERENCES game_states (session_id) ON DELETE CASCADE,
    item_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (session_id, item_type)
);
