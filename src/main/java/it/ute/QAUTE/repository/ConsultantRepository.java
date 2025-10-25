package it.ute.QAUTE.repository;

import it.ute.QAUTE.dto.ConsultantReportDTO;
import it.ute.QAUTE.entity.Consultant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConsultantRepository extends JpaRepository<Consultant, Integer> {
    
    @Query("SELECT c FROM Consultant c JOIN FETCH c.profile p ORDER BY p.fullName")
    List<Consultant> findAllWithProfiles();
    
    Consultant findByProfile_ProfileID(Integer profileID);

    // Thống kê tổng hợp hiệu suất từng tư vấn viên trong khoảng thời gian
    @Query("""
    SELECT new it.ute.QAUTE.dto.ConsultantReportDTO(
        p.fullName,
        COUNT(DISTINCT a),
        COUNT(DISTINCT f),
        c.experienceYears,

        CASE WHEN COUNT(DISTINCT q) = 0 THEN 0.0
             ELSE (COUNT(DISTINCT a) * 1.0 / COUNT(DISTINCT q)) * 100 END,

        CASE WHEN COUNT(a) = 0 THEN 0.0
             ELSE (SUM(CASE WHEN a.dateAnswered <= FUNCTION('TIMESTAMPADD', DAY, 1, q.dateSend) THEN 1 ELSE 0 END) * 1.0 / COUNT(a)) * 100 END
    )
    FROM Consultant c
    JOIN c.profile p
    
    LEFT JOIN Answer a ON a.consultant.consultantID = c.consultantID AND a.dateAnswered BETWEEN :startDate AND :endDate
    
    LEFT JOIN a.question q
    LEFT JOIN q.field f
    
    GROUP BY c.consultantID, p.fullName, c.experienceYears
    ORDER BY COUNT(a) DESC
    """)
    List<ConsultantReportDTO> getConsultantPerformance(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(c) FROM Consultant c")
    long countAllConsultants();
}