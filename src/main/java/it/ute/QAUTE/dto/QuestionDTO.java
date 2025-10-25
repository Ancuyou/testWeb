package it.ute.QAUTE.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class QuestionDTO {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    @NotNull(message = "Vui lòng chọn một khoa/ban")
    private Integer departmentId;

    private Integer fieldId;

    private MultipartFile file;
}
