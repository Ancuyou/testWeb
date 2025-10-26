package it.ute.QAUTE.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import it.ute.QAUTE.entity.Account;
import lombok.*;
import lombok.experimental.FieldDefaults;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    String token;
    String Refreshtoken;
    String RefreshID;
    boolean authenticated;
    Account.Role role;
    boolean isBlock;
    String message;
    String email;
    boolean isSpecialAccount;
}
