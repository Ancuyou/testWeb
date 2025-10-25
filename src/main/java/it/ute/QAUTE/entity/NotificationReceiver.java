package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class NotificationReceiver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NotificationID", referencedColumnName = "NotificationID")
    private Notification notification;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReceiverID", referencedColumnName = "AccountID")
    private Account receiver;
    @Column(name = "IsRead", nullable = false)
    private boolean isRead = false;
}
