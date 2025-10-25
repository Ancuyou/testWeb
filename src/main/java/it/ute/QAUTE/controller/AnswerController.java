package it.ute.QAUTE.controller;

import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.Answer;
import it.ute.QAUTE.entity.Consultant;
import it.ute.QAUTE.entity.Question;
import it.ute.QAUTE.service.ConsultantService;
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

        // Ensure the user is a consultant
        if (account.getRole() != Account.Role.Consultant) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có tư vấn viên mới có thể trả lời.");
            return "redirect:/consultant/questions";
        }

        Consultant consultant = consultantService.findByProfileId(account.getProfile().getProfileID())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin tư vấn viên."));

        Answer answer = new Answer();
        Question question = new Question();
        question.setQuestionID(questionId);

        answer.setQuestion(question);
        answer.setConsultant(consultant);
        answer.setContent(content);
        answer.setDateAnswered(LocalDateTime.now());

        answerService.saveAnswer(answer);

        redirectAttributes.addFlashAttribute("successMessage", "Câu trả lời của bạn đã được gửi thành công!");
        return "redirect:/consultant/questions";
    }
}