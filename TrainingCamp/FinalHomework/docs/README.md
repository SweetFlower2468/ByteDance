# 技术设计说明（对话页 / 模型管理 / 左右手识别）

> 聚焦实现细节，不做使用引导。涉及的文件以核心逻辑为主：`ChatActivity`、`ChatController`、`ChatAdapter/holders`、`ChatInputPanel`、`ModelManagerActivity`、`ImportModelDialogFragment`、`LlamaService`、`ArkClient`、`HandRecognitionManager`、`MotionEventTracker`、`OperatingHandClassifier` 等。

---

## Part 1：实现方式（按功能拆解）

### 1.1 聊天页面（UI 骨架 + 输入/发送/历史）
- **布局骨架（前端）**：ConstraintLayout 组合 AppBar + RecyclerView(LinearLayoutManager) 聊天列表 + 底部 ChatInputPanel。列表内部多 ViewType（AI/User/File），ViewHolder 内部再含图片/附件子视图，相当于“列表 + 复合子项”嵌套。
- **交互 + 技术流**：
  1) 输入阶段：ChatInputPanel（ConstraintLayout + EditText + 附件区 + 引用条 + 开关）。PDF 走 `PdfUtils.extractTextFromPdf` + `TokenUtils`；左右手切换用 ConstraintSet 动态改按钮位置；状态机 IDLE/TYPING/PANEL_OPEN/GENERATING 控制按钮图标。  
     - 技术：PdfUtils(PDFBox)、TokenUtils、ConstraintSet、Handler 主线程、左/右手模式回调。
  2) 发送点击：ChatActivity 立刻用 Room 写一条 user，再写一条 AI 占位（isGenerating=true），RecyclerView 滚到底部。  
     - 技术：Room(MessageDao/SessionDao)、RecyclerView payload 局部刷新、线程池 + Handler。
  3) 构建 Prompt：拼文本+引用+附件正文（UI 只存纯文本，发送前组装）；深度思考标志透传。  
     - 技术：Prompt 拼装、<think> 片段占位。
  4) 分流：`ChatController.sendChatRequest` 判断模型类型 → 本地 LlamaService 或远端 ArkClient。  
     - 技术：ModelConfig/ChatConfig、线程池。
  5) 流式回写：onChunk/onReasoning 增量写 Message.content/deepThink（Room），Adapter payload 刷新当前气泡，保持滚动跟随。  
     - 技术：Markwon 纯文本清洗后渲染、RecyclerView payload、主线程 Handler。
  6) 历史分页：上滑触底触发 `loadMoreMessages`，按时间/条数分页查询，前端追加到列表顶部。  
     - 技术：Room 分页查询、RecyclerView prepend、列表条数裁剪减内存。

### 1.2 对话功能（流式生成：本地 & 远端）
- **交互 + 技术流**：
  1) 上下文裁剪：线程池读取最近 N 条 user/ai（ChatConfig.MAX_CONTEXT_MESSAGES / MAX_LOCAL_CONTEXT_MESSAGES），剔除占位，控制总字数（MAX_LOCAL_PROMPT_CHARS）。  
     - 技术：Room 查询 + 业务裁剪。
  2) 本地模型（llama.cpp）：若未加载 → `LlamaService.loadModel`(JNI，读取 ModelConfig nContext/nThreads/nBatch/nGpuLayers)；已加载则直接 `infer(prompt, StreamCallback)`。  
     - 技术：JNI、线程池 + 主线程回调、停止词与长度控制、流式 UTF-8 组包。
  3) 远端模型（ArkClient）：构造 `List<ArkClient.Msg>`，OkHttp 发流式请求，回调 onReasoning/<think>、onContent/onComplete/onError。  
     - 技术：OkHttp/HTTP 流、可插拔 Provider（豆包/DeepSeek/OpenAI）。
  4) UI/DB 同步：所有增量写 Room，同步刷新 RecyclerView；失败标记 isFailed，停止时可保留已生成内容。  
     - 技术：RecyclerView payload、主线程 Handler、Markwon 渲染前清洗。

### 1.3 互动能力（点赞 / 收藏 / 复制 / 播放）
- **交互 + 技术流**：
  1) 触发：长按气泡或点击更多 → MessageActionPopup(PopupWindow)；回调传至 ChatActivity/Adapter。  
  2) 点赞/点踩：切换本地状态 → MessageDao.updateMessage → Adapter payload 刷新。  
     - 技术：Room、RecyclerView payload。
  3) 收藏：写/删 Favorite 记录（含引用/附件快照）→ UI 星标即时切换。  
     - 技术：Room(FavoriteDao)。
  4) 复制：清理 <think> 后写 ClipboardManager；Toast 提示。  
  5) 播放（TTS）：初始化 TextToSpeech，清洗 Markdown 文本 → speak；同条二次点击 stop。播放状态通过 setPlayingMessageId 驱动 AiMsgHolder 动画。  
     - 技术：TextToSpeech、Markwon 清洗、RecyclerView payload。
  6) 重新生成：调用 ChatController.stopGeneration（如在生成中）或重发同一 user 内容，沿用当前模型。

