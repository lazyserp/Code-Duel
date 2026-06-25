package com.codeduel.codeduel.arena.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.codeduel.codeduel.auth.model.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @ManyToOne
    @JoinColumn(name = "user1_id")
    private User user1;
    

    @ManyToOne
    @JoinColumn(name = "user2_id")
    private User user2;


    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    private String status;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    
}
