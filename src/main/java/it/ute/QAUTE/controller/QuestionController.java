package it.ute.QAUTE.controller;

import it.ute.QAUTE.dto.FieldDTO;
import it.ute.QAUTE.dto.HotTopicDTO;
import it.ute.QAUTE.dto.QuestionDTO;
import it.ute.QAUTE.entity.*;
import it.ute.QAUTE.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private ConsultantService consultantService;

    @Autowired
    private QuestionLikeService questionLikeService;

    @GetMapping({"/user/questions"})
    public String showQuestionPage(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer fieldId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer highlightQuestionId,
            Model model,
            Principal principal,
            HttpServletRequest request) {

        Pageable pageable = PageRequest.of(page, 2);
        Page<Question> questionPage = questionService.searchAndFilterQuestions(
                departmentId, fieldId, keyword, sortBy, pageable);

        if (principal != null) {
            String username = principal.getName();
            Account account = accountService.findUserByUsername(username);
            model.addAttribute("account", account);

            if (account != null && account.getProfile() != null) {
                User user = userService.findByProfileId(account.getProfile().getProfileID()).orElse(null);
                if (user != null) {
                    model.addAttribute("likedQuestions", questionPage.getContent().stream()
                            .collect(Collectors.toMap(
                                    Question::getQuestionID,
                                    q -> questionLikeService.isLikedByUser(q.getQuestionID(), user)
                            )));
                }
            }
        }

        model.addAttribute("questionDTO", new QuestionDTO());
        model.addAttribute("questions", questionPage.getContent());
        model.addAttribute("totalPages", questionPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("departments", questionService.getAllDepartments());
        model.addAttribute("fields", questionService.getAllFields());

        // Giữ trạng thái filter
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedFieldId", fieldId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);

        if (highlightQuestionId != null) {
            model.addAttribute("highlightQuestionId", highlightQuestionId);
        }
        List<HotTopicDTO> hotTopics = questionService.getTop5HotTopics();
        model.addAttribute("hotTopics", hotTopics);
        String requestedWithHeader = request.getHeader("X-Requested-With");
        if ("fetch".equals(requestedWithHeader)) {
            // Nếu là yêu cầu AJAX, chỉ trả về fragment nội dung
            return "pages/user/questions :: contentFragment";
        }
        return "pages/user/questions";
    }

    @PostMapping({"/user/questions/ask"})
    public String handleAskQuestion(@Valid @ModelAttribute("questionDTO") QuestionDTO questionDTO,
                                    BindingResult bindingResult,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (principal == null) {
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", questionService.getAllDepartments());
            model.addAttribute("fields", questionService.getAllFields());
            String username = principal.getName();
            Account account = accountService.findUserByUsername(username);
            model.addAttribute("account", account);
            model.addAttribute("questions", questionService.getAllQuestions());
            return "pages/user/questions";
        }

        String username = principal.getName();
        Account account = accountService.findUserByUsername(username);
        User user = userService.findByProfileId(account.getProfile().getProfileID())
                .orElseThrow(() -> new RuntimeException(
                        "User not found for profile ID: " + account.getProfile().getProfileID()
                ));

        // Tạo Question entity từ DTO
        Question question = new Question();
        question.setTitle(questionDTO.getTitle());
        question.setContent(questionDTO.getContent());
        question.setUser(user);
        question.setDateSend(LocalDateTime.now());
        question.setStatus(Question.QuestionStatus.Pending);

        // Set Department
        Department department = new Department();
        department.setDepartmentID(questionDTO.getDepartmentId());
        question.setDepartment(department);

        // Set Field (nếu có)
        if (questionDTO.getFieldId() != null) {
            Field field = new Field();
            field.setFieldID(questionDTO.getFieldId());
            question.setField(field);
        }

        // Xử lý file đính kèm
        MultipartFile file = questionDTO.getFile();
        if (file != null && !file.isEmpty()) {
            String acceptfiles = "image/png,image/jpeg,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.presentationml.presentation";
            if (!acceptfiles.contains(file.getContentType())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Loại tệp không được hỗ trợ. Vui lòng chọn ảnh hoặc tài liệu.");
                return "redirect:/user/questions";
            }

            String uploadDir = "src/main/resources/static/images/questions/";
            File uploadFolder = new File(uploadDir);
            if (!uploadFolder.exists()) {
                uploadFolder.mkdirs();
            }

            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String fileName = "Question_" + account.getAccountID() + extension;
            String uuid = java.util.UUID.randomUUID().toString().substring(0, 5);
            fileName = uuid + "_" + fileName;

            question.setFileAttachment("/images/questions/" + fileName);

            try {
                Files.copy(file.getInputStream(), Paths.get(uploadDir, fileName), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi gửi câu hỏi: " + e.getMessage());
                return "redirect:/user/questions";
            }
        }

        questionService.saveQuestion(question);
        redirectAttributes.addFlashAttribute("successMessage", "Câu hỏi của bạn đã được gửi thành công!");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (account.getRole().equals(Account.Role.Consultant)) {
            return "redirect:/consultant/questions-answer";
        }
        return "redirect:/user/questions";
    }

    @PostMapping({"/user/questions/answer"})
    public String handlePostAnswer(@RequestParam("questionId") Integer questionId,
                                   @RequestParam("content") String content,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/auth/login";
        }

        String username = principal.getName();
        Account account = accountService.findUserByUsername(username);

        if (account.getRole() != Account.Role.Consultant) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có tư vấn viên mới có thể trả lời.");
            return "redirect:/consultant/questions";
        }

        Consultant consultant = consultantService.findByProfileId(account.getProfile().getProfileID())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin tư vấn viên."));

        Answer answer = new Answer();
        Question qRef = new Question();
        qRef.setQuestionID(questionId);

        answer.setQuestion(qRef);
        answer.setConsultant(consultant);
        answer.setContent(content);
        answer.setDateAnswered(LocalDateTime.now());

        answerService.saveAnswer(answer);
        Question q = questionService.getQuestionById(questionId);
        if (q == null) {
            throw new IllegalStateException("Không tìm thấy câu hỏi.");
        }
        q.setStatus(Question.QuestionStatus.Answered);
        questionService.saveQuestion(q);

        redirectAttributes.addFlashAttribute("successMessage", "Câu trả lời của bạn đã được gửi thành công!");
        return "redirect:/consultant/questions";
    }

    @GetMapping("/api/fields/{departmentId}")
    @ResponseBody
    public java.util.List<FieldDTO> getFieldsByDepartment(@PathVariable Integer departmentId) {
        java.util.List<Field> fields = questionService.getFieldsByDepartmentId(departmentId);
        return fields.stream()
                .map(f -> new FieldDTO(f.getFieldID(), f.getFieldName()))
                .toList();
    }

    @GetMapping("/api/fields/all")
    @ResponseBody
    public java.util.List<FieldDTO> getAllFields() {
        return questionService.getAllFields().stream()
                .map(f -> new FieldDTO(f.getFieldID(), f.getFieldName()))
                .toList();
    }
}
