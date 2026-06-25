package com.codeduel.codeduel.arena.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.codeduel.codeduel.arena.model.Problem;
import java.util.UUID;

public interface ProblemRepository extends JpaRepository<Problem,UUID>{
    
}
