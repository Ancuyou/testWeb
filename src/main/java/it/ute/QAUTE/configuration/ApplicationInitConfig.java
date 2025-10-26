package it.ute.QAUTE.configuration;

import it.ute.QAUTE.entity.*;
import it.ute.QAUTE.repository.AccountRepository;
import it.ute.QAUTE.repository.DepartmentRepository;
import it.ute.QAUTE.repository.FieldRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;


@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {
    @Autowired
    PasswordEncoder passwordEncoder;
    @Bean
    ApplicationRunner applicationRunner(AccountRepository accountRepository, DepartmentRepository departmentRepository, FieldRepository fieldRepository){
        return args -> {
            // tạo admin
            if(accountRepository.findByUsername("admin") == null){
                Profiles profile = new Profiles();
                profile.setFullName("Administrator");
                profile.setPhone("0000000000");
                profile.setAvatar(null);
                Account account = new Account();
                account.setUsername("admin");
                account.setPassword(passwordEncoder.encode("admin"));
                account.setEmail("admin@gmail.com");
                account.setCreatedDate(new Date());
                account.setRole(Account.Role.Admin);
                Admin admin=new Admin();
                admin.setSecretPin(passwordEncoder.encode("123456"));
                admin.setProfile(profile);
                profile.setAdmin(admin);
                profile.setAccount(account);
                account.setProfile(profile);
                accountRepository.save(account);
                log.warn("Admin account created: username=admin, password=admin. Please change it!");
            }
            // tạo manager
            if(accountRepository.findByUsername("manager") == null){
                Profiles profile = new Profiles();
                profile.setFullName("Manager");
                profile.setPhone("0000000000");
                profile.setAvatar(null);
                Account account = new Account();
                account.setUsername("manager");
                account.setPassword(passwordEncoder.encode("manager"));
                account.setEmail("manager@gmail.com");
                account.setCreatedDate(new Date());
                account.setRole(Account.Role.Manager);
                Manager manager=new Manager();
                manager.setSecretPin(passwordEncoder.encode("123456"));
                manager.setProfile(profile);
                profile.setManager(manager);
                profile.setAccount(account);
                account.setProfile(profile);
                accountRepository.save(account);
                log.warn("Manager account created: username=manager, password=manager. Please change it!");
            }
            // tạo consultant
            if(accountRepository.findByUsername("consultant") == null){
                Profiles profile = new Profiles();
                profile.setFullName("Consultant");
                profile.setPhone("0000000000");
                profile.setAvatar(null);
                Account account = new Account();
                account.setUsername("consultant");
                account.setPassword(passwordEncoder.encode("consultant"));
                account.setEmail("consultant@gmail.com");
                account.setCreatedDate(new Date());
                account.setRole(Account.Role.Consultant);
                Consultant consultant = new Consultant();
                consultant.setExperienceYears(1);
                consultant.setProfile(profile);
                profile.setConsultant(consultant);
                profile.setAccount(account);
                account.setProfile(profile);
                accountRepository.save(account);
                log.warn("✅ Consultant account created: username=consultant, password=consultant. Please change it!");
            }
            if(accountRepository.findByUsername("user") == null) {
                Profiles profile = new Profiles();
                profile.setFullName("User");
                profile.setPhone("0000000000");
                profile.setAvatar(null);
                Account account = new Account();
                account.setUsername("user");
                account.setPassword(passwordEncoder.encode("user"));
                account.setEmail("23112074@student.hcmute.edu.vn");
                account.setRole(Account.Role.User);
                account.setCreatedDate(new Date());
                account.setProfile(profile);
                User user = new User();
                user.setStudentCode("23112074");
                user.setProfile(profile);
                //user.setRoleName("Sinh Viên");
                user.setRoleName(User.Role.SinhVien);
                profile.setUser(user);
                profile.setAccount(account);
                accountRepository.save(account);
                log.warn("✅ User account created: username=user, password=user. Please change it!");
            }
            // --- TẠO KHOA VÀ LĨNH VỰC ---
            if (departmentRepository.count() == 0) {
                log.info("Seeding Departments and Fields...");

                // 1. Tạo các Khoa
                Department cntt = new Department();
                cntt.setDepartmentName("Khoa Công nghệ thông tin");
                cntt.setType(Department.DepartmentType.Faculty);
                departmentRepository.save(cntt);

                Department ckctm = new Department();
                ckctm.setDepartmentName("Khoa Cơ khí Chế tạo máy");
                ckctm.setType(Department.DepartmentType.Faculty);
                departmentRepository.save(ckctm);

                Department ddt = new Department();
                ddt.setType(Department.DepartmentType.Faculty);
                ddt.setDepartmentName("Khoa Điện - Điện tử");
                departmentRepository.save(ddt);

                // 2. Tạo các Lĩnh vực và liên kết với Khoa
                createField("Công nghệ phần mềm", Set.of(cntt), fieldRepository);
                createField("Hệ thống thông tin", Set.of(cntt), fieldRepository);
                createField("An toàn thông tin", Set.of(cntt), fieldRepository);

                createField("Cơ điện tử", Set.of(ckctm, ddt), fieldRepository);
                createField("Kỹ thuật cơ khí", Set.of(ckctm), fieldRepository);

                createField("Kỹ thuật điều khiển và tự động hóa", Set.of(ddt), fieldRepository);
                createField("Hệ thống nhúng", Set.of(ddt), fieldRepository);

                log.info("✅ Departments and Fields seeded successfully.");
            }
        };
    }
    private void createField(String fieldName, Set<Department> departments, FieldRepository fieldRepository) {
        Field field = new Field();
        field.setFieldName(fieldName);
        field.setDepartments(new HashSet<>(departments));
        fieldRepository.save(field);
    }
}
