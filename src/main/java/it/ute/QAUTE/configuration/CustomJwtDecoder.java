package it.ute.QAUTE.configuration;

import java.text.ParseException;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jwt.SignedJWT;
import it.ute.QAUTE.entity.RefreshToken;
import it.ute.QAUTE.repository.RefreshTokenRepository;
import it.ute.QAUTE.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;

@Slf4j
@Component
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.signerKey_access}")
    private String signerKey;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            String tokenType = signedJWT.getJWTClaimsSet().getStringClaim("type");

            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            Date now = new Date();

            String activeSignKey = signerKey;

            if ("refresh".equals(tokenType)) {
                RefreshToken refreshToken = refreshTokenRepository
                        .findById(jti)
                        .orElseThrow(() -> new JwtException("Refresh token not found"));
                activeSignKey = refreshToken.getSignKey();
            }

            if ("access".equals(tokenType) && expiryTime != null && expiryTime.before(now)) {
                log.warn("Access token expired at {}, skipping verification", expiryTime);
            }
            else {
                SignedJWT verified = authenticationService.verifyToken(token);

                if (verified == null) {
                    throw new JwtException("Token verification failed");
                }
            }

            SecretKeySpec secretKeySpec = new SecretKeySpec(activeSignKey.getBytes(), "HmacSHA512");
            NimbusJwtDecoder decoder = NimbusJwtDecoder
                    .withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();

            return decoder.decode(token);

        } catch (ParseException | JOSEException e) {
            log.error("Token decode failed: {}", e.getMessage());
            throw new JwtException("Invalid token format");
        }
    }
}

