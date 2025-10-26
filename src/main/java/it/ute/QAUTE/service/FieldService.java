package it.ute.QAUTE.service;


import it.ute.QAUTE.entity.Field;
import it.ute.QAUTE.repository.FieldRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FieldService {
    @Autowired
    private FieldRepository fieldRepository;


    public Page<Field> searchField(Integer departmentId, String keyword, Pageable pageable) {

        if (departmentId != null && keyword != null && !keyword.trim().isEmpty()) {
            return fieldRepository.findByDepartmentAndKeyword(departmentId, keyword.trim(), pageable);
        } else if (departmentId != null) {
            return fieldRepository.findByDepartment(departmentId, pageable);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            return fieldRepository.findByKeyword(keyword.trim(), pageable);
        } else {
            return fieldRepository.findAll(pageable);
        }
    }

    public Field getFieldById(int fieldId) {
        return fieldRepository.findById(fieldId).orElse(null);
    }
    
    public List<Field> getAllFields() {
        return fieldRepository.findAll();
    }
}
