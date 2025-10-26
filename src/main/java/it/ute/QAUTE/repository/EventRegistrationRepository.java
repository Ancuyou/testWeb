package it.ute.QAUTE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.ute.QAUTE.entity.Event;
import it.ute.QAUTE.entity.EventRegistration;
import it.ute.QAUTE.entity.User;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Integer> {
    
   Optional<EventRegistration> findByEventAndUser(Event event, User user);
    
    boolean existsByEventAndUser(Event event, User user);

    boolean existsByEventAndUserAndStatusNot(Event event, User user, EventRegistration.RegistrationStatus status);

    Page<EventRegistration> findByUser(User user, Pageable pageable);
    
    List<EventRegistration> findByEvent(Event event);
    
    Page<EventRegistration> findByUserAndStatus(User user, EventRegistration.RegistrationStatus status, Pageable pageable);
    
    long countByEventAndStatus(Event event, EventRegistration.RegistrationStatus status);
    
    @Query("SELECT r FROM EventRegistration r WHERE r.event = :event AND r.status != 'Cancelled'")
    List<EventRegistration> findActiveRegistrations(@Param("event") Event event);
    
    Optional<EventRegistration> findByRegistrationIDAndUser(Integer registrationId, User user);
}
