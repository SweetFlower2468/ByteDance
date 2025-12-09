package com.edu.neu.finalhomework.utils;

import android.content.Context;
import android.text.Spanned;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.linkify.LinkifyPlugin;

/**
 * Markdown 渲染工具类
 * 配置 Markwon 渲染器
 */
public class MarkdownUtils {
    
    private static Markwon markwon;
    
    /**
     * 初始化 Markwon
     */
    public static void init(Context context) {
        markwon = Markwon.builder(context)
                .usePlugin(TablePlugin.create(context))
                .usePlugin(LinkifyPlugin.create())
                .build();
        
        // 注意：如果需要图片支持，需要添加 GlideImagesPlugin
        // 需要确保已添加依赖：io.noties.markwon:image-glide:4.6.2
        // 然后取消下面的注释：
        // import io.noties.markwon.image.glide.GlideImagesPlugin;
        // .usePlugin(GlideImagesPlugin.create(context))
    }
    
    /**
     * 渲染 Markdown 文本
     */
    public static Spanned render(String markdown) {
        if (markwon == null) {
            throw new IllegalStateException("Markwon not initialized. Call init() first.");
        }
        return markwon.toMarkdown(markdown);
    }
    
    /**
     * 获取 Markwon 实例
     */
    public static Markwon getMarkwon() {
        return markwon;
    }
}
