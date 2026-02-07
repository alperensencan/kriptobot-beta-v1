package com.example.kriptobot;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final CoinService coinService;
    private final FearGreedService fearGreedService;

    public ApiController(CoinService coinService, FearGreedService fearGreedService) {
        this.coinService = coinService;
        this.fearGreedService = fearGreedService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/piyasa")
    public ResponseEntity<?> getPiyasa() {
        return ResponseEntity.ok(coinService.getPiyasa());
    }

    @GetMapping("/fear-greed")
    public ResponseEntity<?> getFearGreed() {
        return ResponseEntity.ok(fearGreedService.getFearGreed());
    }
}
