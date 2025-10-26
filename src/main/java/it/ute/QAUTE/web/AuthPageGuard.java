package it.ute.QAUTE.web;

import it.ute.QAUTE.configuration.CustomJwtDecoder;
import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.service.AccountService;
import it.ute.QAUTE.service.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
@Component
@RequiredArgsConstructor
public class AuthPageGuard implements HandlerInterceptor {
    private final AuthenticationService authenticationService;
    private final AccountService accountService;
    private final CustomJwtDecoder customJwtDecoder;
    private String resolveRedirectByRole(String role) {
        switch (role) {
            case "ROLE_User":
                return "/user/home";
            case "ROLE_Consultant":
                return "/consultant/home";
            case "ROLE_Admin":
                return "/admin/users";
            case "ROLE_Manager":
                return "/manager/questions";
            default:
                return "/auth/login";
        }
    }
    private String tryResolveRole(HttpServletRequest request, HttpServletResponse response) {
        final String COOKIE_PATH = "/";
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object at = session.getAttribute("ACCESS_TOKEN");
            Object scope = session.getAttribute("SCOPE");
            if (at instanceof String access && !access.isBlank() && scope instanceof String role) {
                try {
                    authenticationService.verifyToken(access);
                    return role; // còn hạn -> dùng luôn
                } catch (Exception ignored) {
                    session.removeAttribute("ACCESS_TOKEN");
                    session.removeAttribute("SCOPE");
                    session.invalidate();
                }
            }
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("REFRESH_TOKEN".equals(c.getName())) {
                    String refresh = c.getValue();
                    if (refresh != null && !refresh.isBlank()) {
                        try {
                            var rjwt = authenticationService.verifyToken(refresh);
                            String username = rjwt.getJWTClaimsSet().getSubject();
                            Account acc = accountService.findUserByUsername(username);
                            if (acc != null) {
                                String newAccess = authenticationService.generateToken(acc, null, false);
                                HttpSession newSession = request.getSession(true);
                                newSession.setAttribute("ACCESS_TOKEN", newAccess);
                                String role = (String) customJwtDecoder.decode(newAccess).getClaims().get("scope");
                                newSession.setAttribute("SCOPE", role);
                                return role;
                            } else {
                                ResponseCookie delete = ResponseCookie.from("REFRESH_TOKEN","")
                                        .httpOnly(true).secure(false).sameSite("Lax")
                                        .path(COOKIE_PATH).maxAge(0).build();
                                response.addHeader(HttpHeaders.SET_COOKIE, delete.toString());
                            }
                        } catch (Exception ex) {
                            ResponseCookie delete = ResponseCookie.from("REFRESH_TOKEN","")
                                    .httpOnly(true).secure(false).sameSite("Lax")
                                    .path(COOKIE_PATH).maxAge(0).build();
                            response.addHeader(HttpHeaders.SET_COOKIE, delete.toString());
                        }
                    }
                    break;
                }
            }
        }
        return null;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String role = tryResolveRole(request, response);
        if (role != null) {
            response.sendRedirect(request.getContextPath() + resolveRedirectByRole(role));
            return false;
        }
        return true;
    }
}
