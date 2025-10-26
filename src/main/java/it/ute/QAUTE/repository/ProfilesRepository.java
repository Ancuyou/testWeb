package it.ute.QAUTE.repository;

import it.ute.QAUTE.entity.Profiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfilesRepository extends JpaRepository<Profiles, Integer> {
    @Query("SELECT p FROM Profiles p WHERE p.account.accountID=:accountId")
    Profiles findByAccountId(Long accountId);
}