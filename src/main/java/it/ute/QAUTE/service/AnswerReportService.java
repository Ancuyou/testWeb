package it.ute.QAUTE.service;

import it.ute.QAUTE.dto.AnswerReportDTO;
import it.ute.QAUTE.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AnswerReportService {
    @Autowired
    private AnswerRepository answerRepository;

    public long getTotalAnswers(LocalDateTime startDate, LocalDateTime endDate) {
        return answerRepository.countAllAnswers(startDate, endDate);
    }

    public double getAverageResponseTime(LocalDateTime startDate, LocalDateTime endDate) {
        Double avg = answerRepository.averageResponseTime(startDate, endDate);
        return avg != null ? avg : 0.0;
    }

    public List<AnswerReportDTO> getAnswersByConsultant(LocalDateTime startDate, LocalDateTime endDate) {
        return answerRepository.getAnswersByConsultant(startDate, endDate);
    }

    public List<AnswerReportDTO> getAnswersByDate(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = answerRepository.getAnswersByDateRaw(startDate, endDate);
        return results.stream()
                .map(r -> new AnswerReportDTO(
                        null,
                        ((Number) r[1]).longValue(),
                        ((java.sql.Date) r[0]).toLocalDate()
                ))
                .toList();
    }
}
