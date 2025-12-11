package com.edu.neu.finalhomework.activity.main.network.provider;

import com.edu.neu.finalhomework.activity.main.network.ArkClient;
import com.edu.neu.finalhomework.domain.entity.LocalModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import com.edu.neu.finalhomework.config.ApiConfig;
import java.util.concurrent.TimeUnit;

import com.edu.neu.finalhomework.activity.main.network.Cancellable;

public abstract class BaseProvider implements LLMProvider {

    protected final OkHttpClient client;
    protected final Gson gson;

    public BaseProvider() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(ApiConfig.STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    @Override
    public Cancellable sendChat(LocalModel model, List<ArkClient.Msg> messages, boolean isDeepThink, ArkClient.StreamListener listener) {
        if (model == null || model.apiUrl == null || model.apiKey == null) {
            if (listener != null) {
                listener.onError("Model configuration missing (API URL or Key)", null);
            }
            return () -> {}; // No-op cancellable
        }

        String finalUrl = getEndpoint(model.apiUrl);
        String jsonBody = createRequestBody(model, messages, isDeepThink);

        android.util.Log.d("BaseProvider", "Request URL: " + finalUrl);
        
        // Log "Thinking" status explicitly for easy debugging
        if (jsonBody.contains("\"thinking\"")) {
             android.util.Log.d("BaseProvider", "Thinking Enabled: " + isDeepThink);
        }
        
        // Log full body in chunks
        logLongString("BaseProvider_Body", jsonBody);

        String safeKey = model.apiKey;
        if (safeKey != null && safeKey.startsWith("Bearer ")) {
            safeKey = safeKey.substring(7).trim();
        }

        Request request = new Request.Builder()
                .url(finalUrl)
                .addHeader("Authorization", "Bearer " + safeKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) return; // Ignore if cancelled
                if (listener != null) listener.onError("Network error: " + e.getMessage(), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (call.isCanceled()) return;
                try {
                    handleResponse(response, listener, model.name);
                } catch (Exception e) {
                   if (!call.isCanceled()) {
                       listener.onError("Processing error: " + e.getMessage(), e);
                   }
                } finally {
                    response.close();
                }
            }
        });
        
        return () -> call.cancel();
    }

    protected String getEndpoint(String baseUrl) {
        if (baseUrl != null && !baseUrl.endsWith("/chat/completions")) {
            return baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        }
        return baseUrl;
    }

    protected abstract String createRequestBody(LocalModel model, List<ArkClient.Msg> messages, boolean isDeepThink);

    protected void handleResponse(Response response, ArkClient.StreamListener listener, String modelName) throws IOException {
        if (!response.isSuccessful()) {
            String errorBody = "";
            try (ResponseBody errBody = response.body()) {
                if (errBody != null) errorBody = errBody.string();
            } catch (Exception ignored) {}
            
            android.util.Log.e("BaseProvider", "Response Error: " + response.code() + ", Body: " + errorBody);
            
            if (listener != null) {
                listener.onError("Request failed code=" + response.code() + ", model=" + modelName + "\nResp: " + errorBody, null);
            }
            return;
        }

        StringBuilder fullResponse = new StringBuilder();

        try (ResponseBody body = response.body()) {
            if (body == null) {
                if (listener != null) listener.onError("Empty response body", null);
                return;
            }
            BufferedReader reader = new BufferedReader(body.charStream());
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("data:")) {
                    trimmed = trimmed.substring(5).trim();
                }
                if ("[DONE]".equals(trimmed)) {
                    continue;
                }
                processChunk(trimmed, listener, fullResponse);
            }
            
            android.util.Log.d("BaseProvider", "Full Response: " + fullResponse.toString());
            if (listener != null) listener.onComplete();
        }
    }

    protected abstract void processChunk(String chunkJson, ArkClient.StreamListener listener, StringBuilder fullResponse);
    
    private void logLongString(String tag, String content) {
        if (content.length() > 2000) { // Reduced chunk size to 2000 to be safe
            android.util.Log.d(tag, content.substring(0, 2000));
            logLongString(tag, content.substring(2000));
        } else {
            android.util.Log.d(tag, content);
        }
    }
}

