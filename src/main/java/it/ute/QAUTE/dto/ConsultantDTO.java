package it.ute.QAUTE.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultantDTO {
    private Integer consultantID;
    private Integer profileID;
    private String fullName;
    private String avatar;
    private String specialization;
    private Integer experienceYears;
    private Boolean isOnline = false;
}