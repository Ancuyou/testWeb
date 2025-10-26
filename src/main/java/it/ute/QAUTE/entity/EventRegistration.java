package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event_registrations")
public class EventRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RegistrationID")
    private Integer registrationID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EventID", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    @Column(name = "RegisteredAt", nullable = false)
    private LocalDateTime registeredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private RegistrationStatus status = RegistrationStatus.Registered;

    @Column(name = "Note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "AttendedAt")
    private LocalDateTime attendedAt;

    @Column(name = "CancelledAt")
    private LocalDateTime cancelledAt;

    @Column(name = "CancellationReason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "Rating")
    private Integer rating;

    @Column(name = "Feedback", columnDefinition = "TEXT")
    private String feedback;

    public enum RegistrationStatus {
        Registered,     // Đã đăng ký
        Confirmed,      // Đã xác nhận
        Attended,       // Đã tham dự
        Absent,         // Vắng mặt
        Cancelled       // Đã hủy
    }

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        if (status == null) {
            status = RegistrationStatus.Registered;
        }
    }
}