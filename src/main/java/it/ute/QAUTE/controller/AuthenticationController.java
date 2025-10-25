package it.ute.QAUTE.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import it.ute.QAUTE.configuration.CustomJwtDecoder;
import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.Profiles;
import it.ute.QAUTE.service.AccountService;
import it.ute.QAUTE.service.AuthenticationService;
import it.ute.QAUTE.service.EmailService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Controller
@NoArgsConstructor
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private AccountService accountService;
    //Post
    @GetMapping("/auth/login")
    public String loginForm(@ModelAttribute("account") Account account,
                            Model model,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        final String COOKIE_PATH = "/";

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object at = session.getAttribute("ACCESS_TOKEN");
            if (at instanceof String access && !access.isBlank()) {
                try {
                    var jwt = authenticationService.verifyToken(access); // access
                    String role = (String) customJwtDecoder.decode(access).getClaims().get("scope");
                    if (jwt != null && role != null) {
                        return "redirect:" + resolveRedirectByRole(role);
                    }
                } catch (Exception ex) {
                    session.removeAttribute("ACCESS_TOKEN");
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
                            var rjwt = authenticationService.verifyToken(refresh); // refresh
                            String username = rjwt.getJWTClaimsSet().getSubject();
                            Account acc = accountService.findUserByUsername(username);
                            if (acc != null) {
                                String newAccess = authenticationService.generateToken(acc, null, false);
                                request.getSession(true).setAttribute("ACCESS_TOKEN", newAccess);

                                String role = (String) customJwtDecoder.decode(newAccess).getClaims().get("scope");
                                if (role != null) {
                                    return "redirect:" + resolveRedirectByRole(role);
                                }
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

        if (account == null) account = new Account();
        if (!model.containsAttribute("account")) model.addAttribute("account", account);
        return "pages/login";
    }

    @GetMapping("/auth/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        var session = request.getSession(false);
        if (session != null) {
            Object tk = session.getAttribute("ACCESS_TOKEN");
            if (tk instanceof String token && !token.isBlank()) {
                try {
                    authenticationService.logout(token, null);
                } catch (Exception ignore) {
                }
            }
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        final String COOKIE_PATH = "/";
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("REFRESH_TOKEN".equals(c.getName())) {
                    String token = c.getValue();
                    try {
                        if (token != null && !token.isBlank()) authenticationService.logout(null, token);
                    } catch (Exception ignored) {}
                }
            }
        }
        ResponseCookie delete = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true).secure(false).sameSite("Lax").path(COOKIE_PATH).maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, delete.toString());
        return "redirect:/auth/login";
    }
    @GetMapping("/auth/login/MFA")
    public String mfa(Model model){
        model.addAttribute("emailForm", true);
        return "pages/mfa";
    }
    @GetMapping("/auth/forgotPassword")
    public String forgotPasswordForm(Model model,
                                     @RequestParam(value = "email", required = false) String email,
                                     HttpSession session) {
        if (session.getAttribute("otp") != null) {
            model.addAttribute("showOtpForm", true);
            model.addAttribute("email", email);
        } else if (!model.containsAttribute("showOtpForm")) {
            model.addAttribute("showEmailForm", true);
        }
        return "pages/forgotPassword";
    }
    @GetMapping("/auth/register")
    public String registerForm() {
        return "pages/register";
    }
    // Post
    @Autowired
    private CustomJwtDecoder customJwtDecoder;
    @PostMapping("/auth/login")
    public String authLogin(@ModelAttribute("account") Account account,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            RedirectAttributes redirectAttributes) {
        try {
            var auth = authenticationService.authentication(account, InetAddress.getLocalHost().getHostName(), false); // device name demo

            System.out.println("Token: " + auth.getToken());

            if (auth.isAuthenticated()) {

                HttpSession session = request.getSession(true);
                session.setAttribute("ACCESS_TOKEN", auth.getToken());

                ResponseCookie cookie = ResponseCookie.from("REFRESH_TOKEN", auth.getRefreshtoken())
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofDays(7))
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

                return "redirect:" + resolveRedirectByRole("ROLE_" + auth.getRole());
            }
            redirectAttributes.addFlashAttribute("error", auth.getMessage());
            redirectAttributes.addFlashAttribute("account", account);
            return "redirect:/auth/login";
        } catch (Exception e) {
            log.warn("Login error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi trong quá trình đăng nhập");
            redirectAttributes.addFlashAttribute("account", account);
            return "redirect:/auth/login";
        }
    }

    @PostMapping("/auth/forgotPassword")
    public String forgotPassword(@RequestParam("email") String email,Model model,HttpSession session){
        System.out.println(email);
        if(email!=null && email.endsWith("@student.hcmute.edu.vn") ){
            String otp=authenticationService.forgetPassword(email);
            if (otp!=null) {
                session.setAttribute("otp", otp);
                session.setAttribute("otpExpiry", System.currentTimeMillis() + (3 * 60 * 1000));
                model.addAttribute("email", email);
                model.addAttribute("showOtpForm", true);
            }else {
                model.addAttribute("error", "Email không khớp với tài khoản nào vui lòng nhập lại");
                model.addAttribute("showEmailForm", true);
            }
        }else {
            model.addAttribute("error", "Email không hợp lệ vui lòng nhập lại");
            model.addAttribute("showEmailForm", true);
        }
        return "pages/forgotPassword";
    }
    @PostMapping("/auth/resendOtp")
    public String resendOTP(@RequestParam("email") String email, Model model, HttpSession session) {
        Long lastResendTime = (Long) session.getAttribute("lastResendTime");
        long currentTime = System.currentTimeMillis();
        if (lastResendTime != null) {
            long timeDiff = (currentTime - lastResendTime) / 1000;
            if (timeDiff < 30) {
                model.addAttribute("error", "Vui lòng đợi " + (30 - timeDiff) + " giây trước khi gửi lại OTP");
                model.addAttribute("email", email);
                model.addAttribute("showOtpForm", true);
                return "redirect:pages/forgotPassword";
            }
        }
        String otp = authenticationService.forgetPassword(email);
        session.setAttribute("otp", otp);
        session.setAttribute("otpExpiry", currentTime + (3 * 60 * 1000));
        session.setAttribute("lastResendTime", currentTime);
        model.addAttribute("email", email);
        model.addAttribute("showOtpForm", true);
        return "pages/forgotPassword";
    }

    @PostMapping("/auth/verifyOtp")
    public String verifyOTP(@RequestParam Map<String, String> params, Model model, HttpSession session){
        String inputOTP = params.get("otp1") + params.get("otp2") + params.get("otp3") + params.get("otp4") + params.get("otp5") + params.get("otp6");
        Integer failCount=(Integer) session.getAttribute("failCount");
        Object sessionOtp= session.getAttribute("otp");
        if (sessionOtp==null) return "redirect:/auth/login";
        String hashedOtp= sessionOtp.toString();
        Long otpExpiry = (Long) session.getAttribute("otpExpiry");
        if (failCount==null) failCount=0;
        if(authenticationService.check(inputOTP,hashedOtp)){
            session.removeAttribute("otp");
            session.removeAttribute("otpExpiry");
            session.removeAttribute("failCount");
            session.removeAttribute("lastResendTime");
            model.addAttribute("showResetForm", true);
            model.addAttribute("email", params.get("email"));
        }else {
            failCount++;
            session.setAttribute("failCount", failCount);
            if (failCount>=3 || otpExpiry==null||otpExpiry<System.currentTimeMillis()) {
                session.removeAttribute("otp");
                session.removeAttribute("otpExpiry");
                session.removeAttribute("failCount");
                session.removeAttribute("lastResendTime");
                System.out.println("otp đã hết hiệu lực");
                model.addAttribute("error", "OTP đã bị vô hiệu hoá hoặc đã hết hiệu lực. Vui lòng gửi lại mã.");
                model.addAttribute("showOtpForm", true);
                model.addAttribute("email", params.get("email"));
            }else {
                int remain = 3 - failCount;
                System.out.println("bạn còn "+remain+" lần thử");
                model.addAttribute("error", "OTP không đúng. Bạn còn " + (3 - failCount) + " lần thử.");
                model.addAttribute("showOtpForm", true);
                model.addAttribute("email", params.get("email"));
            }
        }
        return "pages/forgotPassword";
    }

    @PostMapping("/auth/resetPassword")
    public String resetPassword(@RequestParam Map<String, String> params,Model model){
        String newPassword = params.get("newPassword");
        String confirmPassword = params.get("confirmPassword");
        String email = params.get("email");
        if (newPassword.equals(confirmPassword)) {
            accountService.changePassword(email, newPassword);
            System.out.println("đổi mật khẩu thành công");
            return "redirect:/auth/login";
        }else {
            model.addAttribute("showResetForm", true);
            model.addAttribute("email", params.get("email"));
            return "pages/forgotPassword";
        }
    }
    @PostMapping("/auth/register")
    public String register(@RequestParam Map<String, String> params,Model model,HttpSession session){
        String username=params.get("username");
        String email=params.get("email");
        String password=params.get("password");
        String otp=authenticationService.register(username,email);
        if (otp==null) {
            model.addAttribute("error", "Tài khoản đã tồn tại");
            return "pages/register";
        }
        session.removeAttribute("otp");
        session.removeAttribute("otpExpiry");
        session.removeAttribute("failCount");
        session.setAttribute("otp", otp);
        session.setAttribute("otpExpiry", System.currentTimeMillis() + (3 * 60 * 1000));
        model.addAttribute("showOtpForm", true);
        model.addAttribute("email", email);
        model.addAttribute("username", username);
        model.addAttribute("password", password);
        return "pages/register";
    }
    @PostMapping("/auth/register/resendOtp")
    public String resendRegisterOTP(@RequestParam("email") String email,
                                    @RequestParam(value = "username", required = false) String username,
                                    @RequestParam(value = "password", required = false) String password, Model model, HttpSession session){
        String otp=authenticationService.register(username,email);
        session.removeAttribute("otp");
        session.removeAttribute("otpExpiry");
        session.removeAttribute("failCount");
        session.setAttribute("otp", otp);
        session.setAttribute("otpExpiry", System.currentTimeMillis() + (3 * 60 * 1000));
        model.addAttribute("showOtpForm", true);
        model.addAttribute("email", email);
        model.addAttribute("username", username);
        model.addAttribute("password", password);
        return "pages/register";
    }
    @PostMapping("/auth/verifyRegisterOtp")
    public String verifyRegisterOtp(@RequestParam Map<String, String> params, RedirectAttributes ra, HttpSession session){
        String inputOTP = params.get("otp1") + params.get("otp2") + params.get("otp3") + params.get("otp4") + params.get("otp5") + params.get("otp6");
        Object sessionOtp= session.getAttribute("otp");
        if (sessionOtp==null) return "redirect:/auth/login";
        String hashedOtp= sessionOtp.toString();
        Integer failCount=(Integer) session.getAttribute("failCount");
        Long otpExpiry=(Long)session.getAttribute("otpExpiry");
        String username=params.get("username");
        String email=params.get("email");
        String password=params.get("password");
        System.out.println("đăng ký tài khoản");
        if (failCount==null) failCount=0;
        if (authenticationService.check(inputOTP,hashedOtp)){
            session.removeAttribute("otp");
            session.removeAttribute("otpExpiry");
            session.removeAttribute("failCount");
            accountService.createAccount(username,password,email);
            System.out.println("đăng ký thành công rồi");
            ra.addFlashAttribute("message", "Đăng ký thành công. Vui lòng đăng nhập.");
        }else {
            failCount++;
            session.setAttribute("failCount", failCount);
            if (failCount>=3 || otpExpiry==null||otpExpiry<System.currentTimeMillis()) {
                session.removeAttribute("otp");
                session.removeAttribute("otpExpiry");
                session.removeAttribute("failCount");
                System.out.println("otp đã hết hiệu lực");
                ra.addFlashAttribute("error", "OTP đã bị vô hiệu hoá hoặc đã hết hiệu lực. Vui lòng gửi lại mã.");
            }else {
                int remain = 3 - failCount;
                System.out.println("bạn còn "+remain+" lần thử");
                ra.addFlashAttribute("error", "OTP không đúng. Bạn còn " + (3 - failCount) + " lần thử.");
            }
            ra.addFlashAttribute("showOtpForm", true);
            ra.addFlashAttribute("email", email);
            ra.addFlashAttribute("username", username);
            return "redirect:/auth/register";
        }
        return "redirect:/auth/login";
    }
    @GetMapping("/auth/google")
    public String loginGoogle(){
        System.out.println("chạy gg");
        return "redirect:/oauth2/authorization/google";
    }

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
}
