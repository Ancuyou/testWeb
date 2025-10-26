package it.ute.QAUTE.web;

import com.github.benmanes.caffeine.cache.Cache;
import it.ute.QAUTE.repository.ProfilesRepository;
import it.ute.QAUTE.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class OnlineStatusHandler extends TextWebSocketHandler {
    @Autowired
    private Cache<Integer, Boolean> onlineCache;
    @Autowired
    private AccountService accountService;
    private final ConcurrentHashMap<Integer, WebSocketSession> onlineSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer id = getId(session);
        if (id != null) {
            onlineCache.put(id, true);
            onlineSessions.put(id, session);
            System.out.println("✅ User: " + id + " connected (online)");
        }
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Integer id = getId(session);
        if (id != null) {
            onlineCache.invalidate(id);
            onlineSessions.remove(id);
            accountService.updateAccountOffline(id);
            System.out.println("❌ " + id + " disconnected (offline)");
        }
    }
    @Override
    public void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message){
        String payload = message.getPayload();
        Integer id = getId(session);
        if (id == null) return;
        if("PING".equalsIgnoreCase(payload)){
            onlineCache.put(id, true);
        }
    }
    private Integer getId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) return null;
            String query = uri.getQuery();
            if (query != null && query.startsWith("id=")) {
                return Integer.parseInt(query.substring(3));
            }
        } catch (Exception e) {
            System.err.println("⚠️ Không lấy được ID từ URI: " + e.getMessage());
        }
        return null;
    }
}
