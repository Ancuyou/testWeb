package it.ute.QAUTE.dto;

import java.time.LocalDateTime;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerQuestionDTO {
    private Integer answerID;
    private Integer questionID;
    private String title;
    private String contentAnswer;
    private String contentQuestion;
    private LocalDateTime createdAt;
    private LocalDateTime answerAt;
    private Integer consultantID;
    private Integer userID;
    private String consultantName;
    private String userName;
}
