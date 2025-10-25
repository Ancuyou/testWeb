package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Profile;

@Entity
@Getter
@Setter
@Table(name = "Admin")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int adminId;
    @Column(name="secretPin", nullable = false)
    private String secretPin;
    @OneToOne
    @JoinColumn(name = "ProfileID", referencedColumnName = "ProfileID")
    private Profiles profile;
}
