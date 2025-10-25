package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "Manager")
public class Manager {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int managerId;
    @Column(name="secretPin", nullable = false)
    private String secretPin;
    @OneToOne
    @JoinColumn(name = "ProfileID", referencedColumnName = "ProfileID")
    private Profiles profile;
}
