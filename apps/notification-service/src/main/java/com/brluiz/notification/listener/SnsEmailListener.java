package com.brluiz.notification.listener;

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
public class SnsEmailListener {

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

            subscribeQueueToTopic();
            log.info("Email Notification Listener initialized. Queue: {} | Topic: {}", queueUrl, topicArn);

            executor = Executors.newSingleThreadExecutor();
            executor.submit(this::pollMessages);

        } catch (Exception e) {
            log.error("Email SNS/SQS initialization failed: {}", e.getMessage(), e);
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

            log.info("Email queue subscribed to SNS topic. Queue ARN: {}", queueArn);
        } catch (Exception e) {
            log.warn("Could not subscribe email queue to topic: {}", e.getMessage());
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
                        processEmailMessage(message.body());
                        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .receiptHandle(message.receiptHandle())
                                .build());
                    } catch (Exception e) {
                        log.error("Error processing email message: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error polling email SQS: {}", e.getMessage());
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processEmailMessage(String body) throws Exception {
        JsonNode snsEnvelope = objectMapper.readTree(body);
        String messageContent = snsEnvelope.has("Message")
                ? snsEnvelope.get("Message").asText()
                : body;

        JsonNode event = objectMapper.readTree(messageContent);
        String email = event.has("email") ? event.get("email").asText() : "unknown";
        String username = event.has("username") ? event.get("username").asText() : "User";

        log.info("--------------------------------------------------");
        log.info("SENDING EMAIL NOTIFICATION");
        log.info("To: {}", email);
        log.info("Subject: Welcome to our platform!");
        log.info("Body: Hello {}, your account has been successfully created!", username);
        log.info("--------------------------------------------------");
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