package com.cosmeticsshop.controller;

import com.cosmeticsshop.dto.ChatAskRequest;
import com.cosmeticsshop.dto.ChatResponse;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.service.ChatbotService;
import com.cosmeticsshop.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatbotService chatbotService;
    private final UserService userService;

    public ChatController(ChatbotService chatbotService, UserService userService) {
        this.chatbotService = chatbotService;
        this.userService = userService;
    }

    @GetMapping("/messages")
    public List<String> getMessages() {
        return List.of("Ask about revenue, orders, products, categories, or shipments.");
    }

    @PostMapping("/ask")
    public ChatResponse ask(@RequestBody ChatAskRequest request, Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return chatbotService.ask(request.getQuestion(), user);
    }
}
