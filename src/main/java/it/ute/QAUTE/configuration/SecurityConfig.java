package it.ute.QAUTE.configuration;

import it.ute.QAUTE.exception.AppException;
import it.ute.QAUTE.exception.ErrorCode;
import it.ute.QAUTE.dto.response.AuthenticationResponse;
import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.service.AuthenticationService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.text.ParseException;
import java.time.Duration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {
    private final String[] PUBLIC_ENDPOINT = {"/auth/**", "/oauth2/**","/ws/**", "/app/**", "/topic/**","/queue/**","/api/**", "/app-error/**", "/notifications/**", "/css/**", "/js/**", "/images/**" };

    @Autowired private CustomJwtDecoder customJwtDecoder;
    @Autowired private AuthenticationService authenticationService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        .requestMatchers(PUBLIC_ENDPOINT).permitAll()
                        // 1. ĐỊNH NGHĨA QUY TẮC CHUNG TRƯỚC: Cho phép cả User và Consultant
                        .requestMatchers("/user/questions/**").hasAnyAuthority("ROLE_User", "ROLE_Consultant")
                        .requestMatchers("/images/**").permitAll()
                        // 2. CÁC QUY TẮC CỤ THỂ CHO TỪNG ROLE:
                        .requestMatchers("/user/**").hasAuthority("ROLE_User") // Áp dụng cho các link /user/ còn lại
                        .requestMatchers("/consultant/**").hasAuthority("ROLE_Consultant")
                        .requestMatchers("/admin/**").hasAuthority("ROLE_Admin")
                        .requestMatchers("/manager/**").hasAuthority("ROLE_Manager")

                        // 3. QUY TẮC CUỐI CÙNG:
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/auth/login")
                        .redirectionEndpoint(r -> r.baseUri("/auth/google/callback"))
                        .userInfoEndpoint(u -> u.userService(oauth2UserService()))
                        .successHandler(oauth2SuccessHandler())
                        .failureHandler(oauth2FailureHandler())
                );

        httpSecurity
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(customJwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .bearerTokenResolver(bearerTokenResolver())
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                );
        httpSecurity
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                        .accessDeniedHandler(new CustomAccessDeniedHandler())
                );

        return httpSecurity.build();
    }
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        return request -> {
            String uri = request.getRequestURI();
            if (uri.startsWith(request.getContextPath() + "/auth")
                    || uri.startsWith(request.getContextPath() + "/oauth2")
                    || uri.startsWith(request.getContextPath() + "/app-error")
                    || uri.startsWith(request.getContextPath() + "/auth/google/callback")) {
                return null;
            }
            var session = request.getSession(false);
            if (session != null) {
                Object token = session.getAttribute("ACCESS_TOKEN");
                if (token instanceof String s && !s.isBlank()) {
                    return s;
                }
            }
            var cookies = request.getCookies();
            if (cookies != null) {
                for (var c : cookies) {
                    if ("REFRESH_TOKEN".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank())
                        return c.getValue();
                }
            }
            return null;
        };
    }
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("scope");
        gac.setAuthorityPrefix("");

        var jac = new JwtAuthenticationConverter();
        jac.setJwtGrantedAuthoritiesConverter(gac);
        return jac;
    }
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        var delegate = new DefaultOAuth2UserService();
        return delegate::loadUser;
    }

    @Bean
    public AuthenticationSuccessHandler oauth2SuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauthUser = oauthToken.getPrincipal();

            String email = (String) oauthUser.getAttributes().get("email");

            log.info("email: " + email);
            AuthenticationResponse auth = null;
            try {
                auth = authenticationService.authentication(Account.builder().email(email).build(), "DANGNGOCNHAN", true);
            } catch (ParseException e) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            if (auth != null) {
                HttpSession session = request.getSession(true);
                session.setAttribute("ACCESS_TOKEN", auth.getToken());
                String role = (String) customJwtDecoder.decode(auth.getToken()).getClaims().get("scope");
                session.setAttribute("SCOPE", role);
                ResponseCookie cookie = ResponseCookie.from("REFRESH_TOKEN", auth.getRefreshtoken())
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofDays(7))
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

                response.sendRedirect(request.getContextPath() + resolveRedirectByRole("ROLE_" + auth.getRole().toString()));
            }
            else {
                response.sendRedirect(request.getContextPath() + "/auth/login");  // fix lai tra ve loi tai khoan nay khong ton tai trong db
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler oauth2FailureHandler() {
        return (request, response, ex) -> {
            response.sendRedirect("/auth/login");  // su ly cai message error
        };
    }

    private String resolveRedirectByRole(String role) {
        switch (role) {
            case "ROLE_User":
                return "/user/home";
            case "ROLE_Consultant":
                return "/consultant/home";
            case  "ROLE_Admin":
                return "/admin/users";
            case "ROLE_Manager":
                return "/manager/questions";
            default:
                return "/auth/login";
        }
    }
}
