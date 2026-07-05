package com.codeduel.codeduel.arena.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import com.codeduel.codeduel.arena.model.Match;

import java.util.List;

import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match,UUID>
{
    @Query("SELECT m FROM Match m WHERE (m.user1.id = :userId OR m.user2.id = :userId) AND m.status = 'ACTIVE'")
    List<Match> findActiveMatchByUserId(@Param("userId") UUID userId);
    
}
