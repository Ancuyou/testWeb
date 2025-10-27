package it.ute.QAUTE.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultantDTO {
    private Integer consultantID;
    private Integer accountID;
    private Integer profileID;
    private String fullName;
    private String avatar;
    private String specialization;
    private Integer experienceYears;
    private Boolean isOnline = false;
    private String onlineStatusString = "Offline";
    private Long totalAnswers = 0L;
    private Double avgResponseTime = 0.0; // Đổi thành Double để chính xác hơn
    private Long totalUsersAnswered = 0L;
}