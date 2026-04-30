package com.cosmeticsshop.service.chatgraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QuestionAnalysisNode implements ChatGraphNode {

    private static final Logger log = LoggerFactory.getLogger(QuestionAnalysisNode.class);

    @Override
    public String name() {
        return "QuestionAnalysisNode";
    }

    @Override
    public void execute(ChatGraphState state) {
        log.info(
                "chat_graph analyze_question question={} role={} userId={} storeId={}",
                state.getOriginalQuestion(),
                state.getSession().role(),
                state.getSession().userId(),
                state.getSession().storeId()
        );
        if (state.getOriginalQuestion() == null || state.getOriginalQuestion().isBlank()) {
            state.setStatus("ERROR");
            state.setAgent("ERROR");
            state.setErrorMessage("Question is required.");
            state.setComplete(true);
        }
    }
}
