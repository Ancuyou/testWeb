package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Entity
@Getter
@Setter
@Table(name="notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID")
    private Long notificationID;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SenderID", referencedColumnName = "AccountID")
    private Account sender;
    @Enumerated(EnumType.STRING)
    @Column(name = "TargetType", nullable = false, length = 50)
    private NotificationTarget targetType;
    @Column(name = "Title", length = 200)
    private String title;
    @Column(name = "Content", columnDefinition = "TEXT")
    private String content;
    @Column(name = "CreatedAt", nullable = false)
    private Date createdDate;
    @Column(name = "Status", length = 20)
    private String status; //Nháp, xuất bản,hết hiệu lực
    private boolean is_priority;
    public enum NotificationTarget {
        ALL, Manager, Consultant, User
    }
}
