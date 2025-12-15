package com.edu.neu.finalhomework.activity.main.adapter.holder;

import android.view.View;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.main.adapter.ChatAdapter;
import com.edu.neu.finalhomework.domain.entity.Message;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;

/**
 * AI 消息 ViewHolder
 * 对应 item_msg_ai.xml
 */
public class AiMsgHolder extends RecyclerView.ViewHolder {
    
    private TextView tvContent;
    private View viewDeepThink;
    private TextView tvDtContent;
    private TextView tvDtDuration;
    private ImageView ivDtArrow;
    private View layoutActions;
    private View loadingIndicator;
    
    // 操作按钮区
    private View btnCopy;
    private View btnTts;
    private View btnFavorite;
    private View btnRegenerate;
    private View btnDelete;

    private ChatAdapter.OnMessageActionListener listener;
    // 已移除本地状态字段：private boolean isDeepThinkExpanded = true;
    private boolean isReadOnly = false;
    private Markwon markwon;
    private Message currentMessage; // 保存当前消息以便切换状态
    private boolean longPressTriggered = false; // 避免长按后仍触发单击滚动
    private View lastTouchView = null; // 记录触发手势的视图，用于菜单定位

    public AiMsgHolder(View itemView, ChatAdapter.OnMessageActionListener listener) {
        super(itemView);
        this.listener = listener;
        markwon = Markwon.builder(itemView.getContext())
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(TablePlugin.create(itemView.getContext()))
                .usePlugin(LinkifyPlugin.create())
                .build();
        
        tvContent = itemView.findViewById(R.id.tv_content);
        if (tvContent != null) {
            // 禁用系统文本选择，事件交给自定义菜单
            tvContent.setTextIsSelectable(false);
            tvContent.setClickable(false);
            tvContent.setLongClickable(false);
            tvContent.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
                @Override public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) { return false; }
                @Override public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) { return false; }
                @Override public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) { return false; }
                @Override public void onDestroyActionMode(android.view.ActionMode mode) {}
            });
        }
        viewDeepThink = itemView.findViewById(R.id.view_deep_think);
        
        if (viewDeepThink != null) {
            tvDtContent = viewDeepThink.findViewById(R.id.tv_dt_content);
            if (tvDtContent != null) {
                tvDtContent.setTextIsSelectable(false);
                tvDtContent.setClickable(false);
                tvDtContent.setLongClickable(false);
                tvDtContent.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
                    @Override public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) { return false; }
                    @Override public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) { return false; }
                    @Override public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) { return false; }
                    @Override public void onDestroyActionMode(android.view.ActionMode mode) {}
                });
            }
            tvDtDuration = viewDeepThink.findViewById(R.id.tv_dt_duration);
            ivDtArrow = viewDeepThink.findViewById(R.id.iv_dt_arrow);
            
            // 展开/收起监听
            viewDeepThink.setOnClickListener(v -> toggleDeepThink());
        }
        
        loadingIndicator = itemView.findViewById(R.id.loading_indicator);
        // 其他初始化
        layoutActions = itemView.findViewById(R.id.layout_actions);
        if (layoutActions != null) {
            btnCopy = layoutActions.findViewById(R.id.btn_copy);
            btnTts = layoutActions.findViewById(R.id.btn_tts);
            btnFavorite = layoutActions.findViewById(R.id.btn_favorite);
            btnRegenerate = layoutActions.findViewById(R.id.btn_regenerate);
            btnDelete = layoutActions.findViewById(R.id.btn_delete);
        }
    }
    
    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        if (layoutActions != null) {
            layoutActions.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        }
    }

    private void toggleDeepThink() {
        if (tvDtContent == null || currentMessage == null) return;
        
        // 切换模型中的展开状态
        currentMessage.isDeepThinkExpanded = !currentMessage.isDeepThinkExpanded;
        
        // 更新界面
        updateDeepThinkUI(currentMessage.isDeepThinkExpanded);
    }
    
    private void updateDeepThinkUI(boolean expanded) {
        if (tvDtContent != null) {
            tvDtContent.setVisibility(expanded ? View.VISIBLE : View.GONE);
        }
        if (tvDtDuration != null) {
            tvDtDuration.setVisibility(expanded ? View.VISIBLE : View.GONE);
        }
        if (ivDtArrow != null) {
            ivDtArrow.animate().rotation(expanded ? 0 : -90).setDuration(200).start();
        }
    }
    
    public void bind(Message message, long playingMessageId) {
        if (message == null) return;
        this.currentMessage = message;

        // 绑定正文
        if (tvContent != null) {
            markwon.setMarkdown(tvContent, prepareLatexContent(message.content));
        }
        
        // 控制加载动画显隐
        if (loadingIndicator != null) {
            if (message.isGenerating) {
                loadingIndicator.setVisibility(View.VISIBLE);
                if (tvContent != null && (message.content == null || message.content.isEmpty())) {
                    tvContent.setVisibility(View.GONE);
                } else {
                    tvContent.setVisibility(View.VISIBLE);
                }
            } else {
                loadingIndicator.setVisibility(View.GONE);
                if (tvContent != null) tvContent.setVisibility(View.VISIBLE);
            }
        }

        // 绑定深度思考区域
        if (viewDeepThink != null) {
            boolean hasDeepThink = message.deepThink != null && !message.deepThink.isEmpty();
            
            if (hasDeepThink) {
                viewDeepThink.setVisibility(View.VISIBLE);
                if (tvDtContent != null) {
                    // 思考过程同样渲染 Markdown
                    markwon.setMarkdown(tvDtContent, prepareLatexContent(message.deepThink));
                }
                
                // 使用消息持久化的展开状态
                updateDeepThinkUI(message.isDeepThinkExpanded);
                // 绑定时立即设置箭头角度（不做动画）
                if (ivDtArrow != null) {
                    ivDtArrow.setRotation(message.isDeepThinkExpanded ? 0 : -90);
                }
                
            } else {
                viewDeepThink.setVisibility(View.GONE);
            }
        }
        
        // 绑定操作按钮
        if (listener != null) {
            if (btnCopy != null) btnCopy.setOnClickListener(v -> listener.onCopy(message));
            if (btnTts != null) {
                btnTts.setOnClickListener(v -> listener.onTts(message));
                updateTtsState(message.id == playingMessageId);
            }
            if (btnFavorite != null) {
                btnFavorite.setOnClickListener(v -> listener.onFavorite(message));
                // 更新收藏按钮状态
                if (btnFavorite instanceof ImageView) {
                    ImageView iv = (ImageView) btnFavorite;
                    if (message.isFavorite) {
                        iv.setImageResource(R.drawable.ic_star_filled);
                        iv.setColorFilter(iv.getContext().getResources().getColor(R.color.brand_primary));
                    } else {
                        iv.setImageResource(R.drawable.ic_star);
                        iv.setColorFilter(iv.getContext().getResources().getColor(R.color.text_secondary));
                    }
                }
            }
            if (btnRegenerate != null) btnRegenerate.setOnClickListener(v -> listener.onRegenerate(message));
            if (btnDelete != null) btnDelete.setOnClickListener(v -> listener.onDelete(message));
        }
        
        setupTouchHandlers(message);
    }
    
    private String prepareLatexContent(String raw) {
        if (raw == null) return "";
        return raw;
    }

    /**
     * 设置触摸行为：
     * - 生成中：关闭所有焦点/触摸，避免抖动与菜单
     * - 完成后：单击滚到顶部，长按弹菜单，长按后不触发单击滚动
     */
    private void setupTouchHandlers(Message message) {
        if (itemView == null) return;

        View.OnTouchListener blockTouch = (v, event) -> true;

        if (message != null && message.isGenerating) {
            // 生成中：禁用触摸/焦点，防止抖动
            itemView.setOnTouchListener(blockTouch);
            itemView.setLongClickable(false);
            if (tvContent != null) {
                tvContent.setOnTouchListener(blockTouch);
                tvContent.setLongClickable(false);
                tvContent.setClickable(false);
            }
            if (tvDtContent != null) {
                tvDtContent.setOnTouchListener(blockTouch);
                tvDtContent.setLongClickable(false);
                tvDtContent.setClickable(false);
            }
            return;
        }

        // 生成结束：恢复手势
        GestureDetector detector = new GestureDetector(itemView.getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        // 必须返回 true 才能接收后续事件（长按）
                        return true;
                    }

                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        scrollToTopSmooth();
                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        longPressTriggered = true;
                        View anchor = lastTouchView != null ? lastTouchView : itemView;
                        if (listener != null) {
                            listener.onLongClick(anchor, message);
                        } else {
                            Log.w("AiMsgHolder", "onLongClick no listener");
                        }
                    }
                });

        View.OnTouchListener touchRelay = (v, event) -> {
            lastTouchView = v;
            boolean handled = detector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (longPressTriggered) {
                    longPressTriggered = false;
                    return true; // 消费抬手，避免长按后触发单击滚动
                }
            }
            return handled;
        };

        itemView.setOnTouchListener(touchRelay);
        itemView.setLongClickable(true);

        if (tvContent != null) {
            tvContent.setOnTouchListener(touchRelay);
            tvContent.setClickable(false);
            tvContent.setLongClickable(false);
        }
        if (tvDtContent != null) {
            tvDtContent.setOnTouchListener(touchRelay);
            tvDtContent.setClickable(false);
            tvDtContent.setLongClickable(false);
        }
    }

    private void scrollToTopSmooth() {
        RecyclerView rv = null;
        if (itemView != null && itemView.getParent() instanceof RecyclerView) {
            rv = (RecyclerView) itemView.getParent();
        }
        if (rv == null) {
            Log.w("AiMsgHolder", "scrollToTopSmooth no RecyclerView parent");
            return;
        }
        int dy = itemView.getTop() - rv.getPaddingTop();
        if (dy != 0) {
            rv.smoothScrollBy(0, dy);
        }
    }

    public void updateTtsState(boolean isPlaying) {
        if (btnTts instanceof ImageView) {
            ImageView iv = (ImageView) btnTts;
            if (isPlaying) {
                iv.setImageResource(R.drawable.anim_speaker_playing);
                android.graphics.drawable.AnimationDrawable anim = (android.graphics.drawable.AnimationDrawable) iv.getDrawable();
                anim.start();
                iv.setColorFilter(iv.getContext().getResources().getColor(R.color.brand_primary));
            } else {
                iv.setImageResource(R.drawable.ic_volume_high);
                iv.setColorFilter(iv.getContext().getResources().getColor(R.color.text_secondary));
            }
        }
    }
}

