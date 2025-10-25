package it.ute.QAUTE.service;

import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.Profiles;
import it.ute.QAUTE.entity.User;
import it.ute.QAUTE.repository.AccountRepository;
import it.ute.QAUTE.repository.ProfilesRepository;
import it.ute.QAUTE.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProfilesRepository profilesRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    public Optional<User> findByProfileId(Integer profileId) {
        return userRepository.findByProfile_ProfileID(profileId);
    }
    public Profiles getCurrentUserProfile(String username) {
        Account account = accountRepository.findByUsername(username);
        Profiles profiles = account.getProfile();
        return profiles;
    }
    
    public Profiles getProfileById(Integer profileId) {
        return profilesRepository.findById(profileId)
            .orElseThrow(() -> new RuntimeException("Profile not found with ID: " + profileId));
    }
    public Map<String,String> mapRole(){
        Map<String,String> roleLabels = new LinkedHashMap<>();
        roleLabels.put(User.Role.SinhVien.name(),   "Sinh viên");
        roleLabels.put(User.Role.HocSinh.name(),    "Học sinh");
        roleLabels.put(User.Role.PhuHuynh.name(),   "Phụ huynh");
        roleLabels.put(User.Role.CuuSinhVien.name(),"Cựu sinh viên");
        roleLabels.put(User.Role.Khac.name(),       "Khác");
        return roleLabels;
    }
}
