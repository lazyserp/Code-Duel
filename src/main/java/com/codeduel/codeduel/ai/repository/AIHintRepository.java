package com.codeduel.codeduel.ai.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeduel.codeduel.ai.model.AIHint;

public interface AIHintRepository extends JpaRepository<AIHint,UUID> {
    
}
