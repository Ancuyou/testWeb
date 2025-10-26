package it.ute.QAUTE.repository;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.ute.QAUTE.entity.Consultant;
import it.ute.QAUTE.entity.Event;

@Repository
public interface EventRepository  extends JpaRepository<Event, Integer> {

    Page<Event> findByConsultant(Consultant consultant, Pageable pageable);

    Page<Event> findByStatus(Event.EventStatus status, Pageable pageable);
    
    Page<Event> findByTypeAndStatus(Event.EventType type, Event.EventStatus status, Pageable pageable);
    
    Page<Event> findByStatusOrderByCreatedAtDesc(Event.EventStatus status, Pageable pageable);

    Page<Event> findByConsultantAndStatus(Consultant consultant, Event.EventStatus status, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'Approved' AND e.startTime > :now ORDER BY e.startTime ASC")
    Page<Event> findUpcomingEvents(@Param("now") LocalDateTime now, Pageable pageable);
    
    @Query("SELECT e FROM Event e WHERE " +
           "(LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "e.status = 'Approved'")
    Page<Event> searchEvents(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT e FROM Event e WHERE " +
           "(:type IS NULL OR e.type = :type) AND " +
           "(:mode IS NULL OR e.mode = :mode) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:departmentId IS NULL OR e.department.departmentID = :departmentId) AND " +
           "(:consultantId IS NULL OR e.consultant.consultantID = :consultantId)")
    Page<Event> filterEvents(
        @Param("type") Event.EventType type,
        @Param("mode") Event.EventMode mode,
        @Param("status") Event.EventStatus status,
        @Param("departmentId") Integer departmentId,
        @Param("consultantId") Integer consultantId,
        Pageable pageable
    );
    
    long countByStatus(Event.EventStatus status);
    
    long countByConsultant(Consultant consultant);

    @Query("SELECT e FROM Event e WHERE e.startTime BETWEEN :start AND :end")
    List<Event> findEventsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusAndCreatedAtBetween(Event.EventStatus status, LocalDateTime start, LocalDateTime end);

    long countByStatusInAndCreatedAtBetween(List<Event.EventStatus> statuses, LocalDateTime start, LocalDateTime end);

    Page<Event> findByConsultantAndType(Consultant consultant, Event.EventType type, Pageable pageable);
    Page<Event> findByConsultantAndTypeAndStatus(Consultant consultant, Event.EventType type, Event.EventStatus status, Pageable pageable);

    long countByConsultantAndStatus(Consultant consultant, Event.EventStatus status);
    long countByConsultantAndStatusIn(Consultant consultant, List<Event.EventStatus> statuses);
} 
