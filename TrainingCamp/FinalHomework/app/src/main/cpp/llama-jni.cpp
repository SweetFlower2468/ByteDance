#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>
#include <thread>
#include <mutex>
#include <cstring>

#include "llama.h"

#define TAG "LlamaJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Wrapper structure to hold model and context
struct LlamaContextWrapper {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_sampler* sampler = nullptr;
    int n_batch = 512; // Default
    int n_ctx = 4096;  // Context window used to guard prompt/output
    
    ~LlamaContextWrapper() {
        if (sampler) {
            llama_sampler_free(sampler);
            sampler = nullptr;
        }
        if (ctx) {
            llama_free(ctx);
            ctx = nullptr;
        }
        if (model) {
            llama_model_free(model);
            model = nullptr;
        }
    }
};

extern "C"
JNIEXPORT jlong JNICALL
Java_com_edu_neu_finalhomework_service_LlamaService_loadModelNative(JNIEnv *env, jobject thiz, jstring model_path_str, jint n_ctx, jint n_threads, jint n_batch, jint n_gpu_layers) {
    const char *model_path = env->GetStringUTFChars(model_path_str, nullptr);
    
    LOGD("Loading model from: %s (ctx=%d, threads=%d, batch=%d, gpu=%d)", model_path, n_ctx, n_threads, n_batch, n_gpu_layers);
    
    // Initialize backend
    llama_backend_init();

    // Model parameters
    auto mparams = llama_model_default_params();
    mparams.n_gpu_layers = n_gpu_layers; // Use config value
    
    // Updated API: llama_model_load_from_file
    llama_model* model = llama_model_load_from_file(model_path, mparams);
    env->ReleaseStringUTFChars(model_path_str, model_path);

    if (!model) {
        LOGE("Failed to load model");
        return 0;
    }

    // Context parameters
    auto cparams = llama_context_default_params();
    cparams.n_ctx = n_ctx; // Use Config
    cparams.n_threads = n_threads; // Use Config
    cparams.n_threads_batch = n_threads; // Usually same as threads
    // n_batch is used in llama_batch_init during inference/eval, not here context creation directly usually
    // but useful to log or store if we had a class member. 
    // Actually we re-create batch in completionNative, but we don't have access to n_batch there unless we store it.
    // Let's store it in wrapper? Or just rely on hardcoded/config passed later? 
    // Ideally completionNative should take batch size or we store it in wrapper.
    
    // Let's add n_batch to LlamaContextWrapper to use it in completionNative?
    // Or just use a reasonable default in completionNative since the user config is static java side.
    // For now, completionNative creates a batch of size 2048 (or dynamic).
    // Let's modify wrapper to store n_batch preference.

    // Updated API: llama_init_from_model
    llama_context* ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        LOGE("Failed to create context");
        llama_model_free(model);
        return 0;
    }
    
    // Initialize Sampler
    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.8f));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    // Updated API: llama_sampler_init_top_p requires min_keep
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(1234)); // Random seed

    auto* wrapper = new LlamaContextWrapper();
    wrapper->model = model;
    wrapper->ctx = ctx;
    wrapper->sampler = sampler;
    wrapper->n_batch = n_batch;
    wrapper->n_ctx = n_ctx;

    LOGD("Model loaded successfully");
    return reinterpret_cast<jlong>(wrapper);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_edu_neu_finalhomework_service_LlamaService_freeModelNative(JNIEnv *env, jobject thiz, jlong context_ptr) {
    if (context_ptr == 0) return;
    auto* wrapper = reinterpret_cast<LlamaContextWrapper*>(context_ptr);
    delete wrapper;
    LOGD("Model freed");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_edu_neu_finalhomework_service_LlamaService_completionNative(JNIEnv *env, jobject thiz, jlong context_ptr, jstring prompt_str, jobject callback) {
    if (context_ptr == 0) return;
    auto* wrapper = reinterpret_cast<LlamaContextWrapper*>(context_ptr);
    
    const char *prompt = env->GetStringUTFChars(prompt_str, nullptr);
    std::string prompt_text(prompt);
    env->ReleaseStringUTFChars(prompt_str, prompt);

    LOGD("Starting inference for prompt: %s", prompt_text.c_str());

    // Clear KV cache to ensure fresh context for new prompt
    llama_memory_clear(llama_get_memory(wrapper->ctx), true);

    // Prepare callback method ID
    jclass callbackClass = env->GetObjectClass(callback);
    // Changed signature to accept byte[]: ([B)V
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "([B)V");
    jmethodID shouldStopMethod = env->GetMethodID(callbackClass, "shouldStop", "()Z");

    // Get Vocab first!
    const llama_vocab* vocab = llama_model_get_vocab(wrapper->model);

    // Identify special stop tokens by ID
    const char* stop_strs[] = {"<|im_end|>", "<|im_start|>", "<|endoftext|>"};
    std::vector<llama_token> stop_tokens;
    for (const char* s : stop_strs) {
        // Try to tokenize the stop string to see if it's a single token
        std::vector<llama_token> tmp(10);
        int n = llama_tokenize(vocab, s, strlen(s), tmp.data(), tmp.size(), false, true); // add_special=true
        if (n == 1) {
            stop_tokens.push_back(tmp[0]);
            LOGD("Registered stop token ID: %d for '%s'", tmp[0], s);
        }
    }

    // Tokenize Prompt
    int n_tokens_max = prompt_text.length() + 100; // rough estimate
    std::vector<llama_token> tokens(n_tokens_max);
    
    int n_tokens = llama_tokenize(vocab, prompt_text.c_str(), prompt_text.length(), tokens.data(), n_tokens_max, true, false);
    
    if (n_tokens < 0) {
        // Buffer too small, resize
        n_tokens_max = -n_tokens;
        tokens.resize(n_tokens_max);
        n_tokens = llama_tokenize(vocab, prompt_text.c_str(), prompt_text.length(), tokens.data(), n_tokens_max, true, false);
    }
    
    if (n_tokens < 0) {
         LOGE("Tokenization failed");
         return;
    }
    
    tokens.resize(n_tokens);

    // If prompt tokens exceed context, keep the tail to fit in window (reserve room for generation)
    const int ctx_window = wrapper->n_ctx;
    const int reserve_for_gen = 512; // leave space for generation
    if (ctx_window > 0 && n_tokens > ctx_window - reserve_for_gen) {
        int keep = ctx_window - reserve_for_gen;
        if (keep < 0) keep = ctx_window / 2; // fallback to half window
        if (keep > 0 && keep < n_tokens) {
            std::vector<llama_token> trimmed(tokens.end() - keep, tokens.end());
            tokens.swap(trimmed);
            n_tokens = tokens.size();
            LOGD("Prompt trimmed to last %d tokens to fit context", n_tokens);
        }
    }

    // Create a batch
    // Initialize with capacity for chunk processing (e.g. 512) or max allowed by memory
    const int n_batch_size = wrapper->n_batch;
    llama_batch batch = llama_batch_init(n_batch_size, 0, 1); 

    // Process prompt in chunks
    for (int i = 0; i < n_tokens; i += n_batch_size) {
        int n_eval = n_tokens - i;
        if (n_eval > n_batch_size) n_eval = n_batch_size;
        
        // Reset batch for new chunk
        batch.n_tokens = 0;
        
        for (int j = 0; j < n_eval; j++) {
            int pos = i + j;
            batch.token[batch.n_tokens] = tokens[pos];
            batch.pos[batch.n_tokens] = pos;
            batch.n_seq_id[batch.n_tokens] = 1;
            batch.seq_id[batch.n_tokens][0] = 0;
            batch.logits[batch.n_tokens] = false;
            batch.n_tokens++;
        }
        
        // Set logits=true only for the very last token of the entire prompt
        if (i + n_eval == n_tokens) {
            batch.logits[batch.n_tokens - 1] = true;
        }

        // Decode chunk
        if (llama_decode(wrapper->ctx, batch) != 0) {
            LOGE("llama_decode failed during prompt processing at chunk %d", i);
            llama_batch_free(batch);
            return;
        }
    }

    int n_cur = n_tokens;
    int n_decode = 0;
    // Output limit: leave headroom inside context window if available
    int max_tokens = 2048;
    if (ctx_window > 0) {
        int available = ctx_window - n_cur - 8; // small safety margin
        if (available < 64) available = 64; // minimum to attempt
        if (available < max_tokens) max_tokens = available;
    }

    while (n_decode < max_tokens) {
        // Check for Java cancellation request
        if (shouldStopMethod != nullptr) {
            jboolean shouldStop = env->CallBooleanMethod(callback, shouldStopMethod);
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
                LOGE("Exception during shouldStop callback");
                break;
            }
            if (shouldStop) {
                LOGD("Inference stopped by Java request");
                break;
            }
        }

        // Sample next token
        llama_token new_token_id = llama_sampler_sample(wrapper->sampler, wrapper->ctx, -1);

        // Check for EOS (Standard)
        if (llama_vocab_is_eog(vocab, new_token_id)) {
            LOGD("EOS token detected");
            break;
        }

        // Check for Custom Stop Tokens (by ID)
        bool is_stop_token = false;
        for (llama_token t : stop_tokens) {
            if (new_token_id == t) {
                is_stop_token = true;
                break;
            }
        }
        if (is_stop_token) {
             LOGD("Custom stop token ID detected: %d", new_token_id);
             break;
        }

        // Convert token to piece
        char buf[256];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        
        if (n < 0) {
             // Buffer too small? Ignore for now
        } else {
            // Safety Check: Detect ChatML special tags if model outputs them as text strings
            std::string piece(buf, n);
            auto lowerPiece = piece;
            std::transform(lowerPiece.begin(), lowerPiece.end(), lowerPiece.begin(), ::tolower);
            if (lowerPiece.find("<|im_end|>") != std::string::npos || 
                lowerPiece.find("<|im_start|>") != std::string::npos ||
                lowerPiece.find("<|endoftext|>") != std::string::npos ||
                lowerPiece.find("im_start") != std::string::npos || // catch malformed tags
                lowerPiece.find("im_end") != std::string::npos) {
                LOGD("Stop token string detected: %s", piece.c_str());
                break;
            }

            // Pass raw bytes to Java
            jbyteArray jBytes = env->NewByteArray(n);
            if (jBytes != nullptr) {
                env->SetByteArrayRegion(jBytes, 0, n, reinterpret_cast<const jbyte*>(buf));
                env->CallVoidMethod(callback, onTokenMethod, jBytes);
                if (env->ExceptionCheck()) {
                     env->ExceptionClear();
                     LOGE("Exception during onToken callback");
                     env->DeleteLocalRef(jBytes);
                     break;
                }
                env->DeleteLocalRef(jBytes);
            } else {
                LOGE("Failed to allocate byte array");
                break;
            }
        }

        // Prepare next batch - clear previous
        batch.n_tokens = 0;
        
        // Add new token
        batch.token[batch.n_tokens] = new_token_id;
        batch.pos[batch.n_tokens] = n_cur;
        batch.n_seq_id[batch.n_tokens] = 1;
        batch.seq_id[batch.n_tokens][0] = 0;
        batch.logits[batch.n_tokens] = true;
        batch.n_tokens++;

        n_decode++;
        n_cur++;

        // Decode
        if (llama_decode(wrapper->ctx, batch) != 0) {
            LOGE("llama_decode failed during generation");
            break;
        }
    }
    
    llama_batch_free(batch);
    LOGD("Inference finished");
}