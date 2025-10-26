package it.ute.QAUTE.controller;

import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.Answer;
import it.ute.QAUTE.entity.Consultant;
import it.ute.QAUTE.entity.Question;
import it.ute.QAUTE.service.ConsultantService;
import it.ute.QAUTE.service.NotificationService;
import it.ute.QAUTE.service.QuestionService;
import it.ute.QAUTE.service.AccountService;
import it.ute.QAUTE.service.AnswerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/consultant")
public class AnswerController {

    @Autowired
    private AnswerService answerService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ConsultantService consultantService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private QuestionService questionService;

     @PostMapping("/questions/answer")
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


        Question question = questionService.findById(questionId);
        if(question == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Câu hỏi không tồn tại.");
            return "redirect:/consultant/questions";
        }

        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setConsultant(consultant);
        answer.setContent(content);
        answer.setDateAnswered(LocalDateTime.now());

        answerService.saveAnswer(answer);
        System.out.println("Question ID: " + questionId);

        String notificationTitle = "Câu hỏi của bạn đã được trả lời";
        String notificationContent = String.format(
            "Câu hỏi '%s' của bạn đã được tư vấn viên %s trả lời.",
            question.getTitle() != null ? question.getTitle() : "câu hỏi",
            account.getProfile().getFullName()
        );

        System.out.println(notificationContent + "  notificationContent 123");
        
        notificationService.createNotificationForSpecificUser(
            account, 
            question.getUser().getProfile().getAccount(), 
            notificationTitle,
            notificationContent,
            true 
        );

        redirectAttributes.addFlashAttribute("successMessage", "Câu trả lời của bạn đã được gửi thành công!");
        return "redirect:/consultant/questions";
    }
}