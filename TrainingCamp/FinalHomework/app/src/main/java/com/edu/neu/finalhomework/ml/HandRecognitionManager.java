package com.edu.neu.finalhomework.ml;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import org.json.JSONArray;

import com.edu.neu.finalhomework.config.HandConfig;

public class HandRecognitionManager implements MotionEventTracker.OnTrackListener, SensorEventListener {
    private static final String TAG = "HandRecognitionManager";
    private Context context;
    private MotionEventTracker tracker;
    private OperatingHandClassifier classifier;
    private OnHandChangeListener listener;
    
    // Gravity Sensor
    private SensorManager sensorManager;
    private Sensor gravitySensor;
    private boolean isSensorRegistered = false;
    private float lastStableGravityX = 0f;
    private boolean hasGravityData = false;
    
    // State Management
    private boolean currentHandIsLeft = false; // Default Right
    private int consecutiveWrongCount = 0;
    
    // Thresholds
    // Moved to HandConfig
    
    // UI Handler
    private Handler handler = new Handler(Looper.getMainLooper());
    private int screenWidth;

    public interface OnHandChangeListener {
        void onHandChanged(boolean isLeftHand);
    }

    public HandRecognitionManager(Context context) {
        this.context = context;
        
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        this.screenWidth = metrics.widthPixels;
        this.tracker = new MotionEventTracker(metrics.widthPixels, metrics.heightPixels, metrics.density);
        this.tracker.setListener(this);
        
        this.classifier = new OperatingHandClassifier(context);
        
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            this.gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            // Fallback to Accelerometer if Gravity not available
            if (this.gravitySensor == null) {
                this.gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }
        }
    }

    public void setListener(OnHandChangeListener listener) {
        this.listener = listener;
    }
    
    public void start() {
        if (!isSensorRegistered && sensorManager != null && gravitySensor != null) {
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            isSensorRegistered = true;
        }
    }
    
    public void stop() {
        if (isSensorRegistered && sensorManager != null) {
            sensorManager.unregisterListener(this);
            isSensorRegistered = false;
        }
    }

    public void processTouchEvent(MotionEvent event) {
        if (tracker != null) {
            tracker.recordMotionEvent(event);
        }
    }
    
    // --- Sensor Logic ---

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY || event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            
            // "Gravity sensor only refreshes page when change is large"
            if (!hasGravityData || Math.abs(x - lastStableGravityX) > HandConfig.GRAVITY_CHANGE_THRESHOLD) {
                lastStableGravityX = x;
                hasGravityData = true;
                
                // Determine Hand based on Gravity
                boolean newHandIsLeft = currentHandIsLeft;
                
                if (x < -HandConfig.GRAVITY_SIDE_LYING_THRESHOLD) {
                    // Right Side Lying (X < -8.8) -> Left Hand holds?
                    newHandIsLeft = true;
                } else if (x > HandConfig.GRAVITY_SIDE_LYING_THRESHOLD) {
                    // Left Side Lying (X > 8.8) -> Right Hand holds?
                    newHandIsLeft = false;
                } else if (x > HandConfig.GRAVITY_TILT_THRESHOLD) {
                    // Normal Left Tilt -> Left Hand
                    newHandIsLeft = true;
                } else if (x < -HandConfig.GRAVITY_TILT_THRESHOLD) {
                    // Normal Right Tilt -> Right Hand
                    newHandIsLeft = false;
                } else {
                    // -1 <= x <= 1: Both Hands -> Right Hand (Default)
                    newHandIsLeft = false;
                }
                
                updateHandState(newHandIsLeft, true); // Force update due to gravity change
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // --- Touch Logic ---

    @Override
    public void onTrackDataReady(JSONArray trackData) {
        new Thread(() -> {
            int result = -1;
            if (classifier.isModelLoaded()) {
                result = classifier.classify(trackData);
            }
            
            if (result != -1) {
                boolean isLeft = (result == OperatingHandClassifier.CLASS_LEFT);
                handler.post(() -> handleTouchInput(isLeft));
            }
        }).start();
    }
    
    @Override
    public void onTap(float x, float y) {
        // Simple tap heuristic:
        // Left half -> Left Hand
        // Right half -> Right Hand
        boolean isLeft = x < (screenWidth / 2f);
        handleTouchInput(isLeft);
    }
    
    private void handleTouchInput(boolean detectedIsLeft) {
        if (detectedIsLeft == currentHandIsLeft) {
            consecutiveWrongCount = 0;
        } else {
            consecutiveWrongCount++;
            if (consecutiveWrongCount >= HandConfig.TOUCH_OVERRIDE_COUNT) {
                // "If continuous 3 times are other hand ... need to change button"
                updateHandState(detectedIsLeft, false);
                consecutiveWrongCount = 0;
            }
        }
    }
    
    private void updateHandState(boolean isLeft, boolean isGravityForced) {
        if (currentHandIsLeft != isLeft) {
            currentHandIsLeft = isLeft;
            if (listener != null) {
                listener.onHandChanged(currentHandIsLeft);
            }
        }
        // If gravity forced (large change), we reset counters
        if (isGravityForced) {
            consecutiveWrongCount = 0;
        }
    }

    public void close() {
        stop();
        if (classifier != null) {
            classifier.close();
        }
    }
}
