package it.ute.QAUTE.exception;


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
    SAVE_MESSAGE_ERROR(500, "Internal Server Error: Unable to save message"),
    INVALID_EVENT_TIME(420, "Invalid event time"),
    INVALID_EVENT_DURATION(421, "Invalid event duration"),
    REGISTRATION_NOT_FOUND(422, "Event registration not found"),
    REGISTRATION_ALREADY_CANCELLED(423, "Event registration already cancelled"),
    INVALID_REGISTRATION_CANCELLATION(424, "Invalid registration cancellation"),
    EVENT_FULL(430, "Event is full"),
    EVENT_NOT_APPROVED(431, "Event is not approved"),
    EVENT_ALREADY_STARTED(432, "Event has already started"),
    USER_ALREADY_REGISTERED(433, "User is already registered for this event"),
    USER_NOT_FOUND(440, "User not found"),
    INVALID_REQUEST(400, "Invalid request"),
    FIELD_NOT_FOUND(441, "Field not found"),
    DEPARTMENT_NOT_FOUND(442, "Department not found"),
    CONSULTANT_NOT_FOUND(443, "Consultant not found"),
    EVENT_NOT_FOUND(444, "Event not found") ,
    FEEDBACK_ALREADY_SUBMITTED(445, "Feedback has already been submitted for this registration"),
    CONFLICTING_FEEDBACK(446, "Conflicting feedback submission"),;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private final int code;
    private final String message;
}
