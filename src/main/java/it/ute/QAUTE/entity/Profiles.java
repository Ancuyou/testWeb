package it.ute.QAUTE.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="Profiles")
@Getter
@Setter
@JsonIdentityInfo(
  generator = ObjectIdGenerators.PropertyGenerator.class, 
  property = "profileID"
)
public class Profiles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ProfileID")
    private int profileID;
    @Column(name = "FullName", nullable = false, length = 100)
    private String fullName;
    @Column(name = "Phone")
    private String phone;
    @Column(name="Avatar")
    private String avatar;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "profile", fetch = FetchType.LAZY)
    private Account account;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "profile", fetch = FetchType.LAZY)
    private Consultant consultant;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "profile", fetch = FetchType.LAZY)
    private User user;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "profile", fetch = FetchType.LAZY)
    private Admin admin;
}