### 1.4 模型管理（端侧 gguf + 远端 API 配置）
- **交互 + 技术流**：
  1) 入口/列表：ModelManagerActivity 用 RecyclerView+ModelListAdapter 展示 LocalModel，右侧进度/状态/操作按钮（下载/暂停/删除/设为当前）。  
     - 技术：Room(ModelDao) + Live 刷新、RecyclerView 多状态条目。
  2) 导入弹窗：ImportModelDialogFragment 切换“本地/网络”。  
     - 本地：content:// 拷贝到 `files/models`，写 localPath、标记 READY/NOT_DOWNLOADED。  
     - 网络：保存 URL/Key/ModelId/Provider/深度思考/多模态标志。  
     - 技术：文件 IO、Room。
  3) 下载/续传：点击“下载/继续” → OkHttp 带 Range 写文件，403/416 回退全量；进度写 LocalModel.downloadProgress；失败置 PAUSED。  
     - 技术：OkHttp、文件流、进度计算、Room。
  4) 删除/暂停：UI 状态即时切换；可选删除文件并重置记录。  
  5) 激活：将目标置 ACTIVE，其他 ACTIVE → READY，同步深度思考/多模态能力到聊天页模型选择。  
     - 技术：Room 事务式更新。
  6) 本地推理准备：ChatController 在首次使用 ACTIVE 本地模型时调用 `LlamaService.loadModel`，读取 ModelConfig（nContext/nBatch/nThreads/nGpuLayers）；推理流与 1.2 本地分支一致。

### 1.5 左右手识别（触摸 + 传感器 + TFLite）
- **交互 + 技术流**：
  1) 轨迹采样：ChatActivity 将 MotionEvent 透传给 MotionEventTracker；ACTION_UP 且点数≥6 生成 9×6 特征矩阵。  
     - 技术：归一化轨迹、重采样。
  2) TFLite 分类：OperatingHandClassifier 加载 assets `mymodel.tflite`，支持 sigmoid/softmax；返回 LEFT/RIGHT。  
  3) 传感器判定：SensorManager 订阅重力/加速度，HandRecognitionManager 依据 X 轴阈值判侧躺/倾斜，显著变化强制更新。  
  4) 融合去抖：若轨迹判与当前手别不一致则累加计数，超过 HandConfig.TOUCH_OVERRIDE_COUNT 才切换；重力强制时清零计数。  
  5) 驱动 UI：onHandChanged 回调到 ChatActivity → ChatInputPanel.setHandMode → ConstraintSet 即时切换发送键左右，用户立刻看到布局变化。

---

## Part 2：UML 结构提示词（供绘图 AI 使用）
```
请绘制类图：
- ChatActivity 组合 ChatHeaderView, RecyclerView+ChatAdapter, ChatInputPanel, MessageActionPopup, HandRecognitionManager, ChatController, TextToSpeech；依赖 MessageDao, SessionDao, FavoriteDao, ArkClient, LlamaService。
- ChatController 依赖 MessageDao, LlamaService, ArkClient, ChatConfig, ModelConfig；回调 ChatGenerationListener。
- ChatAdapter 拥有 AiMsgHolder, UserMsgHolder, FileMsgHolder；回调 OnMessageActionListener。
- ModelManagerActivity 组合 RecyclerView+ModelListAdapter, ImportModelDialogFragment；依赖 ModelDao, OkHttpClient。
- ImportModelDialogFragment 依赖 LocalModel，回调 OnModelImportListener。
- HandRecognitionManager 组合 MotionEventTracker, OperatingHandClassifier, SensorManager；回调 OnHandChangeListener。
- MotionEventTracker 回调 OnTrackListener（onTrackDataReady,onTap）。
- OperatingHandClassifier 依赖 TensorFlowLite Interpreter（mymodel.tflite），输出 CLASS_LEFT/CLASS_RIGHT。
请标注关键关联：组合/依赖/回调（接口）。
```

---

## Part 3：主要流程（按用户操作拆解，前后端同步叙述）

### 3.1 本地/远端对话生成（发送一条消息的完整链路）
1) 用户操作：在 ChatInputPanel 输入文本，可选引用消息/添加附件(PDF/图片)/切换深度思考；状态机切换发送键（添加/发送/停止）。  
   - 前端：提取文本、引用、附件元数据；PDF 异步提取正文与 token 估算。  
