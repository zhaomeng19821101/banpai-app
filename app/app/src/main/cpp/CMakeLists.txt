# 半拍 Banpai — llama.cpp 桥接（端侧 1.7B 推理，管线验证版，2026-11-20）
#
# 引擎选择：llama.cpp（不选 MLC LLM）。
# 理由：GGUF 原生加载，无需把模型再转一份格式；成熟稳定，评测用的
# llama-cli 就是同一份源码编出来的。MLC LLM 要额外转模型格式，
# 这次只验证管线，不多走一步。
#
# llama.cpp 源码不进 git：CI 在构建前 clone 到本目录的 llama.cpp/ 子目录，
# pin 在提交 4c4a0f0a4246bc36a3724d12238d5624714dcb2f（2026-11-20，
# 与评测用的同一份源码）。本地编：先手动 clone 并 checkout 同一提交。
# 见 .github/workflows/android-debug.yml 的 "Clone llama.cpp" 一步。

cmake_minimum_required(VERSION 3.22.1)
project(banpai_llama)

if(NOT EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/CMakeLists.txt)
    message(FATAL_ERROR "llama.cpp not found at ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp - CI clones it (pinned commit 4c4a0f0); see .github/workflows/android-debug.yml")
endif()

# 只要核心推理库：不要 examples / tests / tools / server / app。
set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP      OFF CACHE BOOL "" FORCE)

add_subdirectory(llama.cpp)

add_library(banpai_llama SHARED banpai_llama_jni.cpp)

# llama 目标自带 PUBLIC 头文件路径与 ggml 依赖，link 它就够了。
# log：banpai_llama_jni.cpp 用 __android_log_print（<android/log.h>），
# 2026-11-20 CI 首次构建缺它报 undefined symbol，link 失败。
target_link_libraries(banpai_llama PRIVATE llama log)
