package it.ute.QAUTE.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import it.ute.QAUTE.Exception.AppException;
import it.ute.QAUTE.Exception.ErrorCode;
import it.ute.QAUTE.dto.response.AuthenticationResponse;
import it.ute.QAUTE.dto.response.RefreshTokenResponse;
import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.InvalidatedToken;
import it.ute.QAUTE.entity.RefreshToken;
import it.ute.QAUTE.repository.AccountRepository;
import it.ute.QAUTE.repository.InvalidatedTokenRepository;
import it.ute.QAUTE.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpSession;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@Slf4j
public class AuthenticationService {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    InvalidatedTokenRepository invalidatedTokenRepository;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private SecurityService securityService;
    @NonFinal
    @Value("${jwt.signerKey_access}")
    protected String SIGNER_KEY;
    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;
    @NonFinal
    @Value("${jwt.refresh-duration}")
    protected long REFRESH_DURATION;
    @NonFinal
    @Value("${openweather.apikey}")
    protected String APIKEY;

    public boolean check(String text,String hasedText){
        return passwordEncoder.matches(text,hasedText);
    }

    public String hashed(String text){
        return passwordEncoder.encode(text);
    }

    public AuthenticationResponse authentication(Account account, String name_device, boolean isGoogle) throws ParseException {
        Account accountRep;
        boolean authenticated;
        if(!isGoogle){
            accountRep = accountRepository.findByUsername(account.getUsername());
            if (accountRep == null) {
                return AuthenticationResponse.builder()
                        .authenticated(false)
                        .isBlock(false)
                        .message("Tên đăng nhập hoặc mật khẩu không đúng")
                        .build();
            }
            String message=securityService.isAccountLocked(accountRep);
            if (!message.isBlank()) {
                return AuthenticationResponse.builder()
                        .authenticated(false)
                        .isBlock(true)
                        .message(message)
                        .build();
            }
            else {
                authenticated = check(account.getPassword(),
                        accountRep.getPassword());
                if (!authenticated) securityService.handleFailedLogin(account.getUsername());
            }
        } else{
            accountRep = accountRepository.findByEmail(account.getEmail());
            if (accountRep == null) {
                return AuthenticationResponse.builder()
                        .authenticated(false)
                        .isBlock(false)
                        .message("Tên đăng nhập hoặc mật khẩu không đúng")
                        .build();
            }
            String message=securityService.isAccountLocked(accountRep);
            if (!message.isBlank()) {
                return AuthenticationResponse.builder()
                        .authenticated(false)
                        .isBlock(true)
                        .message(message)
                        .build();
            }else {
                authenticated = true;
            }
        }
        if (authenticated){
            RefreshTokenResponse refreshToken = refreshToken(accountRep, name_device);
            log.info("Tao Refresh thanh cong voi token: " + refreshToken.getRefreshtoken());
            securityService.reduceLevelSecurity(accountRep);
            return AuthenticationResponse.builder()
                    .authenticated(true)
                    .token(generateToken(accountRep, null, false))
                    .RefreshID(refreshToken.getRefreshID())
                    .Refreshtoken(refreshToken.getRefreshtoken())
                    .role(accountRep.getRole())
                    .build();
        }
        return AuthenticationResponse.builder()
                .authenticated(false)
                .message("Tên đăng nhập hoặc mật khẩu không đúng")
                .build();
    }

    private String builtScope(Account account){
        StringJoiner stringJoiner = new StringJoiner(" ");
        stringJoiner.add("ROLE_" + account.getRole().name());
        return stringJoiner.toString();
    }

