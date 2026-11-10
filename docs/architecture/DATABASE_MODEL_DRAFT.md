# Database Model – Initial Draft

This is a conceptual draft only. Do not create final migrations until the team reviews and approves the ER model.

Proposed entities:

- `User`
- `Team`
- `TeamMembership`
- `Mission`
- `MissionRun`
- `MissionRunParticipant`
- `Route`
- `Puzzle`
- `PuzzlePrerequisite`
- `PuzzleProgress`
- `RouteProgress`
- `EvidenceDefinition`
- `CollectedEvidence`
- `ApiSnapshot`

Important distinctions:

- A `Mission` is the reusable definition/content of an adventure.
- A `MissionRun` is one actual playthrough by an individual or team.
- `EvidenceDefinition` describes a clue available in the mission.
- `CollectedEvidence` records that a clue was discovered in a particular run.
- `PuzzleProgress` and `RouteProgress` belong to a specific run, not globally to the mission.

Open design questions:

1. Exact user/team relationship and membership constraints.
2. Whether final-stage puzzles belong to a special route or have no route association.
3. How puzzle prerequisites are represented and validated.
4. Snapshot versioning and association with mission/puzzle versions.
5. How solo runs are represented alongside team runs.
