package it.ute.QAUTE.configuration;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import it.ute.QAUTE.dto.response.MFAResponse;
import it.ute.QAUTE.repository.RefreshTokenRepository;
import it.ute.QAUTE.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MFARemovalListener implements RemovalListener<String, MFAResponse> {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onRemoval(String key, MFAResponse value, RemovalCause cause) {
        if (value != null && value.getAccesstoken() != null) {
            if (cause == RemovalCause.EXPIRED || cause == RemovalCause.SIZE) {
                try {
                    refreshTokenRepository.deleteById(value.getRefreshID());
                    System.out.println("✅ Successfully deleted orphan token from DB");
                } catch (Exception e) {
                    System.err.println("❌ Failed to delete token: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("ℹ️ Token not deleted (cause: " + cause + ")");
            }
        }
    }
}