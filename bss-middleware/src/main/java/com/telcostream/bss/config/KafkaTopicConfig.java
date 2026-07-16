package com.telcostream.bss.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${bss.topics.raw-cdrs}")
    private String rawCdrsTopic;

    @Value("${bss.topics.dlq}")
    private String dlqTopic;

    @Bean
    public NewTopic rawCdrsTopic() {
        return TopicBuilder.name(rawCdrsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
