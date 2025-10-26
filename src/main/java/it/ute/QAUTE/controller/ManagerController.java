package it.ute.QAUTE.controller;

import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.exception.AppException;
import com.nimbusds.jose.JOSEException;
import it.ute.QAUTE.dto.AnswerReportDTO;
import it.ute.QAUTE.dto.ConsultantReportDTO;
import it.ute.QAUTE.dto.QuestionReportDTO;
import it.ute.QAUTE.entity.*;
import it.ute.QAUTE.repository.FieldRepository;
import it.ute.QAUTE.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Controller
@RequestMapping("/manager")
public class ManagerController {
    @Autowired
    private QuestionService questionService;
    @Autowired
    private FieldRepository fieldRepository;

    @Autowired
    private DepartmentService  departmentService;

    @Autowired
    private FieldService fieldService;

    @Autowired
    private AnswerReportService answerReportService;

    @Autowired
    private QuestionReportService questionReportService;

    @Autowired
    private UserReportService  userReportService;

    @Autowired
    private ConsultantReportService consultantReportService;

    @Autowired
    private ToxicContentService toxicContentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AccountService accountService;

    @GetMapping("/questions")
    public String listQuestions(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(required = false) Integer departmentId,
                                @RequestParam(required = false) Integer fieldId,
                                @RequestParam(required = false) String userName,
                                @RequestParam(required = false) String status,
                                Model model) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("dateSend").descending());
        Page<Question> questionPage = questionService.filterQuestions(departmentId, fieldId, userName, status, pageable);  // this username is Ho va ten not acc

        model.addAttribute("questions", questionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", questionPage.getTotalPages());
        model.addAttribute("totalElements", questionPage.getTotalElements());

        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("fields", fieldRepository.findAllByDepartments_departmentID(departmentId));

        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedFieldId", fieldId);
        model.addAttribute("userName", userName);
        model.addAttribute("selectedStatus", status);

        return "pages/manager/questions";
    }

    @GetMapping("/questions/edit/{id}")
    public String editQuestionForm(@PathVariable Integer id, Model model) {
        Question question = questionService.findById(id);

        if (question == null) {
            return "redirect:/manager/questions";
        }

        List<Department> departments = departmentService.findAll();
        List<Field> fields = fieldRepository.findAll();

        model.addAttribute("question", question);
        model.addAttribute("departments", departments);
        model.addAttribute("fields", fields);

        return "pages/manager/editQuestion";
    }

    @PostMapping("/questions/update/{id}")
    public String updateQuestion(
            @PathVariable Integer id,
            @ModelAttribute Question question,
            RedirectAttributes redirectAttributes) {

        try {
            Question existingQuestion = questionService.findById(id);

            if (existingQuestion == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy câu hỏi!");
                return "redirect:/manager/questions";
            }

            existingQuestion.setTitle(question.getTitle());
            existingQuestion.setContent(question.getContent());
            existingQuestion.setStatus(question.getStatus());
            existingQuestion.setDepartment(question.getDepartment());
            existingQuestion.setField(question.getField());

            questionService.save(existingQuestion);

            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Sửa thành công! Question ");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sửa thất bại: " + e.getMessage());
        }

        return "redirect:/manager/questions";
    }

    @PostMapping("/questions/delete/{id}")
    public String deleteQuestion(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            questionService.deleteQuestion(id);
            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage", "Question deleted successfully!");
        }catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa thất bại" + e.getMessage());
        }
        return  "redirect:/manager/questions";
    }

    @GetMapping("/fields")
    public String listFields(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String keyword,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("fieldID").descending());
        Page<Field> fieldPage = fieldService.searchField(departmentId, keyword, pageable);

        List<Department> departments = departmentService.findAll();

        model.addAttribute("fields", fieldPage.getContent());
        model.addAttribute("departments", departments);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", fieldPage.getTotalPages());
        model.addAttribute("totalElements", fieldPage.getTotalElements());
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", "fields");


        return "pages/manager/fields";
    }

    @GetMapping("/fields/new")
    public String newField(Model model) {
        model.addAttribute("field", new Field());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("active", "fields");
        return "pages/manager/addField";
    }

    @PostMapping("/fields/delete/{id}")
    public String deleteField(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try{
            fieldRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage", "Field deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessages", "Xóa thất bại" + e.getMessage());
        }
        return  "redirect:/manager/fields";
    }

    @PostMapping("/fields/save")
    public String saveField(
            @ModelAttribute("field") Field field,
            @RequestParam(value = "departmentIds", required = false) List<Integer> departmentIds,
            RedirectAttributes redirectAttributes) {

        try {
            if (departmentIds != null && !departmentIds.isEmpty()) {
                Set<Department> selectedDepartments = new HashSet<>(departmentService.findAllById(departmentIds));
                field.setDepartments(selectedDepartments);
            } else {
                field.setDepartments(new HashSet<>());
            }

            fieldRepository.save(field);

            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm lĩnh vực thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Thêm lĩnh vực thất bại: " + e.getMessage());
        }

        return "redirect:/manager/fields";
    }

    @GetMapping("/fields/edit/{id}")
    public String editField(@PathVariable Integer id, Model model) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lĩnh vực ID: " + id));

        model.addAttribute("field", field);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("active", "fields");

        return "pages/manager/editField";
    }

    @PostMapping("/fields/update/{id}")
    public String updateField(
            @PathVariable Integer id,
            @ModelAttribute("field") Field field,
            @RequestParam(value = "departmentIds", required = false) List<Integer> departmentIds,
            RedirectAttributes redirectAttributes) {

        try {
            Field existing = fieldRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lĩnh vực ID: " + id));

            existing.setFieldName(field.getFieldName());

            if (departmentIds != null && !departmentIds.isEmpty()) {
                Set<Department> selectedDepartments = new HashSet<>(departmentService.findAllById(departmentIds));
                existing.setDepartments(selectedDepartments);
            } else {
                existing.setDepartments(new HashSet<>());
            }

            fieldRepository.save(existing);

            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lĩnh vực thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
        }

        return "redirect:/manager/fields";
    }

    @GetMapping("/reports/answers")
    public String getAnswerReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDateTime startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDateTime endDate,
            Model model) {
        // Nếu người dùng chưa chọn, mặc định lấy 7 ngày gần nhất
        if (startDate == null) startDate = LocalDateTime.now().minusDays(7);
        if (endDate == null) endDate = LocalDateTime.now();

        List<AnswerReportDTO> answersByConsultant = answerReportService.getAnswersByConsultant(startDate, endDate);
        List<AnswerReportDTO> answersByDate = answerReportService.getAnswersByDate(startDate, endDate);

        model.addAttribute("answersByConsultant", answersByConsultant);
        model.addAttribute("answersByDate", answersByDate);
        model.addAttribute("totalAnswers", answerReportService.getTotalAnswers(startDate, endDate));
        model.addAttribute("avgResponseTime", answerReportService.getAverageResponseTime(startDate, endDate));
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "pages/manager/reports/answers";
    }

    @GetMapping("/reports/questions")
    public String getQuestionReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDateTime startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDateTime endDate,
            Model model)
    {
        if (startDate == null) startDate = LocalDateTime.now().minusDays(7);
        if (endDate == null) endDate = LocalDateTime.now();


        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        long total = questionReportService.getTotalQuestions(startDate, endDate);
        var byField = questionReportService.getByField(startDate, endDate);
        var byDept = questionReportService.getByDepartment(startDate, endDate);
        var byStatus = questionReportService.getByStatus(startDate, endDate);
        var byDate = questionReportService.getByDate(startDate, endDate);

        model.addAttribute("totalQuestions", total);

        model.addAttribute("byField", byField);
        model.addAttribute("fieldLabels",
                byField.stream().map(QuestionReportDTO::getName).toList());
        model.addAttribute("fieldData",
                byField.stream().map(QuestionReportDTO::getCount).toList());

        model.addAttribute("byDept", byDept);
        model.addAttribute("deptLabels",
                byDept.stream().map(QuestionReportDTO::getName).toList());
        model.addAttribute("deptData",
                byDept.stream().map(QuestionReportDTO::getCount).toList());

        model.addAttribute("byStatus", byStatus);
        model.addAttribute("statusLabels",
                byStatus.stream().map(QuestionReportDTO::getName).toList());
        model.addAttribute("statusData",
                byStatus.stream().map(QuestionReportDTO::getCount).toList());

        model.addAttribute("byDate", byDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        model.addAttribute("dateLabels",
                byDate.stream()
                        .map(dto -> dto.getDate() != null ? dto.getDate().format(formatter) : "")
                        .toList());
        model.addAttribute("dateData",
                byDate.stream().map(QuestionReportDTO::getCount).toList());

        long answeredCount = byStatus.stream()
                .filter(s -> "ANSWERED".equalsIgnoreCase(s.getName()))
                .mapToLong(QuestionReportDTO::getCount)
                .sum();
        double answeredRate = total > 0 ? (answeredCount * 100.0 / total) : 0.0;
        model.addAttribute("answeredRate", Math.round(answeredRate));

        return "pages/manager/reports/questions";
    }

    @GetMapping("/reports/users")
    public String getUserReport(@RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDateTime startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDateTime endDate,
            Model model)
    {

        if (startDate == null) startDate = LocalDateTime.now().minusDays(7);
        if (endDate == null) endDate = LocalDateTime.now();


        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        model.addAttribute("totalUsers", userReportService.getTotalUsers(startDate, endDate));
        model.addAttribute("activeUsers", userReportService.getActiveUsers());
        model.addAttribute("usersByRole", userReportService.getUsersByRole(startDate, endDate));
        model.addAttribute("topUsers", userReportService.getTop10Users(startDate, endDate));
        return "pages/manager/reports/users";
    }

    @GetMapping("/reports/consultants")
    public String getConsultantReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Model model) {

        if (startDate == null) startDate = LocalDateTime.now().minusDays(30);   // 30 ngay gan nhat
        if (endDate == null) endDate = LocalDateTime.now();

        List<ConsultantReportDTO> reports = consultantReportService.getPerformance(startDate, endDate);
        long totalConsultants = consultantReportService.getTotalConsultants();

        model.addAttribute("totalConsultants", totalConsultants);
        model.addAttribute("reports", reports);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "pages/manager/reports/consultants";
    }

    @GetMapping("/bad-contents")
    public String listBadContents(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                  Model model) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Question> toxicQuestions = toxicContentService.findToxicQuestionsByDateRange(startDate, endDate, pageable);
        model.addAttribute("toxicPage", toxicQuestions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", toxicQuestions.getTotalPages());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "pages/manager/badContents";
    }
    @GetMapping("/profile")
    public String showAdminProfile(Model model) {
        Account acc = authenticationService.getCurrentAccount();
        if (acc == null) return "redirect:/auth/login";

        try {
            model.addAttribute("account", acc);
            return "pages/manager/profile";

        } catch (Exception e) {
            return "redirect:/auth/login";
        }
    }

    @PostMapping("/profile")
    public String ProfileUpdate(
            @ModelAttribute("account") Account form,
            @RequestParam("newPassword") String newPassword,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            RedirectAttributes redirectAttributes){
        try {
            accountService.editManagerOrConsultant(form, newPassword, avatarFile);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sửa thất bại: " + e.getMessage());
            return "redirect:/manager/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sửa thất bại: " + e.getMessage());
            return "redirect:/manager/profile";
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage",
                "Sửa thành công! Manager " + form.getProfile().getFullName());
        return "redirect:/manager/profile";
    }

    @PostMapping("/bad-contents/rejected/{id}")
    public String deleteToxicQuestion(@PathVariable Integer id,
                                      RedirectAttributes redirectAttributes) {
        try {
            toxicContentService.rejectedQuestion(id);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
            return "redirect:/manager/bad-contents";
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage", "Rejected thành công!");
        return "redirect:/manager/bad-contents";
    }

    @PostMapping("/bad-contents/approved/{id}")
    public String markAsClean(@PathVariable Integer id,
                              RedirectAttributes redirectAttributes) {
        try {
            toxicContentService.approvedQuestion(id);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
            return "redirect:/manager/bad-contents";
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage", "Approved thành công!");
        return "redirect:/manager/bad-contents";
    }



    @ResponseBody
    @GetMapping("/fields/by-department/{departmentId}")
    public List<Field> getFieldsByDepartment(@PathVariable Integer departmentId) {
        return fieldRepository.findAllByDepartments_departmentID(departmentId);  // speed run
    }
    @GetMapping("/notifications")
    public String notifications(@RequestParam(defaultValue = "") String q,
                                @RequestParam(defaultValue = "") String status,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) throws ParseException, JOSEException {
        Object tokenObj = session.getAttribute("ACCESS_TOKEN");
        int id = Math.toIntExact(authenticationService.getCurrentUserId(tokenObj,request,response));
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by("createdDate").descending());
        Page<Notification> notifications=notificationService.findNotificationsBySenderId(id,pageable);
        model.addAttribute("notifications", notifications.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifications.getTotalPages());
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        return "pages/manager/notifications";
    }
    @GetMapping("/notifications/new")
    public String addNotification(Model model){
        model.addAttribute("notification", new Notification());
        return "pages/manager/addNotification";
    }
    @GetMapping("/notifications/edit/{id}")
    public String editNotification(@PathVariable("id") Integer id, Model model){
        model.addAttribute("notification", notificationService.findNotificationById(id));
        return "pages/manager/addNotification";
    }
    @PostMapping("/notifications/add")
    public String addNotifications(@RequestParam("title") String title,
                                   @RequestParam("content") String content,
                                   @RequestParam("targetType") String targetType,
                                   @RequestParam("priority") Boolean priority,
                                   @RequestParam("status") String status,
                                   HttpSession session,HttpServletRequest request, HttpServletResponse response) throws ParseException, JOSEException {
        Object tokenObj = session.getAttribute("ACCESS_TOKEN");
        int id = Math.toIntExact(authenticationService.getCurrentUserId(tokenObj,request,response));
        Account account = accountService.findById(id);
        notificationService.createNotification(account, title, content, targetType, status,priority);
        return "redirect:/manager/notifications";
    }
    @PostMapping("/notifications/edit/{id}")
    public String editNotification(@PathVariable("id") Long id,
                                   @RequestParam("title") String title,
                                   @RequestParam("content") String content,
                                   @RequestParam("priority") Boolean priority,
                                   @RequestParam("targetType") String targetType,
                                   @RequestParam("status") String status){
        notificationService.updateNotification(id,title,content,targetType,status,priority);
        return "redirect:/manager/notifications";
    }
    @PostMapping("/notifications/delete/{id}")
    public String deleteNotification(@PathVariable("id") Long id,RedirectAttributes ra){
        boolean result=notificationService.deleteNotification(id);
        if(result) ra.addFlashAttribute("success", "Xóa thông báo thành công.");
        else ra.addFlashAttribute("error", "Hãy thay đổi trạng thái thông báo trước khi thực hiện hành động xoá");
        return "redirect:/manager/notifications";
    }
}
