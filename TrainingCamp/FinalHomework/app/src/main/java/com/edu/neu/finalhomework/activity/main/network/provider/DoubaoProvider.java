package com.edu.neu.finalhomework.activity.main.network.provider;

import com.edu.neu.finalhomework.activity.main.network.ArkClient;
import com.edu.neu.finalhomework.domain.entity.LocalModel;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DoubaoProvider extends BaseProvider {

    @Override
    protected String createRequestBody(LocalModel model, List<ArkClient.Msg> messages, boolean isDeepThink) {
        String modelId = (model.version != null && !model.version.isEmpty()) ? model.version : model.name;
        
        ChatPayload payload = new ChatPayload(
                modelId,
                messages,
                true,
                isDeepThink
        );
        return gson.toJson(payload);
    }

    @Override
    protected void processChunk(String chunkJson, ArkClient.StreamListener listener, StringBuilder fullResponse) {
        try {
            ChatStreamChunk chunk = gson.fromJson(chunkJson, ChatStreamChunk.class);
            if (chunk != null && chunk.choices != null) {
                for (ChatStreamChunk.Choice c : chunk.choices) {
                    if (c.delta != null) {
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
        } catch (Exception parseEx) {
            // ignore malformed chunks
        }
    }

    // Inner DTOs specific to Doubao if needed, or reuse generic ones if they match.
    // Doubao has "thinking" param.

    static class ChatPayload {
        String model;
        List<ArkClient.Msg> messages;
        boolean stream;
        @SerializedName("thinking")
        Thinking thinking;

        ChatPayload(String model, List<ArkClient.Msg> messages, boolean stream, boolean enableThinking) {
            this.model = model;
            this.messages = messages;
            this.stream = stream;
            // Always set thinking parameter with "enabled" or "disabled"
            this.thinking = new Thinking(enableThinking ? "enabled" : "disabled");
        }
    }

    static class Thinking {
        String type;
        Thinking(String type) { this.type = type; }
    }

    static class ChatStreamChunk {
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

