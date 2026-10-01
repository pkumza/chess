package cn.parentchess

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.google.gson.Gson
import kotlinx.coroutines.*

data class SavedState(val pvp: GameRecord, val bot: GameRecord, val assistance: Boolean, val flipped: Boolean, val level: Int, val humanWhite: Boolean, val completed: Set<String>, val screen: String)

class ChessViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("parent-chess-v1", 0)
    private val progressionLessons = LessonCatalog.parse(app.assets.open("progression-lessons.json").bufferedReader().use { it.readText() })
    private val originalLessons = LessonCatalog.parse(app.assets.open("lessons.json").bufferedReader().use { it.readText() })
    // Original puzzle IDs preserve saved progress; round-robin avoids long blocks of rook puzzles.
    private val reviewGroups = originalLessons.groupBy { it.chapter }.values.map { it.toMutableList() }
    val lessons = progressionLessons + buildList {
        while (reviewGroups.any { it.isNotEmpty() }) {
            reviewGroups.forEach { group -> if (group.isNotEmpty()) add(group.removeAt(0).copy(chapter = "06 · 原题复习")) }
        }
    }
    var lessonChapter by mutableStateOf(
        prefs.getString("lesson-chapter", null)?.takeIf { chapter -> lessons.any { it.chapter == chapter } }
            ?: lessons.first().chapter
    ); private set
    fun selectLessonChapter(chapter: String) {
        if (lessons.none { it.chapter == chapter }) return
        lessonChapter = chapter
        prefs.edit().putString("lesson-chapter", chapter).apply()
    }
    var screen by mutableStateOf("home"); private set
    var pvp = ChessGame(); private set
    var bot = ChessGame(); private set
    var assistance by mutableStateOf(true); private set
    var flipped by mutableStateOf(false); private set
    var level by mutableIntStateOf(0); private set
    var humanWhite by mutableStateOf(true); private set
    var completed by mutableStateOf(setOf<String>()); private set
    val completedLessonCount get() = LessonCatalog.completedCount(lessons, completed)
    var selected by mutableStateOf<Square?>(null); private set
    var promotion by mutableStateOf<List<Move>>(emptyList()); private set
    var thinking by mutableStateOf(false); private set
    var feedback by mutableStateOf(""); private set
    var revision by mutableIntStateOf(0); private set
    var attempt by mutableStateOf<LessonAttempt?>(null); private set
    var hintIndex by mutableIntStateOf(0); private set
    var highlighted by mutableStateOf<Move?>(null); private set
    var animations by mutableStateOf(emptyList<MoveVisual>()); private set
    var soundEnabled by mutableStateOf(prefs.getBoolean("sound-enabled", true)); private set
    private val sounds = MoveSounds(app)
    var speechEnabled by mutableStateOf(prefs.getBoolean("speech-enabled", true)); private set
    var speaking by mutableStateOf(false); private set
    var speechMessage by mutableStateOf<String?>(null); private set
    private var lastNarration = ""
    private var celebrationJob: Job? = null
    private val narrator = LessonNarrator(app) { active, message -> speaking = active; speechMessage = message }
    private fun narrate(text: String) {
        lastNarration = text
        if (speechEnabled && screen == "lesson") narrator.speak(text)
    }
    private fun stopNarration() { celebrationJob?.cancel(); celebrationJob = null; narrator.stop() }
    fun repeatNarration() { if (speechEnabled && screen == "lesson") { stopNarration(); narrator.speak(lastNarration) } }
    fun toggleSpeech() {
        speechEnabled = !speechEnabled
        prefs.edit().putBoolean("speech-enabled", speechEnabled).apply()
        if (speechEnabled) repeatNarration() else stopNarration()
    }
    fun clickSound() { if (soundEnabled) sounds.click() }
    private var animationId = 0L
    fun toggleSound() { soundEnabled = !soundEnabled; if (!soundEnabled) sounds.stop(); prefs.edit().putBoolean("sound-enabled", soundEnabled).apply() }
    private var soundedAnimation = -1L
    fun landAnimation(id: Long) {
        val current = animations.firstOrNull() ?: return
        if (current.id != id || soundedAnimation == id) return
        soundedAnimation = id
        if (current.retry) return
        if (soundEnabled) sounds.play(current.capture)
    }
    fun finishAnimation(id: Long) {
        val current = animations.firstOrNull() ?: return
        if (current.id != id) return
        animations = animations.drop(1)
        if (current.retry && screen == "lesson") {
            selected = current.travels.first().from
            val a = attempt!!
            highlighted = a.lesson.lines.firstOrNull { it.take(a.played.size) == a.played }
                ?.getOrNull(a.played.size)?.let { uci -> game.legal().firstOrNull { it.uci() == uci } }
            selected = highlighted?.from ?: current.travels.first().from
            feedback = if (current.blocked) "走不到这里，试试亮起的格子。" else "再试试亮起的格子。"
        }
        if (screen == "lesson" && animations.isEmpty() && feedback.isNotBlank()) {
            if (attempt!!.complete) {
                lastNarration = "挑战成功，真棒！${attempt!!.lesson.explanation}"
                celebrationJob = viewModelScope.launch {
                    if (soundEnabled) { sounds.success(); delay(1100) }
                    narrate(lastNarration)
                }
            } else narrate(feedback)
        }
    }
    private fun animateMoves(before: Board, moves: List<String>) {
        val frames = moves.map { uci ->
            val move = before.legalMoves().first { it.uci() == uci }
            val frame = MoveVisual.create(++animationId, before, move)
            before.doMove(move)
            frame
        }
        animations = animations + frames
    }
    override fun onCleared() { narrator.release(); sounds.release(); super.onCleared() }
    private var job: Job? = null
    private var generation = 0L
    val game: ChessGame get() = when(screen) { "lesson" -> attempt!!.game; "bot" -> bot; else -> pvp }
    val humanSide get() = if (humanWhite) Side.WHITE else Side.BLACK
    init {
        val raw = prefs.getString("state", null)
        if (raw != null) runCatching {
            val s = Gson().fromJson(raw, SavedState::class.java)
            pvp = ChessGame.restore(s.pvp); bot = ChessGame.restore(s.bot)
            assistance = s.assistance; flipped = s.flipped; level = s.level.coerceIn(0,1)
            humanWhite = s.humanWhite; completed = s.completed
            screen = s.screen.takeIf { it in listOf("pvp", "bot", "learn", "home") } ?: "home"
        }.onFailure { prefs.edit().putString("recovery-state", raw).apply(); feedback = "上次存档无法读取，已保留原始记录。" }
        scheduleBot()
    }
    private fun save() {
        prefs.edit().putString("state", Gson().toJson(SavedState(pvp.record(), bot.record(), assistance, flipped, level, humanWhite, completed, if (screen == "lesson") "learn" else screen))).apply()
    }
    private fun changed() { revision++; save() }
    private fun cancelWork() { stopNarration(); sounds.stop(); animations = emptyList(); generation++; job?.cancel(); job = null; thinking = false; highlighted = null }
    fun navigate(to: String) {
        narrator.stop(); cancelWork(); screen = to; selected = null; promotion = emptyList(); feedback = ""; changed(); scheduleBot()
    }
    fun openLesson(lesson: Lesson) { selectLessonChapter(lesson.chapter); narrator.stop(); cancelWork(); attempt = LessonAttempt(lesson); screen = "lesson"; flipped = ChessGame(lesson.fen).board.sideToMove == Side.BLACK; selected = null; promotion = emptyList(); hintIndex = 0; feedback = ""; changed(); narrate(LessonSpeechText.instruction(lesson)) }
    fun toggleAssistance() { assistance = !assistance; highlighted = null; changed() }
    fun flip() { animations = emptyList(); flipped = !flipped; changed() }
    fun configureBot(newLevel: Int, white: Boolean) { level = newLevel; humanWhite = white; bot = ChessGame(); flipped = !white; navigate("bot") }
    fun newGame() { cancelWork(); if (screen == "bot") bot = ChessGame() else pvp = ChessGame(); selected = null; promotion = emptyList(); feedback = ""; changed(); scheduleBot() }
    fun tap(square: Square) {
        if (animations.isNotEmpty()) return
        if (screen !in listOf("pvp", "bot", "lesson")) return
        if (game.result != null || (screen == "lesson" && (attempt!!.complete || attempt!!.lesson.question != null))) return
        if (screen == "bot" && game.board.sideToMove != humanSide) return
        val options = game.legal().filter { it.from == selected && it.to == square }
        if (options.isNotEmpty()) {
            if (options.size > 1) promotion = options else move(options.first())
            return
        }
        val p = game.board.getPiece(square)
        if (screen == "lesson" && selected != null && square != selected &&
            (p == Piece.NONE || p.pieceSide != game.board.sideToMove)) {
            animations = listOf(MoveVisual.rejected(++animationId, game.board, Move(selected!!, square)))
            selected = null; highlighted = null; feedback = ""
            return
        }
        selected = if (p != Piece.NONE && p.pieceSide == game.board.sideToMove && selected != square) square else null
        highlighted = null
    }
    fun dismissPromotion() { promotion = emptyList() }
    fun move(m: Move) {
        if (animations.isNotEmpty()) return
        val before = game.board.clone()
        val moveCount = game.moves.size
        cancelWork(); promotion = emptyList(); selected = null
        if (screen == "lesson") {
            val a = attempt!!
            if (!a.move(m.uci())) {
                feedback = ""
                animations = listOf(MoveVisual.rejected(++animationId, before, m))
            }
            else if (a.complete) finishLesson() else { hintIndex = 0; feedback = if (a.lesson.mateIn > 0) "看看对手的回应，再找一步将杀。" else "看看对手的回应，再找下一步。" }
        } else {
            if (game.play(m.uci())) feedback = ""
        }
        animateMoves(before, game.moves.drop(moveCount))
        changed(); scheduleBot()
    }
    fun answer(value: String) {
        if (attempt!!.answer(value)) finishLesson() else feedback = "再观察一下：王被攻击了吗？还有合法走法吗？"
        changed()
    }
    private fun finishLesson() { completed = completed + attempt!!.lesson.id; feedback = "挑战成功，真棒！${attempt!!.lesson.explanation}" }
    fun retryLesson() { attempt?.lesson?.let { openLesson(it) } }
    fun nextLesson() {
        val index = lessons.indexOfFirst { it.id == attempt?.lesson?.id }
        if (index + 1 < lessons.size) openLesson(lessons[index + 1]) else navigate("learn")
    }
    fun undo() {
        if (!assistance || screen == "lesson") return
        cancelWork(); promotion = emptyList(); selected = null
        if (screen == "bot") {
            if (bot.moves.isNotEmpty()) { bot.undo(); if (bot.board.sideToMove != humanSide && bot.moves.isNotEmpty()) bot.undo() }
        } else game.undo()
        feedback = "一起想一想，再走一次。"; changed(); scheduleBot()
    }
    fun resign() { if (game.result != null) return; cancelWork(); game.result = "${(if (screen == "bot") humanSide.flip() else game.board.sideToMove.flip()).chinese()}获胜 · 对方认输"; changed() }
    fun agreeDraw() { if (game.result != null) return; cancelWork(); game.result = "和棋 · 双方同意"; changed() }
    fun claims(): Map<String,String> = game.intendedClaims()
    fun claim(uci: String? = null) {
        val reason = if (uci == null) game.claimReason() else game.intendedClaims()[uci]
        if (reason != null) { cancelWork(); game.result = "和棋 · $reason"; changed() }
    }
    fun hint() {
        if (screen == "lesson") {
            if (animations.isNotEmpty() || attempt!!.complete) return
            val a = attempt!!
            val uci = a.lesson.lines.firstOrNull { it.take(a.played.size) == a.played }?.getOrNull(a.played.size)
            val next = game.legal().firstOrNull { it.uci() == uci }
            if (hintIndex == 0 && a.played.isEmpty()) feedback = a.lesson.hints.first()
            else if (next != null) {
                selected = next.from; highlighted = next
                val promotionHint = if (next.promotion == Piece.NONE) "" else "，升变成${next.promotion.chinese()}"
                feedback = "试试${game.board.getPiece(next.from).chinese()}从 ${next.from} 到 ${next.to}$promotionHint。"
            }
            hintIndex++; narrate(feedback); return
        }
        if (!assistance || game.result != null || thinking || (screen == "bot" && game.board.sideToMove != humanSide)) return
        cancelWork(); val token = generation; val snapshot = game.board.clone(); thinking = true
        job = viewModelScope.launch {
            val task = coroutineContext[Job]!!
            val best = withContext(Dispatchers.Default) { ChessBot().choose(snapshot, 1, 700) { !task.isActive } }
            if (token == generation) { thinking = false; highlighted = best; feedback = best?.let { "试着把${snapshot.getPiece(it.from).chinese()}从 ${it.from} 走到 ${it.to}。" } ?: "当前没有可用提示。" }
        }
    }
    private fun scheduleBot() {
        if (thinking) return
        if (screen != "bot" || bot.result != null || bot.board.sideToMove == humanSide) return
        if (bot.claimReason() != null) { claim(); return }
        val token = generation; val snapshot = bot.board.clone(); thinking = true
        job = viewModelScope.launch {
            val task = coroutineContext[Job]!!
            delay(300)
            val best = withContext(Dispatchers.Default) { ChessBot().choose(snapshot, level) { !task.isActive } }
            if (token == generation && screen == "bot" && best != null) { val before = bot.board.clone(); if (bot.play(best.uci())) animateMoves(before, listOf(best.uci())); thinking = false; changed() }
            else if (token == generation) thinking = false
        }
    }
    fun suspendWork() { narrator.stop(); cancelWork(); save() }
    fun resumeWork() { scheduleBot() }
}
