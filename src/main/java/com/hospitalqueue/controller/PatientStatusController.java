package com.hospitalqueue.controller;

import com.hospitalqueue.service.QueueBroadcastService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Unauthenticated by design — see PRD §5 / CLAUDE.md. Do not add a role
 * requirement here.
 */
@Controller
public class PatientStatusController {

    private final QueueBroadcastService queueBroadcastService;

    public PatientStatusController(QueueBroadcastService queueBroadcastService) {
        this.queueBroadcastService = queueBroadcastService;
    }

    @GetMapping("/patients/{tokenId}")
    public String status(@PathVariable Long tokenId, Model model) {
        model.addAttribute("status", queueBroadcastService.buildPatientStatusView(tokenId));
        return "patient-status";
    }
}
