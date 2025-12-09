package com.edu.neu.finalhomework.config;

import com.edu.neu.finalhomework.domain.entity.LocalModel;
import java.util.ArrayList;
import java.util.List;

public class ModelConfig {
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

        return list;
    }
}


