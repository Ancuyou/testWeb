package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private int userID;

    @OneToOne
    @JoinColumn(name = "ProfileID", referencedColumnName = "ProfileID")
    private Profiles profile;

    @Column(name = "StudentCode", length = 20)
    private String studentCode;

    @Column(name = "RoleName", nullable = false)  // dùng để xác minh danh tinh khong co tac dung autho
    @Enumerated(EnumType.STRING)
    private Role roleName;

    public enum Role {
        SinhVien,
        HocSinh,
        PhuHuynh,
        CuuSinhVien,
        Khac
    }
}
