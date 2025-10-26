package it.ute.QAUTE.service;

import it.ute.QAUTE.exception.AppException;
import it.ute.QAUTE.exception.ErrorCode;
import it.ute.QAUTE.entity.*;
import it.ute.QAUTE.repository.EventRepository;
import it.ute.QAUTE.repository.EventRegistrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class EventService {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private EventRegistrationRepository registrationRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private NotificationService notificationService;
    
    private static final List<Event.EventStatus> APPROVED_STATUSES = List.of(
                Event.EventStatus.Approved,
                Event.EventStatus.Ongoing,
                Event.EventStatus.Completed
        );


    public Page<Event> findEventsByConsultantAndFilters(Consultant consultant, Event.EventType type, Event.EventStatus status, Pageable pageable) {
        if (type != null && status != null) {
            return eventRepository.findByConsultantAndTypeAndStatus(consultant, type, status, pageable);
        } else if (type != null) {
            return eventRepository.findByConsultantAndType(consultant, type, pageable);
        } else if (status != null) {
            return eventRepository.findByConsultantAndStatus(consultant, status, pageable);
        } else {
            return eventRepository.findByConsultant(consultant, pageable);
        }
    }

    public long countConsultantEventsByStatus(Consultant consultant, Event.EventStatus status) {
        return eventRepository.countByConsultantAndStatus(consultant, status);
    }

    public long countConsultantEventsByStatusIn(Consultant consultant, List<Event.EventStatus> statuses) {
        return eventRepository.countByConsultantAndStatusIn(consultant, statuses);
    }

    @Transactional
    public Event createEvent(Event event, MultipartFile bannerFile) {
      
        validateEventTime(event);
        

        if (bannerFile != null && !bannerFile.isEmpty()) {
            String bannerUrl = fileStorageService.storeFile(
                bannerFile, 
                null, 
                event.getConsultant().getConsultantID()
            );
            event.setBanner(bannerUrl);
        }
        
        event.setStatus(Event.EventStatus.Pending);
        event.setCreatedAt(LocalDateTime.now());
        event.setCurrentParticipants(0);
        
        Event savedEvent = eventRepository.save(event);
        log.info("Created new event: {} by consultant: {}", 
                 savedEvent.getTitle(), 
                 savedEvent.getConsultant().getConsultantID());
        

        notificationService.notifyManagersNewEvent(savedEvent);
        
        return savedEvent;
    }
    
    @Transactional
    public Event updateEvent(Integer eventId, Event updatedEvent, MultipartFile bannerFile) {
        Event existingEvent = findById(eventId);
        

        if (existingEvent.getStatus() == Event.EventStatus.Completed ||
            existingEvent.getStatus() == Event.EventStatus.Cancelled) {
            throw new AppException(ErrorCode.INVALID_EVENT_TIME);
        }
        
   
        validateEventTime(updatedEvent);
        
 
        existingEvent.setTitle(updatedEvent.getTitle());
        existingEvent.setDescription(updatedEvent.getDescription());
        existingEvent.setType(updatedEvent.getType());
        existingEvent.setMode(updatedEvent.getMode());
        existingEvent.setLocation(updatedEvent.getLocation());
        existingEvent.setMeetingLink(updatedEvent.getMeetingLink());
        existingEvent.setStartTime(updatedEvent.getStartTime());
        existingEvent.setEndTime(updatedEvent.getEndTime());
        existingEvent.setMaxParticipants(updatedEvent.getMaxParticipants());
        existingEvent.setDepartment(updatedEvent.getDepartment());
        existingEvent.setField(updatedEvent.getField());
        

        if (bannerFile != null && !bannerFile.isEmpty()) {
            String oldBanner = existingEvent.getBanner();
            String newBannerUrl = fileStorageService.storeFile(bannerFile, oldBanner, 
                                  existingEvent.getConsultant().getConsultantID());
            existingEvent.setBanner(newBannerUrl);
        }
        
        existingEvent.setUpdatedAt(LocalDateTime.now());
        
       
        if (existingEvent.getStatus() == Event.EventStatus.Approved) {
            existingEvent.setStatus(Event.EventStatus.Pending);
            notificationService.notifyManagersEventUpdated(existingEvent);
        }
        
        return eventRepository.save(existingEvent);
    }
    
    @Transactional
    public void deleteEvent(Integer eventId) {
        Event event = findById(eventId);
        
       
        long registrationCount = registrationRepository.countByEventAndStatus(
            event, EventRegistration.RegistrationStatus.Registered
        );
        
        if (registrationCount > 0) {
            throw new AppException(ErrorCode.INVALID_REGISTRATION_CANCELLATION);
        }
        
        // Xóa banner nếu có
        if (event.getBanner() != null) {
            fileStorageService.deleteFile(event.getBanner());
        }
        
        eventRepository.delete(event);
        log.info("Deleted event: {}", eventId);
    }
    
    @Transactional
    public Event approveEvent(Integer eventId, Integer managerId) {
        Event event = findById(eventId);
        
        if (event.getStatus() != Event.EventStatus.Pending) {
            throw new AppException(ErrorCode.INVALID_EVENT_TIME);
        }
        
        event.setStatus(Event.EventStatus.Approved);
        event.setApprovedBy(managerId);
        event.setApprovedAt(LocalDateTime.now());
        event.setRejectionReason(null);
        
        Event approvedEvent = eventRepository.save(event);
        notificationService.notifyConsultantEventApproved(approvedEvent);
        log.info("Event {} approved by manager {}", eventId, managerId);
        return approvedEvent;
    }
    
    @Transactional
    public Event rejectEvent(Integer eventId, Integer managerId, String reason) {
        Event event = findById(eventId);
        
        if (event.getStatus() != Event.EventStatus.Pending) {
            throw new AppException(ErrorCode.INVALID_EVENT_TIME);
        }
        
        event.setStatus(Event.EventStatus.Rejected);
        event.setApprovedBy(managerId);
        event.setApprovedAt(LocalDateTime.now());
        event.setRejectionReason(reason);
        
        Event rejectedEvent = eventRepository.save(event);
        
       
        notificationService.notifyConsultantEventRejected(rejectedEvent, reason);
        
        log.info("Event {} rejected by manager {}", eventId, managerId);
        return rejectedEvent;
    }
    
    @Transactional
    public Event cancelEvent(Integer eventId, String reason) {
        Event event = findById(eventId);
        
        if (event.getStatus() == Event.EventStatus.Completed ||
            event.getStatus() == Event.EventStatus.Cancelled) {
            throw new AppException(ErrorCode.INVALID_EVENT_TIME);
        }
        
        event.setStatus(Event.EventStatus.Cancelled);
        event.setRejectionReason(reason);
        event.setUpdatedAt(LocalDateTime.now());
        
        Event cancelledEvent = eventRepository.save(event);
        
       
        List<EventRegistration> registrations = registrationRepository.findActiveRegistrations(event);
        for (EventRegistration registration : registrations) {
            registration.setStatus(EventRegistration.RegistrationStatus.Cancelled);
            registration.setCancelledAt(LocalDateTime.now());
            registration.setCancellationReason("Sự kiện đã bị hủy: " + reason);
            registrationRepository.save(registration);
            
            notificationService.notifyUserEventCancelled(registration.getUser(), event, reason);
        }
        
        log.info("Event {} cancelled", eventId);
        return cancelledEvent;
    }
    
    
    public Event findById(Integer eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
    }
    
    public Page<Event> findAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }
    
    public Page<Event> findEventsByConsultant(Consultant consultant, Pageable pageable) {
        return eventRepository.findByConsultant(consultant, pageable);
    }


    
    public Page<Event> findEventsByStatus(Event.EventStatus status, Pageable pageable) {
        return eventRepository.findByStatus(status, pageable);
    }
    
    public Page<Event> findUpcomingEvents(Pageable pageable) {
        return eventRepository.findUpcomingEvents(LocalDateTime.now(), pageable);
    }
    
    public Page<Event> searchEvents(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findUpcomingEvents(pageable);
        }
        return eventRepository.searchEvents(keyword, pageable);
    }
    
    public Page<Event> filterEvents(
        Event.EventType type,
        Event.EventMode mode,
        Event.EventStatus status,
        Integer departmentId,
        Integer consultantId,
        Pageable pageable
    ) {
        return eventRepository.filterEvents(type, mode, status, departmentId, consultantId, pageable);
    }
    

    
    @Transactional
    public EventRegistration registerForEvent(Integer eventId, User user, String note) {
        Event event = findById(eventId);
  
        if (!event.canRegister()) {
            if (event.isFull()) {
                throw new AppException(ErrorCode.EVENT_FULL);
            }
            if (event.getStatus() != Event.EventStatus.Approved) {
                throw new AppException(ErrorCode.EVENT_NOT_APPROVED);
            }
            if (event.getStartTime().isBefore(LocalDateTime.now())) {
                throw new AppException(ErrorCode.EVENT_ALREADY_STARTED);
            }
        }
        
        if (registrationRepository.existsByEventAndUser(event, user)) {
            throw new AppException(ErrorCode.USER_ALREADY_REGISTERED);
        }
        

        EventRegistration registration = EventRegistration.builder()
            .event(event)
            .user(user)
            .status(EventRegistration.RegistrationStatus.Registered)
            .note(note)
            .build();
        
        EventRegistration saved = registrationRepository.save(registration);
        
      
        event.setCurrentParticipants(event.getCurrentParticipants() + 1);
        eventRepository.save(event);
        

        notificationService.notifyConsultantNewRegistration(event, user);
        notificationService.notifyUserRegistrationSuccess(user, event);
        
        log.info("User {} registered for event {}", user.getUserID(), eventId);
        return saved;
    }
    
    @Transactional
    public void cancelRegistration(Integer registrationId, String reason) {
        EventRegistration registration = registrationRepository.findById(registrationId)
            .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));

        if (registration.getStatus() == EventRegistration.RegistrationStatus.Cancelled) {
            ErrorCode errorCode = ErrorCode.REGISTRATION_ALREADY_CANCELLED;
            throw new AppException(errorCode);
        }
        

        if (registration.getEvent().getStartTime().minusHours(24).isBefore(LocalDateTime.now())) {
            ErrorCode errorCode = ErrorCode.INVALID_REGISTRATION_CANCELLATION;
            throw new AppException(errorCode);
        }
        
        registration.setStatus(EventRegistration.RegistrationStatus.Cancelled);
        registration.setCancelledAt(LocalDateTime.now());
        registration.setCancellationReason(reason);
        registrationRepository.save(registration);
        

        Event event = registration.getEvent();
        event.setCurrentParticipants(Math.max(0, event.getCurrentParticipants() - 1));
        eventRepository.save(event);
        
        log.info("Registration {} cancelled", registrationId);
    }
    
    public Page<EventRegistration> findUserRegistrations(User user, Pageable pageable) {
        return registrationRepository.findByUser(user, pageable);
    }
    
    public List<EventRegistration> findEventParticipants(Integer eventId) {
        Event event = findById(eventId);
        return registrationRepository.findActiveRegistrations(event);
    }
    

    
    public long countPendingEvents() {
        return eventRepository.countByStatus(Event.EventStatus.Pending);
    }
    
    public long countApprovedEvents() {
        return eventRepository.countByStatus(Event.EventStatus.Approved);
    }
    
    public long countConsultantEvents(Consultant consultant) {
        return eventRepository.countByConsultant(consultant);
    }
    

    private void validateEventTime(Event event) {
        if (event.getEndTime().isBefore(event.getStartTime())) {
            ErrorCode errorCode = ErrorCode.INVALID_EVENT_TIME;
            throw new AppException(errorCode);
        }
        
        if (event.getStartTime().isBefore(LocalDateTime.now())) {
            ErrorCode errorCode = ErrorCode.INVALID_EVENT_TIME;
            throw new AppException(errorCode);
        }

        if (java.time.Duration.between(event.getStartTime(), event.getEndTime()).toMinutes() < 30) {
            ErrorCode errorCode = ErrorCode.INVALID_EVENT_DURATION;
            throw new AppException(errorCode);
        }
    }
    
    @Transactional
    public void updateEventStatuses() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Event> ongoingEvents = eventRepository.findEventsBetween(
            now.minusMinutes(5), now.plusMinutes(5)
        );
        for (Event event : ongoingEvents) {
            if (event.getStatus() == Event.EventStatus.Approved) {
                event.setStatus(Event.EventStatus.Ongoing);
                eventRepository.save(event);
            }
        }
        List<Event> completedEvents = eventRepository.findEventsBetween(
            now.minusDays(1), now
        );
        for (Event event : completedEvents) {
            if (event.getStatus() == Event.EventStatus.Ongoing && 
                event.getEndTime().isBefore(now)) {
                event.setStatus(Event.EventStatus.Completed);
                eventRepository.save(event);
            }
        }
    }
    public long countEventsInDateRange(LocalDateTime start, LocalDateTime end) {
        return eventRepository.countByCreatedAtBetween(start, end);
    }
    
    public long countPendingEventsInDateRange(LocalDateTime start, LocalDateTime end) {
        return eventRepository.countByStatusAndCreatedAtBetween(Event.EventStatus.Pending, start, end);
    }

    public long countApprovedEventsInDateRange(LocalDateTime start, LocalDateTime end) {
        return eventRepository.countByStatusInAndCreatedAtBetween(APPROVED_STATUSES, start, end);
    }

    @Transactional(readOnly = true)
    public Page<EventRegistration> findUserRegistrations(User user, EventRegistration.RegistrationStatus status, Pageable pageable) {
        if (status != null) {
            return registrationRepository.findByUserAndStatus(user, status, pageable);
        } else {
            return registrationRepository.findByUser(user, pageable);
        }
    }
    
    @Transactional
    public void submitFeedback(Integer registrationId, User user, Integer rating, String feedback) {
        EventRegistration registration = registrationRepository.findByRegistrationIDAndUser(registrationId, user)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));

        if (registration.getStatus() != EventRegistration.RegistrationStatus.Attended && 
            registration.getEvent().getStatus() != Event.EventStatus.Completed) {
            
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        if (registration.getRating() != null) {
             throw new AppException(ErrorCode.CONFLICTING_FEEDBACK);
        }

        registration.setRating(rating);
        registration.setFeedback(feedback);
        registrationRepository.save(registration);
        
        log.info("Feedback submitted for registration {} by user {}", registrationId, user.getUserID());
    }
    @Transactional
    public void cancelRegistration(Integer registrationId, User user, String reason) {
        
        EventRegistration registration = registrationRepository.findByRegistrationIDAndUser(registrationId, user)
            .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));

        if (registration.getStatus() == EventRegistration.RegistrationStatus.Cancelled) {
            ErrorCode errorCode = ErrorCode.REGISTRATION_ALREADY_CANCELLED;
            throw new AppException(errorCode);
        }
        
        if (registration.getEvent().getStartTime().isBefore(LocalDateTime.now())) {
            ErrorCode errorCode = ErrorCode.EVENT_ALREADY_STARTED;
            throw new AppException(ErrorCode.INVALID_REGISTRATION_CANCELLATION);
        }
        
        registration.setStatus(EventRegistration.RegistrationStatus.Cancelled);
        registration.setCancelledAt(LocalDateTime.now());
        registration.setCancellationReason(reason);
        registrationRepository.save(registration);
        
        Event event = registration.getEvent();
        event.setCurrentParticipants(Math.max(0, event.getCurrentParticipants() - 1));
        eventRepository.save(event);
        
        log.info("Registration {} cancelled by user {}", registrationId, user.getUserID());
    }
}