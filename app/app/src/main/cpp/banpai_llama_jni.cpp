// Banpai JNI bridge to llama.cpp, pinned commit 4c4a0f0 (2026-11-20).
// Pattern follows examples/simple/simple.cpp from the same checkout:
// llama_model_load_from_file + llama_init_from_model + llama_batch_ext +
// llama_process(LLAMA_PROCESS_TYPE_DECODE) + llama_sampler_chain_*.
// All API names were read from that checkout's include/llama.h, not from memory:
// - llama_model_load_from_file(path, llama_model_default_params())
// - llama_init_from_model(model, llama_context_params)  (this checkout has no
//   llama_new_context_with_model; llama_init_from_model is the current name)
// - llama_batch_ext_init / add_token / set_pos / set_output_logits / clear / free
// - llama_process(ctx, LLAMA_PROCESS_TYPE_DECODE, batch)
// - llama_sampler_sample(chain, ctx, -1), llama_vocab_is_eog, llama_token_to_piece
// - llama_memory_clear(llama_get_memory(ctx), true) to reset KV between turns
//
// CPU only (n_gpu_layers = 0). No streaming: generate() returns the whole text.
// One global mutex serializes load/generate/unload; the model stays loaded
// across turns and the KV cache is cleared at the start of each turn
// (single-turn chat, no history kept in C++).
#include <jni.h>

#include <android/log.h>
#include <mutex>
#include <random>
#include <string>
#include <vector>

#include "llama.h"

#define TAG "BanpaiLlama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

std::mutex g_mutex;
llama_model * g_model = nullptr;
llama_context * g_ctx = nullptr;
const llama_vocab * g_vocab = nullptr;
bool g_backend_init = false;

std::string jstring_to_std(JNIEnv * env, jstring s) {
    if (!s) return {};
    const char * c = env->GetStringUTFChars(s, nullptr);
    std::string out(c ? c : "");
    if (c) env->ReleaseStringUTFChars(s, c);
    return out;
}

// Fill a batch with tokens at consecutive positions, logits only on the last
// token. Mirrors batch_set_tokens() in examples/simple/simple.cpp.
void batch_set_tokens(llama_batch_ext * batch, const llama_token * tokens, int32_t n_tokens, llama_pos pos_0) {
    llama_batch_ext_clear(batch);
    for (int32_t i = 0; i < n_tokens; ++i) {
        const int32_t idx = llama_batch_ext_add_token(batch, 0, tokens[i]);
        const llama_pos pos = pos_0 + i;
        llama_batch_ext_set_pos(batch, idx, &pos);
    }
    llama_batch_ext_set_output_logits(batch, n_tokens - 1, true);
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_banpai_app_data_BanpaiLlama_nativeLoadModel(JNIEnv * env, jobject /* thiz */, jstring jpath) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_model && g_ctx) return JNI_TRUE;

    const std::string path = jstring_to_std(env, jpath);
    if (!g_backend_init) {
        llama_backend_init();
        g_backend_init = true;
    }

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0; // CPU only
    llama_model * model = llama_model_load_from_file(path.c_str(), mparams);
    if (!model) {
        LOGE("load model failed: %s", path.c_str());
        return JNI_FALSE;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = 1024;
    cparams.n_batch = 512;
    cparams.n_threads = 4;
    cparams.n_threads_batch = 4;
    llama_context * ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        LOGE("create context failed");
        llama_model_free(model);
        return JNI_FALSE;
    }

    g_model = model;
    g_ctx = ctx;
    g_vocab = llama_model_get_vocab(model);
    LOGI("model loaded: %s", path.c_str());
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_banpai_app_data_BanpaiLlama_nativeGenerate(JNIEnv * env, jobject /* thiz */,
                                                    jstring jprompt, jint jmax_tokens, jfloat jtemp) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_model || !g_ctx || !g_vocab) return nullptr;

    const std::string prompt = jstring_to_std(env, jprompt);
    const int max_tokens = (int) jmax_tokens;

    // Fresh sampler chain per call so temp can vary per call
    // (eval used temp 0 for facts, 0.7 for style; app default 0.7).
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler * smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(jtemp));
    std::random_device rd;
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(rd()));

    llama_batch_ext * batch = llama_batch_ext_init(g_ctx);
    std::string out;
    bool ok = true;

    // Tokenize with add_special + parse_special: ChatML <|im_start|> must be
    // parsed as control tokens, not plain text.
    const int n_prompt = -llama_tokenize(g_vocab, prompt.c_str(), (int32_t) prompt.size(), nullptr, 0, true, true);
    if (n_prompt <= 0 || n_prompt + max_tokens > 1024) {
        LOGE("tokenize failed or prompt+output exceeds n_ctx 1024: %d", n_prompt);
        ok = false;
    }

    std::vector<llama_token> prompt_tokens;
    if (ok) {
        prompt_tokens.resize(n_prompt);
        if (llama_tokenize(g_vocab, prompt.c_str(), (int32_t) prompt.size(),
                           prompt_tokens.data(), (int32_t) prompt_tokens.size(), true, true) < 0) {
            ok = false;
        }
    }

    if (ok) {
        // Single-turn: start from an empty KV cache.
        llama_memory_clear(llama_get_memory(g_ctx), true);

        batch_set_tokens(batch, prompt_tokens.data(), n_prompt, 0);
        int n_tokens = n_prompt;
        int n_pos = 0;
        int n_decode = 0;
        for (; n_pos + n_tokens < n_prompt + max_tokens;) {
            if (llama_process(g_ctx, LLAMA_PROCESS_TYPE_DECODE, batch) != 0) {
                LOGE("decode failed");
                ok = false;
                break;
            }
            n_pos += n_tokens;

            llama_token id = llama_sampler_sample(smpl, g_ctx, -1);
            if (llama_vocab_is_eog(g_vocab, id)) break;

            char buf[128];
            int n = llama_token_to_piece(g_vocab, id, buf, sizeof(buf), 0, false);
            if (n < 0) {
                ok = false;
                break;
            }
            out.append(buf, n);

            batch_set_tokens(batch, &id, 1, n_pos);
            n_tokens = 1;
            if (++n_decode >= max_tokens) break;
        }
    }

    llama_batch_ext_free(batch);
    llama_sampler_free(smpl);
    if (!ok) return nullptr;
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_banpai_app_data_BanpaiLlama_nativeUnloadModel(JNIEnv * /* env */, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    g_vocab = nullptr;
}
