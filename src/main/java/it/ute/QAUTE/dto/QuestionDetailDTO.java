package it.ute.QAUTE.dto;

import lombok.Data;

@Data // lombok
 public class QuestionDetailDTO {
     private Integer questionID;
     private String title;
     private String content;
     private DepartmentDTO department;
     private FieldDTO field;
     private String fileAttachment;
     private boolean canEdit; // Thêm trường này
 }