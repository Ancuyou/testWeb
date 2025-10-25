package it.ute.QAUTE.entity;

import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

     // Loại nội dung bị báo cáo (VD: question, answer, comment, user profile)
    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long contentId;

    @Column(name = "reason", nullable = false, length = 500)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private Account reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

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
        PENDING,   // Đang chờ xử lý
        REVIEWED,  // Đã xem
        APPROVED,  // Báo cáo hợp lệ
        REJECTED   // Báo cáo sai
    }
}
