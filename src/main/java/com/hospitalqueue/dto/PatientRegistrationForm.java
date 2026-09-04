package com.hospitalqueue.dto;

import com.hospitalqueue.entity.Priority;

/**
 * Form-backing bean for the staff registration page (Thymeleaf th:object).
 */
public class PatientRegistrationForm {

    private String fullName;
    private String phone;
    private Long doctorId;
    private Priority priority = Priority.NORMAL;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
}
