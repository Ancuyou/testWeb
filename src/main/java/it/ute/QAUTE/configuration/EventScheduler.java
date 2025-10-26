package it.ute.QAUTE.configuration;

import it.ute.QAUTE.entity.Event;
import it.ute.QAUTE.entity.EventRegistration;
import it.ute.QAUTE.repository.EventRepository;
import it.ute.QAUTE.repository.EventRegistrationRepository;
import it.ute.QAUTE.service.EventService;
import it.ute.QAUTE.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class EventScheduler {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private EventRegistrationRepository registrationRepository;
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Cập nhật trạng thái sự kiện mỗi 5 phút
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void updateEventStatuses() {
        log.info("Running event status update task");
        eventService.updateEventStatuses();
    }
    
    /**
     * Gửi nhắc nhở 24 giờ trước sự kiện (chạy mỗi giờ)
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void send24HourReminders() {
        log.info("Checking for events to send 24-hour reminders");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = now.plusHours(24);
        
        // Tìm events bắt đầu trong khoảng 24-25 giờ tới
        List<Event> upcomingEvents = eventRepository.findEventsBetween(
            targetTime,
            targetTime.plusHours(1)
        );
        
        for (Event event : upcomingEvents) {
            if (event.getStatus() == Event.EventStatus.Approved) {
                List<EventRegistration> registrations = 
                    registrationRepository.findActiveRegistrations(event);
                
                for (EventRegistration registration : registrations) {
                    try {
                        notificationService.notifyUserEventReminder(
                            registration.getUser(),
                            event
                        );
                    } catch (Exception e) {
                        log.error("Failed to send reminder for event {}: {}", 
                                 event.getEventID(), e.getMessage());
                    }
                }
                
                log.info("Sent 24-hour reminders for event: {}", event.getTitle());
            }
        }
    }
    
    /**
     * Gửi thông báo 15 phút trước sự kiện (chạy mỗi 5 phút)
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void send15MinuteReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = now.plusMinutes(15);
        
        // Tìm events bắt đầu trong 15-20 phút tới
        List<Event> startingEvents = eventRepository.findEventsBetween(
            targetTime,
            targetTime.plusMinutes(5)
        );
        
        for (Event event : startingEvents) {
            if (event.getStatus() == Event.EventStatus.Approved) {
                try {
                    notificationService.notifyUsersEventStartingSoon(event);
                    log.info("Sent 15-minute reminders for event: {}", event.getTitle());
                } catch (Exception e) {
                    log.error("Failed to send starting soon notification for event {}: {}", 
                             event.getEventID(), e.getMessage());
                }
            }
        }
    }
    
    /**
     * Tự động chuyển status sang Ongoing khi sự kiện bắt đầu
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void markEventsAsOngoing() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Event> startingEvents = eventRepository.findEventsBetween(
            now.minusMinutes(5),
            now.plusMinutes(5)
        );
        
        for (Event event : startingEvents) {
            if (event.getStatus() == Event.EventStatus.Approved &&
                event.getStartTime().isBefore(now)) {
                event.setStatus(Event.EventStatus.Ongoing);
                eventRepository.save(event);
                log.info("Marked event {} as Ongoing", event.getEventID());
            }
        }
    }
    
    /**
     * Tự động chuyển status sang Completed khi sự kiện kết thúc (chạy mỗi 10 phút)
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void markEventsAsCompleted() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Event> endingEvents = eventRepository.findEventsBetween(
            now.minusHours(12),
            now
        );
        
        for (Event event : endingEvents) {
            if (event.getStatus() == Event.EventStatus.Ongoing &&
                event.getEndTime().isBefore(now)) {
                event.setStatus(Event.EventStatus.Completed);
                eventRepository.save(event);
                log.info("Marked event {} as Completed", event.getEventID());
            }
        }
    }
    
    /**
     * Cảnh báo về sự kiện sắp đầy (90% capacity)
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void notifyNearlyFullEvents() {
        List<Event> approvedEvents = eventRepository.findByStatus(
            Event.EventStatus.Approved,
            org.springframework.data.domain.PageRequest.of(0, 100)
        ).getContent();
        
        for (Event event : approvedEvents) {
            if (event.getMaxParticipants() != null) {
                double fillRate = (double) event.getCurrentParticipants() / event.getMaxParticipants();
                
                if (fillRate >= 0.9 && fillRate < 1.0) {
                    // Thông báo cho consultant
                    try {
                        notificationService.createNotification(
                            event.getConsultant().getProfile().getAccount(),
                            "Sự kiện sắp đầy",
                            String.format("Sự kiện '%s' đã có %d/%d người đăng ký (%.0f%%). " +
                                        "Hãy chuẩn bị kỹ lưỡng!",
                                        event.getTitle(),
                                        event.getCurrentParticipants(),
                                        event.getMaxParticipants(),
                                        fillRate * 100),
                            "Consultant",
                            "Active",
                            false
                        );
                    } catch (Exception e) {
                        log.error("Failed to send nearly full notification: {}", e.getMessage());
                    }
                }
            }
        }
    }
}