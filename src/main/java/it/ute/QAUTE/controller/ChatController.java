package it.ute.QAUTE.controller;

import it.ute.QAUTE.dto.ConsultantDTO;
import it.ute.QAUTE.dto.MessageDTO;
import it.ute.QAUTE.dto.UserDTO;
import it.ute.QAUTE.entity.Consultant;
import it.ute.QAUTE.entity.Messages;
import it.ute.QAUTE.entity.Profiles;
import it.ute.QAUTE.entity.User;
import it.ute.QAUTE.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ChatController {
    @Autowired
    private MessageService messageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

  
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload MessageDTO messageDTO) {
        // Save message using the service
        Messages savedMessage = messageService.saveMessage(messageDTO);
        
        // Broadcast to both sender and receiver channels
        String senderChannel = "/topic/chat/" + messageDTO.getSenderID() + "/" + messageDTO.getReceiverID();
        String receiverChannel = "/topic/chat/" + messageDTO.getReceiverID() + "/" + messageDTO.getSenderID();
        
        messagingTemplate.convertAndSend(senderChannel, savedMessage);
        messagingTemplate.convertAndSend(receiverChannel, savedMessage);
    }


    @GetMapping("/api/chat/history")
    @ResponseBody
    public List<Messages> getChatHistory(
            @RequestParam("senderId") Integer senderId,
            @RequestParam("receiverId") Integer receiverId) {
        return messageService.getChatHistory(senderId, receiverId);
    }
    

    @GetMapping("/api/chat/users")
    @ResponseBody
    public List<UserDTO> getChatUsers(@RequestParam("profileId") Integer profileId) {
        System.out.println("API /api/chat/users called with profileId: " + profileId);
        List<Profiles> chatUsers = messageService.getAllChatUsers(profileId);
        System.out.println("Found " + chatUsers.size() + " profiles in chat history");
        for (Profiles profile : chatUsers) {
            User user = profile.getUser();
            System.out.println("Profile ID: " + profile.getProfileID() + ", Full Name: " + profile.getFullName() + ", User: " + (user != null ? "exists" : "null"));
        }
        
        List<UserDTO> results = chatUsers.stream()
            .map(profile -> {
                User user = profile.getUser();
                System.out.println("Processing profile: " + profile.getProfileID() + ", fullName: " + profile.getFullName() + ", user: " + (user != null ? "found" : "null"));
                if(user == null) {
                    return null; 
                }
                UserDTO dto = new UserDTO();
                dto.setUserID(user.getUserID());
                dto.setProfileID(profile.getProfileID());
                dto.setFullName(profile.getFullName());
                dto.setAvatar(profile.getAvatar());
                dto.setIsOnline(false);
                return dto;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
        
        System.out.println("Returning " + results.size() + " users after filtering");
        return results;
    }
 
    @GetMapping("/api/chat/consultants")
    @ResponseBody
    public List<ConsultantDTO> getChatConsultants(@RequestParam("profileId") Integer profileId) {
        List<Profiles> chatConsultants = messageService.getAllChatUsers(profileId);
        
        return chatConsultants.stream()
            .map(profile -> {
                Consultant consultant = profile.getConsultant();
                if (consultant != null) {
                    ConsultantDTO dto = new ConsultantDTO();
                    dto.setConsultantID(consultant.getConsultantID());
                    dto.setProfileID(profile.getProfileID());
                    dto.setFullName(profile.getFullName());
                    dto.setAvatar(profile.getAvatar());
                    dto.setExperienceYears(consultant.getExperienceYears());
                    dto.setIsOnline(false);
                    return dto;
                }
                return null;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }
    @PostMapping("/api/chat/recall")
    @ResponseBody
    public Map<String, Object> recallMessage(@RequestParam("messageId") Long messageId, 
                                            @RequestParam("userId") Integer userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Messages message = messageService.findById(messageId);
            
            if (message == null) {
                response.put("success", false);
                response.put("message", "Tin nhắn không tồn tại");
                return response;
            }
            
            // Chỉ người gửi mới được thu hồi
            if (!message.getSenderID().equals(userId)) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thu hồi tin nhắn này");
                return response;
            }
            
            // Kiểm tra thời gian (ví dụ: chỉ thu hồi được trong 15 phút)
            LocalDateTime now = LocalDateTime.now();
            long minutesDiff = java.time.Duration.between(message.getCreatedAt(), now).toMinutes();
            if (minutesDiff > 15) {
                response.put("success", false);
                response.put("message", "Chỉ có thể thu hồi tin nhắn trong vòng 15 phút");
                return response;
            }
            
            // Thu hồi tin nhắn
            message.setIsRecalled(true);
            message.setContent("Tin nhắn đã được thu hồi");
            messageService.save(message);
            
            // Gửi thông báo qua WebSocket
            String senderChannel = "/topic/chat/" + message.getSenderID() + "/" + message.getReceiverID();
            String receiverChannel = "/topic/chat/" + message.getReceiverID() + "/" + message.getSenderID();
            
            Map<String, Object> recallNotification = new HashMap<>();
            recallNotification.put("type", "RECALL");
            recallNotification.put("messageID", messageId);
            
            messagingTemplate.convertAndSend(senderChannel, recallNotification);
            messagingTemplate.convertAndSend(receiverChannel, recallNotification);
            
            response.put("success", true);
            response.put("message", "Thu hồi tin nhắn thành công");
            return response;
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return response;
        }
    }
}