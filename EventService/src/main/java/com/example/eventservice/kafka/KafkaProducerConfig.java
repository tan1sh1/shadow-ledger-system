package com.example.eventservice.kafka;

import com.example.eventservice.dto.EventRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaTemplate<String, EventRequest> kafkaTemplate(
            ProducerFactory<String, EventRequest> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
