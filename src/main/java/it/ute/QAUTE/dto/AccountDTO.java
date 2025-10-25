package it.ute.QAUTE.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {
    private Integer accountID;
    private Integer profileID;
    private Integer userID;
    private String fullName;
    private String email;
    private String avatar;
    private Date createdDate;
}
