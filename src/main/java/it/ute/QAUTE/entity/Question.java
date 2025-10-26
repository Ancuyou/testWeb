package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Formula;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "Question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "QuestionID")
    private Integer questionID;

    // ... (các trường khác giữ nguyên)
    @Column(name = "Title", nullable = false, length = 200)
    private String title;

    @Column(name = "Content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "DateSend", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dateSend;

    @Column(name = "Views", columnDefinition = "INT DEFAULT 0")
    private int views = 0;

    @Column(name = "FileAttachment", length = 255)
    private String fileAttachment;

    @Column(name = "IsToxic", nullable = false)
    private boolean isToxic = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DepartmentID", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "FieldID"
    )
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Field field;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private QuestionStatus status = QuestionStatus.Pending;

    @OneToMany(mappedBy = "question", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @OrderBy("dateAnswered ASC")
    private Set<Answer> answers;

    @Formula("(SELECT COUNT(*) FROM Answer a WHERE a.QuestionID = QuestionID)")
    private int answerCount;

    public enum QuestionStatus {
        Pending,
        Answered,
        Approved,
        Rejected
    }

    @Column(name = "Likes", columnDefinition = "INT DEFAULT 0")
    private int likes = 0;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionLike> questionLikes = new HashSet<>();
}