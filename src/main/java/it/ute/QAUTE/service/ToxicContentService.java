package it.ute.QAUTE.service;

import it.ute.QAUTE.Exception.AppException;
import it.ute.QAUTE.Exception.ErrorCode;
import it.ute.QAUTE.entity.Question;
import it.ute.QAUTE.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ToxicContentService {

    @Autowired
    private QuestionRepository questionRepository;

    public Page<Question> findToxicQuestionsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        if  (startDate == null || endDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
            endDate = LocalDateTime.now();
        }
        return questionRepository.findToxicQuestionsByDateRange(startDate, endDate, pageable);
    }

    public void rejectedQuestion(Integer id) {
        Question question = questionRepository.findById(id).orElseThrow( () -> new AppException(ErrorCode.QUESTION_UNEXISTED));
        question.setStatus(Question.QuestionStatus.Rejected);
        questionRepository.save(question);
    }

    public void approvedQuestion(Integer id) {
        Question question = questionRepository.findById(id).orElseThrow( () -> new AppException(ErrorCode.QUESTION_UNEXISTED));
        question.setStatus(Question.QuestionStatus.Approved);
        question.setToxic(true);
        questionRepository.save(question);
    }
}
