package com.hospitalqueue.repository;

import com.hospitalqueue.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByDepartmentId(Long departmentId);
}
