package it.ute.QAUTE.service;

import it.ute.QAUTE.dto.HotTopicDTO;
import it.ute.QAUTE.entity.Department;
import it.ute.QAUTE.entity.Field;
import it.ute.QAUTE.entity.Question;
import it.ute.QAUTE.repository.DepartmentRepository;
import it.ute.QAUTE.repository.FieldRepository;
import it.ute.QAUTE.repository.QuestionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private FieldRepository fieldRepository;

    public List<Question> getAllQuestions() {
        return questionRepository.findAll(Sort.by(Sort.Direction.DESC, "dateSend"));
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public List<Field> getAllFields() {
        return fieldRepository.findAll();
    }

    public void saveQuestion(Question question) {
        questionRepository.save(question);
    }
    
    public Question getQuestionById(Integer questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại với ID: " + questionId));
    }

    public Page<Question> filterQuestions(Integer departmentId, Integer fieldId, String userName, String status, Pageable pageable) {
        boolean hasDept = departmentId != null;
        boolean hasField = fieldId != null;
        boolean hasUser = userName != null && !userName.isEmpty();
        boolean hasStatus = status != null && !status.isEmpty();

        Question.QuestionStatus statusEnum = null;
        if (hasStatus) {
            statusEnum = Question.QuestionStatus.valueOf(status);
        }

        if (hasUser) {
            return questionRepository.findQuestionsByUserName(userName, pageable);
        }

        if (hasDept && hasField && hasStatus) {
            return questionRepository.findQuestionsByDeptAndField(departmentId, fieldId, statusEnum, pageable);
        }
        if (hasDept && !hasField && hasStatus) {
            return questionRepository.findQuestionsByDeptAndStatus(departmentId, statusEnum, pageable);
        }
        if (hasDept && !hasField) {
            return questionRepository.findQuestionsByDept(departmentId, pageable);
        }
        if (!hasDept && !hasField && hasStatus) {
            return questionRepository.findQuestionsByStatus(statusEnum, pageable);
        }

        return questionRepository.findAllWithUser(pageable);
    }


    public Question findById(Integer questionId) {
        return questionRepository.findById(questionId).orElse(null);
    }

    public Question save(Question question) {
        return questionRepository.save(question);
    }

    public List<HotTopicDTO> getTop5HotTopics() {
        Pageable topFive = PageRequest.of(0, 5);
        List<HotTopicDTO> topDepartments = questionRepository.findTopDepartments(topFive);
        List<HotTopicDTO> topFields = questionRepository.findTopFields(topFive);

        return Stream.concat(topDepartments.stream(), topFields.stream())
                .sorted(Comparator.comparing(HotTopicDTO::getCount).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    public long countQuestionsByUser(it.ute.QAUTE.entity.User user) {
        return questionRepository.countByUser(user);
    }

    public List<Question> getTop3RecentQuestionsByUser(it.ute.QAUTE.entity.User user) {
        return questionRepository.findTop3ByUserOrderByDateSendDesc(user);
    }

    public List<Question> getTop5RecentCommunityQuestions() {
        return questionRepository.findTop5ByOrderByDateSendDesc();
    }

    public List<Question> getAllQuestionsByUserSortedByDate(it.ute.QAUTE.entity.User user) {
        return questionRepository.findByUserOrderByDateSendDesc(user);
    }
    public List<Field> getFieldsByDepartmentId(Integer departmentId) {
        return fieldRepository.findAllByDepartments_departmentID(departmentId);
    }

    public Page<Question> searchAndFilterQuestions(
            Integer departmentId,
            Integer fieldId,
            String keyword,
            String sortBy,
            Pageable pageable) {

        // Cập nhật Pageable với thông tin sắp xếp
        Sort sort = switch (sortBy) {
            case "views" -> Sort.by(Sort.Direction.DESC, "views");
            case "answers" -> Sort.by(Sort.Direction.DESC, "answerCount");
            default -> Sort.by(Sort.Direction.DESC, "dateSend"); // Mới nhất
        };
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("departmentID"), departmentId));
            }
            if (fieldId != null) {
                predicates.add(cb.equal(root.get("field").get("fieldID"), fieldId));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern)
                ));
            }

            // Chỉ lấy câu hỏi đã được duyệt
//            predicates.add(cb.equal(root.get("status"), Question.QuestionStatus.Approved));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return questionRepository.findAll(spec, pageable);
    }
}