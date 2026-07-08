package com.codeduel.codeduel.arena.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.codeduel.codeduel.arena.repository.ProblemRepository;
import com.codeduel.codeduel.arena.model.Difficulty;
import com.codeduel.codeduel.arena.model.Problem;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProblemSeeder implements CommandLineRunner {

    private final ProblemRepository problemRepository;

    @Override
    public void run(String... args) throws Exception {
        // Obsolete: All 30 standard, fully-supported single-line problems are seeded via Flyway V7 migration.
    }
}
