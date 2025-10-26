package it.ute.QAUTE.repository;

import it.ute.QAUTE.entity.Question;
import it.ute.QAUTE.entity.QuestionLike;
import it.ute.QAUTE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionLikeRepository extends JpaRepository<QuestionLike, Integer> {
    Optional<QuestionLike> findByQuestionAndUser(Question question, User user);
    boolean existsByQuestionAndUser(Question question, User user);
    long countByQuestion(Question question);
}

