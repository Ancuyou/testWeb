package it.ute.QAUTE.repository;

import it.ute.QAUTE.dto.AnswerReportDTO;
import it.ute.QAUTE.entity.Answer;
import java.time.LocalDateTime;
import java.util.List;
import it.ute.QAUTE.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer> {

    @Query("SELECT a FROM Answer a WHERE a.consultant.consultantID = :consultantId")
    List<Answer> findByConsultant_ConsultantID(Integer consultantId);
    // Đếm tổng số câu trả lời cho các câu hỏi của một user
    long countByQuestionUser(User user);

    @Query("""
        SELECT a FROM Answer a
        WHERE a.consultant.consultantID = :consultantId
          AND (:cutoffDate IS NULL OR a.dateAnswered > :cutoffDate)
          AND (:keyword IS NULL OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR (a.question IS NOT NULL AND LOWER(a.question.title) LIKE LOWER(CONCAT('%', :keyword, '%'))))
        ORDER BY a.dateAnswered DESC
    """)
    Page<Answer> findAnswersHistoryByConsultant(
            @Param("consultantId") Integer consultantId,
            @Param("cutoffDate") LocalDateTime cutoffDate,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<Answer> findByQuestion_QuestionID(Integer questionId);


    @Query("""
        SELECT COUNT(a)
        FROM Answer a
        WHERE a.dateAnswered BETWEEN :startDate AND :endDate
    """)
    long countAllAnswers(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT new it.ute.QAUTE.dto.AnswerReportDTO(
            a.consultant.profile.fullName,
            COUNT(a),
            NULL
        )
        FROM Answer a
        WHERE a.dateAnswered BETWEEN :startDate AND :endDate
        GROUP BY a.consultant.profile.fullName
    """)
    List<AnswerReportDTO> getAnswersByConsultant(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT FUNCTION('DATE', a.dateAnswered) AS date, COUNT(a) AS count
    FROM Answer a
    WHERE a.dateAnswered BETWEEN :startDate AND :endDate
    GROUP BY FUNCTION('DATE', a.dateAnswered)
    ORDER BY FUNCTION('DATE', a.dateAnswered)
    """)
    List<Object[]> getAnswersByDateRaw(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT AVG(TIMESTAMPDIFF(HOUR, q.dateSend, a.dateAnswered))
    FROM Answer a
    JOIN a.question q
    WHERE a.dateAnswered BETWEEN :startDate AND :endDate
    """)
    Double averageResponseTime(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Đếm tổng số câu trả lời của một consultant trong khoảng thời gian
    @Query("""
    SELECT COUNT(a)
    FROM Answer a
    WHERE a.consultant.consultantID = :consultantId
      AND a.dateAnswered BETWEEN :startDate AND :endDate
    """)
    long countConsultantAllAnswers(
            @Param("consultantId") Integer consultantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    //Tính thời gian phản hồi trung bình của một consultant (giờ)
    @Query(value = """
    SELECT AVG(TIMESTAMPDIFF(HOUR, q.DateSend, a.DateAnswered))
    FROM Answer a
    JOIN Question q ON a.QuestionID = q.QuestionID
    WHERE a.ConsultantID = :consultantId
      AND a.DateAnswered BETWEEN :startDate AND :endDate
    """, nativeQuery = true)
    Double averageConsultantResponseTime(
            @Param("consultantId") Integer consultantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Đếm số lượng user duy nhất mà consultant đã trả lời
    @Query("""
    SELECT COUNT(DISTINCT a.question.user.userID)
    FROM Answer a
    WHERE a.consultant.consultantID = :consultantId
      AND a.dateAnswered BETWEEN :startDate AND :endDate
    """)
    long countDistinctUsersAnsweredByConsultant(
            @Param("consultantId") Integer consultantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


}
