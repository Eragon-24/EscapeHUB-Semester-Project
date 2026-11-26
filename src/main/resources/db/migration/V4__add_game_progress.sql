-- US-024: authoritative puzzle progress, collected items and exit state of each game.
ALTER TABLE game_state
    ADD COLUMN exit_unlocked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE game_completed_puzzle (
    session_id UUID NOT NULL REFERENCES game_session(id) ON DELETE CASCADE,
    puzzle_id VARCHAR(120) NOT NULL,
    PRIMARY KEY (session_id, puzzle_id)
);

CREATE TABLE game_collected_item (
    session_id UUID NOT NULL REFERENCES game_session(id) ON DELETE CASCADE,
    item_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (session_id, item_type)
);
