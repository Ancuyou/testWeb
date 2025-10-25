package it.ute.QAUTE.repository;

import it.ute.QAUTE.entity.Field;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldRepository extends JpaRepository<Field, Integer> {
    List<Field> findAllByDepartments_departmentID(Integer departmentId);

    @Query("""
        SELECT DISTINCT f FROM Field f
        JOIN f.departments d
        WHERE d.departmentID = :departmentId
          AND LOWER(f.fieldName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Field> findByDepartmentAndKeyword(@Param("departmentId") Integer departmentId,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);
    @Query("""
        SELECT f FROM Field f
        JOIN f.departments d
        WHERE d.departmentID = :departmentId
    """)
    Page<Field> findByDepartment(@Param("departmentId") Integer departmentId, Pageable pageable);

    @Query("""
        SELECT DISTINCT f FROM Field f
        WHERE LOWER(f.fieldName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Field> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
}