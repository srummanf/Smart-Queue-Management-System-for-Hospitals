package com.hospitalqueue.controller;

import com.hospitalqueue.service.AnalyticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("dailyStats", analyticsService.dailyStats());
        model.addAttribute("doctorStats", analyticsService.doctorStats());
        return "analytics";
    }
}
