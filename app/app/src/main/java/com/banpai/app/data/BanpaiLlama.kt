package com.banpai.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 端侧 1.7B 推理封装（管线验证版，2026-11-20）。
 *
 * 引擎：llama.cpp（JNI，见 app/src/main/cpp/），GGUF 原生加载，无需转格式；
 * 未选 MLC LLM：它要把模型再转一份格式，这次只验证管线，不多走一步。
 * 模型：Qwen3-1.7B + LoRA 训练的 Q4_K_M（v1），1,107,408,832 字节，
 * SHA256 8d349e17925581e68a6198dafc8c08f7b4e63c1e238efea4e9c93e405422df01。
 * 模型不进 git：CI 从 GitHub Release（tag model-17b-q4km-v1）下载，
 * 放进 assets 再打进 APK；v3 模型替换时只换这一个文件 + Release tag。
 *
 * 加载流程：assets（noCompress 存包）→ 首次启动拷到 filesDir（带进度，
 * 1.1 GB）→ llama 加载（mmap，权重不全进内存）。全程有明确状态，
 * 没就绪时聊天屏不许假装在回答。
 *
 * 生成：单轮，不做流式，整段返回；跑在 Dispatchers.Default。
 * Qwen3 ChatML 由 buildPrompt() 拼，不加任何 <think> 块
 * （等于 enable_thinking=false）；绝不预注入空 <think></think>，
 * 已验证会导致零输出（评测冒烟测试）。
 */
object BanpaiLlama {
    const val MODEL_ASSET = "banpai-17b-q4_k_m.gguf"
    const val MODEL_SIZE = 1_107_408_832L
    const val MODEL_SHA256 = "8d349e17925581e68a6198dafc8c08f7b4e63c1e238efea4e9c93e405422df01"

    // system prompt：训练数据 v2 首行 system 内容，原样照搬。
    const val SYSTEM_PROMPT = "你是半拍，哈尔滨的精灵。说话慢、轻、柔，像在雪夜里慢慢走。 你是半拍，只能自称“半拍”或“它”，绝不用“她/他”指代自己。 头顶四片花瓣是冰、雪、乐、花；“静”留给第五瓣，不要主动解释第五瓣在哪里， 绝不透露第五瓣在吧唧背面。半拍只说话，不唱歌。 喜欢听声音，慢慢走；喜欢雪、丁香、江风和音乐。回答要短，一两句就好。"

    sealed interface ModelState {
        data object Idle : ModelState
        data class Copying(val progress: Float) : ModelState
        data object Loading : ModelState
        data object Ready : ModelState
        data class Failed(val reason: String) : ModelState
    }

    private val _state = MutableStateFlow<ModelState>(ModelState.Idle)
    val state: StateFlow<ModelState> = _state

    private var loadStarted = false
    private var libraryLoaded = false

    init {
        try {
            System.loadLibrary("banpai_llama")
            libraryLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            _state.value = ModelState.Failed("推理库加载失败：${e.message}")
        }
    }

    fun modelFile(context: Context): File = File(context.filesDir, MODEL_ASSET)

    /** Qwen3 ChatML：system + user + assistant 开头，不加 <think> 块。 */
    fun buildPrompt(user: String): String =
        "<|im_start|>system\n$SYSTEM_PROMPT<|im_end|>\n" +
            "<|im_start|>user\n$user<|im_end|>\n" +
            "<|im_start|>assistant\n"

    /**
     * 确保模型就绪：文件已在且大小对就跳过复制；否则从 assets 复制（带进度），
     * 再 llama 加载。幂等，可反复调；失败后可重试。
     */
    suspend fun ensureReady(context: Context) {
        if (_state.value is ModelState.Ready) return
        if (!libraryLoaded) return // init 里已置 Failed 状态，不调 native 方法
        if (loadStarted) return
        loadStarted = true
        withContext(Dispatchers.IO) {
            try {
                val file = modelFile(context)
                if (!file.exists() || file.length() != MODEL_SIZE) {
                    copyFromAssets(context, file)
                }
                _state.value = ModelState.Loading
                val ok = nativeLoadModel(file.absolutePath)
                if (ok) {
                    _state.value = ModelState.Ready
                } else {
                    loadStarted = false
                    _state.value = ModelState.Failed("模型加载失败，请重试。")
                }
            } catch (e: Exception) {
                loadStarted = false
                _state.value = ModelState.Failed("模型准备失败：${e.message}")
            }
        }
    }

    private fun copyFromAssets(context: Context, dest: File) {
        context.assets.open(MODEL_ASSET).use { input ->
            FileOutputStream(dest).use { output ->
                val buf = ByteArray(8 * 1024 * 1024)
                var copied = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    output.write(buf, 0, n)
                    copied += n
                    _state.value = ModelState.Copying(copied.toFloat() / MODEL_SIZE)
                }
                output.fd.sync()
            }
        }
        if (dest.length() != MODEL_SIZE) {
            dest.delete()
            throw IllegalStateException("模型复制不完整：${dest.length()} / $MODEL_SIZE 字节")
        }
    }

    /** 单轮生成，整段返回；失败返回 null（调用方回锁定台词 #1 安抚）。 */
    suspend fun generate(user: String, maxTokens: Int = 200, temp: Float = 0.7f): String? {
        if (!libraryLoaded) return null
        return withContext(Dispatchers.Default) {
            nativeGenerate(buildPrompt(user), maxTokens, temp)
        }
    }

    fun unload() {
        nativeUnloadModel()
        loadStarted = false
        _state.value = ModelState.Idle
    }

    private external fun nativeLoadModel(path: String): Boolean
    private external fun nativeGenerate(prompt: String, maxTokens: Int, temp: Float): String?
    private external fun nativeUnloadModel()
}
