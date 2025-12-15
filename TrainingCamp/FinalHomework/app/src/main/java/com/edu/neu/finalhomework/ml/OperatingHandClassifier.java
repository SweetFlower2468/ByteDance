package com.edu.neu.finalhomework.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class OperatingHandClassifier {
    private static final String TAG = "OperatingHandClassifier";
    private Interpreter tflite;
    private static final String MODEL_FILE = "mymodel.tflite";

    private static final int SAMPLE_COUNT = 9;
    private static final int TENSOR_SIZE = 6;
    private static final int MODEL_INPUT_SIZE = SAMPLE_COUNT * TENSOR_SIZE * 4; // 每个 float 4 字节

    // Labels
    public static final int CLASS_LEFT = 0;
    public static final int CLASS_RIGHT = 1;

    private int[] inputShape;
    private int[] outputShape;

    public OperatingHandClassifier(Context context) {
        try {
            loadModelFile(context.getAssets());
        } catch (IOException e) {
            Log.e(TAG, "Error loading model file: " + e.getMessage());
        }
    }

    private void loadModelFile(AssetManager assetManager) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        
        Interpreter.Options options = new Interpreter.Options();
        tflite = new Interpreter(modelBuffer, options);
        
        // 读取输入/输出形状
        if (tflite.getInputTensorCount() > 0) {
            Tensor inputTensor = tflite.getInputTensor(0);
            inputShape = inputTensor.shape();
            Log.d(TAG, "Model Input Shape: " + Arrays.toString(inputShape));
        }
        
        if (tflite.getOutputTensorCount() > 0) {
            Tensor outputTensor = tflite.getOutputTensor(0);
            outputShape = outputTensor.shape();
            Log.d(TAG, "Model Output Shape: " + Arrays.toString(outputShape));
        }
        
        Log.d(TAG, "Model loaded successfully");
    }

    public int classify(JSONArray pointList) {
        if (tflite == null) return -1;
        
        ByteBuffer inputBuffer = convertFloatArrayToByteBuffer(pointList);

        // 动态准备输出缓冲
        float[][] output;
        if (outputShape != null && outputShape.length == 2) {
            output = new float[outputShape[0]][outputShape[1]];
        } else {
            output = new float[1][2]; // 二分类默认 [1,2]
        }

        try {
            tflite.run(inputBuffer, output);
        } catch (Exception e) {
            Log.e(TAG, "Inference error: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

        // 判别逻辑：
        // 单输出 sigmoid：>0.5 判为右手，否则左手
        // 双输出 softmax：取最大概率索引
        float score = output[0][0];
        if (output[0].length == 1) {
            // 单输出（sigmoid）二分类
            return score > 0.5f ? CLASS_RIGHT : CLASS_LEFT;
        } else {
            // 双输出（softmax）→ 取最大概率
            int maxIndex = -1;
            float maxProb = -1;
             for (int i = 0; i < output[0].length; i++) {
                if (output[0][i] > maxProb) {
                    maxProb = output[0][i];
                    maxIndex = i;
                }
            }
            return maxIndex; 
        }
    }
    
    private ByteBuffer convertFloatArrayToByteBuffer(JSONArray pointList) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(MODEL_INPUT_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());

        // 重新采样：按步长选点（近似插值）
        float step = (float) pointList.length() / SAMPLE_COUNT;

        try {
            for (int i = 0; i < SAMPLE_COUNT; i++) {
                int index = (int) (i * step);
                if (index >= pointList.length()) index = pointList.length() - 1;
                
                JSONArray point = pointList.getJSONArray(index);
                for (int j = 0; j < TENSOR_SIZE; j++) {
                    float value = (float) point.getDouble(j);
                    byteBuffer.putFloat(value);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON point data", e);
        }

        return byteBuffer;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
    
    public boolean isModelLoaded() {
        return tflite != null;
    }
}
