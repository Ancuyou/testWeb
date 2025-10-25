package it.ute.QAUTE.repository;

import it.ute.QAUTE.entity.NotificationReceiver;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationReceiverRepository extends JpaRepository<NotificationReceiver, Integer> {
    @Query("SELECT nr FROM NotificationReceiver " +
            "nr JOIN FETCH nr.notification n " +
            "JOIN FETCH nr.receiver r " +
            "WHERE r.accountID=:accountId " +
            "ORDER BY n.createdDate DESC")
    List<NotificationReceiver> findByAccountId(Long accountId);
    NotificationReceiver findById(Long id);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM NotificationReceiver nr WHERE nr.notification.notificationID=:notificationId")
    void deleteAllByNotificationId(Long notificationId);
}
