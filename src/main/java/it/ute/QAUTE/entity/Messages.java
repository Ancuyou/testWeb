package it.ute.QAUTE.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "Messages")
@JsonIdentityInfo(
  generator = ObjectIdGenerators.PropertyGenerator.class, 
  property = "messageID"
)
public class Messages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MessageID")
    private Long messageID;

    @Column(name = "SenderID", nullable = false)
    private Integer senderID;

    @Column(name = "ReceiverID", nullable = false)
    private Integer receiverID;

    @Column(name = "Content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "Type", columnDefinition = "ENUM('text','image','file') DEFAULT 'text'")
    private MessageType type = MessageType.text;

    @Column(name = "Status", length = 20)
    private String status="sent";

    @Column(name = "CreatedAt", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "SenderID", referencedColumnName = "ProfileID", insertable = false, updatable = false)
    private Profiles sender;

    @ManyToOne
    @JoinColumn(name = "ReceiverID", referencedColumnName = "ProfileID", insertable = false, updatable = false)
    private Profiles receiver;
    
    @Column(name = "is_recalled")
    private Boolean isRecalled = false;

    
    public enum MessageType {
        text, image, file
    }
}
