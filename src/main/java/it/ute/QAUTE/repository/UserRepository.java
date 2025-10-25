package it.ute.QAUTE.repository;

import it.ute.QAUTE.dto.UserReportDTO;
import it.ute.QAUTE.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByProfile_ProfileID(Integer profileId);

    @Query("""
        SELECT COUNT(u)
        FROM User u
        JOIN u.profile q
        JOIN q.account a
        WHERE a.createdDate BETWEEN :startDate AND :endDate
    """)
    Long countAllUsers(@Param("startDate") LocalDateTime startDate,
                       @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT new it.ute.QAUTE.dto.UserReportDTO(
            u.roleName, COUNT(u)
        )
        FROM User u
        JOIN u.profile q
        JOIN q.account a
        WHERE a.createdDate BETWEEN :startDate AND :endDate
        GROUP BY u.roleName
    """)
    List<UserReportDTO> getUsersByRole(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT COUNT(DISTINCT q.user)
        FROM Question q
        WHERE q.dateSend >= :cutoffDate
    """)
    Long countActiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("""
        SELECT new it.ute.QAUTE.dto.UserReportDTO(
            p.fullName, COUNT(q)
        )
        FROM Question q
        JOIN q.user u
        JOIN u.profile p
        GROUP BY p.fullName
        ORDER BY COUNT(q) DESC
    """)
    List<UserReportDTO> getTopUsersByQuestions(Pageable pageable);
}
