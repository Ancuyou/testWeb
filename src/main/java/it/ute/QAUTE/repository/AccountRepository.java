package it.ute.QAUTE.repository;

import it.ute.QAUTE.entity.Account;
import it.ute.QAUTE.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Account findUserByEmail(String email);
    Account findByAccountID(Integer id);
    Account findByUsername(String username);
    Account findByEmail(String email);
    boolean existsByEmailIgnoreCaseAndAccountIDNot(String email, Integer accountID);
    // search consultant and manager
    boolean existsByUsernameAndAccountIDNot(String username, Integer accountID);
    @Query("SELECT a FROM Account a JOIN a.profile p WHERE a.role != 'Admin' AND a.role=:role AND " +
            "(LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Account> searchByKeywordAndRole(String keyword, Account.Role role, Pageable pageable);
    // search User
    @EntityGraph(attributePaths = {"profile", "profile.user"})
    @Query("SELECT a FROM Account a JOIN a.profile p JOIN p.user u " +
            "WHERE a.role = 'User' AND u.roleName = :userRole")
    Page<Account> findAccountByRoleAndUserRole(@Param("userRole") User.Role userRole,
                                               Pageable pageable);
    @Query("""
    SELECT a FROM Account a
    JOIN a.profile p JOIN p.user u
    WHERE a.role != 'Admin'
      AND a.role = :role
      AND (
          LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(p.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    Page<Account> searchUserByKeywordAndRoleName(
            @Param("keyword") String keyword,
            @Param("role") Account.Role role,
            Pageable pageable
    );


    @Query("SELECT a FROM Account a JOIN a.profile p JOIN p.user WHERE a.role=:role")
    Page<Account> findAccountByUser(Account.Role role, Pageable pageable);
    @Query("SELECT a FROM Account a JOIN a.profile p WHERE a.role != 'Admin' AND a.role=:role")
    Page<Account> getListAccount(Account.Role role, Pageable pageable);
    @EntityGraph(attributePaths = "profile")
    @Query("SELECT a FROM Account a WHERE a.accountID=:id")
    Account findByAccountIDWithProfiles(int id);
    @Query("SELECT a FROM Account a JOIN a.profile p WHERE a.role != 'Admin' AND a.role = :role")
    List<Account> findByRoleExcludeAdmin(Account.Role role);
    @Query("SELECT a FROM Account a WHERE a.role != 'Admin'")
    List<Account> findAllExcludeAdmin();
    @Query("SELECT a FROM Account a WHERE a.profile.profileID = :profileId")
    Account findByProfile_ProfileID(@Param("profileId") Integer profileId);

    /*@Modifying
    @Transactional
    @Query("UPDATE Account a SET a.isBlock = true WHERE a.accountID = :id")
    void blockAccount(@Param("id") Integer id);*/
}
