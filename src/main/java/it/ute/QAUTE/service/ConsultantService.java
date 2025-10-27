package it.ute.QAUTE.service;

import it.ute.QAUTE.dto.ConsultantDTO;
import it.ute.QAUTE.entity.Account; // Thêm import
import it.ute.QAUTE.entity.Consultant;
import it.ute.QAUTE.entity.Profiles;
import it.ute.QAUTE.repository.AccountRepository; // Thêm import
import it.ute.QAUTE.repository.ConsultantRepository;
import it.ute.QAUTE.repository.AnswerRepository; // Thêm import
import it.ute.QAUTE.repository.QuestionRepository; // Thêm import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.github.benmanes.caffeine.cache.Cache; // Thêm import Cache

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConsultantService {

    @Autowired
    private ConsultantRepository consultantRepository;

    @Autowired
    private AccountRepository accountRepository; // Thêm AccountRepository

    @Autowired
    private AccountService accountService; // Thêm AccountService để lấy status

    @Autowired
    private AnswerRepository answerRepository; // Thêm AnswerRepository

    @Autowired
    private QuestionRepository questionRepository; // Thêm QuestionRepository

    @Autowired
    private Cache<Integer, Boolean> onlineCache; // Inject Online Cache

    public List<ConsultantDTO> getAllConsultants() {
        List<Consultant> consultants = consultantRepository.findAllWithProfiles();

        return consultants.stream().map(consultant -> {
            Profiles profile = consultant.getProfile();
            Account account = accountRepository.findByProfile_ProfileID(profile.getProfileID()); // Lấy Account từ ProfileID

            ConsultantDTO dto = new ConsultantDTO();
            dto.setConsultantID(consultant.getConsultantID());
            dto.setProfileID(profile.getProfileID());
            dto.setFullName(profile.getFullName());
            dto.setAvatar(profile.getAvatar());
            dto.setExperienceYears(consultant.getExperienceYears());

            if (account != null) {
                dto.setAccountID(account.getAccountID()); // Gán accountID
                // Kiểm tra trạng thái online từ Cache trước
                Boolean onlineStatus = onlineCache.getIfPresent(account.getAccountID());
                dto.setIsOnline(onlineStatus != null && onlineStatus);
                dto.setOnlineStatusString(accountService.isAccountOnline(account.getAccountID())); // Lấy chuỗi trạng thái
                // Lưu ý: Các hàm này có thể cần tối ưu nếu gọi nhiều lần
                LocalDateTime endDate = LocalDateTime.now();
                LocalDateTime startDate = endDate.minusDays(30); // Lấy stats 30 ngày gần nhất (tùy chỉnh)
                try {
                    dto.setTotalAnswers(answerRepository.countConsultantAllAnswers(consultant.getConsultantID(), startDate, endDate));

                    Double avgTime = answerRepository.averageConsultantResponseTime(consultant.getConsultantID(), startDate, endDate);
                    dto.setAvgResponseTime(avgTime != null ? avgTime : 0.0);

                    dto.setTotalUsersAnswered(answerRepository.countDistinctUsersAnsweredByConsultant(consultant.getConsultantID(), startDate, endDate));
                } catch (Exception e) {
                    // Log lỗi nếu có
                    System.err.println("Error fetching stats for consultant " + consultant.getConsultantID() + ": " + e.getMessage());
                }
            } else {
                dto.setIsOnline(false);
                dto.setOnlineStatusString("Offline");
            }
            return dto;
        }).collect(Collectors.toList());
    }
    public Optional<Consultant> findByProfileId(Integer profileId) {
        return Optional.ofNullable(consultantRepository.findByProfile_ProfileID(profileId));
    }
    public List<Consultant> findAllConsultants() {
        return consultantRepository.findAllWithProfiles();
    }

    public void updateConsultant(Consultant consultant) {
        consultantRepository.save(consultant);
    }
    
    public void saveConsultant(Consultant consultant) {
        consultantRepository.save(consultant);
    }
}