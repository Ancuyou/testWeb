package it.ute.QAUTE.service;

import it.ute.QAUTE.dto.MessageDTO;
import it.ute.QAUTE.entity.Messages;
import it.ute.QAUTE.entity.Messages.MessageType;
import it.ute.QAUTE.entity.Profiles;
import it.ute.QAUTE.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;
    
    public Messages saveMessage(MessageDTO messageDTO) {
        Messages message = new Messages();
        message.setSenderID(messageDTO.getSenderID());
        message.setReceiverID(messageDTO.getReceiverID());
        message.setContent(messageDTO.getContent());
        
        try {
            if (messageDTO.getType().equals("TEXT")) {
                message.setType(MessageType.text);
            } else {
                message.setType(MessageType.valueOf(messageDTO.getType()));
            }
        } catch (IllegalArgumentException e) {
            message.setType(MessageType.text);
        }
        
        message.setStatus(messageDTO.getStatus());
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        
        return messageRepository.save(message);
    }
    
    public List<Messages> getChatHistory(Integer senderId, Integer receiverId) {
        return messageRepository.findChatHistory(senderId, receiverId);
    }

    public List<Messages> getRecentChats(Integer profileId) {
        return messageRepository.findRecentChats(profileId);
    }

    public List<Profiles> getAllChatUsers(int profileID) {
        System.out.println("Fetching chat users for profile ID: " + profileID);
       List<Messages> recentChats = getRecentChats(profileID);
       Map<Integer, Profiles> userMap = new HashMap<>();
       for (Messages message : recentChats) {
           userMap.put(message.getSenderID(), message.getSender());
           userMap.put(message.getReceiverID(), message.getReceiver());
       }
        userMap.remove(profileID);
        List<Profiles> result = userMap.values().stream().collect(Collectors.toList());
        return result;
    }

    public Messages findById(Long messageId) {
        return messageRepository.findById(messageId).orElse(null);
    }

    public Messages save(Messages message) {
        return messageRepository.save(message);
    }

}