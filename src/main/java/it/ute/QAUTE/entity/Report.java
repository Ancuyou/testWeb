package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 500)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "contentType")
    private String contentType;

    @Column(name = "contentId")
    private Long contentId;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private Account reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Transient
    private Question question;

    @Transient
    private Answer answer;

    @Transient
    private Messages message;

    public Report() {
        this.createdAt = LocalDateTime.now();
        this.status = ReportStatus.PENDING;
    }

    public enum ReportReason {
        SPAM_ADS,              // Spam/Quảng cáo
        HATEFUL_LANGUAGE,      // Ngôn từ thù hận, xúc phạm
        ADULT_CONTENT,         // Nội dung 18+
        MISINFORMATION,        // Thông tin sai lệch, nguy hiểm
        HARASSMENT,            // Quấy rối
        OTHER                  // Khác
    }

    public enum ReportStatus {
        PENDING,
        PROCESSED,
    }
}
