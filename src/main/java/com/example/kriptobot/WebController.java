package com.example.kriptobot;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String root() {
        return "redirect:/piyasa";
    }

    @GetMapping("/piyasa")
    public String piyasa() {
        return "forward:/piyasa.html";
    }

    @GetMapping("/fear-greed")
    public String fearGreed() {
        return "forward:/fear-greed.html";
    }
}
