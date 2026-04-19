package com.cosmeticsshop.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @GetMapping("/messages")
    public List<String> getMessages() {
        // Dummy implementation - return empty list
        return List.of();
    }

    @PostMapping("/send")
    public Map<String, String> sendMessage(@RequestBody Map<String, String> message) {
        // Dummy implementation - return success
        return Map.of("status", "sent");
    }
}