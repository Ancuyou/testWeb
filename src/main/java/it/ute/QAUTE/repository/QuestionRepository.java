package it.ute.QAUTE.repository;

import it.ute.QAUTE.dto.HotTopicDTO;
import it.ute.QAUTE.dto.QuestionReportDTO;
import it.ute.QAUTE.entity.Question;
import it.ute.QAUTE.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer>, JpaSpecificationExecutor<Question> {
    // Đếm số câu hỏi của một user
    long countByUser(User user);
    // Lấy 3 câu hỏi gần nhất của một user
    List<Question> findTop3ByUserOrderByDateSendDesc(User user);
    // Lấy các câu hỏi mới nhất từ cộng đồng để hiển thị
    List<Question> findTop5ByOrderByDateSendDesc();
    @Query("""
        SELECT q
        FROM Question q
        JOIN q.user u
        JOIN u.profile p
        WHERE q.department.departmentID = :departmentId
          AND q.field.fieldID = :fieldId
          AND q.status = :status
    """)
    Page<Question> findQuestionsByDeptAndField(
            @Param("departmentId") Integer departmentId,
            @Param("fieldId") Integer fieldId,
            @Param("status") Question.QuestionStatus status,
            Pageable pageable);


    @Query("""
    SELECT q
    FROM Question q
    JOIN q.user u
    JOIN u.profile p
    WHERE p.fullName LIKE CONCAT('%', :username, '%')
    """)
    Page<Question> findQuestionsByUserName(@Param("username") String username,
                                           Pageable pageable);

    @Query("""
        SELECT q
        FROM Question q
        JOIN q.user u
        JOIN u.profile p
        WHERE q.status = :status
    """)
    Page<Question> findQuestionsByStatus(
            @Param("status") Question.QuestionStatus status,
            Pageable pageable);


    @Query("""
        SELECT q
        FROM Question q
        JOIN q.user u
        JOIN u.profile p
        WHERE q.department.departmentID = :departmentId
          AND q.status = :status
    """)
    Page<Question> findQuestionsByDeptAndStatus(
            @Param("departmentId") Integer departmentId,
            @Param("status") Question.QuestionStatus status,
            Pageable pageable);

    @Query("""
        SELECT q
        FROM Question q
        JOIN q.user u
        JOIN u.profile p
        WHERE q.department.departmentID = :departmentId
    """)
    Page<Question> findQuestionsByDept(
            @Param("departmentId") Integer departmentId,
            Pageable pageable);

    @Query("""
    SELECT q
    FROM Question q
    JOIN q.user u
    JOIN u.profile p
    """)
    Page<Question> findAllWithUser(Pageable pageable);

    @Query("""
        SELECT COUNT(q)
        FROM Question q
        WHERE q.dateSend BETWEEN :startDate AND :endDate
    """)
    long countAllQuestions(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT new it.ute.QAUTE.dto.QuestionReportDTO(
            q.field.fieldName, COUNT(q), null
        )
        FROM Question q
        WHERE q.dateSend BETWEEN :startDate AND :endDate
        GROUP BY q.field.fieldName
    """)
    List<QuestionReportDTO> getQuestionsByField(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT new it.ute.QAUTE.dto.QuestionReportDTO(
        q.department.departmentName, COUNT(q), null
    )
    FROM Question q
    WHERE q.dateSend BETWEEN :startDate AND :endDate
    GROUP BY q.department.departmentName
    """)
    List<QuestionReportDTO> getQuestionsByDepartment(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT new it.ute.QAUTE.dto.QuestionReportDTO(
        CAST(q.status AS string), COUNT(q), null
    )
    FROM Question q
    WHERE q.dateSend BETWEEN :startDate AND :endDate
    GROUP BY q.status
    """)
    List<QuestionReportDTO> getQuestionsByStatus(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT new it.ute.QAUTE.dto.QuestionReportDTO(
        null,
        COUNT(q),
        CAST(q.dateSend AS LocalDate)
    )
    FROM Question q
    WHERE q.dateSend BETWEEN :startDate AND :endDate
    GROUP BY CAST(q.dateSend AS LocalDate)
    ORDER BY CAST(q.dateSend AS LocalDate)
    """)
    List<QuestionReportDTO> getQuestionsByDate(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT q
    FROM Question q
    JOIN FETCH q.user u
    JOIN FETCH u.profile p
    WHERE q.isToxic = true
      AND (:startDate IS NULL OR q.dateSend >= :startDate)
      AND (:endDate IS NULL OR q.dateSend <= :endDate)
    ORDER BY q.dateSend DESC
    """)
    Page<Question> findToxicQuestionsByDateRange(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable
    );

    List<Question> findByUserOrderByDateSendDesc(User user);
    List<Question> findAllByOrderByDateSendDesc();
    @Query("SELECT new it.ute.QAUTE.dto.HotTopicDTO(d.departmentID, d.departmentName, 'department', COUNT(q.questionID)) " +
            "FROM Question q JOIN q.department d " +
            "GROUP BY d.departmentID, d.departmentName ORDER BY COUNT(q.questionID) DESC")
    List<HotTopicDTO> findTopDepartments(Pageable pageable);

    @Query("SELECT new it.ute.QAUTE.dto.HotTopicDTO(f.fieldID, f.fieldName, 'field', COUNT(q.questionID)) " +
            "FROM Question q JOIN q.field f " +
            "GROUP BY f.fieldID, f.fieldName ORDER BY COUNT(q.questionID) DESC")
    List<HotTopicDTO> findTopFields(Pageable pageable);
}