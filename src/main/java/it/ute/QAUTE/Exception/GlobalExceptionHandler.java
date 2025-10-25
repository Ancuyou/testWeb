package it.ute.QAUTE.Exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public String handleAppException(AppException ex, RedirectAttributes ra) {
        // Nếu là lỗi auth thì đẩy về trang login kèm message
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("unauthorized")) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập lại.");
            return "redirect:/auth/login";
        }
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/auth/login";
    }
}