2) 点击“发送”  
   - 前端：ChatActivity 立即插入一条 user Message（DB+UI），再插入一条 AI 占位（isGenerating=true，DB+UI），列表滚动到底部。  
3) ChatController 分流  
   - 本地模型：裁剪最近 N 条上下文（ChatConfig），拼接 ChatML Prompt；若未加载则 LlamaService.loadModel，再调用 LlamaService.infer(prompt, callback)。  
   - 远端模型：裁剪上下文，组 ArkClient 消息数组，调用 ArkClient.sendChat(stream)。  
4) 流式回调  
   - 本地：callback.onDeepThink / onChunk 逐段写入 Message.deepThink / content，DB 持久化；Adapter 局部刷新 payload，保持滚动跟随。  
   - 远端：onReasoning / onContent 同步写入并刷新；错误回调标记 isFailed。  
5) 结束/取消  
   - 正常完成：isGenerating=false，写入完成时间，更新 session.lastMessage。  
   - 用户点击“停止”或命中停止词：ChatController.cancel / ArkClient.cancel / LlamaService.cancelInference；占位消息内容保留已生成部分。  
6) 深度思考开关反馈  
   - 若当前模型不支持深度思考，ChatInputPanel 禁用该开关；支持时将布尔值透传到 prompt / 请求体。

### 3.2 互动（长按后的动作链）
1) 用户长按消息或点气泡操作区 → 弹出 MessageActionPopup。  
2) 点赞/点踩：前端切换状态并立即更新 UI 图标，DB 写入 isLiked/isDisliked。  
3) 收藏：若未收藏则写 Favorite 记录（含引用快照/附件快照）；已收藏则删除对应记录；UI 立即切换星标。  
4) 复制：清理 <think> 段后写入剪贴板；Toast 提示。  
5) 播放：初始化 TextToSpeech，清洗 Markdown → speak；再次点击同条则 stop；播放状态通过 ChatAdapter.setPlayingMessageId 触发 AiMsgHolder 的动画更新。  
6) 重新生成：调用 ChatController.stopGeneration (如在生成中) 或重新发送同一 user 消息内容，复用当前模型。

### 3.3 模型下载/导入/激活（端侧 llama.cpp）
1) 导入弹窗（ImportModelDialogFragment）  
   - 本地模式：选择文件/扫描私有目录，复制到 `files/models`，写 LocalModel 记录为 READY 或 NOT_DOWNLOADED。  
   - 网络模式：填 URL/Key/ModelId/Provider，写入 DB；状态 NOT_DOWNLOADED（待拉取）或 READY（纯 API）。  
2) 下载（ModelManagerActivity）  
   - 用户点“下载/继续”：OkHttp + Range 写文件；进度回写 LocalModel.downloadProgress；失败置 PAUSED。  
   - 用户点“暂停”：写状态 PAUSED，保留已下字节。  
   - 用户点“删除”：可选删除文件并重置状态。  
3) 激活  
   - 用户点“设为当前”或进入聊天选择：将目标置 ACTIVE，其他 ACTIVE → READY；更新是否支持深度思考/多模态标志；Chat 页刷新按钮可用性。  
4) 本地推理  
   - ChatController 调用 LlamaService.loadModel(activeModel) 确保唯一加载；infer 时按 ModelConfig.nBatch/nContext 设批大小与上下文；流式回调同 3.1。

### 3.4 左右手识别链路（驱动输入区左右切换）
1) 用户滑动/点击：MotionEventTracker 采样触摸轨迹；UP 且点数≥6 → 喂入 OperatingHandClassifier(TFLite) 输出 LEFT/RIGHT；短按走左右半屏启发。  
2) 传感器：SensorManager 提供重力/加速度，HandRecognitionManager 依据 X 轴阈值判定手别，显著变化立即更新。  
3) 融合与去抖：轨迹/重力结果累积计数，连续判反达阈值才切换；重力强制更新会清空计数。  
4) 回调 UI：onHandChanged → ChatInputPanel.setHandMode(isLeftHand)，ConstraintSet 切换发送键左右，用户立刻看到布局变更。

### 3.5 关键方法主要逻辑（对话/模型/手势）
- `ChatInputPanel.handleSendButtonClick()`  
  1) 若当前状态为 GENERATING：回调 onSendListener.onStop。  
  2) 若为 TYPING：取文本+引用+附件，回调 onSendListener.onSend(...)，清空输入并清理引用。  
  3) 若为 IDLE：展开底部面板，清焦点并收键盘。  
  4) 若为 PANEL_OPEN：收起面板。之后统一调用 updateState 切换按钮图标/背景。  
  
- `ChatController.sendChatRequest(prompt, isDeepThink, aiMsg, userMsg, activeModel, listener)`  
  1) 判空模型，空则回调错误。  
  2) 置 isGenerating=true。  
  3) 分流：activeModel.isLocal ? handleLocalRequest : handleNetworkRequest。  
  
