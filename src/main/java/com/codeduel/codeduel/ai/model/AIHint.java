package com.codeduel.codeduel.ai.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_hints")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIHint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "review_type")
    private String context; 

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String hint; 

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "tokens_consumed")
    private Integer tokensConsumed;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();


    public AIHint(String hint, String context) {
        this.hint = hint;
        this.context = context;
        this.createdAt = LocalDateTime.now();
    }
}
