package com.syncdoc.collaboration.messaging.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RedisWebSocketBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(RedisWebSocketBroadcaster.class);

    public static final String BROADCAST_CHANNEL = "collaboration:websocket:broadcast";

    private final String instanceId = UUID.randomUUID().toString();
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisWebSocketBroadcaster(
        SimpMessagingTemplate messagingTemplate,
        RedisTemplate<String, String> redisTemplate,
        ObjectMapper objectMapper
    ) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void broadcast(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);

        try {
            RedisBroadcastEnvelope envelope = new RedisBroadcastEnvelope();
            envelope.setInstanceId(instanceId);
            envelope.setDestination(destination);
            envelope.setPayload(objectMapper.valueToTree(payload));

            redisTemplate.convertAndSend(BROADCAST_CHANNEL, objectMapper.writeValueAsString(envelope));
        } catch (Exception ex) {
            logger.error("Failed to publish Redis WebSocket broadcast", ex);
        }
    }

    public void onRedisMessage(String rawMessage) {
        try {
            RedisBroadcastEnvelope envelope = objectMapper.readValue(rawMessage, RedisBroadcastEnvelope.class);

            if (instanceId.equals(envelope.getInstanceId())) {
                return;
            }

            messagingTemplate.convertAndSend(envelope.getDestination(), envelope.getPayload());
        } catch (Exception ex) {
            logger.error("Failed to consume Redis WebSocket broadcast", ex);
        }
    }

    public static class RedisBroadcastEnvelope {
        private String instanceId;
        private String destination;
        private JsonNode payload;

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public JsonNode getPayload() {
            return payload;
        }

        public void setPayload(JsonNode payload) {
            this.payload = payload;
        }
    }
}
