package com.edu.neu.finalhomework.service;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.edu.neu.finalhomework.domain.entity.LocalModel;
import com.edu.neu.finalhomework.service.callback.SimpleCallback;
import com.edu.neu.finalhomework.service.callback.StreamCallback;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 本地模型服务
 * 核心功能：本地模型加载、推理 (JNI)
 */
public class LlamaService {
    
    private static final String TAG = "LlamaService";
    private static LlamaService instance;
    private long contextPointer = 0; // Pointer to native llama_context
    private LocalModel currentModel;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Load native library
    static {
        try {
            System.loadLibrary("llama-android");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library: " + e.getMessage());
        }
    }
    
    private LlamaService() {
    }
    
    public static LlamaService getInstance() {
        if (instance == null) {
            synchronized (LlamaService.class) {
                if (instance == null) {
                    instance = new LlamaService();
                }
            }
        }
        return instance;
    }
    
    /**
     * 加载模型
     */
    public void loadModel(LocalModel model, SimpleCallback callback) {
        if (model == null || model.localPath == null) {
            if (callback != null) callback.onFailure("Invalid model path");
            return;
        }

        if (contextPointer != 0 && currentModel != null && currentModel.id == model.id) {
            if (callback != null) callback.onSuccess();
            return;
        }
        
        // Unload previous if exists
        unloadModel();
        
        executor.execute(() -> {
            try {
                File file = new File(model.localPath);
                if (!file.exists()) {
                    mainHandler.post(() -> {
                         if (callback != null) callback.onFailure("Model file not found: " + model.localPath);
                    });
                    return;
                }
                
                // Native Load
                // Use Config values
                int nContext = com.edu.neu.finalhomework.config.ModelConfig.nContext;
                int nThreads = com.edu.neu.finalhomework.config.ModelConfig.nThreads;
                int nBatch = com.edu.neu.finalhomework.config.ModelConfig.nBatch;
                int nGpuLayers = com.edu.neu.finalhomework.config.ModelConfig.nGpuLayers;
                
                long ptr = loadModelNative(model.localPath, nContext, nThreads, nBatch, nGpuLayers);
                if (ptr != 0) {
                    contextPointer = ptr;
                    currentModel = model;
                    mainHandler.post(() -> {
                         if (callback != null) callback.onSuccess();
                    });
                } else {
                    mainHandler.post(() -> {
                         if (callback != null) callback.onFailure("Failed to load model (Native error)");
                    });
                }
            } catch (UnsatisfiedLinkError e) {
                mainHandler.post(() -> {
                     if (callback != null) callback.onFailure("Native library not loaded. Please add libllama-android.so");
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                     if (callback != null) callback.onFailure("Load error: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * 卸载当前模型
     */
    public void unloadModel() {
        // 1. Cancel any running inference immediately
        cancelInference();
        
        // 2. Capture the pointer to free
        final long ptrToFree = contextPointer;
        
        // 3. Reset state immediately so new calls fail gracefully or wait
        contextPointer = 0;
        currentModel = null;
        
        if (ptrToFree != 0) {
            // 4. Perform the native free on the executor thread
            // This ensures it runs AFTER any currently running inference task finishes
            executor.execute(() -> {
                try {
                    freeModelNative(ptrToFree);
                } catch (UnsatisfiedLinkError e) {
                    Log.e(TAG, "Native error during unload", e);
                }
            });
        }
    }
    
    /**
     * 执行推理
     */
    public void infer(String prompt, StreamCallback callback) {
        if (contextPointer == 0) {
            if (callback != null) callback.onError("Model not loaded", new IllegalStateException("Model not loaded"));
            return;
        }
        
        executor.execute(() -> {
            try {
                // Blocking native call that triggers callbacks
                // Since native callbacks from C thread to Java UI thread are tricky,
                // we assume native method accepts a jobject callback and calls its methods.
                currentCallbackAdapter = new NativeCallbackAdapter(callback, mainHandler);
                completionNative(contextPointer, prompt, currentCallbackAdapter);
                
                mainHandler.post(() -> {
                    if (callback != null) callback.onComplete();
                });
            } catch (UnsatisfiedLinkError e) {
                 mainHandler.post(() -> {
                    if (callback != null) callback.onError("Native inference error", e);
                 });
            } catch (Exception e) {
                 mainHandler.post(() -> {
                    if (callback != null) callback.onError("Inference error", e);
                 });
            } finally {
                currentCallbackAdapter = null;
            }
        });
    }
    
    public boolean isModelLoaded() {
        return contextPointer != 0;
    }
    
    public LocalModel getCurrentModel() {
        return currentModel;
    }
    
    // --- JNI Interfaces ---
    // These methods should be implemented in C++ (llama-android)
    
    // Updated signature to accept config parameters
    private native long loadModelNative(String modelPath, int nContext, int nThreads, int nBatch, int nGpuLayers);
    private native void freeModelNative(long contextPtr);
    private native void completionNative(long contextPtr, String prompt, NativeCallbackAdapter callback);
    
    // Callback Adapter to be called from JNI
    public static class NativeCallbackAdapter {
        private StreamCallback callback;
        private Handler handler;
        private volatile boolean stopRequested = false;
        
        // Buffer for handling split UTF-8 characters
        private java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
        
        public NativeCallbackAdapter(StreamCallback callback, Handler handler) {
            this.callback = callback;
            this.handler = handler;
        }
        
        public void cancel() {
            stopRequested = true;
        }
        
        // Called from Native C++ to check if we should stop
        public boolean shouldStop() {
            return stopRequested;
        }
        
        // Called from Native C++
        // Changed to receive raw bytes to avoid JNI UTF-8 validation crashes on partial multi-byte characters
        public void onToken(byte[] tokenBytes) {
            if (handler != null && callback != null && !stopRequested) {
                synchronized (byteBuffer) {
                    try {
                        byteBuffer.write(tokenBytes);
                        
                        // Try to decode buffer
                        byte[] allBytes = byteBuffer.toByteArray();
                        String decoded = new String(allBytes, java.nio.charset.StandardCharsets.UTF_8);
                        
                        // Check if the last character is a replacement char (indicating incomplete byte sequence)
                        // This is a heuristic. A better way is to use CharsetDecoder, but checking the last char 
                        // against the known length of bytes usually works for streaming if we are careful.
                        // Actually, new String() replaces invalid bytes with \uFFFD.
                        // If the end of the byte array matches the end of a valid UTF-8 sequence, it's fine.
                        // But if it was cut off, it puts \uFFFD.
                        
                        // Better approach: Check if the end of byte array forms a complete UTF-8 char.
                        if (isUtf8Truncated(allBytes)) {
                            // Keep in buffer, don't emit yet
                            return;
                        }
                        
                        // Buffer is valid (or empty)
                        if (decoded.length() > 0) {
                            handler.post(() -> callback.onChunk(decoded));
                            byteBuffer.reset();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding token bytes", e);
                    }
                }
            }
        }
        
        // Helper to detect if the last byte sequence is incomplete
        private boolean isUtf8Truncated(byte[] bytes) {
            if (bytes.length == 0) return false;
            int lastByte = bytes[bytes.length - 1] & 0xFF;
            
            // ASCII
            if (lastByte < 0x80) return false;
            
            // Multi-byte start
            // 110xxxxx (C0-DF) -> 2 bytes
            // 1110xxxx (E0-EF) -> 3 bytes
            // 11110xxx (F0-F7) -> 4 bytes
            
            // Continuation byte: 10xxxxxx (80-BF)
            
            // Scan backwards to find the start byte of the last character
            int i = bytes.length - 1;
            int count = 0;
            while (i >= 0 && (bytes[i] & 0xC0) == 0x80) { // while is continuation byte
                i--;
                count++;
            }
            
            if (i < 0) return true; // Only continuation bytes found? strange, but treat as truncated/invalid
            
            int startByte = bytes[i] & 0xFF;
            int expectedLen = 0;
            
            if ((startByte & 0xE0) == 0xC0) expectedLen = 2;
            else if ((startByte & 0xF0) == 0xE0) expectedLen = 3;
            else if ((startByte & 0xF8) == 0xF0) expectedLen = 4;
            else return false; // Not a start byte or invalid
            
            // existing bytes for this char = 1 (start) + count (continuations)
            return (1 + count) < expectedLen;
        }
    }
    
    private NativeCallbackAdapter currentCallbackAdapter;
    
    public void cancelInference() {
        if (currentCallbackAdapter != null) {
            currentCallbackAdapter.cancel();
        }
    }
}
