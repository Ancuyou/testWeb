package it.ute.QAUTE.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EventID")
    private Integer eventID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ConsultantID", referencedColumnName = "ConsultantID")
    private Consultant consultant;

    @Column(name = "Title", nullable = false, length = 200)
    private String title;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "Type", nullable = false)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "Mode", nullable = false)
    private EventMode mode;

    @Column(name = "Location", length = 255)
    private String location;

    @Column(name = "MeetingLink", length = 500)
    private String meetingLink;

    @Column(name = "StartTime", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "EndTime", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "MaxParticipants")
    private Integer maxParticipants;

    @Column(name = "CurrentParticipants", columnDefinition = "INT DEFAULT 0")
    private Integer currentParticipants = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private EventStatus status = EventStatus.Pending;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "ApprovedBy")
    private Integer approvedBy;

    @Column(name = "ApprovedAt")
    private LocalDateTime approvedAt;

    @Column(name = "RejectionReason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "Banner", length = 500)
    private String banner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DepartmentID")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FieldID")
    private Field field;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventRegistration> registrations;

    public enum EventType {
        Workshop,       // Hội thảo
        Consultation,   // Tư vấn 1-1
        Seminar,        // Buổi học
        GroupSession    // Tư vấn nhóm
    }

    public enum EventMode {
        Online,
        Offline,
        Hybrid
    }

    public enum EventStatus {
        Pending,        // Chờ duyệt
        Approved,       // Đã duyệt
        Rejected,       // Từ chối
        Ongoing,        // Đang diễn ra
        Completed,      // Hoàn thành
        Cancelled       // Đã hủy
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currentParticipants == null) {
            currentParticipants = 0;
        }
        if (status == null) {
            status = EventStatus.Pending;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isFull() {
        return maxParticipants != null && currentParticipants >= maxParticipants;
    }

    public boolean canRegister() {
        return status == EventStatus.Approved && 
               !isFull() && 
               startTime.isAfter(LocalDateTime.now());
    }
}