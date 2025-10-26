package it.ute.QAUTE.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.ute.QAUTE.entity.Report;

import java.time.LocalDateTime;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    long countByStatus(Report.ReportStatus status);

    @Query("SELECT r FROM Report r WHERE " +
            "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR r.createdAt <= :endDate) AND " +
            "(:contentType IS NULL OR r.contentType = :contentType) AND " +
            "(:reason IS NULL OR r.reason = :reason) AND " +
            "(:status IS NULL OR r.status = :status)"
    )
    Page<Report> searchReports(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("contentType") String contentType,
            @Param("reason") Report.ReportReason reason,
            @Param("status") Report.ReportStatus status,
            Pageable pageable
    );

}
