package it.ute.QAUTE.controller;

import com.nimbusds.jose.JOSEException;
import it.ute.QAUTE.entity.Notification;
import it.ute.QAUTE.entity.NotificationReceiver;
import it.ute.QAUTE.service.AuthenticationService;
import it.ute.QAUTE.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AuthenticationService authenticationService;
    @GetMapping("/user")
    public String getNotificationsByAccount(HttpSession session, Model model, HttpServletRequest request)
            throws ParseException, JOSEException {
        int accountId = Math.toIntExact(authenticationService.getCurrentUserId(session));
        List<NotificationReceiver> notifications = notificationService.findNotificationByAccountId(accountId);
        long unreadCount = notifications.stream()
                .filter(n -> !n.isRead())
                .count();
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        return "fragments/userDropDown :: notificationItems";
    }
    @GetMapping("/detail")
    public String notificationDetail(
            @RequestParam("receiverId") Long receiverId,
            Model model
    ) {
        Notification notification=notificationService.findNotificationByNotificationReceiverId(receiverId);
        model.addAttribute("notification", notification);
        return "fragments/notificationModal :: modal";
    }
}
