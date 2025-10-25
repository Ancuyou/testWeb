package it.ute.QAUTE.repository;

import it.ute.QAUTE.entity.RefreshToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,String> {
    @EntityGraph(attributePaths = "account")
    @Query("""
        SELECT rt FROM RefreshToken rt
        JOIN rt.account acc
        WHERE (
                :keyword IS NULL OR :keyword = '' OR
                LOWER(COALESCE(rt.deviceName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(acc.email)                    LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(acc.username)                 LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
        """)
    Page<RefreshToken> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<RefreshToken> findByExpiresAtAfter(Date date, Pageable pageable);   // còn hạn
    Page<RefreshToken> findByExpiresAtBefore(Date date, Pageable pageable);  // hết hạn

    @EntityGraph(attributePaths = "account")
    @Query("""
        SELECT rt FROM RefreshToken rt
        JOIN rt.account acc
        WHERE rt.expiresAt > :now
          AND (
                :keyword IS NULL OR :keyword = '' OR
                LOWER(COALESCE(rt.deviceName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(acc.email)                    LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(acc.username)                 LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
        """)
    Page<RefreshToken> searchActiveTokensByKeyword(
            @Param("keyword") String keyword,
            @Param("now") Date now,
            Pageable pageable);

    @EntityGraph(attributePaths = "account")
    @Query("""
        SELECT rt FROM RefreshToken rt
        JOIN rt.account acc
        WHERE rt.expiresAt < :now
          AND (
                :keyword IS NULL OR :keyword = '' OR
                LOWER(COALESCE(rt.deviceName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(acc.email)                    LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(acc.username)                 LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
        """)
    Page<RefreshToken> searchExpiredTokensByKeyword(
            @Param("keyword") String keyword,
            @Param("now") Date now,
            Pageable pageable);
    long countByExpiresAtAfter(Date date);
    long countByExpiresAtBefore(Date date);

    int deleteByExpiresAtBefore(Date date);
}
