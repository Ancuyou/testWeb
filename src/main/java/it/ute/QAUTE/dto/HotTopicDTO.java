package it.ute.QAUTE.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotTopicDTO {
    private Integer id;
    private String name;
    private String type; // "department" or "field"
    private Long count;
}
