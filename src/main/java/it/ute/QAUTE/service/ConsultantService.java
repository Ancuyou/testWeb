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

import java.util.Comparator;
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

    public List<ConsultantDTO> getConsultantsWithSortingAndFilter(
            String sortBy,
            String timeRange) { // Mặc định là 'all' hoặc null

        List<Consultant> consultants = consultantRepository.findAllWithProfiles();
        // Xác định khoảng thời gian bắt đầu dựa trên timeRange
        LocalDateTime startDate = calculateStartDate(timeRange);
        LocalDateTime endDate = LocalDateTime.now();
        List<ConsultantDTO> dtos = consultants.stream().map(consultant -> {
            Profiles profile = consultant.getProfile();
            Account account = accountRepository.findByProfile_ProfileID(profile.getProfileID());

            ConsultantDTO dto = new ConsultantDTO();
            // ... (gán các thuộc tính cơ bản như cũ) ...
            dto.setConsultantID(consultant.getConsultantID());
            dto.setProfileID(profile.getProfileID());
            dto.setFullName(profile.getFullName());
            dto.setAvatar(profile.getAvatar());
            dto.setExperienceYears(consultant.getExperienceYears());

            if (account != null) {
                dto.setAccountID(account.getAccountID());
                Boolean onlineStatus = onlineCache.getIfPresent(account.getAccountID());
                dto.setIsOnline(onlineStatus != null && onlineStatus);
                dto.setOnlineStatusString(accountService.isAccountOnline(account.getAccountID()));

                // Tính toán stats dựa trên startDate và endDate
                try {
                    dto.setTotalAnswers(answerRepository.countConsultantAllAnswers(consultant.getConsultantID(), startDate, endDate));
                    Double avgTime = answerRepository.averageConsultantResponseTime(consultant.getConsultantID(), startDate, endDate);
                    // Chuyển đổi từ giờ sang phút nếu cần, hoặc giữ nguyên giờ
                    dto.setAvgResponseTime(avgTime != null ? avgTime : 0.0); // Giả sử đơn vị là giờ
                    dto.setTotalUsersAnswered(answerRepository.countDistinctUsersAnsweredByConsultant(consultant.getConsultantID(), startDate, endDate));
                } catch (Exception e) {
                    System.err.println("Error fetching stats for consultant " + consultant.getConsultantID() + ": " + e.getMessage());
                    // Set giá trị mặc định khi lỗi
                    dto.setTotalAnswers(0L);
                    dto.setAvgResponseTime(0.0);
                    dto.setTotalUsersAnswered(0L);
                }
            } else {
                dto.setIsOnline(false);
                dto.setOnlineStatusString("Offline");
                dto.setTotalAnswers(0L);
                dto.setAvgResponseTime(0.0);
                dto.setTotalUsersAnswered(0L);
            }
            return dto;
        }).collect(Collectors.toList());
        // Thực hiện sắp xếp dựa trên sortBy
        sortConsultants(dtos, sortBy);
        return dtos;
    }

    // Hàm hỗ trợ tính ngày bắt đầu
    private LocalDateTime calculateStartDate(String timeRange) {
        if (timeRange == null) {
            // Mặc định, ví dụ lấy tất cả hoặc 30 ngày gần nhất
            return LocalDateTime.now().minusDays(30); // Ví dụ mặc định 30 ngày
        }
        return switch (timeRange) {
            case "7days" -> LocalDateTime.now().minusDays(7);
            case "30days" -> LocalDateTime.now().minusDays(30);
            case "90days" -> LocalDateTime.now().minusMonths(3);
            case "all" -> LocalDateTime.of(1970, 1, 1, 0, 0); // Lấy từ rất xa để bao gồm tất cả
            default -> LocalDateTime.now().minusDays(30); // Mặc định
        };
    }

    // Hàm hỗ trợ sắp xếp danh sách DTO
    private void sortConsultants(List<ConsultantDTO> dtos, String sortBy) {
        if (sortBy == null) return;
        Comparator<ConsultantDTO> comparator = switch (sortBy) {
            case "answers_desc" -> Comparator.comparing(ConsultantDTO::getTotalAnswers).reversed();
            case "answers_asc" -> Comparator.comparing(ConsultantDTO::getTotalAnswers);
            case "response_time_asc" -> Comparator.comparing(ConsultantDTO::getAvgResponseTime); // Thời gian thấp -> tốt hơn
            case "response_time_desc" -> Comparator.comparing(ConsultantDTO::getAvgResponseTime).reversed(); // Thời gian cao -> chậm hơn
            default -> null; // Không sắp xếp hoặc sắp xếp mặc định (ví dụ: theo tên)
        };
        if (comparator != null) {
            dtos.sort(comparator);
        } else {
            // Mặc định sắp xếp theo tên nếu không có tiêu chí hợp lệ
            dtos.sort(Comparator.comparing(ConsultantDTO::getFullName, String.CASE_INSENSITIVE_ORDER));
        }
    }
}