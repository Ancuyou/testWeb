package it.ute.QAUTE.service;

import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.Event;
import it.ute.QAUTE.entity.EventRegistration;
import it.ute.QAUTE.entity.Notification;
import it.ute.QAUTE.entity.NotificationReceiver;
import it.ute.QAUTE.entity.User;
import it.ute.QAUTE.repository.AccountRepository;
import it.ute.QAUTE.repository.EventRegistrationRepository;
import it.ute.QAUTE.repository.NotificationReceiverRepository;
import it.ute.QAUTE.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
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

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    public Page<Notification> findAllNotifications(Pageable pageable) {
        return notificationRepository.findAll(pageable);
    }

    public Notification findNotificationByNotificationReceiverId(Long id) {
        NotificationReceiver notificationReceiver = notificationReceiverRepository.findById(id);
        if (!notificationReceiver.isRead()) {
            notificationReceiver.setRead(true);
            notificationReceiverRepository.save(notificationReceiver);
        }
        return notificationReceiver.getNotification();
    }

    public List<NotificationReceiver> findNotificationByAccountId(long receiverId) {
        return notificationReceiverRepository.findByAccountId(receiverId);
    }

    public Page<Notification> findNotificationsBySenderId(long senderId, Pageable pageable) {
        return notificationRepository.findNotificationsBySenderId(senderId, pageable);
    }

    public boolean deleteNotification(Long id) {
        Notification notification = notificationRepository.findById(Math.toIntExact(id)).orElse(null);
        if (notification == null || notification.getStatus().equals("PUBLISHED")) {
            return false;
        }
        notificationRepository.delete(notification);
        return true;
    }

    public void updateNotification(Long id, String title, String content, String targetType, String status,
            boolean is_priority) {
        Notification notification = notificationRepository.findByNotificationID(id);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetType(Notification.NotificationTarget.valueOf(targetType));
        notification.setStatus(status);
        notification.set_priority(is_priority);
        Notification savedNotification = notificationRepository.save(notification);
        Account account = accountRepository.findByAccountID(notification.getSender().getAccountID());
        sendByRole(savedNotification, targetType, status, account.getRole());
        if (status.equals("DRAFT"))
            deleteNotificationReceiverByNotificationId(id);
    }

    public void deleteNotificationReceiverByNotificationId(Long notificationId) {
        notificationReceiverRepository.deleteAllByNotificationId(notificationId);
    }

    public void createNotification(Account sender, String title, String content, String targetType, String status,
            boolean is_priority) {
        Notification notification = new Notification();
        notification.setContent(content);
        notification.setSender(sender);
        notification.set_priority(is_priority);
        notification.setTitle(title);
        notification.setTargetType(Notification.NotificationTarget.valueOf(targetType));
        notification.setStatus(status);
        notification.setCreatedDate(new Date());
        Notification savedNotification = notificationRepository.save(notification);
        sendByRole(savedNotification, targetType, status, sender.getRole());
    }

    public void sendByRole(Notification savedNotification, String targetType, String status, Account.Role roleSender) {
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            List<Account> receivers;
            if ("ALL".equalsIgnoreCase(targetType)) {
                if (roleSender.equals(Account.Role.Admin))
                    receivers = accountRepository.findAllExcludeAdmin();
                else
                    receivers = accountRepository.findUserAndConsultant();
            } else {
                receivers = accountRepository.findByRoleExcludeAdmin(Account.Role.valueOf(targetType));
            }
            for (Account receiver : receivers) {
                NotificationReceiver notificationReceiver = new NotificationReceiver();
                notificationReceiver.setReceiver(receiver);
                notificationReceiver.setRead(false);
                notificationReceiver.setNotification(savedNotification);
                notificationReceiverRepository.save(notificationReceiver);
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(receiver.getUsername()),
                        "/queue/notifications",
                        savedNotification.getTitle() + ": " + savedNotification.getContent());
            }
            System.out.println("✅ Gửi thông báo tới " + receivers.size() + " người dùng.");
        } else {
            System.out.println("lưu nháp");
        }
    }

    public Notification findNotificationById(Integer id) {
        return notificationRepository.findById(id).orElse(null);
    }

    public void createNotificationForSpecificUser(Account sender, Account receiver,
            String title, String content,
            boolean isPriority) {
        try {
            // Tạo notification
            Notification notification = new Notification();
            notification.setSender(sender);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setTargetType(Notification.NotificationTarget.User); // hoặc null nếu muốn
            notification.setStatus("PUBLISHED");
            notification.set_priority(isPriority);
            notification.setCreatedDate(new Date());

            Notification savedNotification = notificationRepository.save(notification);

            // Tạo NotificationReceiver cho User cụ thể
            NotificationReceiver notificationReceiver = new NotificationReceiver();
            notificationReceiver.setReceiver(receiver);
            notificationReceiver.setRead(false);
            notificationReceiver.setNotification(savedNotification);
            notificationReceiverRepository.save(notificationReceiver);

            System.out.println("-------------- thông tin người nhận: " + receiver.getUsername());

            // Gửi real-time notification qua WebSocket
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(receiver.getUsername()),
                    "/queue/notifications",
                    savedNotification.getTitle() + ": " + savedNotification.getContent());

            System.out.println("-------------- Đã gửi thông báo tới User: " + receiver.getUsername());
        } catch (Exception e) {
            System.err.println("123456- Lỗi khi gửi thông báo: " + e.getMessage());
        }
    }

    public void notifyManagersNewEvent(Event event) {
        List<Account> managers = accountRepository.findAll().stream()
                .filter(acc -> acc.getRole() == Account.Role.Manager)
                .toList();

        String title = "Sự kiện mới cần duyệt";
        String content = String.format(
                "Tư vấn viên %s đã tạo sự kiện mới: %s. Vui lòng xem xét và phê duyệt.",
                event.getConsultant().getProfile().getFullName(),
                event.getTitle());

        for (Account manager : managers) {
            createNotification(
                    manager,
                    title,
                    content,
                    "Manager",
                    "PUBLISHED",
                    true 
            );
        }
    }

    public void notifyManagersEventUpdated(Event event) {
        List<Account> managers = accountRepository.findAll().stream()
                .filter(acc -> acc.getRole() == Account.Role.Manager)
                .toList();

        String title = "Sự kiện đã được cập nhật";
        String content = String.format(
                "Tư vấn viên %s đã cập nhật sự kiện: %s. Vui lòng duyệt lại.",
                event.getConsultant().getProfile().getFullName(),
                event.getTitle());

        for (Account manager : managers) {
            createNotification(
                    manager,
                    title,
                    content,
                    "Manager",
                    "PUBLISHED",
                    false);
        }
    }

    public void notifyConsultantEventApproved(Event event) {
        Account consultant = event.getConsultant().getProfile().getAccount();

        String title = "Sự kiện đã được phê duyệt";
        String content = String.format(
                "Chúc mừng! Sự kiện '%s' của bạn đã được phê duyệt và công khai. " +
                        "Người dùng giờ đây có thể đăng ký tham gia.",
                event.getTitle());

        createNotification(
                consultant,
                title,
                content,
                "Consultant",
                "PUBLISHED",
                true);
    }

    public void notifyConsultantEventRejected(Event event, String reason) {
        Account consultant = event.getConsultant().getProfile().getAccount();

        String title = "Sự kiện bị từ chối";
        String content = String.format(
                "Rất tiếc, sự kiện '%s' của bạn đã bị từ chối. Lý do: %s. " +
                        "Bạn có thể chỉnh sửa và gửi lại.",
                event.getTitle(),
                reason);

        createNotification(
                consultant,
                title,
                content,
                "Consultant",
                "PUBLISHED",
                true);
    }

    public void notifyConsultantNewRegistration(Event event, User user) {
        Account consultant = event.getConsultant().getProfile().getAccount();

        String title = "Có người đăng ký sự kiện";
        String content = String.format(
                "%s vừa đăng ký tham gia sự kiện '%s' của bạn. " +
                        "Hiện có %d/%s người đăng ký.",
                user.getProfile().getFullName(),
                event.getTitle(),
                event.getCurrentParticipants(),
                event.getMaxParticipants() != null ? event.getMaxParticipants().toString() : "không giới hạn");

        createNotification(
                consultant,
                title,
                content,
                "Consultant",
                "PUBLISHED",
                false);
    }

    public void notifyUserRegistrationSuccess(User user, Event event) {
        Account account = user.getProfile().getAccount();

        String title = "Đăng ký sự kiện thành công";
        String content = String.format(
                "Bạn đã đăng ký thành công sự kiện '%s'. " +
                        "Thời gian: %s. Hãy đánh dấu lịch của bạn!",
                event.getTitle(),
                event.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        createNotification(
                account,
                title,
                content,
                "User",
                "PUBLISHED",
                false);
    }

    public void notifyUserEventCancelled(User user, Event event, String reason) {
        Account account = user.getProfile().getAccount();

        String title = "Sự kiện đã bị hủy";
        String content = String.format(
                "Rất tiếc, sự kiện '%s' mà bạn đã đăng ký đã bị hủy. Lý do: %s. " +
                        "Chúng tôi xin lỗi vì sự bất tiện này.",
                event.getTitle(),
                reason);

        createNotification(
                account,
                title,
                content,
                "User",
                "PUBLISHED",
                true);
    }

    public void notifyUserEventReminder(User user, Event event) {
        Account account = user.getProfile().getAccount();

        String title = "Nhắc nhở sự kiện";
        String content = String.format(
                "Sự kiện '%s' sẽ bắt đầu vào %s (còn 24 giờ nữa). " +
                        "Hãy chuẩn bị sẵn sàng!",
                event.getTitle(),
                event.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        createNotification(
                account,
                title,
                content,
                "User",
                "PUBLISHED",
                true);
    }

    public void notifyUsersEventStartingSoon(Event event) {
        List<EventRegistration> registrations = eventRegistrationRepository.findActiveRegistrations(event);

        String title = "Sự kiện sắp bắt đầu";
        String content = String.format(
                "Sự kiện '%s' sẽ bắt đầu sau 15 phút. %s",
                event.getTitle(),
                event.getMeetingLink() != null ? "Link tham gia: " + event.getMeetingLink()
                        : event.getLocation() != null ? "Địa điểm: " + event.getLocation() : "");

        for (EventRegistration registration : registrations) {
            Account account = registration.getUser().getProfile().getAccount();
            createNotification(
                    account,
                    title,
                    content,
                    "User",
                    "PUBLISHED",
                    true);
        }
    }
}
