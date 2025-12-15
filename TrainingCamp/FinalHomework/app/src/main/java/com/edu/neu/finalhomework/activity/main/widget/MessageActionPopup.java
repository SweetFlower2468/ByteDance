package com.edu.neu.finalhomework.activity.main.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.neu.finalhomework.R;
import com.edu.neu.finalhomework.activity.main.adapter.PopupActionAdapter;
import com.edu.neu.finalhomework.domain.entity.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息操作弹出菜单
 * 对应 popup_msg_actions.xml
 */
public class MessageActionPopup extends PopupWindow {
    
    private Context context;
    private RecyclerView recyclerView;
    private OnActionSelectListener listener;

    public static final int ACTION_COPY = 1;
    public static final int ACTION_QUOTE = 2;
    // public static final int ACTION_SELECT_TEXT = 3; // 已移除
    public static final int ACTION_LIKE = 4;
    public static final int ACTION_DISLIKE = 5;
    public static final int ACTION_DELETE = 6;
    public static final int ACTION_FAVORITE = 7;
    public static final int ACTION_TTS = 8;
    public static final int ACTION_REGENERATE = 9;

    public interface OnActionSelectListener {
        void onActionSelect(int action, Message message);
    }

    public MessageActionPopup(Context context, OnActionSelectListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        init(context);
    }
    
    private void init(Context context) {
        // 通过带父容器的 inflate 保留布局宽度参数
        View contentView = LayoutInflater.from(context).inflate(R.layout.popup_msg_actions, new android.widget.FrameLayout(context), false);
        setContentView(contentView);
        
        // 基础属性设置
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        if (layoutParams != null && layoutParams.width > 0) {
            setWidth(layoutParams.width);
        } else {
            setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setOutsideTouchable(true);
        setElevation(8);

        recyclerView = contentView.findViewById(R.id.recycler_popup_actions);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }
    
    private int dip2px(float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
    
    /**
     * 显示弹出菜单
     */
    public void show(View anchor, Message message) {
        List<PopupActionAdapter.ActionItem> items = new ArrayList<>();
        
        // 通用操作
        items.add(new PopupActionAdapter.ActionItem(ACTION_COPY, R.drawable.ic_copy, "复制"));
        items.add(new PopupActionAdapter.ActionItem(ACTION_QUOTE, R.drawable.ic_quote, "引用"));
        // items.add(new PopupActionAdapter.ActionItem(ACTION_SELECT_TEXT, R.drawable.ic_search, "选择文本")); // 已移除
        
        if ("ai".equals(message.type)) {
            items.add(new PopupActionAdapter.ActionItem(ACTION_TTS, R.drawable.ic_volume_high, "朗读"));
            items.add(new PopupActionAdapter.ActionItem(ACTION_REGENERATE, R.drawable.ic_refresh, "重新生成"));
            
            // 动态图标与文案
            PopupActionAdapter.ActionItem favItem = new PopupActionAdapter.ActionItem(
                ACTION_FAVORITE, 
                message.isFavorite ? R.drawable.ic_star_filled : R.drawable.ic_star, 
                message.isFavorite ? "取消收藏" : "收藏"
            );
            if (message.isFavorite) favItem.colorRes = R.color.brand_primary;
            items.add(favItem);
            
            PopupActionAdapter.ActionItem likeItem = new PopupActionAdapter.ActionItem(ACTION_LIKE, R.drawable.ic_thumb_up, message.isLiked ? "取消点赞" : "点赞");
            if (message.isLiked) likeItem.colorRes = R.color.brand_primary;
            items.add(likeItem);

            PopupActionAdapter.ActionItem dislikeItem = new PopupActionAdapter.ActionItem(ACTION_DISLIKE, R.drawable.ic_thumb_down, message.isDisliked ? "取消点踩" : "不喜欢");
            if (message.isDisliked) dislikeItem.colorRes = R.color.error;
            items.add(dislikeItem);
            
            PopupActionAdapter.ActionItem deleteItem = new PopupActionAdapter.ActionItem(ACTION_DELETE, R.drawable.ic_trash_red, "删除");
            deleteItem.colorRes = R.color.error; // 删除使用红色强调
            items.add(deleteItem);
        } else {
             // 用户消息的操作
             PopupActionAdapter.ActionItem deleteItem = new PopupActionAdapter.ActionItem(ACTION_DELETE, R.drawable.ic_trash_red, "删除");
             deleteItem.colorRes = R.color.error; 
             items.add(deleteItem);
        }

        PopupActionAdapter adapter = new PopupActionAdapter(items, item -> {
            if (listener != null) {
                listener.onActionSelect(item.id, message);
            }
            dismiss();
        });
        recyclerView.setAdapter(adapter);

        // 计算偏移：此处让菜单在锚点上方大致居中
        showAsDropDown(anchor, 0, -anchor.getHeight() / 2); // 粗略垂直居中
        // 如需更精确可调用 showAsDropDown(anchor, x, y)
    }
}
