// src/main/java/it/ute/QAUTE/controller/HomeController.java
package it.ute.QAUTE.controller;

import com.nimbusds.jose.JOSEException;
import it.ute.QAUTE.dto.ConsultantDTO;
import it.ute.QAUTE.dto.HotTopicDTO;
import it.ute.QAUTE.entity.*;
import it.ute.QAUTE.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
    @Autowired
    private AccountService accountService;
    @Autowired
    private UserService userService;
    @Autowired
    private ConsultantService consultantService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private AnswerService answerService;
    @Autowired
    private QuestionLikeService questionLikeService;
    @Autowired
    private EventService eventService;

    @GetMapping("/user/home")
    public String homeUser(Model model, Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            Account account = accountService.findUserByUsername(username);
            User user = userService.findByProfileId(account.getProfile().getProfileID()).orElse(null);
            model.addAttribute("account", account);

            if (user != null) {
                long questionsAsked = questionService.countQuestionsByUser(user);
                long answersReceived = answerService.countAnswersForUser(user);
                long totalLikes = questionLikeService.getTotalLikesForUser(user);
                long consultantsChatted = messageService.getRecentChats(account.getProfile().getProfileID())
                        .stream()
                        .map(m -> m.getSenderID().equals(account.getProfile().getProfileID()) ? m.getReceiverID() : m.getSenderID())
                        .distinct()
                        .count();
                Map<String, Long> userStats = new HashMap<>();
                userStats.put("questionsAsked", questionsAsked);
                userStats.put("answersReceived", answersReceived);
                userStats.put("consultantsChatted", consultantsChatted);
                userStats.put("totalLikes", totalLikes);
                model.addAttribute("userStats", userStats);

                List<Question> recentQuestions = questionService.getTop3RecentQuestionsByUser(user);
                model.addAttribute("recentActivities", recentQuestions);
            }

            List<Question> communityQuestions = questionService.getTop5RecentCommunityQuestions();
            model.addAttribute("communityQuestions", communityQuestions);

            List<ConsultantDTO> consultants = consultantService.getAllConsultants();
            model.addAttribute("consultants", consultants);

            List<Profiles> chatConsultants = messageService.getAllChatUsers(account.getProfile().getProfileID());
            List<ConsultantDTO> chatConsultantDTOs = chatConsultants.stream()
                    .map(profile -> {
                        Consultant consultant = profile.getConsultant();
                        if (consultant != null) {
                            ConsultantDTO dto = new ConsultantDTO();
                            dto.setConsultantID(consultant.getConsultantID());
                            dto.setProfileID(profile.getProfileID());
                            dto.setFullName(profile.getFullName());
                            dto.setAvatar(profile.getAvatar());
                            dto.setExperienceYears(consultant.getExperienceYears());
                            dto.setIsOnline(false);
                            return dto;
                        }
                        return null;
                    })
                    .toList();
            model.addAttribute("chatConsultants", chatConsultantDTOs);

            if (account.getProfile() != null) {
                List<Messages> recentChats = messageService.getRecentChats(account.getProfile().getProfileID());
                model.addAttribute("recentChats", recentChats);
            }

            List<Event> recentEvents = eventService.findTop3UpcomingEvents(); // Thêm dòng này
            model.addAttribute("recentEvents", recentEvents);
            List<HotTopicDTO> hotTopics = questionService.getTop5HotTopics();
            model.addAttribute("hotTopics", hotTopics);
        }
        return "pages/user/home";
    }

    @GetMapping("/user/history")
    public String userHistory(
            @RequestParam(required = false) Integer highlightQuestionId,
            Model model,
            Principal principal
    ) {
        if (principal != null) {
            String username = principal.getName();
            Account account = accountService.findUserByUsername(username);
            User user = userService.findByProfileId(account.getProfile().getProfileID()).orElse(null);

            if (user != null) {
                List<Question> userQuestions = questionService.getAllQuestionsByUserSortedByDate(user);
                long questionsAsked = userQuestions.size();
                long answersReceived = answerService.countAnswersForUser(user);

                model.addAttribute("userQuestions", userQuestions);
                model.addAttribute("questionsAsked", questionsAsked);
                model.addAttribute("answersReceived", answersReceived);

                if (highlightQuestionId != null) {
                    model.addAttribute("highlightQuestionId", highlightQuestionId);
                }
            }
            model.addAttribute("account", account);
        }
        return "pages/user/history";
    }

    // ... các phương thức POST và profile không thay đổi
    @GetMapping("/home/profile")
    public String profile(Model model, Principal principal) {
        String username = principal.getName();
        Account account = accountService.findUserByUsername(username);
        User user = userService.findByProfileId(account.getProfile().getProfileID())
                .orElse(null);
        if (user == null) {
            user = new User();
            user.setProfile(account.getProfile());
        }
        System.out.println(account.getProfile().getAvatar());
        model.addAttribute("account", account);
        model.addAttribute("user", user);
        model.addAttribute("roleLabels", userService.mapRole());
        return "pages/profile";
    }
    @PostMapping("/home/profile/send-otp")
    public String sendOTP (Principal principal,HttpSession session){
        Account account = accountService.findUserByUsername(principal.getName());
        String otp=authenticationService.changePassword(account.getEmail());
        session.setAttribute("otp", otp);
        session.setAttribute("otpExpiry", System.currentTimeMillis() + (3 * 60 * 1000));
        return "redirect:/home/profile";
    }
    @PostMapping("/home/profile/verify-otp")
    public String verifyOTP(@RequestParam("otp") String inputOTP,RedirectAttributes ra, HttpSession session){
        String otp=session.getAttribute("otp").toString();
        Integer failCount=(Integer) session.getAttribute("failCount");
        Long otpExpiry=(Long)session.getAttribute("otpExpiry");
        if (failCount==null) failCount=0;
        if (otpExpiry==null||otpExpiry<System.currentTimeMillis()){
            ra.addFlashAttribute("otpModalMsg", "OTP chưa được gửi hoặc đã hết hạn. Vui lòng gửi lại OTP.");
            ra.addFlashAttribute("otpModalError", true);
            ra.addAttribute("otpModal", 1);
        }else if(!authenticationService.check(inputOTP,otp)){
            failCount++;
            session.setAttribute("failCount", failCount);
            if(failCount>3){
                session.removeAttribute("otp");
                session.removeAttribute("otpExpiry");
                session.removeAttribute("failCount");
                ra.addFlashAttribute("otpModalMsg", "Bạn đã nhập sai quá 3 lần. OTP đã bị huỷ. Vui lòng gửi lại.");
            }else {
                int remain = 3 - failCount;
                ra.addFlashAttribute("otpModalMsg", "OTP không đúng. Bạn còn " + remain + " lần thử.");
            }
        }else {
            session.removeAttribute("otp");
            session.removeAttribute("otpExpiry");
            session.removeAttribute("failCount");
            ra.addAttribute("otpVerified", 1);
        }
        return "redirect:/home/profile";
    }
    @PostMapping("/home/profile/update")
    public String update(@RequestParam String fullName,
                         @RequestParam(required = false) String phone,
                         @RequestParam User.Role roleName,
                         @RequestParam(required = false) String studentCode,
                         @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                         @RequestParam(value = "newPassword", required = false) String newPassword,
                         HttpSession session, HttpServletRequest request, HttpServletResponse response) throws ParseException, JOSEException {
        Object tokenObj = session.getAttribute("ACCESS_TOKEN");
        int id = Math.toIntExact(authenticationService.getCurrentUserId(tokenObj,request,response));
        System.out.println(id);
        Account account = accountService.findById(id);
        if(newPassword !=null &&!newPassword.isBlank()) account.setPassword(authenticationService.hashed(newPassword));
        account.getProfile().setFullName(fullName);
        account.getProfile().setPhone(phone);
        account.getProfile().getUser().setRoleName(roleName);
        account.getProfile().getUser().setStudentCode(studentCode);
        String oldAvatar = account.getProfile().getAvatar();
        if((avatarFile== null || avatarFile.isEmpty()) && oldAvatar != null && oldAvatar.contains("cloudinary.com")){
            fileStorageService.deleteFile(oldAvatar);
            account.getProfile().setAvatar(null);
        }
        else if (avatarFile != null && !avatarFile.isEmpty()) {
            String newAvatarUrl = fileStorageService.storeFile(avatarFile,oldAvatar, id);
            account.getProfile().setAvatar(newAvatarUrl);
        }
        accountService.save(account);
        System.out.println("lưu thành công");
        return "redirect:/home/profile";
    }
}
