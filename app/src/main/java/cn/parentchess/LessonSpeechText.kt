package cn.parentchess

/** Keep square names intelligible to Chinese voices: E4 -> E 四, never “forty-four”. */
object LessonSpeechText {
    private val square = Regex("(?i)(?<![a-z0-9])([a-h])([1-8])(?![a-z0-9])")
    private val ranks = listOf("", "一", "二", "三", "四", "五", "六", "七", "八")
    fun normalize(text: String): String = square.replace(text) {
        "${it.groupValues[1].uppercase()} ${ranks[it.groupValues[2].toInt()]}"
    }.replace("→", "，走到，")
        // Android system TTS has no portable phoneme override. Use an unambiguous
        // jiang1 homophone in speech input only; all visible text stays unchanged.
        .replace("将军", "江军")
        .replace("将杀", "江杀")
        .replace("车", "居") // User's chess pronunciation: ju1, including 白车/黑车/双车.
    fun instruction(lesson: Lesson) = "第 ${lesson.number} 关。${lesson.goal}。${lesson.intro}"
}

/** Only the latest requested sentence can survive initialization; stop invalidates callbacks. */
internal class NarrationQueue {
    var pending: String? = null; private set
    var generation = 0L; private set
    fun replace(text: String) { generation++; pending = text }
    fun take(): Pair<Long, String>? = pending?.let { generation to it }.also { pending = null }
    fun clear() { generation++; pending = null }
    fun isCurrent(id: String?) = id == generation.toString()
}
