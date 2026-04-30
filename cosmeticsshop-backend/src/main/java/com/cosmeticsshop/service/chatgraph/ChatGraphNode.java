package com.cosmeticsshop.service.chatgraph;

public interface ChatGraphNode {

    String name();

    void execute(ChatGraphState state);
}
