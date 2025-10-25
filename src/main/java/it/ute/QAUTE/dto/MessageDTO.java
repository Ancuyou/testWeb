package it.ute.QAUTE.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Integer senderID;
    private Integer receiverID;
    private String content;
    private String type = "text";
    private String status = "sent";
    private LocalDateTime timestamp;
    
    public MessageDTO(Integer senderID, Integer receiverID, String content) {
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.content = content;
        this.type = "text";
        this.status = "sent";
        this.timestamp = LocalDateTime.now();
    }
}