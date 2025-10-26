package it.ute.QAUTE.configuration;

import it.ute.QAUTE.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException ex)
            throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        String base = request.getContextPath();
        response.sendRedirect(base + "/app-error?errorCode=" + errorCode.getCode() + "&errorTitle=" + ex.getMessage() +
                "&message=" + URLEncoder.encode(errorCode.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
