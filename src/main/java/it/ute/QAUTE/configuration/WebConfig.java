package it.ute.QAUTE.configuration;

import it.ute.QAUTE.web.AuthPageGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final AuthPageGuard authPageGuard;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authPageGuard)
                .addPathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/auth/forgotPassword"
                )
                .excludePathPatterns(
                        "/auth/google",
                        "/auth/google/callback",
                        "/auth/login/MFA/**",
                        "/auth/logout"
                );
    }
}
