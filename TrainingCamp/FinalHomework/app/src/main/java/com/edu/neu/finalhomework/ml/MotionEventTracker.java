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
                    if (currentEvents.length() >= 6) { // 过滤短按
                        if (listener != null) {
                            listener.onTrackDataReady(currentEvents);
                        }
                    } else {
                        // 处理点击
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
            point.put(ev.getX());          // 特征1：X
            point.put(ev.getY());          // 特征2：Y
            point.put(width);              // 特征3：屏幕宽
            point.put(height);             // 特征4：屏幕高
            point.put(density);            // 特征5：屏幕密度
            point.put(ev.getEventTime() - currentDownTime); // 特征6：相对时间
            currentEvents.put(point);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

