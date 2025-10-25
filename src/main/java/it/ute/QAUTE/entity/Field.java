package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "Field")
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FieldID")
    private int fieldID;

    @Column(name = "FieldName", nullable = false, length = 100)
    private String fieldName;

    @ManyToMany
    @JoinTable(
            name = "Field_Department",
            joinColumns = @JoinColumn(name = "FieldID"),
            inverseJoinColumns = @JoinColumn(name = "DepartmentID")
    )
    private Set<Department> departments;
}