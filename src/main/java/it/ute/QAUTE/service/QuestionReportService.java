package it.ute.QAUTE.service;

import it.ute.QAUTE.dto.QuestionReportDTO;
import it.ute.QAUTE.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuestionReportService {
    @Autowired
    private QuestionRepository questionRepository;

    public long getTotalQuestions(LocalDateTime start, LocalDateTime end) {
        return questionRepository.countAllQuestions(start, end);
    }

    public List<QuestionReportDTO> getByField(LocalDateTime start, LocalDateTime end) {
        return questionRepository.getQuestionsByField(start, end);
    }

    public List<QuestionReportDTO> getByDepartment(LocalDateTime start, LocalDateTime end) {
        return questionRepository.getQuestionsByDepartment(start, end);
    }

    public List<QuestionReportDTO> getByStatus(LocalDateTime start, LocalDateTime end) {
        return questionRepository.getQuestionsByStatus(start, end);
    }

    public List<QuestionReportDTO> getByDate(LocalDateTime start, LocalDateTime end) {
        return questionRepository.getQuestionsByDate(start, end);
    }
}
