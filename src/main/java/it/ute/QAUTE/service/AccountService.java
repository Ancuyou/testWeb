package it.ute.QAUTE.service;

import it.ute.QAUTE.exception.AppException;
import it.ute.QAUTE.exception.ErrorCode;
import com.github.benmanes.caffeine.cache.Cache;
import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.Profiles;
import it.ute.QAUTE.entity.User;
import it.ute.QAUTE.repository.AccountRepository;
import it.ute.QAUTE.repository.ProfilesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private Cache<Integer, Boolean> onlineCache;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ProfilesRepository profilesRepository;

    public void changePassword(String email,String password){
        Account account=accountRepository.findByEmail(email);
        account.setPassword(authenticationService.hashed(password));
        accountRepository.save(account);
    }
    public Account findById(int id){
        return accountRepository.findByAccountID(id);
    }
    public void updateAccount(Account account){
        accountRepository.save(account);
    }
    public void createAccount(String username, String password, String email) {
        Profiles profiles = new Profiles();
        profiles.setFullName("user" + (1000 + new Random().nextInt(9000)));
        profiles.setPhone("0000000000");
        profiles.setAvatar(null);
        Account account = new Account();
        account.setUsername(username);
        account.setEmail(email);
        account.setPassword(authenticationService.hashed(password));
        account.setRole(Account.Role.User);
        account.setCreatedDate(new Date());
        account.setProfile(profiles);
        User user = new User();
        user.setStudentCode("123");
        user.setProfile(profiles);
        user.setRoleName(User.Role.SinhVien);
        profiles.setUser(user);
        profiles.setAccount(account);
        accountRepository.save(account);
    }

    public Account findUserByUsername(String username){
        return accountRepository.findByUsername(username);
    }
    
    public Profiles getProfileByUsername(String username) {
        Account account = accountRepository.findByUsername(username);
        if (account != null) {
            return account.getProfile();
        }
        return null;
    }

    public Account findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }
    public Page<Account> searchByKeywordAndRole(String search, Account.Role role, Pageable pageable){
        return accountRepository.searchByKeywordAndRole(search, role, pageable);
    }

    public Page<Account> searchUserByKeywordAndRoleName(String search, Pageable pageable){
        return accountRepository.searchUserByKeywordAndRoleName(search, Account.Role.User, pageable);
    }
    public Page<Account> findAccountByRoleAndUserRole(User.Role roleName, Pageable pageable){
        return accountRepository.findAccountByRoleAndUserRole(roleName, pageable);
    }
    // ??? wtf
    public Page<Account> findAccountByRole(Account.Role role, Pageable pageable){
        if (role == Account.Role.User) {
            return accountRepository.findAccountByUser(role, pageable);
        }
        return accountRepository.getListAccount(role, pageable);
    }

    public Account insertAccount(Account account){
        account.setCreatedDate(new Date());
        return accountRepository.save(account);
    }
    public Account blockOrOpenAccount(Integer id){
        Account acc = accountRepository.findByAccountID(id);
        if (acc.isBlock()){
            acc.setBlock(Boolean.FALSE);
            accountRepository.save(acc);
            return acc;
        }
        else {
            acc.setBlock(Boolean.TRUE);
            accountRepository.save(acc);
            return acc;
        }
    }
    public Account findAccountByID(Integer id){
        return accountRepository.findByAccountID(id);
    }

    @Transactional
    public Account createManagerOrConsultant(Account account, String password, MultipartFile avatarFile) {
        if (accountRepository.findByUsername(account.getUsername()) != null) {
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }
        if (accountRepository.findByEmail(account.getEmail()) != null) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        account.setPassword(passwordEncoder.encode(password));
        account.setCreatedDate(new Date());
        Account acc = accountRepository.save(account);  // save lan 1

        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarFileName = fileStorageService.storeFile(avatarFile,acc.getProfile().getAvatar(), acc.getAccountID());
            acc.getProfile().setAvatar(avatarFileName);
        }
        return accountRepository.save(acc);
    }

    @Transactional
    public Account editManagerOrConsultant(Account account,String pass, MultipartFile avatarFile) {
        if (accountRepository.existsByUsernameAndAccountIDNot(account.getUsername(), account.getAccountID())) {
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }
        if (accountRepository.existsByEmailIgnoreCaseAndAccountIDNot(account.getEmail(), account.getAccountID())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        account.setPassword(passwordEncoder.encode(pass));
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarFileName = fileStorageService.storeFile(avatarFile,account.getProfile().getAvatar(), account.getAccountID());
            account.getProfile().setAvatar( avatarFileName);
        }
        return accountRepository.save(account);
    }
    public void deleteAccount(Integer id) {
        try {
            accountRepository.deleteById(id);
        } catch (AppException e) {
            throw new AppException(ErrorCode.ERROR_DELETED);
        }
    }

    public void save(Account account) {
        accountRepository.save(account);
    }
    public void updateAccountOffline(Integer id){
        Profiles profiles=profilesRepository.findByAccountId(Long.valueOf(id));
        profiles.setOnlineAt(new Date());
        profilesRepository.save(profiles);
    }
    public String isAccountOnline(Integer id){
        Boolean status = onlineCache.getIfPresent(id);
        if (status != null && status) {
            return "Online";
        }
        Profiles profiles = profilesRepository.findByAccountId(Long.valueOf(id));
        if (profiles == null || profiles.getOnlineAt() == null) {
            return "Offline (unknown)";
        }
        Date lastOnline = profiles.getOnlineAt();
        Date now = new Date();
        long diffInMillis = now.getTime() - lastOnline.getTime();
        long diffInMinutes = diffInMillis / (60 * 1000);
        if (diffInMinutes < 1) {
            return "Vừa mới offline";
        } else if (diffInMinutes < 60) {
            return "Offline " + diffInMinutes + " phút trước";
        } else if (diffInMinutes < 1440) {
            long hours = diffInMinutes / 60;
            return "Offline " + hours + " giờ trước";
        } else {
            long days = diffInMinutes / 1440;
            return "Offline " + days + " ngày trước";
        }
    }
    public List<Integer> listUserOnline(){
        List<Integer> allUserIds = new ArrayList<>(onlineCache.asMap().keySet());
        return allUserIds;
    }
}
