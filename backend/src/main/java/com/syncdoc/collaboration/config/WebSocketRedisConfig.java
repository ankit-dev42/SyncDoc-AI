package com.syncdoc.collaboration.config;

import com.syncdoc.collaboration.messaging.websocket.RedisWebSocketBroadcaster;
import com.syncdoc.collaboration.messaging.websocket.RedisWebSocketSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@Profile("!test")
public class WebSocketRedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        RedisWebSocketSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic(RedisWebSocketBroadcaster.BROADCAST_CHANNEL));
        return container;
    }
}
