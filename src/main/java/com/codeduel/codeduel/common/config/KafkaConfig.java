package com.codeduel.codeduel.common.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import com.codeduel.codeduel.matchmaking.event.MatchCreatedEvent;
import com.codeduel.codeduel.submission.event.SubmissionEvaluatedEvent;
import com.codeduel.codeduel.submission.event.SubmissionReceivedEvent;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, MatchCreatedEvent> matchCreatedKafkaTemplate(ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>((ProducerFactory<String, MatchCreatedEvent>) (ProducerFactory<?, ?>) pf);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, SubmissionReceivedEvent> submissionReceivedKafkaTemplate(ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>((ProducerFactory<String, SubmissionReceivedEvent>) (ProducerFactory<?, ?>) pf);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, SubmissionEvaluatedEvent> submissionEvaluatedKafkaTemplate(ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>((ProducerFactory<String, SubmissionEvaluatedEvent>) (ProducerFactory<?, ?>) pf);
    }
}
