package com.brluiz.tickets.listener;

import com.brluiz.tickets.enums.TicketType;
import com.brluiz.tickets.service.TicketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.sns.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class SnsTicketListener {

    private final TicketService ticketService;
    private final ObjectMapper objectMapper;

    @Value("${app.sns.topic-arn}")
    private String topicArn;

    @Value("${app.sns.region:us-east-1}")
    private String region;

    @Value("${app.sns.queue-url}")
    private String queueUrl;

    @Value("${app.sns.localstack-endpoint}")
    private String localstackEndpoint;

    private SqsClient sqsClient;
    private SnsClient snsClient;
    private ExecutorService executor;
    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        try {
            URI endpoint = URI.create(localstackEndpoint);
            var credentials = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test", "test"));

            sqsClient = SqsClient.builder()
                    .endpointOverride(endpoint)
                    .region(Region.of(region))
                    .credentialsProvider(credentials)
                    .build();
            snsClient = SnsClient.builder()
                    .endpointOverride(endpoint)
                    .region(Region.of(region))
                    .credentialsProvider(credentials)
                    .build();

            // Subscribe the existing SQS queue to the SNS topic
            subscribeQueueToTopic();

            log.info("Listener initialized. Queue: {} | Topic: {}", queueUrl, topicArn);

            // Start polling in background
            executor = Executors.newSingleThreadExecutor();
            executor.submit(this::pollMessages);

        } catch (Exception e) {
            log.error("SNS/SQS initialization failed: {}", e.getMessage(), e);
        }
    }

    private void subscribeQueueToTopic() {
        try {
            String queueArn = sqsClient.getQueueAttributes(
                    GetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributeNames(QueueAttributeName.QUEUE_ARN)
                            .build()
            ).attributes().get(QueueAttributeName.QUEUE_ARN);

            snsClient.subscribe(SubscribeRequest.builder()
                    .topicArn(topicArn)
                    .protocol("sqs")
                    .endpoint(queueArn)
                    .build());

            log.info("Queue subscribed to SNS topic. Queue ARN: {}", queueArn);
        } catch (Exception e) {
            log.warn("Could not subscribe queue to topic (may already be subscribed): {}", e.getMessage());
        }
    }

    private void pollMessages() {
        while (running) {
            try {
                ReceiveMessageResponse response = sqsClient.receiveMessage(
                        ReceiveMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .maxNumberOfMessages(5)
                                .waitTimeSeconds(20)
                                .build());

                for (Message message : response.messages()) {
                    try {
                        processSnsMessage(message.body());
                        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .receiptHandle(message.receiptHandle())
                                .build());
                    } catch (Exception e) {
                        log.error("Error processing message: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error polling SQS: {}", e.getMessage());
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processSnsMessage(String body) throws Exception {
        // SNS wraps the message in a JSON with "Message" key
        JsonNode snsEnvelope = objectMapper.readTree(body);
        String messageContent = snsEnvelope.has("Message")
                ? snsEnvelope.get("Message").asText()
                : body;

        JsonNode ticketEvent = objectMapper.readTree(messageContent);
        String typeStr = ticketEvent.has("type") ? ticketEvent.get("type").asText() : null;

        if (typeStr != null) {
            try {
                TicketType ticketType = TicketType.valueOf(typeStr.toUpperCase());
                ticketService.incrementCount(ticketType);
                log.info("Processed ticket event: {}", typeStr);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown ticket type: {}", typeStr);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
        }
        if (sqsClient != null) sqsClient.close();
        if (snsClient != null) snsClient.close();
    }
}