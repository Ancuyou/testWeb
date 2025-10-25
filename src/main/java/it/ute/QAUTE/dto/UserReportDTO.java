package it.ute.QAUTE.dto;

import it.ute.QAUTE.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserReportDTO {
    String name;
    Long count;

    public UserReportDTO(User.Role roleName, Long userCount) {
        this.name = roleName.name(); // Chuyển đổi Enum -> String ở đây
        this.count = userCount;
    }}
