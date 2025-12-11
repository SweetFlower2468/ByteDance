package com.edu.neu.finalhomework.activity.main.network.provider;

import com.edu.neu.finalhomework.activity.main.network.ArkClient;
import com.edu.neu.finalhomework.domain.entity.LocalModel;

import java.util.List;

public interface LLMProvider {
    /**
     * Send a chat request
     * @param model Configuration of the model (API Key, URL, etc.)
     * @param messages Context messages
     * @param isDeepThink Whether to enable reasoning/thinking
     * @param listener Callback for stream events
     * @return A cancellable object to stop the request
     */
    com.edu.neu.finalhomework.activity.main.network.Cancellable sendChat(LocalModel model, List<ArkClient.Msg> messages, boolean isDeepThink, ArkClient.StreamListener listener);
}





