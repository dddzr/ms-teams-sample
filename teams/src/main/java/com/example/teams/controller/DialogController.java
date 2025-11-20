package com.example.teams.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dialog 컨트롤러
 * Teams Dialog API에서 사용하는 페이지들을 제공합니다.
 */
@Controller
public class DialogController {
    
    /**
     * Alert 다이얼로그 페이지
     * Teams Dialog API에서 사용
     */
    @GetMapping("/dialog/alert")
    public String alertDialog() {
        return "dialog/alert";
    }
}

