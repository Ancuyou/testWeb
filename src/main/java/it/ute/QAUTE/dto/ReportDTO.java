package it.ute.QAUTE.dto;

import it.ute.QAUTE.entity.Report;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    private Long id;
    private String reason;
    private String reasonDisplay;
    private String description;
    private String reporterName;
    private String reporterEmail;
    private String contentType;
    private String status;
    private String statusDisplay;
    private LocalDateTime createdAt;

    // Thông tin nội dung bị báo cáo
    private Long contentId;
    private String contentTitle;
    private String contentPreview;
    private String contentAuthorName;
    private LocalDateTime contentCreatedAt;

}