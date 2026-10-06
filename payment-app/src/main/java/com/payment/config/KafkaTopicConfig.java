package com.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_PAYMENT_INITIATED  = "payment.initiated";
    public static final String TOPIC_PAYMENT_PROCESSED  = "payment.processed";
    public static final String TOPIC_PAYMENT_FAILED     = "payment.failed";
    public static final String TOPIC_PAYMENT_DLQ        = "payment.dlq";

    @Value("${spring.kafka.topic.replication-factor:1}")
    private int replicationFactor;

    @Value("${spring.kafka.topic.partitions:3}")
    private int partitions;

    @Bean
    public NewTopic paymentInitiatedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_INITIATED)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    public NewTopic paymentProcessedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_PROCESSED)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_FAILED)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    public NewTopic paymentDlqTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_DLQ)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}
