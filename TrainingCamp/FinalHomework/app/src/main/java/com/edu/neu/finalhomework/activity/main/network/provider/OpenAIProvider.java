package com.edu.neu.finalhomework.activity.main.network.provider;

import com.edu.neu.finalhomework.activity.main.network.ArkClient;
import com.edu.neu.finalhomework.domain.entity.LocalModel;

import java.util.List;

public class OpenAIProvider extends BaseProvider {

    @Override
    protected String createRequestBody(LocalModel model, List<ArkClient.Msg> messages, boolean isDeepThink) {
        String modelId = (model.version != null && !model.version.isEmpty()) ? model.version : model.name;
        // Standard OpenAI Payload
        OpenAIPayload payload = new OpenAIPayload(modelId, messages, true);
        return gson.toJson(payload);
    }

    @Override
    protected void processChunk(String chunkJson, ArkClient.StreamListener listener, StringBuilder fullResponse) {
        try {
            OpenAIChunk chunk = gson.fromJson(chunkJson, OpenAIChunk.class);
            if (chunk != null && chunk.choices != null) {
                for (OpenAIChunk.Choice c : chunk.choices) {
                    if (c.delta != null && c.delta.content != null) {
                        fullResponse.append(c.delta.content);
                        if (listener != null) {
                            listener.onChunk(c.delta.content);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    static class OpenAIPayload {
        String model;
        List<ArkClient.Msg> messages;
        boolean stream;
        
        OpenAIPayload(String model, List<ArkClient.Msg> messages, boolean stream) {
            this.model = model;
            this.messages = messages;
            this.stream = stream;
        }
    }

    static class OpenAIChunk {
        List<Choice> choices;
        static class Choice {
            Delta delta;
        }
        static class Delta {
            String content;
        }
    }
}





