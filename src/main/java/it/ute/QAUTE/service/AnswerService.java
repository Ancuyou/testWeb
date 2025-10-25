package it.ute.QAUTE.service;

import it.ute.QAUTE.entity.Answer;
import it.ute.QAUTE.repository.AnswerRepository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AnswerService {

    @Autowired
    private AnswerRepository answerRepository;

    public void saveAnswer(Answer answer) {
        answerRepository.save(answer);
    }

    public List<Answer> getAllAnswersByConsultant(Integer consultantId) {
        return answerRepository.findByConsultant_ConsultantID(consultantId);
    }

    public Page<Answer> getAnswersHistoryByConsultant(Integer consultantId, Integer timeRange, String keyword, Pageable pageable) {
        LocalDateTime cutoffDate = null;
        if (timeRange != null) {
            cutoffDate = LocalDateTime.now().minusDays(timeRange);
        }
        return answerRepository.findAnswersHistoryByConsultant(consultantId, cutoffDate, keyword, pageable);
    }

    public List<Answer> getAnswersByQuestionId(Integer questionId) {
        return answerRepository.findByQuestion_QuestionID(questionId);
    }

    public List<Answer> getAllAnswers() {
        return answerRepository.findAll();
    }
    public long countAnswersForUser(it.ute.QAUTE.entity.User user) {
        return answerRepository.countByQuestionUser(user);
    }
}