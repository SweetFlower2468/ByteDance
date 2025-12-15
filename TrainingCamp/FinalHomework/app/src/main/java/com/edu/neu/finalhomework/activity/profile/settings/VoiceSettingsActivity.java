package com.edu.neu.finalhomework.activity.profile.settings;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.base.BaseActivity;
import com.edu.neu.finalhomework.activity.main.network.TTSManager;
import com.edu.neu.finalhomework.utils.SPUtils;
import com.edu.neu.finalhomework.utils.ToastUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.slider.Slider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.material.textfield.TextInputEditText;

public class VoiceSettingsActivity extends BaseActivity {

    private RecyclerView recyclerVoices;
    private VoiceAdapter adapter;
    private Slider sliderPitch;
    private Slider sliderSpeed;
    private TextView tvPitchValue;
    private TextView tvSpeedValue;
    private TextView tvCurrentVoice;
    private TextInputEditText etAppId;
    private TextInputEditText etToken;
    private TextView tabRecommended, tabFemale, tabMale;

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    // 配置缓存
    private String selectedVoiceId;
    private int pitchVal; // 0-100
    private int speedVal; // 0-100
    private List<VoiceOption> allVoices = new ArrayList<>();
    private String currentCategory = "recommended"; // 分类：推荐/女声/男声
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_settings);

        initViews();
        initData();
        initListeners();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // 自定义返回键说明：使用 setSupportActionBar 后采用标准导航图标（约 48dp 命中区域），
        // XML 已恢复 app:navigationIcon，不再额外绑定自定义 ImageView，返回/保存交由 Toolbar 导航与菜单处理。

        recyclerVoices = findViewById(R.id.recycler_voices);
        recyclerVoices.setLayoutManager(new LinearLayoutManager(this));
        recyclerVoices.setNestedScrollingEnabled(false);

        sliderPitch = findViewById(R.id.slider_pitch);
        sliderSpeed = findViewById(R.id.slider_speed);
        tvPitchValue = findViewById(R.id.tv_pitch_value);
        tvSpeedValue = findViewById(R.id.tv_speed_value);
        tvCurrentVoice = findViewById(R.id.tv_current_voice_name);
        
        etAppId = findViewById(R.id.et_app_id);
        etToken = findViewById(R.id.et_token);
        
        tabRecommended = findViewById(R.id.tab_recommended);
        tabFemale = findViewById(R.id.tab_female);
        tabMale = findViewById(R.id.tab_male);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_voice_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveConfig();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void initData() {
        // 读取本地配置
        selectedVoiceId = SPUtils.getString("tts_voice_id", "BV700_streaming");
        pitchVal = SPUtils.getInt("tts_pitch", 50);
        speedVal = SPUtils.getInt("tts_speed", 50);
        
        String appId = SPUtils.getString("tts_app_id", "");
        String token = SPUtils.getString("tts_token", "");
        etAppId.setText(appId);
        etToken.setText(token);

        if (sliderPitch != null) sliderPitch.setValue(pitchVal);
        if (sliderSpeed != null) sliderSpeed.setValue(speedVal);
        updateValueTexts();

        // 加载音色列表
        allVoices.clear();
        allVoices.add(new VoiceOption("BV700_streaming", "温柔桃子", "亲切自然，适合日常对话", true, "female"));
        allVoices.add(new VoiceOption("BV406_streaming", "开朗青年", "阳光活力，充满热情", true, "male"));
        allVoices.add(new VoiceOption("BV001_streaming", "通用女声", "标准播音，清晰准确", false, "female"));
        allVoices.add(new VoiceOption("BV002_streaming", "通用男声", "沉稳大气，富有磁性", false, "male"));
        
        filterVoices("recommended");
        updateCurrentVoiceText(allVoices);
    }
    
    private void filterVoices(String category) {
        currentCategory = category;
        List<VoiceOption> filtered = new ArrayList<>();
        
        for (VoiceOption v : allVoices) {
            if ("recommended".equals(category)) {
                if (v.isRecommended) filtered.add(v);
            } else if ("female".equals(category)) {
                if ("female".equals(v.gender)) filtered.add(v);
            } else if ("male".equals(category)) {
                if ("male".equals(v.gender)) filtered.add(v);
            }
        }
        
        adapter = new VoiceAdapter(filtered);
        recyclerVoices.setAdapter(adapter);
        updateTabsUI();
    }
    
    private void updateTabsUI() {
        updateTabStyle(tabRecommended, "recommended".equals(currentCategory));
        updateTabStyle(tabFemale, "female".equals(currentCategory));
        updateTabStyle(tabMale, "male".equals(currentCategory));
    }
    
    private void updateTabStyle(TextView tab, boolean isSelected) {
        if (tab == null) return;
        if (isSelected) {
            tab.setTextColor(getResources().getColor(R.color.text_primary));
            tab.setTypeface(null, android.graphics.Typeface.BOLD);
            tab.setTextSize(16);
        } else {
            tab.setTextColor(getResources().getColor(R.color.text_secondary));
            tab.setTypeface(null, android.graphics.Typeface.NORMAL);
            tab.setTextSize(16);
        }
    }
    
    private void saveConfig() {
        String appId = etAppId.getText().toString().trim();
        String token = etToken.getText().toString().trim();
        
        SPUtils.putString("tts_app_id", appId);
        SPUtils.putString("tts_token", token);
        SPUtils.putInt("tts_pitch", pitchVal);
        SPUtils.putInt("tts_speed", speedVal);
        SPUtils.putString("tts_voice_id", selectedVoiceId);
        
        ToastUtils.show(this, "设置已保存");
        finish();
    }
    
    private void resetConfig() {
        pitchVal = 50;
        speedVal = 50;
        if (sliderPitch != null) sliderPitch.setValue(pitchVal);
        if (sliderSpeed != null) sliderSpeed.setValue(speedVal);
        updateValueTexts();
        ToastUtils.show(this, "已重置为默认");
    }

    private void updateValueTexts() {
        if (tvPitchValue != null) tvPitchValue.setText(String.valueOf(pitchVal));
        if (tvSpeedValue != null) tvSpeedValue.setText(String.valueOf(speedVal));
    }
    
    private void updateCurrentVoiceText(List<VoiceOption> voices) {
        if (tvCurrentVoice == null) return;
        for (VoiceOption v : voices) {
            if (v.id.equals(selectedVoiceId)) {
                tvCurrentVoice.setText(v.name);
                break;
            }
        }
    }

    private void initListeners() {
        if (sliderPitch != null) {
            sliderPitch.addOnChangeListener((slider, value, fromUser) -> {
                pitchVal = (int) value;
                updateValueTexts();
            });
        }

        if (sliderSpeed != null) {
            sliderSpeed.addOnChangeListener((slider, value, fromUser) -> {
                speedVal = (int) value;
                updateValueTexts();
            });
        }
        
        // 左侧“试听”图标（占位，未实际调用 TTS）
        View ivSpeaker = findViewById(R.id.iv_speaker_mock);
        if (ivSpeaker != null) {
            ivSpeaker.setOnClickListener(v -> ToastUtils.show(this, "试听功能需调用原生TTS接口"));
        }
        
        // 右侧重置按钮（原为刷新/检查入口）
        View btnReset = findViewById(R.id.btn_reset);
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> resetConfig());
        }
        
        // 顶部标签切换
        if (tabRecommended != null) tabRecommended.setOnClickListener(v -> filterVoices("recommended"));
        if (tabFemale != null) tabFemale.setOnClickListener(v -> filterVoices("female"));
        if (tabMale != null) tabMale.setOnClickListener(v -> filterVoices("male"));
    }

    private void playPreview(String voiceId, String text) {
        // 按需求禁用真实试听网络请求
        ToastUtils.show(this, "正在调用语音合成接口..."); 
        // 不发起实际接口调用
    }

    private void playAudio(byte[] audioData) {
        try {
            File tempFile = File.createTempFile("tts_preview", ".mp3", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(audioData);
            fos.close();

            stopAudio();

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                tempFile.delete();
            });

        } catch (IOException e) {
            ToastUtils.show(this, "播放失败");
            e.printStackTrace();
        }
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudio();
    }

    // --- 适配器 ---
    class VoiceAdapter extends RecyclerView.Adapter<VoiceAdapter.ViewHolder> {
        private List<VoiceOption> list;

        public VoiceAdapter(List<VoiceOption> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voice_option, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            VoiceOption item = list.get(position);
            holder.tvName.setText(item.name);
            holder.tvDesc.setText(item.desc);
            holder.rbSelect.setChecked(item.id.equals(selectedVoiceId));

            holder.itemView.setOnClickListener(v -> {
                selectedVoiceId = item.id;
                SPUtils.putString("tts_voice_id", selectedVoiceId);
                notifyDataSetChanged();
                updateCurrentVoiceText(allVoices); // 即便过滤列表，也用完整列表定位名称
            });

            holder.ivPlay.setOnClickListener(v -> playPreview(item.id, "你好，我是 " + item.name));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc;
            RadioButton rbSelect;
            ImageView ivPlay;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_voice_name);
                tvDesc = itemView.findViewById(R.id.tv_voice_desc);
                rbSelect = itemView.findViewById(R.id.rb_select);
                ivPlay = itemView.findViewById(R.id.iv_play);
            }
        }
    }

    // --- 数据模型 ---
    static class VoiceOption {
        String id;
        String name;
        String desc;
        boolean isRecommended;
        String gender; // "male" 或 "female" 表示性别

        public VoiceOption(String id, String name, String desc, boolean isRecommended, String gender) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.isRecommended = isRecommended;
            this.gender = gender;
        }
    }
}
