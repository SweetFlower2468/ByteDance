package com.edu.neu.finalhomework.config;

import com.edu.neu.finalhomework.domain.entity.LocalModel;
import java.util.ArrayList;
import java.util.List;

public class ModelConfig {
    public static int nThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    public static int nContext = 4096;
    // nBatch将根据设备能力动态设置（参见DeviceSpecUtil）
    public static int nBatch = 512;
    // GPU层: 0 = 仅CPU, 99 = 尝试将所有层卸载到GPU（需要Vulkan/OpenCL构建）
    public static int nGpuLayers = 99;
    
    // UI节流（毫秒）以防止SurfaceFlinger过载
    public static long uiUpdateIntervalMs = 100;

    public static List<LocalModel> getBuiltInModels() {
        List<LocalModel> list = new ArrayList<>();
        
        // 1. Llama 3.2 (3B-Instruct)
        LocalModel m1 = new LocalModel();
        m1.name = "Llama 3.2";
        m1.version = "3B-Instruct";
        m1.sizeDisplay = "2.1 GB";
        m1.params = "3B";
        m1.quantization = "Q4_K_M";
        m1.status = LocalModel.Status.NOT_DOWNLOADED; // Default state
        m1.isLocal = true;
        m1.isBuiltIn = true;
        m1.isDeepThink = false;
        m1.isVision = false;
        list.add(m1);

        // 2. Llama 3.2 (1B-Instruct)
        LocalModel m2 = new LocalModel();
        m2.name = "Llama 3.2";
        m2.version = "1B-Instruct";
        m2.sizeDisplay = "1.2 GB";
        m2.params = "1B";
        m2.quantization = "Q4_K_S";
        m2.status = LocalModel.Status.NOT_DOWNLOADED;
        m2.isLocal = true;
        m2.isBuiltIn = true;
        list.add(m2);

        // 3. Llama 3.1 (70B-Instruct)
        LocalModel m3 = new LocalModel();
        m3.name = "Llama 3.1";
        m3.version = "70B-Instruct";
        m3.sizeDisplay = "38.5 GB";
        m3.params = "70B";
        m3.quantization = "Q4_0";
        m3.status = LocalModel.Status.NOT_DOWNLOADED;
        m3.isLocal = true;
        m3.isBuiltIn = true;
        list.add(m3);

        // 4. Llama 3.2 (11B-Vision)
        LocalModel m4 = new LocalModel();
        m4.name = "Llama 3.2";
        m4.version = "11B-Vision";
        m4.sizeDisplay = "7.8 GB";
        m4.params = "11B";
        m4.quantization = "Q5_K_M";
        m4.status = LocalModel.Status.NOT_DOWNLOADED;
        m4.isLocal = true;
        m4.isBuiltIn = true;
        m4.isVision = true;
        list.add(m4);

        // 5. 豆包专业版-32k（内置网络模型）
        LocalModel m5 = new LocalModel();
        m5.name = "Doubao-pro-32k";
        m5.version = "ep-20240604055536-mkp7g"; // Example Endpoint ID
        m5.status = LocalModel.Status.READY; // Network models are ready by default
        m5.isLocal = false;
        m5.isBuiltIn = true;
        m5.isDeepThink = false;
        m5.provider = "doubao";
        m5.apiUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
        // 用户需要设置API密钥
        list.add(m5);

        // 6. DeepSeek-R1（内置网络模型）
        LocalModel m6 = new LocalModel();
        m6.name = "DeepSeek-R1";
        m6.version = "ep-20250208151448-69279"; // Example Endpoint ID
        m6.status = LocalModel.Status.READY;
        m6.isLocal = false;
        m6.isBuiltIn = true;
        m6.isDeepThink = true; // 支持思考
        m6.provider = "doubao"; // 托管在方舟/火山引擎上
        m6.apiUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
        list.add(m6);

        return list;
    }
}


