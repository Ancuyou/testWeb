package it.ute.QAUTE.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private int userID;
    private Integer profileID;
    private String fullName;
    private String email;
    private String avatar;
    private Boolean isOnline = false;
}