    public String generateToken(Account account, String signKey, boolean isRefresh){
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        long duration = isRefresh ? REFRESH_DURATION : VALID_DURATION;

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(account.getUsername())
                .issuer("qaute.com")
                .issueTime(new Date())                .expirationTime(new Date(
                        Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", builtScope(account))
                .claim("type", isRefresh ? "refresh" : "access")
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try{
            if (signKey == null){
                jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
                log.info("Create token: SIGNER_KEY");
            } else {
                jwsObject.sign(new MACSigner(signKey.getBytes()));
                log.info("Create token: SIGNER_KEY_WEATHER + " + jwsObject.serialize());
            }
            return jwsObject.serialize();
        } catch (JOSEException e){
            log.error("Cannot create token", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    public SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        String jti = signedJWT.getJWTClaimsSet().getJWTID();

        if (invalidatedTokenRepository.existsById(jti)) {
            throw new AppException(ErrorCode.TOKEN_REVOKED);
        }

        String signKey = SIGNER_KEY;

        Object typeObj = signedJWT.getJWTClaimsSet().getClaim("type");

        if ("refresh".equals(typeObj)) {
            RefreshToken refreshToken = refreshTokenRepository
                    .findById(jti)
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

            signKey = refreshToken.getSignKey();
            log.info("Using refresh token sign key for jti: {}", jti);
        }

        JWSVerifier verifier = new MACVerifier(signKey.getBytes());
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        boolean verified = signedJWT.verify(verifier);

        if (!verified) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (expiryTime == null || expiryTime.before(new Date())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (invalidatedTokenRepository.existsById(jti)) {
            throw new AppException(ErrorCode.TOKEN_REVOKED);
        }

        return signedJWT;
    }

    public void logout(String token, String tokenRefresh) throws ParseException, JOSEException{
        try {
            if (tokenRefresh == null){
                var signToken = verifyToken(token);
                String jit = signToken.getJWTClaimsSet().getJWTID();
                Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();
                InvalidatedToken invalidatedToken =
                        InvalidatedToken.builder().invalidatedTokenId(jit).expiryTime(expiryTime).build();
                invalidatedTokenRepository.save(invalidatedToken);
            } else {
                var signTokenRefresh = verifyToken(tokenRefresh);
                String jitRefresh = signTokenRefresh.getJWTClaimsSet().getJWTID();
                Date expiryTimeRefresh = signTokenRefresh.getJWTClaimsSet().getExpirationTime();
                InvalidatedToken invalidatedTokenRefresh =
                        InvalidatedToken.builder().invalidatedTokenId(jitRefresh).expiryTime(expiryTimeRefresh).build();
                refreshTokenRepository.deleteById(jitRefresh);
                invalidatedTokenRepository.save(invalidatedTokenRefresh);
            }
        } catch (AppException exception) {
            log.info("Token already expired");
        }
    }

    public String forgetPassword(String email) {
        if (accountRepository.existsByEmail(email)) {
            String otp= emailService.sendForgetPasswordEmail(email);
            System.out.println(otp);
            return hashed(otp);
        }else {
            return null;
        }
    }

    public String register(String username,String email){
        if (accountRepository.existsByUsername(username) || accountRepository.existsByEmail(email)){
            return null;
        }else {
            String otp= emailService.sendRegisterEmail(email);
            System.out.println(otp);
            return hashed(otp);
        }
    }
    public String changePassword(String email){
        if (accountRepository.existsByEmail(email)) {
            String otp= emailService.sendChangePassword(email);
            System.out.println(otp);
            return hashed(otp);
        }else {
            return null;
        }
    }
    // Func call in func Authenticated after check user, pass
    public RefreshTokenResponse refreshToken(Account account, String deviceName) throws ParseException {
        String signKey = generateSignMaxSecurity();

        String token = generateToken(account, signKey, true);
        log.info("Step create token refresh"+ token);

        Instant now = Instant.now();

        Instant expires = now.plus(REFRESH_DURATION, ChronoUnit.SECONDS);  // In seconds

        SignedJWT signedJWT = SignedJWT.parse(token);
        String jit = signedJWT.getJWTClaimsSet().getJWTID();

        RefreshToken refreshToken = RefreshToken.builder()
                .refreshId(jit)
                .signKey(signKey)
                .deviceName(deviceName)
                .createdAt(Date.from(now))
                .expiresAt(Date.from(expires))
                .account(account)
                .build();

        refreshTokenRepository.save(refreshToken);

        return RefreshTokenResponse.builder()
                .RefreshID(jit)
                .Refreshtoken(token)
                .build();
    }

    public String generateSignMaxSecurity() {
        try {
            String city = "Ho Chi Minh City";
            String cityParam = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = "https://api.openweathermap.org/data/2.5/weather?q="
                    + cityParam + "&appid=" + APIKEY + "&units=metric";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            String desc = root.path("weather").get(0).path("description").asText();
            double temp = root.path("main").path("temp").asDouble();

            String noise = desc + ":" + temp + ":" + Instant.now().toEpochMilli();

            //Băm SHA-512 → ra 128 ký tự
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(noise.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return fallbackSecureRandom128();
        }
    }
    private String fallbackSecureRandom128() {
        byte[] buf = new byte[64];
        new SecureRandom().nextBytes(buf);
        return toHex(buf);
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
    public int getCurrentUserId(HttpSession session) throws ParseException, JOSEException {
        Object tokenObj = session.getAttribute("ACCESS_TOKEN");
        if (tokenObj instanceof String token && !token.isBlank()) {
            SignedJWT signedJWT = verifyToken(token);
            String username = signedJWT.getJWTClaimsSet().getSubject();
            Account acc = accountRepository.findByUsername(username);
            if (acc != null) return acc.getAccountID();
        }
        return 0;
    }
}

