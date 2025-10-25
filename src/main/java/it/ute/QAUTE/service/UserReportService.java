package it.ute.QAUTE.service;


import it.ute.QAUTE.dto.UserReportDTO;
import it.ute.QAUTE.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserReportService {
    @Autowired
    private UserRepository userRepository;

    public Long getTotalUsers(LocalDateTime startDate, LocalDateTime endDate) {
        return userRepository.countAllUsers(startDate, endDate);
    }

    public List<UserReportDTO> getUsersByRole(LocalDateTime startDate, LocalDateTime endDate) {
        return userRepository.getUsersByRole(startDate, endDate);
    }

    public Long getActiveUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);  // 30 ngay gan nhat
        return userRepository.countActiveUsers(cutoff);
    }

    public List<UserReportDTO> getTop10Users(LocalDateTime startDate, LocalDateTime endDate) {
        return userRepository.getTopUsersByQuestions(PageRequest.of(0, 10));
    }
}
