package cn.parentchess

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class LessonSource(
    val name: String, val puzzleId: String, val url: String, val gameUrl: String,
    val datasetUrl: String, val license: String, val retrievedAt: String,
    val originalFen: String, val originalMoves: List<String>, val rating: Int,
    val popularity: Int, val plays: Int, val themes: List<String>
)
data class Lesson(
    val id: String, val chapter: String, val title: String, val intro: String,
    val fen: String, val goal: String, val lines: List<List<String>>, val hints: List<String>,
    val explanation: String, val question: String? = null, val answer: String? = null,
    val number: Int = 0, val mateIn: Int = 0, val source: LessonSource? = null,
    val completion: String? = "checkmate"
)
object LessonCatalog {
    fun parse(json: String): List<Lesson> {
        val lessons: List<Lesson> = Gson().fromJson(json, object : TypeToken<List<Lesson>>() {}.type)
        return lessons.map { it.copy(completion = it.completion ?: "checkmate") }
    }
    fun completedCount(lessons: List<Lesson>, completed: Set<String>) = lessons.count { it.id in completed }
}
class LessonAttempt(val lesson: Lesson, private val chooseResponse: (List<String>) -> String = { it.first() }) {
    var game = ChessGame(lesson.fen)
        private set
    val played = mutableListOf<String>()
    var complete = false
        private set
    fun answer(value: String): Boolean {
        if (lesson.answer == value) { complete = true; return true }
        return false
    }
    fun move(uci: String): Boolean {
        if (complete || lesson.question != null) return false
        val candidate = played + uci
        val lines = lesson.lines.filter { it.take(candidate.size) == candidate }
        if (lines.isEmpty()) return false
        check(game.play(uci)); played.add(uci)
        if (lines.any { it.size == played.size }) {
            check(lesson.completion == "sourceLine" || game.board.isMated) { "A mating lesson must finish at checkmate" }
            complete = true; return true
        }
        val responses = lines.map { it[played.size] }.distinct()
        val response = chooseResponse(responses)
        check(response in responses)
        check(game.play(response)); played.add(response)
        return true
    }
}
