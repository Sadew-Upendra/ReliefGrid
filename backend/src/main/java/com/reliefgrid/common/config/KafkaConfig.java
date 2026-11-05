package com.reliefgrid.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Topic names match docs/architecture.md exactly. Spring Boot auto-configures the
 * KafkaTemplate/ProducerFactory from spring.kafka.* in application.yml; these NewTopic
 * beans let Kafka's admin client create topics on startup in local/dev (harmless no-op
 * against MSK, where topics are created once via infra/kafka's init job or Terraform).
 */

@Configuration
public class KafkaConfig {

    public static final String REQUEST_CREATED = "request.created";
    public static final String REQUEST_STATUS_CHANGED = "request.status-changed";
    public static final String TASK_ASSIGNED = "task.assigned";
    public static final String AI_CLASSIFICATION_RESULT = "ai.classification-result";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    NewTopic requestCreatedTopic() { return topic(REQUEST_CREATED); }

    @Bean
    NewTopic requestStatusChangedTopic() { return topic(REQUEST_STATUS_CHANGED); }

    @Bean
    NewTopic taskAssignedTopic() { return topic(TASK_ASSIGNED); }

    private NewTopic topic(String name) {
        return new NewTopic(name, 3, (short) 1);
    }
}