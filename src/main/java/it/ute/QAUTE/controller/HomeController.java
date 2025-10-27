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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            @RequestParam(required = false) Integer departmentId, // Thêm filter params
            @RequestParam(required = false) Integer fieldId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sortBy, // Thêm sort param
            @RequestParam(defaultValue = "0") int page,          // Thêm page param
            Model model,
            Principal principal,
            HttpServletRequest request // Thêm HttpServletRequest
    ) {
        if (principal != null) {
            String username = principal.getName();
            Account account = accountService.findUserByUsername(username);
            User user = userService.findByProfileId(account.getProfile().getProfileID()).orElse(null);

            if (user != null) {
                Pageable pageable = PageRequest.of(page, 5); // Số lượng item mỗi trang, ví dụ 5
                Page<Question> questionPage = questionService.searchAndFilterUserQuestions(
                        user, departmentId, fieldId, keyword, sortBy, pageable);

                model.addAttribute("userQuestions", questionPage.getContent());
                model.addAttribute("totalPages", questionPage.getTotalPages());
                model.addAttribute("currentPage", page);
                model.addAttribute("totalItems", questionPage.getTotalElements()); // Tổng số câu hỏi của user (sau filter)


                // Lấy stats (có thể tính lại dựa trên questionPage.getTotalElements() nếu filter)
                long answersReceived = answerService.countAnswersForUser(user); // Giữ nguyên cách tính này hoặc tính dựa trên list đã filter
                model.addAttribute("questionsAsked", questionPage.getTotalElements()); // Số lượng sau filter
                model.addAttribute("answersReceived", answersReceived);


                if (highlightQuestionId != null) {
                    model.addAttribute("highlightQuestionId", highlightQuestionId);
                }

                // Truyền lại các tham số filter/sort để giữ trạng thái trên view
                model.addAttribute("selectedDepartmentId", departmentId);
                model.addAttribute("selectedFieldId", fieldId);
                model.addAttribute("keyword", keyword);
                model.addAttribute("sortBy", sortBy);
                model.addAttribute("departments", questionService.getAllDepartments());
                // Load fields ban đầu dựa trên selectedDepartmentId nếu có
                List<Field> fieldsForFilter = departmentId != null
                        ? questionService.getFieldsByDepartmentId(departmentId)
                        : questionService.getAllFields(); // Hoặc chỉ load khi có department
                model.addAttribute("fields", fieldsForFilter); // Cần fields cho bộ lọc

            }
            model.addAttribute("account", account); // Luôn add account
            // Kiểm tra nếu là AJAX request
            String requestedWithHeader = request.getHeader("X-Requested-With");
            if ("fetch".equals(requestedWithHeader)) {
                return "pages/user/history :: historyContentFragment"; // Trả về fragment
            }
        } else {
            // Chưa đăng nhập, có thể redirect về login
            return "redirect:/auth/login";
        }
        return "pages/user/history"; // Trả về trang đầy đủ nếu không phải AJAX
    }

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
    @GetMapping("/user/consultants")
    public String showConsultantsPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "name_asc") String sortBy, // Mặc định sắp xếp theo tên
            @RequestParam(required = false, defaultValue = "30days") String timeRange, // Mặc định 30 ngày
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size, // Ví dụ 9 cards mỗi trang (3x3 grid)
            Model model,
            Principal principal) {
        if (principal == null) {
            return "redirect:/auth/login"; // Yêu cầu đăng nhập
        }
        Account currentAccount = accountService.findUserByUsername(principal.getName());
        if (currentAccount == null) {
            // Optional: Handle case where account might not be found
            return "redirect:/auth/login?error=account_not_found";
        }
        model.addAttribute("account", currentAccount);
        // Lấy thông tin người dùng hiện tại (để truyền vào navbar)
        Profiles currentUserProfile = userService.getCurrentUserProfile(principal.getName());
        model.addAttribute("currentUserProfile", currentUserProfile); // Đổi tên biến để rõ ràng hơn
        // Lấy danh sách consultants đã được sắp xếp và lọc theo thời gian
        List<ConsultantDTO> allConsultants = consultantService.getConsultantsWithSortingAndFilter(sortBy, timeRange);
        // Lọc theo keyword (nếu có) - Lọc trên danh sách đã có stats
        List<ConsultantDTO> filteredConsultants;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.trim().toLowerCase();
            filteredConsultants = allConsultants.stream()
                    .filter(c -> c.getFullName().toLowerCase().contains(lowerKeyword))
                    // Thêm điều kiện lọc khác nếu cần (ví dụ: theo specialization)
                    .collect(Collectors.toList());
        } else {
            filteredConsultants = new ArrayList<>(allConsultants);
        }
        // Phân trang thủ công trên danh sách đã lọc và sắp xếp
        int start = Math.min(page * size, filteredConsultants.size());
        int end = Math.min((page + 1) * size, filteredConsultants.size());
        List<ConsultantDTO> paginatedConsultants = filteredConsultants.subList(start, end);
        Page<ConsultantDTO> consultantPage = new PageImpl<>(paginatedConsultants, PageRequest.of(page, size), filteredConsultants.size());
        model.addAttribute("consultants", consultantPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", consultantPage.getTotalPages());
        model.addAttribute("totalItems", consultantPage.getTotalElements());
        // Truyền các tham số tìm kiếm/lọc/sắp xếp lại view để giữ trạng thái
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("timeRange", timeRange);
        // Truyền các tùy chọn cho dropdowns
        model.addAttribute("sortOptions", Map.of(
                "name_asc", "Tên A-Z",
                "answers_desc", "Trả lời nhiều nhất",
                "answers_asc", "Trả lời ít nhất",
                "response_time_asc", "Phản hồi nhanh nhất",
                "response_time_desc", "Phản hồi chậm nhất"
                // "experience_desc", "Kinh nghiệm nhiều nhất" // Thêm nếu cần
        ));
        model.addAttribute("timeOptions", Map.of(
                "7days", "7 ngày qua",
                "30days", "30 ngày qua",
                "90days", "90 ngày qua",
                "all", "Toàn thời gian"
        ));
        return "pages/user/consultants"; // Trả về view mới
    }
}
