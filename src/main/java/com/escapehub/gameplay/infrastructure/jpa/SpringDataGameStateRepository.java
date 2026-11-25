package com.escapehub.gameplay.infrastructure.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataGameStateRepository extends JpaRepository<GameStateEntity, UUID> {
}
