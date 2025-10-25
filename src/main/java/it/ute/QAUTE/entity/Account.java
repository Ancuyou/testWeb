package it.ute.QAUTE.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
@Entity
@Getter
@Setter
@Builder
@Table(name="Account")
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(
  generator = ObjectIdGenerators.PropertyGenerator.class, 
  property = "accountID"
)
@EntityListeners(AuditingEntityListener.class)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccountID")
    private int accountID;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "ProfileID", referencedColumnName = "ProfileID", nullable = false, unique = true)
    private Profiles profile;

    @Column(name = "Username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "Password", nullable = false, length = 255)
    private String password;

    @Column(name = "Email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "IsBlock", nullable = false)
    private boolean isBlock = false;

    @Enumerated(EnumType.STRING) 
    @Column(name = "Role", nullable = false)
    private Role role;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CreatedDate", updatable = false)
    private Date createdDate;

    @Column(name="SecurityLevel")
    private int securityLevel=0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="LockUntil")
    private Date lockUntil;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LevelEventAt")
    private Date levelEventAt;

    public enum Role {
        Admin,
        Manager,
        Consultant,
        User,
    }
    public Account orElseThrow(Object object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'orElseThrow'");
    }
}
