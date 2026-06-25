package com.codeduel.codeduel.arena.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeduel.codeduel.arena.model.Match;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match,UUID>
{
    
}
