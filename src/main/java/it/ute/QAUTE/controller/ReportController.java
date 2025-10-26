package it.ute.QAUTE.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import it.ute.QAUTE.entity.*;
import it.ute.QAUTE.repository.AccountRepository;
import it.ute.QAUTE.repository.AnswerRepository;
import it.ute.QAUTE.repository.MessageRepository;
import it.ute.QAUTE.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.ute.QAUTE.service.AccountService;
import it.ute.QAUTE.service.ReportService;

@Controller
public class ReportController {
    
    @Autowired
    private ReportService reportService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private MessageRepository  messageRepository;


    
    @PostMapping("/reports/create")
    public String createReport(
            @RequestParam String contentType,
            @RequestParam Long contentId,
            @RequestParam Report.ReportReason reason,
            @RequestParam(required = false) String description,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        Account reporter = accountService.findByUsername(principal.getName());
        try {
            System.out.println("reason"+ reason);
            Report report = Report.builder()
                    .reason(reason)
                    .description(description)
                    .contentType(contentType)
                    .contentId(contentId)
                    .reporter(reporter)
                    .status(Report.ReportStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            reportService.save(report);
            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Báo cáo của bạn đã được gửi. Chúng tôi sẽ xem xét trong thời gian sớm nhất.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Có lỗi xảy ra khi gửi báo cáo. Vui lòng thử lại.");
        }
        if(reporter.getRole().equals(Account.Role.Consultant)) {
            return "redirect:/consultant/questions";
        }
        return "redirect:/user/questions";
    }

    @GetMapping("/manager/user-reports")
    public String listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) Report.ReportReason reason,
            @RequestParam(required = false) Report.ReportStatus status,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);

        String finalContentType = (contentType != null && contentType.isEmpty()) ? null : contentType;

        Page<Report> reportPage = reportService.searchReports(
                startDate, endDate, finalContentType, reason, status, pageable
        );

        long pendingCount = reportService.countByStatus(Report.ReportStatus.PENDING);

        model.addAttribute("reportPage", reportPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reportPage.getTotalPages());
        model.addAttribute("pendingCount", pendingCount);

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("contentType", contentType);
        model.addAttribute("reason", reason);
        model.addAttribute("status", status);

        model.addAttribute("active", "reports-user");

        return "pages/manager/userReports";
    }


    @PostMapping("/manager/user-reports/approve/{id}")
    public String approveReport(
            @PathVariable("id") Long reportId,
            @RequestParam String contentType,
            @RequestParam Long contentId,
            RedirectAttributes redirectAttributes) {

        try {
            reportService.approveReport(reportId, contentType, contentId);
            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage", "Đã chấp nhận báo cáo #" + reportId + " thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi chấp nhận báo cáo #" + reportId + ".");
        }
        return "redirect:/manager/user-reports";
    }

    @PostMapping("/manager/user-reports/reject/{id}")
    public String rejectReport(
            @PathVariable("id") Long reportId,
            RedirectAttributes redirectAttributes) {

        try {
            reportService.rejectReport(reportId);
            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối báo cáo #" + reportId + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi từ chối báo cáo #" + reportId + ".");
        }

        return "redirect:/manager/user-reports";
    }
}