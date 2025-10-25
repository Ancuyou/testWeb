package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Consultant")
@Getter @Setter
public class Consultant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ConsultantID")
    private int consultantID;
    
    @Column(name = "ExperienceYears", columnDefinition = "INT DEFAULT 0")
    private int experienceYears = 0;
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "ProfileID", referencedColumnName = "ProfileID")
    private Profiles profile;
}