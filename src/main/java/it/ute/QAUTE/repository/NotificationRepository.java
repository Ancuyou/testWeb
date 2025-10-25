package it.ute.QAUTE.repository;

import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Integer> {
    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.sender s " +
            "JOIN FETCH s.profile")
    Page<Account> findAccountByUser(Account.Role role, Pageable pageable);
    Notification findByNotificationID(Long id);
}
