package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "RefreshToken")
public class RefreshToken {
    @Id
    @Column(name = "Refresh_id")
    String refreshId;

    @Column(name = "Sign_key", nullable = false, length = 128, unique = true)
    String signKey;

    @Column(name = "Device_name", length = 100)
    String deviceName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "Created_at", nullable = false, updatable = false)
    Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "Expires_at", nullable = false)
    Date expiresAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AccountID", nullable = false)
    private Account account;
}
