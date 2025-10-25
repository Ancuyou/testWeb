package it.ute.QAUTE.configuration;

import it.ute.QAUTE.Exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.net.URLEncoder;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        String base = request.getContextPath();
        response.sendRedirect(base + "/app-error?errorCode=" + errorCode.getCode() +
                "&message=" + URLEncoder.encode(errorCode.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
