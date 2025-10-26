package it.ute.QAUTE.service;

import it.ute.QAUTE.dto.HotTopicDTO;
import it.ute.QAUTE.dto.QuestionDTO;
import it.ute.QAUTE.entity.Department;
import it.ute.QAUTE.entity.Field;
import it.ute.QAUTE.entity.Question;
import it.ute.QAUTE.entity.User;
import it.ute.QAUTE.exception.AppException;
import it.ute.QAUTE.exception.ErrorCode;
import it.ute.QAUTE.repository.DepartmentRepository;
import it.ute.QAUTE.repository.FieldRepository;
import it.ute.QAUTE.repository.QuestionRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    public void deleteQuestion(Integer id) {
        questionRepository.deleteById(id);
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

    public Question findQuestionByIdForEditing(Integer questionId, int userId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_UNEXISTED));

        // Kiểm tra xem user hiện tại có phải là người đăng câu hỏi không
        if (question.getUser() == null || question.getUser().getUserID() != userId) {
            throw new AppException(ErrorCode.UNAUTHORIZED); // Không có quyền chỉnh sửa
        }
        // // Logic kiểm tra có thể sửa đổi không được chuyển vào DTO để trả về cho client
         if (!question.getAnswers().isEmpty() || question.getLikes() > 0 || question.getStatus() != Question.QuestionStatus.Pending) {
              throw new AppException(ErrorCode.INVALID_REQUEST);
         }
        return question;
    }


    @Transactional // Đảm bảo tất cả thay đổi được commit hoặc rollback
    public void updateQuestion(Integer questionId, int userId, QuestionDTO questionDTO) {
        Question question = findQuestionByIdForEditing(questionId, userId); // Sử dụng lại hàm kiểm tra quyền

        // Kiểm tra lại lần nữa trước khi cập nhật (phòng trường hợp có tương tác trong lúc modal mở)
        if (!question.getAnswers().isEmpty() || question.getLikes() > 0 || question.getStatus() != Question.QuestionStatus.Pending) {
            throw new AppException(ErrorCode.INVALID_REQUEST); // Thông báo lỗi chung chung hơn
        }

        // Cập nhật thông tin
        question.setTitle(questionDTO.getTitle());
        question.setContent(questionDTO.getContent());

        // Cập nhật Department
        if (questionDTO.getDepartmentId() != null) {
            Department department = departmentRepository.findById(questionDTO.getDepartmentId())
                    .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
            question.setDepartment(department);
        } else {
            throw new AppException(ErrorCode.INVALID_REQUEST); // Department là bắt buộc
        }


        // Cập nhật Field
        if (questionDTO.getFieldId() != null) {
            Field field = fieldRepository.findById(questionDTO.getFieldId())
                    .orElseThrow(() -> new AppException(ErrorCode.FIELD_NOT_FOUND));
            // Optional: Kiểm tra field có thuộc department đã chọn không
            question.setField(field);
        } else {
            question.setField(null); // Cho phép bỏ chọn field
        }

        // Xử lý file đính kèm mới (nếu có)
        MultipartFile newFile = questionDTO.getFile();
        if (newFile != null && !newFile.isEmpty()) {
            String oldAttachmentPath = question.getFileAttachment(); // Lưu lại đường dẫn file cũ

            // Lưu file mới
            String uploadDir = "src/main/resources/static/images/questions/";
            File uploadFolder = new File(uploadDir);
            if (!uploadFolder.exists()) uploadFolder.mkdirs();

            String originalFileName = newFile.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            // Tạo tên file duy nhất, ví dụ: 5charUUID_Question_UserID_QuestionID.ext
            String uuid = java.util.UUID.randomUUID().toString().substring(0, 5);
            String newFileName = uuid + "_Question_" + userId + "_" + questionId + extension;
            String newRelativePath = "/images/questions/" + newFileName;


            try {
                Files.copy(newFile.getInputStream(), Paths.get(uploadDir + newFileName), StandardCopyOption.REPLACE_EXISTING);
                question.setFileAttachment(newRelativePath); // Cập nhật đường dẫn file mới

                // Optional: Xóa file cũ sau khi đã lưu file mới thành công
                // if (oldAttachmentPath != null && !oldAttachmentPath.isEmpty()) {
                //     try {
                //         String staticPath = "src/main/resources/static";
                //         File oldFile = new File(staticPath + oldAttachmentPath);
                //         if (oldFile.exists() && !oldFile.isDirectory()) {
                //             Files.deleteIfExists(oldFile.toPath());
                //             log.info("Deleted old attachment: {}", oldAttachmentPath);
                //         }
                //     } catch (IOException e) {
                //         log.error("Could not delete old attachment: {}", oldAttachmentPath, e);
                //     }
                // }

            } catch (IOException e) {
                log.error("Could not save new attachment for question {}", questionId, e);
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Hoặc mã lỗi cụ thể hơn
            }
        }
        // Lưu thay đổi vào DB
        questionRepository.save(question);
    }
    public Page<Question> searchAndFilterUserQuestions(
            User user, // Thêm tham số user
            Integer departmentId,
            Integer fieldId,
            String keyword,
            String sortBy,
            Pageable pageable) {

        Sort sort;
        switch (sortBy) {
            case "most_liked":
                sort = Sort.by(Sort.Direction.DESC, "likes");
                break;
            case "least_liked":
                sort = Sort.by(Sort.Direction.ASC, "likes");
                break;
            case "views":
                sort = Sort.by(Sort.Direction.DESC, "views");
                break;
            case "answers":
                // Sắp xếp theo số lượng câu trả lời (cần tính toán hoặc dùng @Formula nếu muốn tối ưu)
                // Cách đơn giản là sort sau khi lấy dữ liệu hoặc sort bằng SQL phức tạp hơn
                // Tạm thời vẫn sort theo dateSend cho trường hợp này
                sort = Sort.by(Sort.Direction.DESC, "dateSend"); // Hoặc dùng "answerCount" nếu có @Formula
                break;
            case "latest":
            default:
                sort = Sort.by(Sort.Direction.DESC, "dateSend");
                break;
        }
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Luôn lọc theo user hiện tại
            predicates.add(cb.equal(root.get("user"), user));
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
            // Không cần lọc theo status Approved ở trang history
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return questionRepository.findAll(spec, pageable); // Dùng findAll với Specification
    }
}