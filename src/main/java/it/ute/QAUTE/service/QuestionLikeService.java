package it.ute.QAUTE.service;

import it.ute.QAUTE.entity.Question;
import it.ute.QAUTE.entity.QuestionLike;
import it.ute.QAUTE.entity.User;
import it.ute.QAUTE.repository.QuestionLikeRepository;
import it.ute.QAUTE.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuestionLikeService {

    private final QuestionLikeRepository questionLikeRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public boolean toggleLike(Integer questionId, User user) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Optional<QuestionLike> existingLike = questionLikeRepository.findByQuestionAndUser(question, user);

        if (existingLike.isPresent()) {
            // Unlike
            questionLikeRepository.delete(existingLike.get());
            question.setLikes((int) questionLikeRepository.countByQuestion(question));
            questionRepository.save(question);
            return false;
        } else {
            // Like
            QuestionLike like = new QuestionLike();
            like.setQuestion(question);
            like.setUser(user);
            like.setLikedAt(LocalDateTime.now());
            questionLikeRepository.save(like);
            question.setLikes((int) questionLikeRepository.countByQuestion(question));
            questionRepository.save(question);
            return true;
        }
    }

    public long getLikeCount(Integer questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));
        // Cách 1: Dùng field `likes` đã tính sẵn (nhanh hơn)
         return question.getLikes();
        // Cách 2: Đếm trực tiếp từ bảng Like (chính xác hơn nếu có lỗi đồng bộ)
        // return questionLikeRepository.countByQuestion(question);
    }

    public boolean isLikedByUser(Integer questionId, User user) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        return questionLikeRepository.existsByQuestionAndUser(question, user);
    }

    @Transactional
    public void incrementViews(Integer questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        question.setViews(question.getViews() + 1);
        questionRepository.save(question);
    }

    public long getTotalLikesForUser(User user) {
        Long totalLikes = questionRepository.sumLikesByUser(user);
        return totalLikes != null ? totalLikes : 0L; // Trả về 0 nếu user chưa có câu hỏi nào
    }
}

