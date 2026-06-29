package com.codeduel.codeduel.submission.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codeduel.codeduel.submission.model.Submission;

public interface SubmissionRepository extends JpaRepository<Submission,UUID>{
    List<Submission> findByMatchId(UUID matchId);
}
