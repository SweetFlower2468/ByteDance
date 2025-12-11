package com.edu.neu.finalhomework.activity.main.network.provider;

import com.edu.neu.finalhomework.activity.main.network.ArkClient;
import com.edu.neu.finalhomework.domain.entity.LocalModel;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DeepSeekProvider extends BaseProvider {

    @Override
    protected String createRequestBody(LocalModel model, List<ArkClient.Msg> messages, boolean isDeepThink) {
        String modelId = (model.version != null && !model.version.isEmpty()) ? model.version : model.name;
        // DeepSeek uses standard OpenAI format for request
        DeepSeekPayload payload = new DeepSeekPayload(modelId, messages, true);
        return gson.toJson(payload);
    }

    @Override
    protected void processChunk(String chunkJson, ArkClient.StreamListener listener, StringBuilder fullResponse) {
        try {
            DeepSeekChunk chunk = gson.fromJson(chunkJson, DeepSeekChunk.class);
            if (chunk != null && chunk.choices != null) {
                for (DeepSeekChunk.Choice c : chunk.choices) {
                    if (c.delta != null) {
                        // DeepSeek might return reasoning_content
                        if (c.delta.reasoningContent != null && !c.delta.reasoningContent.isEmpty()) {
                            fullResponse.append(c.delta.reasoningContent);
                            if (listener != null) {
                                listener.onReasoning(c.delta.reasoningContent);
                            }
                        }
                        
                        if (c.delta.content != null) {
                            fullResponse.append(c.delta.content);
                            if (listener != null) {
                                listener.onChunk(c.delta.content);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    static class DeepSeekPayload {
        String model;
        List<ArkClient.Msg> messages;
        boolean stream;

        DeepSeekPayload(String model, List<ArkClient.Msg> messages, boolean stream) {
            this.model = model;
            this.messages = messages;
            this.stream = stream;
        }
    }

    static class DeepSeekChunk {
        List<Choice> choices;
        static class Choice {
            Delta delta;
        }
        static class Delta {
            String content;
            @SerializedName("reasoning_content")
            String reasoningContent;
        }
    }
}





