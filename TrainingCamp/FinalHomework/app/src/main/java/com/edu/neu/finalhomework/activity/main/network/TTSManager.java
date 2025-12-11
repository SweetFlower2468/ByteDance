package com.edu.neu.finalhomework.activity.main.network;

import android.util.Log;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.edu.neu.finalhomework.config.ApiConfig;

public class TTSManager {
    private static final String TAG = "TTSManager";
    private static final String API_URL = "https://openspeech.bytedance.com/api/v1/tts";
    
    // Loaded from ApiConfig
    private static final String APP_ID = ApiConfig.TTS_APP_ID; 
    private static final String TOKEN = ApiConfig.TTS_TOKEN;
    private static final String CLUSTER = "volcano_tts"; // Default cluster

    private static TTSManager instance;
    private final OkHttpClient client;

    private TTSManager() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized TTSManager getInstance() {
        if (instance == null) {
            instance = new TTSManager();
        }
        return instance;
    }

    public interface TTSCallback {
        void onSuccess(byte[] audioData);
        void onError(String error);
    }

    public void generateSpeech(String text, String voiceType, int speedRatio, int volumeRatio, int pitchRatio, String appId, String token, TTSCallback callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            // App
            JSONObject app = new JSONObject();
            app.put("appid", appId != null ? appId : APP_ID);
            app.put("token", token != null ? token : TOKEN);
            app.put("cluster", CLUSTER);
            jsonBody.put("app", app);

            // User
            JSONObject user = new JSONObject();
            user.put("uid", UUID.randomUUID().toString());
            jsonBody.put("user", user);

            // Audio
            JSONObject audio = new JSONObject();
            audio.put("voice_type", voiceType);
            audio.put("encoding", "mp3");
            audio.put("speed_ratio", speedRatio / 50.0f); // Map 0-100 to 0.0-2.0 (approx)
            audio.put("volume_ratio", volumeRatio / 50.0f);
            audio.put("pitch_ratio", pitchRatio / 50.0f);
            jsonBody.put("audio", audio);

            // Request
            JSONObject request = new JSONObject();
            request.put("reqid", UUID.randomUUID().toString());
            request.put("text", text);
            request.put("text_type", "plain");
            request.put("operation", "query");
            jsonBody.put("request", request);

        } catch (JSONException e) {
            callback.onError("JSON Error: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer; " + (token != null ? token : TOKEN))
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network Error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("HTTP Error: " + response.code() + " " + response.message());
                    return;
                }
                
                // The API might return JSON with "data" field (base64) OR raw bytes.
                // Typical Doubao HTTP API returns JSON with "data" field containing base64 audio.
                // Let's check Content-Type or try to parse JSON.
                
                try {
                    String responseStr = response.body().string();
                    JSONObject responseJson = new JSONObject(responseStr);
                    
                    if (responseJson.has("data")) {
                        String base64Data = responseJson.getString("data");
                        byte[] audioBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                        callback.onSuccess(audioBytes);
                    } else if (responseJson.has("message") && responseJson.getString("message").equals("Success") && responseJson.has("data")) {
                        // Sometimes wrapped deeper?
                        // Let's look for "data" at top level based on docs.
                        callback.onError("No audio data found in response");
                    } else {
                        // It might be raw bytes if configured differently, but standard is JSON.
                        // If it fails to parse as JSON, maybe it IS raw bytes?
                        callback.onError("API Error: " + responseStr);
                    }
                } catch (JSONException e) {
                    // Maybe it was raw bytes?
                    // But standard HTTP API usually returns JSON wrapper.
                    callback.onError("Parse Error: " + e.getMessage());
                } catch (Exception e) {
                     callback.onError("Error: " + e.getMessage());
                }
            }
        });
    }
}

