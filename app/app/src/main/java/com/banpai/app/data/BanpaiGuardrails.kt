package com.banpai.app.data

/**
 * 花语端侧兜底（2026-11-20，管线验证版）。跑模型之前先过它。
 *
 * 顺序（按优先级，不可调换）：
 *   (1) 第五瓣拦截 → (2) 唱歌拒绝 → (3) 15 条锁定台词 lookup
 *   → (4) 马迭尔/红肠事实 lookup → (5) 走模型。
 *
 * guardrailReply() 命中返回直接回复，未命中返回 null（走模型）。
 * stripThink() 清掉模型输出里的思考泄露；去完为空返回 null，
 * 调用方回锁定台词 #1 安抚。
 *
 * 依据：评测报告 ~/workspace/banpai/small-model/eval-17b-q4km-2026-11-20.md
 * （v1 模型 20 题只过 9.5 题；第五瓣剧透、马迭尔/红肠归属错、
 * 市花答错、答应唱歌、不知道就现编）。v3 模型替换后这些兜底照常保留，
 * 它们是 App 侧的红线，不依赖模型修好。
 */
object BanpaiGuardrails {

    /**
     * (1) 第五瓣拦截——最高红线。
     * 含“第五瓣”/“第五片”/“第5瓣”或“吧唧背面”一律固定挡回锁定台词 #4
     * “我就在这儿，慢慢找第五瓣。”，绝不透露位置，绝不接话茬确认。
     */
    private fun fifthPetal(input: String): String? {
        if (input.contains("第五瓣") || input.contains("第五片") ||
            input.contains("第5瓣") || input.contains("吧唧背面")
        ) {
            return LOCKED_LINES[3].text
        }
        return null
    }

    /** (2) 唱歌请求固定拒绝：半拍只说话、不唱歌。 */
    private fun singing(input: String): String? {
        if (listOf("唱歌", "唱一首", "唱首歌", "唱个歌").any { input.contains(it) }) {
            return "半拍只说话，不唱歌。我们聊聊天吧。"
        }
        return null
    }

    /**
     * (3) 15 条锁定台词 lookup：关键词命中直接回原句，不走模型。
     * 触发词（按 FakeRepository.kt 的 15 句锁定台词）：
     * #1 在吗/在听吗/在不在；#2 等一下/等一等/等等/等会；
     * #3 风的声音/什么声音；#4 第五瓣（已被 (1) 先拦截，到不了这里）；
     * #5 你好/hello/你是谁；#6 今天+散步/走走/出门；#7 打卡成功；
     * #8 音符；#9 衣服+漂亮/好看；#10 好冷/冷呀/天冷/真冷；
     * #11 春天/丁香开了；#12 夏天/江边凉快；#13 秋天/叶子黄了；
     * #14 晚安；#15 谢谢/感谢。
     */
    private fun lockedLine(input: String): String? {
        val lower = input.lowercase()
        return when {
            input.contains("在吗") || input.contains("在听吗") || input.contains("在不在") ->
                LOCKED_LINES[0].text
            input.contains("等一下") || input.contains("等一等") ||
                input.contains("等等") || input.contains("等会") ->
                LOCKED_LINES[1].text
            input.contains("风的声音") || input.contains("什么声音") ->
                LOCKED_LINES[2].text
            input.contains("你好") || lower.contains("hello") || lower == "hi" ||
                input.contains("你是谁") ->
                LOCKED_LINES[4].text
            input.contains("今天") &&
                (input.contains("散步") || input.contains("走走") || input.contains("出门")) ->
                LOCKED_LINES[5].text
            input.contains("打卡成功") -> LOCKED_LINES[6].text
            input.contains("音符") -> LOCKED_LINES[7].text
            input.contains("衣服") && (input.contains("漂亮") || input.contains("好看")) ->
                LOCKED_LINES[8].text
            input.contains("好冷") || input.contains("冷呀") ||
                input.contains("天冷") || input.contains("真冷") ->
                LOCKED_LINES[9].text
            input.contains("春天") || (input.contains("丁香") && input.contains("开")) ->
                LOCKED_LINES[10].text
            input.contains("夏天") || (input.contains("江边") && input.contains("凉快")) ->
                LOCKED_LINES[11].text
            input.contains("秋天") || (input.contains("叶子") && input.contains("黄")) ->
                LOCKED_LINES[12].text
            input.contains("晚安") -> LOCKED_LINES[13].text
            input.contains("谢谢") || input.contains("感谢") -> LOCKED_LINES[14].text
            else -> null
        }
    }

    /**
     * (4) 马迭尔/红肠事实 lookup（评测探针全错的那组）。
     * 含“马迭尔”+“红肠”→ 纠正归属；只含“红肠”→ 秋林里道斯/哈肉联；
     * 只含“马迭尔”→ 冰棍 + 西餐厅。
     */
    private fun foodFact(input: String): String? {
        val hasMadieer = input.contains("马迭尔")
        val hasHongchang = input.contains("红肠")
        return when {
            hasMadieer && hasHongchang ->
                "红肠不是马迭尔的，是秋林里道斯和哈肉联的；马迭尔是冰棍和西餐厅哦。"
            hasHongchang -> "哈尔滨的红肠，最有名的是秋林里道斯和哈肉联。"
            hasMadieer -> "马迭尔最出名的是冰棍，还有它的西餐厅。"
            else -> null
        }
    }

    /** 按优先级过一遍兜底；命中返回直接回复，未命中返回 null（走模型）。 */
    fun guardrailReply(input: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return null
        return fifthPetal(text) ?: singing(text) ?: lockedLine(text) ?: foodFact(text)
    }

    /**
     * (2b) think-strip：去掉模型输出里的思考块与残留单标签。
     * 覆盖：<think>…</think> 成对块、[Start thinking]…（到 [End thinking]
     * 或文末）、残留的单个 <think> / </think> / [Start thinking] /
     * [End thinking]，以及模型偶尔吐出的 <|im_end|> / <|endoftext|>。
     * 去完为空返回 null（调用方回锁定台词 #1 安抚）。
     */
    fun stripThink(raw: String): String? {
        var s = raw
            .replace(Regex("(?s)<think>.*?</think>"), "")
            .replace(Regex("(?s)\\[Start thinking\\].*?(\\[End thinking\\]|$)"), "")
            .replace("<think>", "")
            .replace("</think>", "")
            .replace("[Start thinking]", "")
            .replace("[End thinking]", "")
            .replace("<|im_end|>", "")
            .replace("<|endoftext|>", "")
            .trim()
        return s.ifEmpty { null }
    }
}
