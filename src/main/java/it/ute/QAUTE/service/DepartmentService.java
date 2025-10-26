package it.ute.QAUTE.service;

import it.ute.QAUTE.exception.AppException;
import it.ute.QAUTE.exception.ErrorCode;
import it.ute.QAUTE.entity.Department;
import it.ute.QAUTE.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {  // done clean
    @Autowired
    DepartmentRepository departmentRepository;

    public Page<Department> searchNameDepartment(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.equals("")) {
            return departmentRepository.findByNameDepartment(keyword, pageable);
        } else {
            return departmentRepository.findAll(pageable);
        }
    }

    public List<Department> findAll(){
        return departmentRepository.findAll();
    }

    public Department findById(Integer id){
        return departmentRepository.findById(id).orElse(null);
    }

    public List<Department> findAllNoPaging(){
        return departmentRepository.findByType(Department.DepartmentType.Faculty);
    }

    public void updateDepartment(Department department) {

        if(departmentRepository.existsByDepartmentNameIsIgnoreCaseAndDepartmentIDNot(department.getDepartmentName(), department.getDepartmentID())){
            throw new AppException(ErrorCode.DEPARTMENTNAME_EXISTED);
        }
        Department saved = departmentRepository.save(department);
        if (saved.getParent() == null) {
            saved.setParent(saved);
            departmentRepository.save(saved);
        }
    }

    public void deleteDepartment(Integer id) {
        departmentRepository.deleteById(id);
    }

    public List<Department> findAllById(List<Integer> departmentIds) {
        return departmentRepository.findAllById(departmentIds);
    }

}
