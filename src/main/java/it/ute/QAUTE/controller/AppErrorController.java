package it.ute.QAUTE.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AppErrorController {
    @GetMapping("/app-error")
    public String handleAppError(
            @RequestParam(name = "errorCode", required = false, defaultValue = "503") String errorCode,
            @RequestParam(name = "message", required = false, defaultValue = "The server is temporarily busy, try again later!") String errorMessage,
            @RequestParam(name = "errorTitle", required = false, defaultValue = "ERROR") String errorTitle,
            Model model) {

        model.addAttribute("errorCode", errorCode);
        model.addAttribute("errorTitle", errorTitle);
        model.addAttribute("errorMessage", errorMessage);

        return "pages/error";
    }
}
