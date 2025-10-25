package it.ute.QAUTE.controller;

import com.nimbusds.jose.JOSEException;
import it.ute.QAUTE.Exception.AppException;
import it.ute.QAUTE.entity.*;
import it.ute.QAUTE.service.*;
import lombok.extern.slf4j.Slf4j;
import it.ute.QAUTE.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Arrays;
import java.text.ParseException;
import java.util.List;


@Slf4j
@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private NotificationService notificationService;


    @GetMapping("/consultants")
    public String listConsultants(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by("accountID").descending());
        Page<Account> data;
        if (q != null && !q.equals("")) {
            data = accountService.searchByKeywordAndRole(q, Account.Role.Consultant, pageable);
        } else {
            data = accountService.findAccountByRole(Account.Role.Consultant, pageable);
        }

        model.addAttribute("accounts", data.getContent());
        model.addAttribute("q", q);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", data.getTotalPages());
        model.addAttribute("totalItems", data.getTotalElements());
        model.addAttribute("pageSizeOptions", new int[]{5, 10, 15, 20});
        model.addAttribute("active", "consultants");

        return "pages/admin/consultants";
    }

    @GetMapping("/consultant/edit/{id}")
    public String editConsultant(
            @PathVariable Integer id,
            Model model
    ) {
        Account account = adminService.findById(id);
        model.addAttribute("account", account);
        return "pages/admin/editConsultant";
    }
    @GetMapping("/user/edit/{id}")
    public String editUser(
            @PathVariable Integer id,
            Model model
    ) {
        Account account = adminService.findById(id);
        model.addAttribute("account", account);
        return "pages/admin/editUser";
    }

    @GetMapping("/manager/edit/{id}")
    public String editManager(
            @PathVariable Integer id,
            Model model) {
        Account account = adminService.findById(id);
        model.addAttribute("account", account);
        return "pages/admin/editManager";
    }

    @GetMapping("/user/add")
    public String addUserForm(Model model) {
        Profiles profile = new Profiles();
        User user = new User();

        profile.setUser(user);
        user.setProfile(profile);

        Account account = new Account();
        account.setProfile(profile);

        model.addAttribute("account", account);
        return "pages/admin/addUser";
    }

    @GetMapping("/consultant/add")
    public String addConsultant(Model model) {
        Profiles profile = new Profiles();
        Consultant consultant = new Consultant();

        profile.setConsultant(consultant);
        consultant.setProfile(profile);

        Account account = new Account();
        account.setProfile(profile);

        model.addAttribute("account", account);

        return "pages/admin/addConsultant";
    }

    @GetMapping("/manager/add")
    public String addManager(Model model) {
        Account acc = new Account();
        acc.setProfile(new Profiles());
        acc.getProfile().setConsultant(new Consultant());
        model.addAttribute("account", acc);
        return "pages/admin/addManager";
    }

    @GetMapping("/departments")
    public String listDepartments(Model model,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "5") int size,
                                  @RequestParam(required = false) String q) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by("departmentID").ascending());
        Page<Department> pageData = departmentService.searchNameDepartment(q, pageable);

        model.addAttribute("departments", pageData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("q", q);

        model.addAttribute("pageSizeOptions", new int[]{5, 10, 20, 5});

        model.addAttribute("activeSection", "departments");

        return "pages/admin/departments";
    }

    @GetMapping("/profile")
    public String showAdminProfile(@CookieValue(value = "REFRESH_TOKEN", required = false) String refresh,
                                   Model model) {
        if (refresh == null || refresh.isBlank()) return "redirect:/auth/login";

        try {
            var jwt = authenticationService.verifyToken(refresh);
            String username = jwt.getJWTClaimsSet().getSubject();
            Account acc = accountService.findUserByUsername(username);

            model.addAttribute("account", acc);
            return "pages/admin/profile";

        } catch (Exception e) {
            return "redirect:/auth/login";
        }
    }
    @GetMapping("/department/edit/{id}")
    public String updateDepartment(
            @PathVariable Integer id,
            Model model){
        model.addAttribute("department", departmentService.findById(id)); // fix lai neu tra ve null thi bao loi he thong
        model.addAttribute("allDepartments", departmentService.findAllNoPaging());
        return "pages/admin/editDepartment";
    }

    @GetMapping("/department/insert")
    public String insertDepartment(Model model){
        model.addAttribute("department", new Department());
        model.addAttribute("allDepartments", departmentService.findAllNoPaging());
        return "pages/admin/addDepartment";
    }

    @PostMapping("/profile")
    public String ProfileUpdate(
            @ModelAttribute("account") Account form,
            @RequestParam("newPassword") String newPassword,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            RedirectAttributes redirectAttributes){
        try {
            accountService.editManagerOrConsultant(form, newPassword, avatarFile);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sửa thất bại: " + e.getMessage());
            return "redirect:/admin/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sửa thất bại: " + e.getMessage());
            return "redirect:/admin/profile";
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage",
                "Sửa thành công! Manager " + form.getProfile().getFullName());
        return "redirect:/admin/profile";
    }

    @PostMapping("/consultant/update")
    public String updateConsultant(
            @ModelAttribute("account") Account form,
            @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile) throws IOException {

        try {
            accountService.editManagerOrConsultant(form, newPassword, avatarFile);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sửa thất bại: " + e.getMessage());
            return "redirect:/admin/consultant/edit/" + form.getAccountID();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sửa thất bại: " + e.getMessage());
            return "redirect:/admin/consultant/edit/" + form.getAccountID();
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage",
                "Sửa thành công! Consultant " + form.getProfile().getFullName());
        return "redirect:/admin/consultants";
    }
    @PostMapping("/user/update")
    public String updateUser(
            @ModelAttribute("account") Account form,
            @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile) throws IOException {

        try {
            accountService.editManagerOrConsultant(form, newPassword, avatarFile);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sửa thất bại: " + e.getMessage());
            return "redirect:/admin/user/edit/" + form.getAccountID();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sửa thất bại: " + e.getMessage());
            return "redirect:/admin/user/edit/" + form.getAccountID();
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage",
                "Sửa thành công! User " + form.getProfile().getFullName());
        return "redirect:/admin/users";
    }


    @PostMapping("/consultant/insert")
    public String insertConsultant(@ModelAttribute("account") Account form,
                                   @RequestParam("newPassword") String newPassword,
                                   RedirectAttributes redirectAttributes,
                                   @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile) throws IOException {
        try {
            if (form.getProfile() != null && form.getProfile().getConsultant() != null) {
                form.getProfile().getConsultant().setProfile(form.getProfile());
            }
            form.setRole(Account.Role.Consultant);
            accountService.createManagerOrConsultant(form, newPassword, avatarFile);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thêm consultant thất bại: " + e.getMessage());
            return "redirect:/admin/consultant/add";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thêm consultant thất bại: " + e.getMessage());
            return "redirect:/admin/consultant/add";
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đăng ký thành công! Consultant: " + form.getProfile().getFullName());
        return "redirect:/admin/consultants";
    }
    @PostMapping("/user/insert")
    public String insertUser(@ModelAttribute("account") Account form,
                                   @RequestParam("password") String newPassword,
                                   RedirectAttributes redirectAttributes,
                                   @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile) throws IOException {
        try {
            if (form.getProfile() != null && form.getProfile().getUser() != null) {
                form.getProfile().getUser().setProfile(form.getProfile());
            }
            form.setRole(Account.Role.User);
            accountService.createManagerOrConsultant(form, newPassword, avatarFile);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thêm user thất bại: " + e.getMessage());
            return "redirect:/admin/user/add";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thêm user thất bại: " + e.getMessage());
            return "redirect:/admin/user/add";
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đăng ký thành công! User: " + form.getProfile().getFullName());
        return "redirect:/admin/users";
    }

    @PostMapping("/manager/insert")
    public String insertManager(@ModelAttribute("account") Account form,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                RedirectAttributes redirectAttributes){
        try {
            form.setRole(Account.Role.Manager);
            accountService.createManagerOrConsultant(form, newPassword, avatarFile);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thêm manager thất bại: " + e.getMessage());
            return "redirect:/admin/manager/add";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thêm manager thất bại: " + e.getMessage());
            return "redirect:/admin/manager/add";
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage",
                "Thêm thành công! Manager: " + form.getProfile().getFullName());
        return "redirect:/admin/managers";
    }

    @PostMapping("/department/update")
    public String updateDepartment(@ModelAttribute Department department,
                                   RedirectAttributes redirectAttributes,
                                   @RequestParam(value = "parent.departmentID", required = false) Integer parentId) {
        if (parentId == null) {
            department.setParent(null);
        }
        try {
            departmentService.updateDepartment(department);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Chỉnh sửa thất bại: " + e.getMessage());
            return "redirect:/admin/department/edit/" + department.getDepartmentID();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Chỉnh sửa thất bại: " + e.getMessage());
            return "redirect:/admin/department/edit/" + department.getDepartmentID();
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage",
                "Chỉnh sửa thành công! Department: " + department.getDepartmentName());
        return "redirect:/admin/departments";
    }

    @PostMapping("/department/insert")
    public String insertDepartment(@ModelAttribute Department department,
                                   RedirectAttributes redirectAttributes,
                                   @RequestParam(value = "parent.departmentID", required = false) Integer parentId){
        if (parentId == null) {
            department.setParent(null);
        }
        try {
            departmentService.updateDepartment(department);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thêm Department thất bại: " + e.getMessage());
            return "redirect:/admin/department/insert";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thêm Department thất bại: " + e.getMessage());
            return "redirect:/admin/department/insert";
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage",
                "Thêm thành công! Department: " + department.getDepartmentName());
        return "redirect:/admin/departments";
    }
    @PostMapping("/account/delete/{id}")
    public String AccountDelete(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {

        Account acc = accountService.findAccountByID(id);
        try {
            accountService.deleteAccount(id);
            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Xóa thành công! Manager " + acc.getProfile().getFullName());

            if (acc.getRole() == Account.Role.Manager) {
                return "redirect:/admin/managers";
            }
            if (acc.getRole() == Account.Role.Consultant) {
                return "redirect:/admin/consultants";
            }
            return "redirect:/admin/users";

        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Xóa thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Xóa thất bại: " + e.getMessage());
        }

        if (acc.getRole() == Account.Role.Manager) {
            return "redirect:/admin/managers";
        }
        if (acc.getRole() == Account.Role.Consultant) {
            return "redirect:/admin/consultants";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/department/delete/{id}")
    public String deleteDepartment(@PathVariable("id") Integer id){
        departmentService.deleteDepartment(id);
        return "redirect:/admin/departments";
    }
    @GetMapping("/refresh-tokens")
    public String listRefreshTokens(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            Model model) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        Page<RefreshToken> tokenPage;
        if (status != null && !status.isEmpty()) {
            if ("active".equals(status)) {
                tokenPage = adminService.findActiveTokens(q, pageable);
            } else if ("expired".equals(status)) {
                tokenPage = adminService.findExpiredTokens(q, pageable);
            } else {
                tokenPage = adminService.searchTokens(q, pageable);
            }
        } else {
            tokenPage = adminService.searchTokens(q, pageable);
        }

        long activeTokens = adminService.countActiveTokens();
        long expiredTokens = adminService.countExpiredTokens();
        long totalTokens = adminService.countAllTokens();

        model.addAttribute("refreshTokens", tokenPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", tokenPage.getTotalPages());
        model.addAttribute("totalItems", tokenPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("pageSizeOptions", new int[]{5, 10, 15, 20});
        model.addAttribute("q", q);
        model.addAttribute("status", status);

        model.addAttribute("activeTokens", activeTokens);
        model.addAttribute("expiredTokens", expiredTokens);
        model.addAttribute("totalTokens", totalTokens);

        return "pages/admin/refreshTokens";
    }

    @GetMapping("/managers")
    public String listManagers(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by("accountID").descending());
        Page<Account> data;
        if (q != null && !q.equals("")) {
            data = accountService.searchByKeywordAndRole(q, Account.Role.Manager, pageable);
        } else {
            data = accountService.findAccountByRole(Account.Role.Manager, pageable);
        }

        model.addAttribute("accounts", data.getContent());
        model.addAttribute("q", q);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", data.getTotalPages());
        model.addAttribute("totalItems", data.getTotalElements());
        model.addAttribute("pageSizeOptions", new int[]{5, 10, 15, 20});
        model.addAttribute("active", "managers");

        return "pages/admin/managers";
    }


    @PostMapping("/manager/update")
    public String updateManager(
            @ModelAttribute("account") Account form,
            @RequestParam("newPassword") String newPassword,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            RedirectAttributes redirectAttributes){
        try {
            accountService.editManagerOrConsultant(form, newPassword, avatarFile);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sửa thất bại: " + e.getMessage());
            return "redirect:/admin/manager/edit/" + form.getAccountID();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sửa thất bại: " + e.getMessage());
            return "redirect:/admin/manager/edit/" + form.getAccountID();
        }
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage",
                "Sửa thành công! Manager " + form.getProfile().getFullName());
        return "redirect:/admin/managers";
    }

    @PostMapping("/refresh-token/revoke/{id}")
    public String  revokeRefreshToken(@PathVariable("id") String id){
        adminService.revokeToken(id);
        return "redirect:/admin/refresh-tokens";
    }

    @PostMapping("/refresh-tokens/cleanup")
    public String cleanupRefreshTokens(){
        adminService.deleteExpiredTokens();
        return "redirect:/admin/refresh-tokens";
    }

    @GetMapping("/account/block/{id}")
    public String blockAccount(@PathVariable("id") Integer id) {
        Account acc = accountService.blockOrOpenAccount(id);  // du thua du lieu tra ve void thay vi Account
        if (acc.getRole() == Account.Role.Manager) {
            return "redirect:/admin/managers";
        }
        if (acc.getRole() == Account.Role.Consultant) {
            return "redirect:/admin/consultants";
        }
        return "redirect:/admin/users";  // neu duoc dung javasript doi trang thai khong nhat thiet phai load lai toan trang gay giam hieu suat

    }
    @GetMapping("/users")
    public String listUsers(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "SinhVien") String roleName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdDate").descending());
        Page<Account> accountPage;
        // search keywork
        if (q != null &&!q.isEmpty()) {
            accountPage = accountService.searchUserByKeywordAndRoleName(q, pageable);
        } else {
            accountPage = accountService.findAccountByRoleAndUserRole(User.Role.valueOf(roleName), pageable);
        }
        model.addAttribute("accounts", accountPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", accountPage.getTotalPages());
        model.addAttribute("totalItems", accountPage.getTotalElements());
        model.addAttribute("q", q);
        model.addAttribute("size", size);
        model.addAttribute("pageSizeOptions", List.of(5, 10, 20, 50));
        model.addAttribute("userRoles", Arrays.asList(User.Role.values()));
        model.addAttribute("selectedRole", roleName);
        return "pages/admin/users";
    }
    @GetMapping("/notifications")
    public String listNotifications(@RequestParam(defaultValue = "") String q,
                                    @RequestParam(defaultValue = "") String status,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    Model model){
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by("createdDate").descending());
        Page<Notification> notifications=notificationService.findAllNotifications(pageable);
        model.addAttribute("notifications", notifications.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifications.getTotalPages());
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        return "pages/admin/notifications";
    }
    @GetMapping("/notifications/new")
    public String addNotification(Model model){
        model.addAttribute("notification", new Notification());
        return "pages/admin/addNotification";
    }

    @GetMapping("/notifications/edit/{id}")
    public String editNotification(@PathVariable("id") Integer id, Model model){
        model.addAttribute("notification", notificationService.findNotificationById(id));
        return "pages/admin/addNotification";
    }

    @PostMapping("/notifications/add")
    public String addNotifications(@RequestParam("title") String title,
                                   @RequestParam("content") String content,
                                   @RequestParam("targetType") String targetType,
                                   @RequestParam("status") String status,
                                   HttpSession session) throws ParseException, JOSEException {
        int id = Math.toIntExact(authenticationService.getCurrentUserId(session));
        Account account = accountService.findById(id);
        notificationService.createNotification(account, title, content, targetType, status,true);
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/edit/{id}")
    public String editNotification(@PathVariable("id") Long id,
                                   @RequestParam("title") String title,
                                   @RequestParam("content") String content,
                                   @RequestParam("targetType") String targetType,
                                   @RequestParam("status") String status){
        notificationService.updateNotification(id,title,content,targetType,status,true);
        return "redirect:/admin/notifications";
    }
    @PostMapping("/notifications/delete/{id}")
    public String deleteNotification(@PathVariable("id") Long id,RedirectAttributes ra){
        boolean result=notificationService.deleteNotification(id);
        if(result) ra.addFlashAttribute("success", "Xóa thông báo thành công.");
        else ra.addFlashAttribute("error", "Hãy thay đổi trạng thái thông báo trước khi thực hiện hành động xoá");
        return "redirect:/admin/notifications";
    }
}
