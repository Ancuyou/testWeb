package it.ute.QAUTE.configuration;

import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.Consultant;
import it.ute.QAUTE.entity.Profiles;
import it.ute.QAUTE.entity.User;
import it.ute.QAUTE.repository.AccountRepository;
import it.ute.QAUTE.repository.ConsultantRepository;
import it.ute.QAUTE.repository.ProfilesRepository;
import it.ute.QAUTE.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ConsultantRepository consultantRepository;
    @Autowired
    ProfilesRepository profilesRepository;
    @Bean
    ApplicationRunner applicationRunner(AccountRepository accountRepository){
        return args -> {
            if(accountRepository.findByUsername("admin") == null){
                Profiles profile = new Profiles();
                profile.setFullName("Administrator");
                profile.setPhone("0000000000");
                profile.setAvatar(null);
                Account account = new Account();
                account.setUsername("admin");
                account.setPassword(passwordEncoder.encode("admin"));
                account.setEmail("admin@gmail.com");
                account.setRole(Account.Role.Admin);
                account.setProfile(profile);
                accountRepository.save(account);
                log.warn("Admin account created: username=admin, password=admin. Please change it!");
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
        };
    }
}
