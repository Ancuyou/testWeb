package it.ute.QAUTE.service;

import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.Notification;
import it.ute.QAUTE.entity.NotificationReceiver;
import it.ute.QAUTE.repository.AccountRepository;
import it.ute.QAUTE.repository.NotificationReceiverRepository;
import it.ute.QAUTE.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private NotificationReceiverRepository notificationReceiverRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    public Page<Notification> findAllNotifications(Pageable pageable) {
        return notificationRepository.findAll(pageable);
    }
    public Notification findNotificationByNotificationReceiverId(Long id) {
        NotificationReceiver notificationReceiver=notificationReceiverRepository.findById(id);
        if(!notificationReceiver.isRead()) {
            notificationReceiver.setRead(true);
            notificationReceiverRepository.save(notificationReceiver);
        }
        return notificationReceiver.getNotification();
    }
    public List<NotificationReceiver> findNotificationByAccountId(long receiverId){
        return notificationReceiverRepository.findByAccountId(receiverId);
    }
    public boolean deleteNotification(Long id){
        Notification notification=notificationRepository.findById(Math.toIntExact(id)).orElse(null);
        if(notification==null || notification.getStatus().equals("PUBLISHED")){
            return false;
        }
        notificationRepository.delete(notification);
        return true;
    }
    public void updateNotification(Long id,String title, String content, String targetType,String status,boolean is_priority) {
        Notification notification = notificationRepository.findByNotificationID(id);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetType(Notification.NotificationTarget.valueOf(targetType));
        notification.setStatus(status);
        notification.set_priority(is_priority);
        Notification savedNotification = notificationRepository.save(notification);
        sendByRole(savedNotification,targetType,status);
        if(status.equals("DRAFT")) deleteNotificationReceiverByNotificationId(id);
    }
    public void deleteNotificationReceiverByNotificationId(Long notificationId){
        notificationReceiverRepository.deleteAllByNotificationId(notificationId);
    }
    public void createNotification(Account sender, String title, String content, String targetType,String status,boolean is_priority){
        Notification notification = new Notification();
        notification.setContent(content);
        notification.setSender(sender);
        notification.set_priority(is_priority);
        notification.setTitle(title);
        notification.setTargetType(Notification.NotificationTarget.valueOf(targetType));
        notification.setStatus(status);
        notification.setCreatedDate(new Date());
        Notification savedNotification = notificationRepository.save(notification);
        sendByRole(savedNotification,targetType,status);
    }
    public void sendByRole(Notification savedNotification,String targetType,String status){
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            List<Account> receivers;
            if ("ALL".equalsIgnoreCase(targetType)) {
                receivers=accountRepository.findAllExcludeAdmin();
            }else {
                receivers=accountRepository.findByRoleExcludeAdmin(Account.Role.valueOf(targetType));
            }
            for (Account receiver : receivers) {
                NotificationReceiver  notificationReceiver = new NotificationReceiver();
                notificationReceiver.setReceiver(receiver);
                notificationReceiver.setRead(false);
                notificationReceiver.setNotification(savedNotification);
                notificationReceiverRepository.save(notificationReceiver);
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(receiver.getUsername()),
                        "/queue/notifications",
                        savedNotification.getTitle() + ": " + savedNotification.getContent()
                );
            }
            System.out.println("✅ Gửi thông báo tới " + receivers.size() + " người dùng.");
        }else {
            System.out.println("lưu nháp");
        }
    }

    public Notification findNotificationById(Integer id) {
        return notificationRepository.findById(id).orElse(null);
    }
}
