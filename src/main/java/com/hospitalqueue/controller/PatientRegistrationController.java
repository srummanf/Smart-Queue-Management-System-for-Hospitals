package com.hospitalqueue.controller;

import com.hospitalqueue.dto.PatientRegistrationForm;
import com.hospitalqueue.entity.Priority;
import com.hospitalqueue.entity.Token;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.service.TokenService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/register")
public class PatientRegistrationController {

    private final TokenService tokenService;
    private final DoctorRepository doctorRepository;

    public PatientRegistrationController(TokenService tokenService, DoctorRepository doctorRepository) {
        this.tokenService = tokenService;
        this.doctorRepository = doctorRepository;
    }

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("form", new PatientRegistrationForm());
        model.addAttribute("doctors", doctorRepository.findAll());
        model.addAttribute("priorities", Priority.values());
        return "register";
    }

    @PostMapping
    public String submit(@ModelAttribute("form") PatientRegistrationForm form, Model model) {
        Token token = tokenService.register(form.getFullName(), form.getPhone(), form.getDoctorId(), form.getPriority());
        model.addAttribute("token", token);
        return "register-success";
    }
}
