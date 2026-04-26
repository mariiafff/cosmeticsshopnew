package com.cosmeticsshop.dto;

import java.util.List;

public class GeminiRequest {

    private Content systemInstruction;
    private List<Content> contents;

    public GeminiRequest() {
    }

    public GeminiRequest(Content systemInstruction, List<Content> contents) {
        this.systemInstruction = systemInstruction;
        this.contents = contents;
    }

    public Content getSystemInstruction() {
        return systemInstruction;
    }

    public void setSystemInstruction(Content systemInstruction) {
        this.systemInstruction = systemInstruction;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public static GeminiRequest fromPrompt(String systemPrompt, String userQuestion) {
        Content instruction = new Content(List.of(new Part(systemPrompt)));
        Content content = new Content(List.of(new Part("User question: " + userQuestion)));
        return new GeminiRequest(instruction, List.of(content));
    }

    public static class Content {
        private List<Part> parts;

        public Content() {
        }

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        private String text;

        public Part() {
        }

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
