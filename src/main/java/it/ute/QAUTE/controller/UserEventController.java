package it.ute.QAUTE.controller;

import it.ute.QAUTE.exception.AppException;
import it.ute.QAUTE.exception.ErrorCode;
import it.ute.QAUTE.entity.*;
import it.ute.QAUTE.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Arrays;

@Controller
@RequestMapping("/user/events")
public class UserEventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentService departmentService;

    @GetMapping("")
    public String listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Integer departmentId,
            Model model,
            Principal principal) {
        Account account = accountService.findUserByUsername(principal.getName());

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").ascending());

        Page<Event> events;
        if (keyword != null && !keyword.trim().isEmpty()) {
            events = eventService.searchEvents(keyword, pageable);
        } else if (type != null || mode != null || departmentId != null) {
            Event.EventType eventType = type != null ? Event.EventType.valueOf(type) : null;
            Event.EventMode eventMode = mode != null ? Event.EventMode.valueOf(mode) : null;
            events = eventService.filterEvents(eventType, eventMode, Event.EventStatus.Approved,
                    departmentId, null, pageable);
        } else {
            events = eventService.findUpcomingEvents(pageable);
        }

        model.addAttribute("account", account);
        model.addAttribute("events", events.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", events.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedMode", mode);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("eventTypes", Arrays.asList(Event.EventType.values()));
        model.addAttribute("eventModes", Arrays.asList(Event.EventMode.values()));
        model.addAttribute("departments", departmentService.findAll());

        return "pages/user/events/list";
    }

    @GetMapping("/{id}")
    public String eventDetails(
            @PathVariable Integer id,
            Model model,
            Principal principal) {
        Account account = accountService.findUserByUsername(principal.getName());
        User user = userService.findByProfileId(account.getProfile().getProfileID())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Event event = eventService.findById(id);

        // Check if user already registered
        boolean isRegistered = event.getRegistrations().stream()
                .anyMatch(r -> r.getUser().getUserID() == user.getUserID() &&
                        r.getStatus() != EventRegistration.RegistrationStatus.Cancelled);

        model.addAttribute("account", account);
        model.addAttribute("event", event);
        model.addAttribute("isRegistered", isRegistered);
        model.addAttribute("canRegister", event.canRegister() && !isRegistered);

        return "pages/user/events/details";
    }

    @PostMapping("/register/{id}")
    public String registerForEvent(
            @PathVariable Integer id,
            @RequestParam(required = false) String note,
            Principal principal,
            RedirectAttributes ra) {
        try {
            Account account = accountService.findUserByUsername(principal.getName());
            User user = userService.findByProfileId(account.getProfile().getProfileID())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            eventService.registerForEvent(id, user, note);

            ra.addFlashAttribute("success", true);
            ra.addFlashAttribute("successMessage", "Đăng ký sự kiện thành công!");

            return "redirect:/user/events/" + id;

        } catch (AppException e) {
            ra.addFlashAttribute("error", true);
            ra.addFlashAttribute("errorMessage", "Đăng ký thất bại: " + e.getMessage());
            return "redirect:/user/events/" + id;
        }
    }


    @GetMapping("/my-registrations")
    public String myRegistrations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            Model model,
            Principal principal) {
        Account account = accountService.findUserByUsername(principal.getName());
        User user = userService.findByProfileId(account.getProfile().getProfileID())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by("registeredAt").descending());

        Page<EventRegistration> registrations;
        if (status != null && !status.isEmpty()) {
            EventRegistration.RegistrationStatus regStatus = EventRegistration.RegistrationStatus.valueOf(status);
            registrations = eventService.findUserRegistrations(user, pageable);
        } else {
            registrations = eventService.findUserRegistrations(user, pageable);
        }

        model.addAttribute("account", account);
        model.addAttribute("registrations", registrations.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", registrations.getTotalPages());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("registrationStatuses",
                Arrays.asList(EventRegistration.RegistrationStatus.values()));

        return "pages/user/events/my-registrations";
    }

    @PostMapping("/registrations/{id}/cancel")
    public String cancelRegistration(
            @PathVariable Integer id,
            @RequestParam(required = false) String reason,
            Principal principal,
            RedirectAttributes ra) {
        try {
            Account account = accountService.findUserByUsername(principal.getName());
            User user = userService.findByProfileId(account.getProfile().getProfileID())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            eventService.cancelRegistration(id, user, reason);

            ra.addFlashAttribute("success", true);
            ra.addFlashAttribute("successMessage", "Đã hủy đăng ký thành công!");

        } catch (AppException e) {
            ra.addFlashAttribute("error", true);
            ra.addFlashAttribute("errorMessage", "Hủy đăng ký thất bại: " + e.getMessage());
        }

        return "redirect:/user/events/my-registrations";
    }

    @PostMapping("/registrations/{id}/feedback")
    public String submitFeedback(
            @PathVariable Integer id,
            @RequestParam Integer rating,
            @RequestParam(required = false) String feedback,
            Principal principal,
            RedirectAttributes ra) {
        try {
            Account account = accountService.findUserByUsername(principal.getName());
            User user = userService.findByProfileId(account.getProfile().getProfileID())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            eventService.submitFeedback(id, user, rating, feedback);

            ra.addFlashAttribute("success", true);
            ra.addFlashAttribute("successMessage", "Cảm ơn bạn đã đánh giá!");

        } catch (Exception e) {
            ra.addFlashAttribute("error", true);
            ra.addFlashAttribute("errorMessage", "Gửi đánh giá thất bại: " + e.getMessage());
        }

        return "redirect:/user/events/my-registrations";
    }
}