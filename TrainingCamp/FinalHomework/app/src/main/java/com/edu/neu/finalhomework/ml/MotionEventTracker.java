package com.edu.neu.finalhomework.ml;

import android.view.MotionEvent;
import org.json.JSONArray;
import org.json.JSONException;

public class MotionEventTracker {
    private static final String TAG = "MotionEventTracker";
    private JSONArray currentEvents;
    private long currentDownTime;
    private int width;
    private int height;
    private float density;
    private OnTrackListener listener;

    public interface OnTrackListener {
        void onTrackDataReady(JSONArray trackData);
        void onTap(float x, float y);
    }

    public MotionEventTracker(int width, int height, float density) {
        this.width = width;
        this.height = height;
        this.density = density;
    }

    public void setListener(OnTrackListener listener) {
        this.listener = listener;
    }

    public void recordMotionEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentEvents = new JSONArray();
                currentDownTime = ev.getEventTime();
                addPoint(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentEvents != null) {
                    addPoint(ev);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (currentEvents != null) {
                    addPoint(ev);
                    if (currentEvents.length() >= 6) { // Filter short taps
                        if (listener != null) {
                            listener.onTrackDataReady(currentEvents);
                        }
                    } else {
                        // Handle Tap
                        if (listener != null) {
                            listener.onTap(ev.getX(), ev.getY());
                        }
                    }
                    currentEvents = null;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                currentEvents = null;
                break;
        }
    }

    private void addPoint(MotionEvent ev) {
        if (currentEvents == null) return;
        try {
            JSONArray point = new JSONArray();
            point.put(ev.getX());          // Feature 1: X
            point.put(ev.getY());          // Feature 2: Y
            point.put(width);              // Feature 3: Width
            point.put(height);             // Feature 4: Height
            point.put(density);            // Feature 5: Density
            point.put(ev.getEventTime() - currentDownTime); // Feature 6: Relative Time
            currentEvents.put(point);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

