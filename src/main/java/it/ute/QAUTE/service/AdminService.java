package it.ute.QAUTE.service;

import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.RefreshToken;
import it.ute.QAUTE.repository.AccountRepository;
import it.ute.QAUTE.repository.RefreshTokenRepository;
import it.ute.QAUTE.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
public class AdminService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private RefreshTokenRepository  refreshTokenRepository;
    public Account findById(Integer id) {
        return accountRepository.findByAccountIDWithProfiles(id);
    }

    public Page<RefreshToken> searchTokens(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return refreshTokenRepository.findAll(pageable);
        }
        return refreshTokenRepository.searchByKeyword(keyword.trim(), pageable);
    }

    public Page<RefreshToken> findActiveTokens(String keyword, Pageable pageable) {
        Date now = new Date();
        if (keyword == null || keyword.trim().isEmpty()) {
            return refreshTokenRepository.findByExpiresAtAfter(now, pageable);
        }
        return refreshTokenRepository.searchActiveTokensByKeyword(keyword.trim(), now, pageable);
    }

    public Page<RefreshToken> findExpiredTokens(String keyword, Pageable pageable) {
        Date now = new Date();
        if (keyword == null || keyword.trim().isEmpty()) {
            return refreshTokenRepository.findByExpiresAtBefore(now, pageable);
        }
        return refreshTokenRepository.searchExpiredTokensByKeyword(keyword.trim(), now, pageable);
    }

    public long countActiveTokens() {
        return refreshTokenRepository.countByExpiresAtAfter(new Date());
    }

    public long countExpiredTokens() {
        return refreshTokenRepository.countByExpiresAtBefore(new Date());
    }

    public long countAllTokens() {
        return refreshTokenRepository.count();
    }


    public Optional<RefreshToken> findById(String id) {
        return refreshTokenRepository.findById(id);
    }

    @Transactional
    public void revokeToken(String id) {
        if (!refreshTokenRepository.existsById(id)) {
            throw new RuntimeException("Token không tồn tại");
        }
        refreshTokenRepository.deleteById(id);
    }

    @Transactional
    public int deleteExpiredTokens() {
        Date now = new Date();
        return refreshTokenRepository.deleteByExpiresAtBefore(now);
    }
}
