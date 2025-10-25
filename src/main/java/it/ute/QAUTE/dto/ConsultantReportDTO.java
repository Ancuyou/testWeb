package it.ute.QAUTE.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConsultantReportDTO {
    String fullName;
    long totalAnswers;
    long totalFields;
    int experienceYears;
    double performanceRate;
    double onTimeRate;
}
