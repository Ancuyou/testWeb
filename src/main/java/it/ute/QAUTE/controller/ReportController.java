package it.ute.QAUTE.controller;

import java.security.Principal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.ute.QAUTE.entity.Account;

import it.ute.QAUTE.entity.Report;
import it.ute.QAUTE.service.AccountService;
import it.ute.QAUTE.service.ReportService;

@Controller
@RequestMapping("/reports")
public class ReportController {
    
    @Autowired
    private ReportService reportService;

    @Autowired
    private AccountService accountService;
    
    @PostMapping("/create")
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
                    .contentType(contentType)
                    .contentId(contentId)
                    .reason(reason)
                    .description(description)
                    .reporter(reporter)
                    .status(Report.ReportStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            reportService.save(report);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Báo cáo của bạn đã được gửi. Chúng tôi sẽ xem xét trong thời gian sớm nhất.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Có lỗi xảy ra khi gửi báo cáo. Vui lòng thử lại.");
        }
        if(reporter.getRole().equals("CONSULTANT")) {
            return "redirect:/consultant/questions";
        }
        return "redirect:/user/questions";
    }
}