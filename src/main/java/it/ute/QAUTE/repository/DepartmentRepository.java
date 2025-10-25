package it.ute.QAUTE.repository;

import it.ute.QAUTE.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    @Override
    Page<Department> findAll(Pageable pageable);
    @Query("SELECT d FROM Department d WHERE LOWER(d.departmentName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Department> findByNameDepartment(String keyword, Pageable pageable);
    List<Department> findByType(Department.DepartmentType type);

    boolean existsByDepartmentNameIsIgnoreCaseAndDepartmentIDNot(String departmentName, Integer departmentID);
}