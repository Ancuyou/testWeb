package it.ute.QAUTE.Exception;


import lombok.Getter;

@Getter
public enum ErrorCode {
    // General and System Errors
    UNCATEGORIZED_EXCEPTION(500, "Internal Server Error"),
    INVALID_KEY(400, "Invalid Key"),

    // Account Management Errors
    USERNAME_EXISTED(410, "Bad Request: Username exists"),
    EMAIL_EXISTED(411, "Conflict: Email already exists"),
    DEPARTMENTNAME_EXISTED(412, "Conflict: Department name already exists"),
    ERROR_DELETED(413, "Error deleted"),
    QUESTION_UNEXISTED(414, "Question Not Found"),

    // Authentication & Authorization Errors
    UNAUTHENTICATED(401, "Unauthorized: Authentication required"),
    UNAUTHORIZED(403, "Forbidden: You do not have permission"),
    INVALID_TOKEN(401, "Unauthorized: Invalid Token"),
    TOKEN_EXPIRED(401, "Unauthorized: Token Expired"),
    TOKEN_REVOKED(401, "Unauthorized: Token Revoked"),
    SAVE_MESSAGE_ERROR(500, "Internal Server Error: Unable to save message")
    ;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private final int code;
    private final String message;
}
