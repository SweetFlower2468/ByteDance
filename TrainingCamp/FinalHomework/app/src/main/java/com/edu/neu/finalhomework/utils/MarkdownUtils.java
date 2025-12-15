package com.edu.neu.finalhomework.utils;

public class MarkdownUtils {
    public static String cleanMarkdown(String text) {
        if (text == null) return "";
        
        // Remove code block fences but KEEP content
        // Remove ```language
        text = text.replaceAll("```\\w*\\s*", "");
        // Remove closing ```
        text = text.replaceAll("```", "");
        
        // Remove inline code ticks
        text = text.replaceAll("`", "");
        
        // Remove bold
        text = text.replaceAll("\\*\\*([^*]*)\\*\\*", "$1");
        
        // Remove italic
        text = text.replaceAll("\\*([^*]*)\\*", "$1");
        
        // Remove links [text](url) -> text
        text = text.replaceAll("\\[([^]]*)\\]\\([^)]*\\)", "$1");
        
        // Remove images ![alt](url) -> 
        text = text.replaceAll("!\\[([^]]*)\\]\\([^)]*\\)", "");
        
        // Remove headers #
        text = text.replaceAll("(?m)^#+\\s*", "");
        
        // Remove blockquotes >
        text = text.replaceAll("(?m)^>\\s*", "");
        
        // Remove horizontal rules
        text = text.replaceAll("(?m)^-{3,}", "");
        
        return text.trim();
    }
}
