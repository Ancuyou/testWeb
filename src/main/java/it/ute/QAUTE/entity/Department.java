package it.ute.QAUTE.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "Department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DepartmentID")
    private Integer departmentID;

    @Column(name = "DepartmentName", nullable = false, unique = true, length = 100)
    private String departmentName;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ParentID")
    private Department parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "Type", nullable = false)
    private DepartmentType type;

    @ManyToMany(mappedBy = "departments")
    private Set<Field> fields;

    public enum DepartmentType {
        Faculty, Department
    }
}