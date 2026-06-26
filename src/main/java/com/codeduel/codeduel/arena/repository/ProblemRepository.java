package com.codeduel.codeduel.arena.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.codeduel.codeduel.arena.model.Problem;
import java.util.UUID;

import com.codeduel.codeduel.arena.model.Difficulty;
import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem,UUID>{
    List<Problem> findByDifficulty(Difficulty difficulty);
}
