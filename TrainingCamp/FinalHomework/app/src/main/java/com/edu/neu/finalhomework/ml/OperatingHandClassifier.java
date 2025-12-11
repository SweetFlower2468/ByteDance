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
    private static final int MODEL_INPUT_SIZE = SAMPLE_COUNT * TENSOR_SIZE * 4; // 4 bytes per float

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
        
        // Inspect shapes
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

        // Prepare Output Buffer dynamically
        float[][] output;
        if (outputShape != null && outputShape.length == 2) {
            output = new float[outputShape[0]][outputShape[1]];
        } else {
            output = new float[1][2]; // Default to [1, 2] for binary class
        }

        try {
            tflite.run(inputBuffer, output);
        } catch (Exception e) {
            Log.e(TAG, "Inference error: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

        // Logic based on provided Kotlin code:
        // output > 0.5 -> Right (1)
        // output <= 0.5 -> Left (0)
        // Wait, the Kotlin code says:
        // return if (output > 0.5f) { "right" } else { "left" }
        // The output shape in Kotlin example seems to be [1][1] or [1][2] but accessing result[0][0].
        // If it's a single sigmoidal output:
        
        float score = output[0][0];
        if (output[0].length == 1) {
            // Binary classification with single output neuron (sigmoid)
            return score > 0.5f ? CLASS_RIGHT : CLASS_LEFT;
        } else {
            // Softmax with 2 neurons
            // output[0][0] = Left prob? Or Right?
            // Usually [Left, Right].
            // But let's stick to the Kotlin reference logic which seemed to use single value or specific index.
            // Reference: val output = result[0][0]; if (output > 0.5f) ...
            
            // If we have 2 outputs, let's find max.
            int maxIndex = -1;
            float maxProb = -1;
             for (int i = 0; i < output[0].length; i++) {
                if (output[0][i] > maxProb) {
                    maxProb = output[0][i];
                    maxIndex = i;
                }
            }
            // Assuming index 0 is Left, 1 is Right.
            // Need to verify model training labels.
            // Based on "output > 0.5 is right" in single neuron, typically 1=Right.
            return maxIndex; 
        }
    }
    
    private ByteBuffer convertFloatArrayToByteBuffer(JSONArray pointList) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(MODEL_INPUT_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());

        // Resampling logic: Linear interpolation / Step sampling
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
