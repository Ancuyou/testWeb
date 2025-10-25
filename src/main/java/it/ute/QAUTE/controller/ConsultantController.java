package it.ute.QAUTE.controller;

import it.ute.QAUTE.dto.AnswerQuestionDTO;
import it.ute.QAUTE.dto.UserDTO;
import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.Answer;
import it.ute.QAUTE.entity.Consultant;
import it.ute.QAUTE.entity.Messages;
import it.ute.QAUTE.entity.Profiles;
import it.ute.QAUTE.entity.Question;
import it.ute.QAUTE.entity.User;
import it.ute.QAUTE.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/consultant")
public class ConsultantController {
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private AccountService accountService;

    @Autowired
    private ConsultantService consultantService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private FileStorageService fileStorageService;
    @GetMapping({"", "/"})
    public String consultantRoot() {
        return "redirect:/consultant/home";
    }
    
    @GetMapping("/home")
    public String homeConsultant(Model model, Authentication authentication) {
        if (authentication != null) {
            Account account = accountService.findByUsername(authentication.getName());
            Profiles profile = account.getProfile();
            Consultant consultant = profile.getConsultant();
            if (consultant != null) {
                Integer consultantId = consultant.getConsultantID();
                List<Answer> answers = answerService.getAllAnswersByConsultant(consultantId);
                Map<String, Long> questionsByField = answers.stream()
                    .filter(a -> a.getQuestion() != null && a.getQuestion().getField() != null)
                    .collect(Collectors.groupingBy(
                        a -> a.getQuestion().getField().getFieldName(),
                        Collectors.mapping(a -> a.getQuestion().getQuestionID(), Collectors.toSet())
                    ))
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (long) e.getValue().size()
                    ));

                // Số câu hỏi duy nhất đã trả lời bởi consultant này (đếm distinct questionID)
                long totalQuestionsAnswered = answers.stream()
                    .filter(a -> a.getQuestion() != null)
                    .map(a -> a.getQuestion().getQuestionID())
                    .distinct()
                    .count();
                
                // Tổng số câu trả lời toàn hệ thống
                List<Answer> allAnswers = answerService.getAllAnswers();
                int totalAnswers = allAnswers.size();
                
                // Tổng số câu hỏi toàn hệ thống
                List<Question> allQuestions = questionService.getAllQuestions();
                long totalQuestions = allQuestions.size();
                
                // Tỷ lệ trả lời: (Số câu hỏi đã trả lời bởi consultant) / (Tổng số câu hỏi) * 100
                double responseRate = totalQuestions > 0 ? (double) totalQuestionsAnswered / totalQuestions * 100 : 0;
                
                
                
                double avgResponseTime = answers.stream()
                    .filter(a -> a.getQuestion() != null && a.getQuestion().getDateSend() != null)
                    .mapToLong(a -> java.time.Duration.between(a.getQuestion().getDateSend(), a.getDateAnswered()).toMinutes())
                    .average()
                    .orElse(0);
                
                List<Answer> recentActivities = answers.stream()
                    .sorted((a1, a2) -> a2.getDateAnswered().compareTo(a1.getDateAnswered()))
                    .limit(5)
                    .toList();
               
                Map<String, Long> questionsByDay = answers.stream()
                .filter(a -> a.getDateAnswered() != null)
                .collect(Collectors.groupingBy(a -> a.getDateAnswered().toLocalDate().toString(), Collectors.counting()));

                Map<String, Long> questionsByWeek = answers.stream()
                    .filter(a -> a.getDateAnswered() != null)
                    .collect(Collectors.groupingBy(a -> "Tuần " + ((a.getDateAnswered().getDayOfMonth() - 1) / 7 + 1), Collectors.counting()));

                Map<String, Long> questionsByMonth = answers.stream()
                    .filter(a -> a.getDateAnswered() != null)
                    .collect(Collectors.groupingBy(a -> a.getDateAnswered().getMonth().toString(), Collectors.counting()));
                long totalUsersAnswered = answers.stream()
                .filter(a -> a.getQuestion() != null && a.getQuestion().getUser() != null)
                .map(a -> a.getQuestion().getUser().getUserID())
                .distinct()
                .count();

                model.addAttribute("totalUsersAnswered", totalUsersAnswered);
                model.addAttribute("questionsByDay", questionsByDay);
                model.addAttribute("questionsByWeek", questionsByWeek);
                model.addAttribute("questionsByMonth", questionsByMonth);
                model.addAttribute("questionsByField", questionsByField);
                model.addAttribute("totalQuestionsAnswered", totalQuestionsAnswered);
                model.addAttribute("totalAnswers", totalAnswers);
                model.addAttribute("responseRate", String.format("%.1f", responseRate));
                model.addAttribute("avgResponseTime", (int) avgResponseTime);
                model.addAttribute("recentActivities", recentActivities);
            }
            model.addAttribute("account", account);
        }
        return "pages/consultant/home";
    }

    @GetMapping("/profile")
    public String profileConsultant(Model model, Principal principal) {
        String username = principal.getName();
        System.out.println("username = " + username);
        Account account = accountService.findUserByUsername(username);
        Profiles profile = account.getProfile();
        Consultant consultant = profile.getConsultant();
        model.addAttribute("account", account);
        model.addAttribute("profile", profile);
        model.addAttribute("consultant", consultant);

        return "pages/consultant/profile";
    }
    @PostMapping("/profile/update")
    public String updateProfile(
        @RequestParam("fullName") String fullName,
        @RequestParam("phone") String phone,
        @RequestParam(value = "experienceYears", required = false) Integer experienceYears,
        @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) throws IOException {
        String username = principal.getName();
        Account account = accountService.findUserByUsername(username);
        account.getProfile().setFullName(fullName);
        account.getProfile().setPhone(phone);
        account.getProfile().getConsultant().setExperienceYears(experienceYears);
        String oldAvatar = account.getProfile().getAvatar();
        if((avatarFile== null || avatarFile.isEmpty()) && oldAvatar != null && oldAvatar.contains("cloudinary.com")){
            fileStorageService.deleteFile(oldAvatar);
            account.getProfile().setAvatar(null);
        }
        else if (avatarFile != null && !avatarFile.isEmpty()) {
            String newAvatarUrl=fileStorageService.storeFile(avatarFile,oldAvatar,account.getAccountID());
            account.getProfile().setAvatar(newAvatarUrl);
        }
        accountService.updateAccount(account);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/consultant/profile";
    }

    @GetMapping("/questions")
    public String questionsConsultant(Principal principal, 
                                     Model model,
                                     @RequestParam(required = false) Integer highlightQuestion) {
        String username = principal.getName();
        Account account = accountService.findUserByUsername(username);

        model.addAttribute("account", account);
        model.addAttribute("questions", questionService.getAllQuestions());
        model.addAttribute("departments", questionService.getAllDepartments());
        model.addAttribute("fields", questionService.getAllFields());
        if (highlightQuestion != null) {
            model.addAttribute("highlightQuestionId", highlightQuestion);
        }
        
        return "pages/consultant/questions-answer";
    }
    
    @GetMapping("/chats")
    public String chatsConsultant(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Profiles profile = accountService.getProfileByUsername(username);
        
        if (profile != null) {
            List<Messages> recentMessages = messageService.getRecentChats(profile.getProfileID());
            model.addAttribute("recentMessages", recentMessages);
        }
        
        return "pages/consultant/chats";
    }
    

    @GetMapping("/history")
    public String historyConsultant(
            Model model,
            Principal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer timeRange,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String username = principal.getName();
        Account account = accountService.findUserByUsername(username);
        Consultant consultant = consultantService.findByProfileId(account.getProfile().getProfileID())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin tư vấn viên."));

        // Gọi service lấy Page<Answer>
        Page<Answer> answerPage = answerService.getAnswersHistoryByConsultant(
                consultant.getConsultantID(), timeRange, keyword, PageRequest.of(page, size));

        List<AnswerQuestionDTO> answerQuestionDTOs = answerPage.getContent().stream()
            .map(answer -> {
                AnswerQuestionDTO dto = new AnswerQuestionDTO();
                dto.setAnswerID(answer.getAnswerID());
                dto.setConsultantID(consultant.getConsultantID());
                dto.setConsultantName(account.getProfile().getFullName());
                dto.setContentAnswer(answer.getContent());
                dto.setAnswerAt(answer.getDateAnswered());

                if (answer.getQuestion() != null) {
                    dto.setQuestionID(answer.getQuestion().getQuestionID());
                    dto.setTitle(answer.getQuestion().getTitle());
                    dto.setCreatedAt(answer.getQuestion().getDateSend());
                    dto.setContentQuestion(answer.getQuestion().getContent());

                    if (answer.getQuestion().getUser() != null &&
                        answer.getQuestion().getUser().getProfile() != null) {
                        dto.setUserName(answer.getQuestion().getUser().getProfile().getFullName());
                        dto.setUserID(answer.getQuestion().getUser().getUserID());
                    }
                }
                return dto;
            })
            .toList();

        long uniqueQuestions = answerQuestionDTOs.stream()
            .map(AnswerQuestionDTO::getQuestionID)
            .distinct()
            .count();
        long uniqueUsers = answerQuestionDTOs.stream()
            .map(AnswerQuestionDTO::getUserID)
            .filter(id -> id != null)
            .distinct()
            .count();

        model.addAttribute("account", account);
        model.addAttribute("answersHistory", answerQuestionDTOs);
        model.addAttribute("totalAnswers", answerPage.getTotalElements());
        model.addAttribute("uniqueQuestions", uniqueQuestions);
        model.addAttribute("uniqueUsers", uniqueUsers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", answerPage.getTotalPages());
        model.addAttribute("pageSize", size);

        return "pages/consultant/history";
    }
    @GetMapping("/question-details/{id}")
    public String questionDetails(@PathVariable("id") Integer questionId, Model model, Principal principal) {
        String username = principal.getName();
        Account account = accountService.findUserByUsername(username);

        var question = questionService.getQuestionById(questionId);
        var answers = answerService.getAnswersByQuestionId(questionId);

        model.addAttribute("account", account);
        model.addAttribute("question", question);
        model.addAttribute("answers", answers);

        return "pages/consultant/questiondetails";
    }
}
