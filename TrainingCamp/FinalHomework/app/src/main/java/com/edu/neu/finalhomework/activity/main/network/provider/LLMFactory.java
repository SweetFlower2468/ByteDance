package com.edu.neu.finalhomework.activity.main.network.provider;

import com.edu.neu.finalhomework.domain.entity.LocalModel;

public class LLMFactory {
    
    public static LLMProvider getProvider(LocalModel model) {
        if (model == null || model.provider == null) {
            return new DoubaoProvider(); // Default fallback
        }
        
        switch (model.provider.toLowerCase()) {
            case "openai":
                return new OpenAIProvider();
            case "deepseek":
                return new DeepSeekProvider();
            case "doubao":
            default:
                return new DoubaoProvider();
        }
    }
}





