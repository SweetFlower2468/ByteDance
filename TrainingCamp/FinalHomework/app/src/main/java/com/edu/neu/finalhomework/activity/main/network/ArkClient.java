package com.edu.neu.finalhomework.activity.main.network;

import com.edu.neu.finalhomework.App;
import com.edu.neu.finalhomework.activity.main.network.provider.LLMFactory;
import com.edu.neu.finalhomework.activity.main.network.provider.LLMProvider;
import com.edu.neu.finalhomework.domain.dao.ModelDao;
import com.edu.neu.finalhomework.domain.entity.LocalModel;

import java.util.List;

/**
 * Unified Chat Client
 * Delegates to specific LLMProvider based on active model configuration.
 */
public class ArkClient {

    public interface StreamListener {
        void onChunk(String delta);
        default void onReasoning(String delta) {}
        void onComplete();
        void onError(String message, Throwable t);
    }

    // Keep Msg as a static inner class or move to a separate file. 
    // Keeping it here minimizes refactoring in ChatActivity.
    public static class Msg {
        public String role;
        public String content;
        public Msg(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public ArkClient() {
    }

    public Cancellable sendChat(List<Msg> messages, boolean isDeepThink, StreamListener listener) {
        // Fetch active model from DB
        ModelDao modelDao = App.getInstance().getDatabase().modelDao();
        LocalModel active = modelDao.getActiveModel(LocalModel.Status.ACTIVE);
        
        if (active == null) {
            if (listener != null) {
                listener.onError("No active model found", null);
            }
            return () -> {};
        }

        // Get appropriate provider
        LLMProvider provider = LLMFactory.getProvider(active);
        
        // Delegate to provider
        return provider.sendChat(active, messages, isDeepThink, listener);
    }
}
