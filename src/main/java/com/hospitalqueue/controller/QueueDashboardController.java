package com.hospitalqueue.controller;

import com.hospitalqueue.entity.Priority;
import com.hospitalqueue.entity.Token;
import com.hospitalqueue.service.QueueBroadcastService;
import com.hospitalqueue.service.TokenService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class QueueDashboardController {

    private final QueueBroadcastService queueBroadcastService;
    private final TokenService tokenService;

    public QueueDashboardController(QueueBroadcastService queueBroadcastService, TokenService tokenService) {
        this.queueBroadcastService = queueBroadcastService;
        this.tokenService = tokenService;
    }

    @GetMapping("/doctors/{doctorId}/dashboard")
    public String dashboard(@PathVariable Long doctorId, Model model) {
        model.addAttribute("snapshot", queueBroadcastService.buildSnapshot(doctorId));
        return "dashboard";
    }

    @PostMapping("/doctors/{doctorId}/call-next")
    public String callNext(@PathVariable Long doctorId) {
        tokenService.callNext(doctorId);
        return "redirect:/doctors/" + doctorId + "/dashboard";
    }

    @PostMapping("/tokens/{tokenId}/complete")
    public String complete(@PathVariable Long tokenId) {
        Token token = tokenService.complete(tokenId);
        return "redirect:/doctors/" + token.getDoctor().getId() + "/dashboard";
    }

    @PostMapping("/tokens/{tokenId}/cancel")
    public String cancel(@PathVariable Long tokenId) {
        Token token = tokenService.cancel(tokenId);
        return "redirect:/doctors/" + token.getDoctor().getId() + "/dashboard";
    }

    @PostMapping("/tokens/{tokenId}/escalate")
    public String escalate(@PathVariable Long tokenId, @RequestParam Priority priority) {
        Token token = tokenService.escalate(tokenId, priority);
        return "redirect:/doctors/" + token.getDoctor().getId() + "/dashboard";
    }
}
