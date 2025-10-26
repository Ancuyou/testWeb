package it.ute.QAUTE.controller;

import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.User;
import it.ute.QAUTE.service.QuestionLikeService;
import it.ute.QAUTE.service.AccountService;
import it.ute.QAUTE.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionLikeController {

    private final QuestionLikeService questionLikeService;
    private final AccountService accountService;
    private final UserService userService;

    @PostMapping("/{questionId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Integer questionId,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        Account account = accountService.findByUsername(authentication.getName());
        if (account == null || account.getProfile() == null) {
            return ResponseEntity.status(401).build();
        }
        // Lấy User từ Profile
        User user = userService.findByProfileId(account.getProfile().getProfileID()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(403).build();
        }
        boolean liked = questionLikeService.toggleLike(questionId, user);

        Map<String, Object> response = new HashMap<>();
        response.put("liked", liked);
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{questionId}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Integer questionId) {
        questionLikeService.incrementViews(questionId);
        return ResponseEntity.ok().build();
    }
}