- `ChatController.handleLocalRequest(prompt, aiMsg, userMsg, model, listener)`  
  1) 从 DB 拉取最近消息，过滤当前占位/当前 user，截断到 MAX_LOCAL_CONTEXT_MESSAGES。  
  2) buildLocalChatPrompt 拼接历史与当前 prompt（截断到 MAX_LOCAL_PROMPT_CHARS）。  
  3) 若未加载或已加载但不同模型：调用 LlamaService.loadModel；成功后 doLocalInference；失败回调错误并复位 isGenerating。  
  4) 若已加载当前模型：直接 doLocalInference。  
  
- `ChatController.doLocalInference(service, chatPrompt, aiMsg, listener)`（本地流式解析）  
  1) 调用 service.infer(prompt, StreamCallback)。  
  2) onChunk：先检查停止标记，解析 <think> 标签，分流到 onThinking/onContent；超长或标记命中则 cancelInference。  
  3) onDeepThink：累加深度思考文本并回调 UI。  
  4) onComplete：刷出残留 buffer，isGenerating=false，回调完成。  
  5) onError：isGenerating=false，回调错误。  
  
- `ChatController.handleNetworkRequest(apiPrompt, isDeepThink, aiMsg, userMsg, listener)`  
  1) 在线程池取 session 历史，过滤占位/当前 user，仅保留 user/ai，截断 MAX_CONTEXT_MESSAGES，拼装 ArkClient.Msg 列表并追加当前 prompt。  
  2) 主线程回调前先将 aiMsg 设为“生成中”并写 DB。  
  3) 调用 ArkClient.sendChat(messages,isDeepThink,listener)：保存返回的 Cancellable，便于 stopGeneration 终止。  
  4) Ark 回调 onReasoning/onContent 增量写 DB 与 UI，onError 标记失败，onComplete 复位 isGenerating。  
  
- `ArkClient.sendChat(messages, isDeepThink, StreamListener)`  
  1) 从 DB 取 ACTIVE 模型，若无则回调错误。  
  2) 通过 LLMFactory 选择 Provider（如豆包/OpenAI/DeepSeek）。  
  3) 将消息和深度思考标志委托给 provider.sendChat，返回可取消的 Cancellable。  
  
- `LlamaService.loadModel(LocalModel, SimpleCallback)`  
  1) 若已加载同一模型直接回调成功。  
  2) 若有旧模型，先 cancelInference，再异步 freeModelNative。  
  3) 在线程池校验文件存在，读取 ModelConfig.nContext/nThreads/nBatch/nGpuLayers，调用 loadModelNative。  
  4) 成功则缓存指针与 currentModel，主线程回调成功；失败回调错误。  
  
- `LlamaService.infer(prompt, StreamCallback)`  
  1) 未加载模型直接回调错误。  
  2) 在线程池调用本地 completionNative，传入 NativeCallbackAdapter。  
  3) NativeCallbackAdapter 按字节缓冲 UTF-8 分片，凑整后在主线程发 onChunk；支持 shouldStop() 以便 cancelInference。  
  4) 完成后主线程回调 onComplete；异常回调 onError。  
  
- `MotionEventTracker.trackEvent(event)` / `analyze()`  
  1) 记录每个触摸点的归一化坐标、时间戳、屏幕尺寸；UP 时若点数≥6 准备特征矩阵。  
  2) analyze() 将轨迹重采样为 9×6 float，返回给 HandRecognitionManager；不足点数返回 null（视为点击）。  
  
- `HandRecognitionManager.onTrackDataReady(data)`  
  1) 调用 OperatingHandClassifier.classify(data) 得到 LEFT/RIGHT 置信度。  
  2) 与当前手别对比，累加反判计数；超过阈值才切换。  
  3) 切换后触发 onHandChanged 回调，驱动 ChatInputPanel.setHandMode。

---

## Part 4：工作拆分与排期（示例，针对全项目）
- Week 1：需求澄清/基线（对话、模型、手势精度与性能目标，梳理 DB/接口/模型文件）
- Week 2：架构与质量（功能分包、文档/规范、CI lint+assemble、资源清理）
- Week 3：对话链路稳固（抽离业务/可选 ViewModel、重试/错误/停止一致性、分页与收藏/再生优化、输出上限与停止词）
- Week 4：模型管理强化（下载续传与错误提示、导入合法性校验、激活与状态一致性、存储占用与清理入口）
- Week 5：左右手识别优化（阈值与模型精度调优、必要的再训练/采样、UI/无障碍提示）
- Week 6：测试与发布（DAO/下载/手势/生成链路测试，长对话与本地推理压测，release 混淆/资源压缩/签名验证）

