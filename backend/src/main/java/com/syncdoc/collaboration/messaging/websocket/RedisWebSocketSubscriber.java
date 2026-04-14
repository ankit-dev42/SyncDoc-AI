package com.syncdoc.collaboration.messaging.websocket;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RedisWebSocketSubscriber implements MessageListener {

    private final RedisWebSocketBroadcaster broadcaster;

    public RedisWebSocketSubscriber(RedisWebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        broadcaster.onRedisMessage(payload);
    }
}
