package it.ute.QAUTE.controller;

import com.nimbusds.jose.JOSEException;
import it.ute.QAUTE.entity.*;
import it.ute.QAUTE.exception.AppException;
import it.ute.QAUTE.exception.ErrorCode;
import it.ute.QAUTE.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@Controller
@RequestMapping("/manager/events")
public class ManagerEventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ConsultantService consultantService;

    @GetMapping("")
    public String listAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer consultantId,
            @RequestParam(required = false) Integer departmentId,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Event> events;

        boolean isStatusFilter = status != null && !status.isEmpty();
        boolean isTypeFilter = type != null && !type.isEmpty();
        boolean isConsultantFilter = consultantId != null;
        boolean isDepartmentFilter = departmentId != null;
        if (isStatusFilter || isTypeFilter || isConsultantFilter || isDepartmentFilter) {
            Event.EventStatus eventStatus = isStatusFilter ? Event.EventStatus.valueOf(status) : null;
            Event.EventType eventType = isTypeFilter ? Event.EventType.valueOf(type) : null;

            events = eventService.filterEvents(eventType, null, eventStatus,
                    departmentId, consultantId, pageable);
        } else {
            events = eventService.findAllEvents(pageable);
        }
        long totalEvents = events.getTotalElements();
        long pendingEvents = eventService.countPendingEvents();
        long approvedEvents = eventService.countApprovedEvents();
        long ongoingCount = events.stream()
                .filter(e -> "Ongoing".equals(e.getStatus().toString()))
                .count();

        model.addAttribute("ongoingCount", ongoingCount);
        model.addAttribute("events", events.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", events.getTotalPages());
        model.addAttribute("totalEvents", totalEvents);
        model.addAttribute("pendingEvents", pendingEvents);
        model.addAttribute("approvedEvents", approvedEvents);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedConsultantId", consultantId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("eventStatuses", Arrays.asList(Event.EventStatus.values()));
        model.addAttribute("eventTypes", Arrays.asList(Event.EventType.values()));
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("consultants", consultantService.getAllConsultants());

        return "pages/manager/events/list";
    }

    @GetMapping("/pending")
    public String listPendingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Event> events = eventService.findEventsByStatus(Event.EventStatus.Pending, pageable);

        model.addAttribute("events", events.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", events.getTotalPages());
        model.addAttribute("totalPending", events.getTotalElements());

        return "pages/manager/events/pending";
    }

    @GetMapping("/{id}")
    public String eventDetails(
            @PathVariable Integer id,
            Model model) {
        Event event = eventService.findById(id);
        var participants = eventService.findEventParticipants(id);

        model.addAttribute("event", event);
        model.addAttribute("participants", participants);

        return "pages/manager/events/participants";
    }

    @GetMapping("/reports")
    public String eventReports(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Model model) {
       
        LocalDate startInput = (startDate != null) ? startDate : LocalDate.now().minusMonths(1);
        LocalDate endInput = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime start = startInput.atStartOfDay();
        LocalDateTime end = endInput.atTime(LocalTime.MAX);

        long totalEventsInRange = eventService.countEventsInDateRange(start, end);
        long pendingEventsInRange = eventService.countPendingEventsInDateRange(start, end);
        long approvedEventsInRange = eventService.countApprovedEventsInDateRange(start, end);

        model.addAttribute("selectedStartDate", startInput);
        model.addAttribute("selectedEndDate", endInput);

        model.addAttribute("totalEventsInRange", totalEventsInRange);
        model.addAttribute("pendingEventsInRange", pendingEventsInRange);
        model.addAttribute("approvedEventsInRange", approvedEventsInRange);

        model.addAttribute("totalAllTime", eventService.countApprovedEvents());
        model.addAttribute("pendingAllTime", eventService.countPendingEvents());

        return "pages/manager/events/reports";
    }

    @PostMapping("/delete/{id}")
    public String deleteEvent(
            @PathVariable Integer id,
            RedirectAttributes ra) {
        try {
            eventService.deleteEvent(id);

            ra.addFlashAttribute("success", true);
            ra.addFlashAttribute("successMessage", "Đã xóa sự kiện thành công!");

        } catch (AppException e) {
            ra.addFlashAttribute("error", true);
            ra.addFlashAttribute("errorMessage", "Xóa sự kiện thất bại: " + e.getMessage());
        }

        return "redirect:/manager/events";
    }

    @PostMapping("/approve/{id}")
    public String approveEvent(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes ra,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            Object tokenObj = session.getAttribute("ACCESS_TOKEN");
            int managerId = Math.toIntExact(authenticationService.getCurrentUserId(tokenObj, request, response));

            eventService.approveEvent(id, managerId);

            ra.addFlashAttribute("success", true);
            ra.addFlashAttribute("successMessage", "Đã phê duyệt sự kiện thành công!");

        } catch (AppException | ParseException | JOSEException e) {
            ra.addFlashAttribute("error", true);
            ra.addFlashAttribute("errorMessage", "Phê duyệt thất bại: " + e.getMessage());
        }

        return "redirect:/manager/events/pending";
    }

    @PostMapping("/reject/{id}")
    public String rejectEvent(
            @PathVariable Integer id,
            @RequestParam String reason,
            HttpSession session,
            RedirectAttributes ra,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            Object tokenObj = session.getAttribute("ACCESS_TOKEN");
            int managerId = Math.toIntExact(authenticationService.getCurrentUserId(tokenObj, request, response));

            if (reason == null || reason.trim().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            eventService.rejectEvent(id, managerId, reason);

            ra.addFlashAttribute("success", true);
            ra.addFlashAttribute("successMessage", "Đã từ chối sự kiện!");

        } catch (AppException | ParseException | JOSEException e) {
            ra.addFlashAttribute("error", true);
            ra.addFlashAttribute("errorMessage", "Từ chối thất bại: " + e.getMessage());
        }

        return "redirect:/manager/events/pending";
    }

    @PostMapping("/cancel/{id}")
    public String cancelEvent(
            @PathVariable Integer id,
            @RequestParam String reason,
            RedirectAttributes ra) {
        try {
            if (reason == null || reason.trim().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            eventService.cancelEvent(id, reason);

            ra.addFlashAttribute("success", true);
            ra.addFlashAttribute("successMessage", "Đã hủy sự kiện thành công!");

        } catch (AppException e) {
            ra.addFlashAttribute("error", true);
            ra.addFlashAttribute("errorMessage", "Hủy sự kiện thất bại: " + e.getMessage());
        }

        return "redirect:/manager/events";
    }

    // ========== Xem danh sách người đăng ký ==========

    @GetMapping("/participants/{id}")
    public String viewParticipants(
            @PathVariable Integer id,
            Model model) {
        Event event = eventService.findById(id);
        var participants = eventService.findEventParticipants(id);

        model.addAttribute("event", event);
        model.addAttribute("participants", participants);

        return "pages/manager/events/participants";
    }
}