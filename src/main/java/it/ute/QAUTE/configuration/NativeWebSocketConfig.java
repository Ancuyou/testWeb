package it.ute.QAUTE.configuration;

import it.ute.QAUTE.web.OnlineStatusHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket  // ✅ Dùng @EnableWebSocket thay vì @EnableWebSocketMessageBroker
public class NativeWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private OnlineStatusHandler onlineStatusHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(onlineStatusHandler, "/realtime/online")
                .setAllowedOriginPatterns("*");  // Không dùng withSockJS()
    }
}