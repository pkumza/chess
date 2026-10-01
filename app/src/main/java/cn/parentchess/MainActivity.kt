package cn.parentchess

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt
import android.os.Bundle
import android.graphics.Typeface
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.bhlangonijr.chesslib.*

private val Ink = Color(0xFF213D36)
private val Green = Color(0xFF286653)
private val Cream = Color(0xFFF8F5EC)
private val Muted = Color(0xFF66786D)
private val Gold = Color(0xFFE9B45B)
private val Pale = Color(0xFFE4ECDB)

class MainActivity : ComponentActivity() {
    private val vm: ChessViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Cream, surface = Cream, onSurface = Ink, secondary = Gold)) {
                CompositionLocalProvider(LocalUiClick provides vm::clickSound) {
                    Surface(Modifier.fillMaxSize(), color = Cream) { ChessApp(vm) }
                }
            }
        }
    }
    override fun onStop() { vm.suspendWork(); super.onStop() }
    override fun onStart() { super.onStart(); vm.resumeWork() }
}

@Composable private fun ChessApp(vm: ChessViewModel) {
    val learnState = rememberSaveableStateHolder()
    val revision = vm.revision
    var configure by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    BackHandler(vm.screen != "home") { vm.navigate(if (vm.screen == "lesson") "learn" else "home") }
    val playing = vm.screen in listOf("pvp", "bot", "lesson")
    val view = LocalView.current
    DisposableEffect(playing, view) {
        val window = (view.context as android.app.Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (playing) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
    if (playing) {
        Play(vm, revision, onConfigure = { configure = true })
    } else Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (vm.screen != "home") PlayfulTextButton(onClick = { vm.navigate("home") }) { Text("‹ 返回", fontSize = 18.sp) }
            Text("一起下棋", fontWeight = FontWeight.Bold, fontSize = 23.sp, color = Ink)
            Spacer(Modifier.weight(1f))
            Text("亲子时光 · 离线可玩", fontSize = 13.sp, color = Muted)
            PlayfulTextButton(onClick = { about = true }) { Text("关于") }
        }
        Spacer(Modifier.height(10.dp))
        when(vm.screen) {
            "home" -> Home(vm, onBot = { if (vm.bot.moves.isNotEmpty() && vm.bot.result == null) vm.navigate("bot") else configure = true })
            "learn" -> learnState.SaveableStateProvider("learn") { Learn(vm) }
        }
    }
    if (configure) BotDialog(vm, onDismiss = { configure = false })
    if (vm.promotion.isNotEmpty()) AlertDialog(
        onDismissRequest = vm::dismissPromotion,
        title = { Text("兵到终点啦！") }, text = { Text("选择要升变的棋子。后、车、象和马都可以。") },
        confirmButton = { Row { vm.promotion.forEach { m -> PlayfulTextButton(onClick = { vm.move(m) }) { Text(m.promotion.chinese(), fontSize = 24.sp) } } } }
    )
    if (about) {
        val context = LocalContext.current
        val licenses = remember { context.assets.open("THIRD-PARTY.txt").bufferedReader().use { it.readText() } }
        val appVersion = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        AlertDialog(onDismissRequest = { about = false }, title = { Text("一起下棋 · $appVersion") }, text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text("author：pkumza\n\n给大朋友和小朋友的棋盘。\n无账号、无广告，棋局与学习记录只保存在本机。\n\n$licenses", fontSize = 14.sp)
            }
        }, confirmButton = { PlayfulTextButton(onClick = { about = false }) { Text("知道了") } })
    }
}

@Composable private fun Home(vm: ChessViewModel, onBot: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Pale).padding(28.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("每天一点点，一起更厉害", color = Green, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                Text("今天，下一盘吧。", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(12.dp))
                Text("和爸爸妈妈轮流走，\n或帮小棋子找到获胜的一步。", color = Muted, fontSize = 18.sp, lineHeight = 28.sp)
            }
            PieceArt(Piece.WHITE_KNIGHT, Modifier.size(110.dp))
        }
        ModeCard("01", "一起下棋", "两个人，一块棋盘。慢慢想，轮流走。", "开始双人对弈", Pale) { vm.navigate("pvp") }
        ModeCard("02", "机器人陪练", "选一个小对手，练练你的新本领。", "${if (vm.bot.moves.isNotEmpty() && vm.bot.result == null) "继续" else "开始"}机器人陪练", Color(0xFFF3E8CC), onBot)
        ModeCard("03", "闯关学习", "观察、吃子、将杀和防守，交错练习慢慢进阶。", "去闯关 · ${vm.completedLessonCount} / ${vm.lessons.size}", Color(0xFFE5EAF1)) { vm.navigate("learn") }
        Text("小约定：不着急，不怕错，每一步都可以一起讨论。", color = Muted, fontSize = 15.sp, modifier = Modifier.padding(8.dp))
    }
}

@Composable private fun ModeCard(number: String, title: String, body: String, action: String, tint: Color, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E6DC))) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(tint), contentAlignment = Alignment.Center) { Text(number, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Green) }
            Column(Modifier.weight(1f)) { Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(body, color = Muted, fontSize = 15.sp) }
            PlayfulButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp)) { Text(action, fontSize = 16.sp) }
        }
    }
}

@Composable private fun BotDialog(vm: ChessViewModel, onDismiss: () -> Unit) {
    var level by remember { mutableIntStateOf(vm.level) }
    var white by remember { mutableStateOf(vm.humanWhite) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选一个小对手") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("初学会留出机会，练习会认真想一想。")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { PlayfulChip(level == 0, { level = 0 }, { Text("初学") }); PlayfulChip(level == 1, { level = 1 }, { Text("练习") }) }
            Text("你想用哪一边？白方先走。")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { PlayfulChip(white, { white = true }, { Text("我用白方") }); PlayfulChip(!white, { white = false }, { Text("我用黑方") }) }
            if (vm.bot.moves.isNotEmpty() && vm.bot.result == null) Text("开始后会替换当前的机器人棋局。", color = Muted)
        }
    }, confirmButton = { PlayfulButton(onClick = { vm.configureBot(level, white); onDismiss() }) { Text("开始新棋局") } }, dismissButton = { PlayfulTextButton(onClick = onDismiss) { Text("取消") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Learn(vm: ChessViewModel) {
    val chapters = remember(vm.lessons) { vm.lessons.groupBy { it.chapter } }
    val chapter = vm.lessonChapter
    val lessons = chapters[chapter].orEmpty()
    val chapterStates = rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize()) {
        Text("小小棋手的冒险", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("${vm.lessons.size} 个进阶挑战 · 已完成 ${vm.completedLessonCount} 关", color = Muted, modifier = Modifier.padding(vertical = 10.dp))
        LinearProgressIndicator(progress = { vm.completedLessonCount.toFloat() / vm.lessons.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)), color = Green, trackColor = Pale)
        Spacer(Modifier.height(10.dp))
        Text("从观察热身开始，每次练几关；熟练后再向前走", color = Muted, fontSize = 14.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chapters.keys.forEach { name ->
                PlayfulChip(selected = chapter == name, onClick = { vm.selectLessonChapter(name) }, label = {
                    Text(name.substringAfter(" · "))
                })
            }
        }
        Text("${chapter.substringAfter(" · ")} · 已完成 ${LessonCatalog.completedCount(lessons, vm.completed)} / ${lessons.size}",
            fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 10.dp))
        chapterStates.SaveableStateProvider(chapter) {
            val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
            LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(lessons, key = { it.id }) { lesson ->
                    PlayfulCard(onClick = { vm.openLesson(lesson) }, shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, Pale)) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(if (lesson.id in vm.completed) Green else Pale), contentAlignment = Alignment.Center) { Text(if (lesson.id in vm.completed) "★" else lesson.number.toString(), color = if (lesson.id in vm.completed) Color.White else Green, fontSize = 20.sp) }
                            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) { Text(lesson.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Text(lesson.goal, color = Muted, fontSize = 14.sp) }
                            Text(if (lesson.id in vm.completed) "再练一次 ›" else "出发 ›", color = Green)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Play(vm: ChessViewModel, revision: Int, onConfigure: () -> Unit) {
    var showPanel by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize().displayCutoutPadding()) {
        val boardSize = minOf(maxWidth, maxHeight)
        val landscape = maxWidth >= maxHeight
        val spare = maxOf(maxWidth, maxHeight) - boardSize
        // The board always gets the largest square; controls use only the remainder.
        if (spare >= 220.dp) {
            if (landscape) Row(Modifier.fillMaxSize()) {
                ChessBoard(vm, revision, Modifier.size(boardSize))
                PlayPanel(vm, revision, onConfigure, Modifier.weight(1f).fillMaxHeight())
            } else Column(Modifier.fillMaxSize()) {
                ChessBoard(vm, revision, Modifier.size(boardSize))
                PlayPanel(vm, revision, onConfigure, Modifier.weight(1f).fillMaxWidth())
            }
        } else {
            // Near-square split-screen windows have no useful side strip.
            ChessBoard(vm, revision, Modifier.size(boardSize).align(Alignment.Center))
            PlayfulTonalButton(onClick = { showPanel = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) { Text("操作") }
        }
    }
    if (showPanel) ModalBottomSheet(onDismissRequest = { showPanel = false }) {
        PlayPanel(vm, revision, onConfigure, Modifier.fillMaxWidth().fillMaxHeight(0.8f))
    }
}

@Composable private fun PlayPanel(vm: ChessViewModel, revision: Int, onConfigure: () -> Unit, modifier: Modifier) {
    val record = remember(revision, vm.screen) { vm.game.record() }
    val board = vm.game.board
    val lesson = if (vm.screen == "lesson") vm.attempt?.lesson else null
    val complete = lesson != null && vm.attempt!!.complete
    val status = if (complete) "挑战成功！" else record.result ?: if (vm.thinking) "正在想一想…" else "轮到${board.sideToMove.chinese()}${if (board.isKingAttacked) " · 将军！" else "走棋"}"
    val caption = lesson?.let { "第 ${it.number} 关 · ${it.title}" } ?: if (vm.screen == "bot") "机器人 · ${if (vm.level == 0) "初学" else "练习"} · 你执${if (vm.humanWhite) "白" else "黑"}" else "双人对弈"
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PlayfulTextButton(onClick = { vm.navigate(if (vm.screen == "lesson") "learn" else "home") }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("‹ 返回", fontSize = 17.sp) }
            PlayfulTextButton(onClick = vm::toggleSound, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(if (vm.soundEnabled) "音效：开" else "音效：关", fontSize = 14.sp) }
            Spacer(Modifier.weight(1f))
            Text(caption, color = Muted, fontSize = 14.sp, modifier = Modifier.weight(3f), textAlign = TextAlign.End)
        }
        Surface(color = if (complete) Pale else Color.White, shape = RoundedCornerShape(16.dp)) {
            Text(status, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                color = if (!complete && board.isKingAttacked && record.result == null) Color(0xFFAA4635) else Green)
        }
        Controls(vm, revision, onConfigure, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable private fun ChessBoard(vm: ChessViewModel, revision: Int, modifier: Modifier) {
    val game = vm.game
    val flipped = vm.flipped
    val frame = vm.animations.firstOrNull()
    val travel = remember(frame?.id) { Animatable(0f) }
    val impact = remember(frame?.id) { Animatable(0f) }
    LaunchedEffect(frame?.id) {
        if (frame != null) {
            val path = frame.travels.first()
            val distance = maxOf(kotlin.math.abs(path.from.ordinal % 8 - path.to.ordinal % 8),
                kotlin.math.abs(path.from.ordinal / 8 - path.to.ordinal / 8)).coerceAtLeast(1)
            travel.animateTo(if (frame.blocked) 0.18f / distance else 1f, tween(300, easing = FastOutSlowInEasing))
            vm.landAnimation(frame.id)
            if (frame.retry) {
                impact.animateTo(1f, tween(180))
                kotlinx.coroutines.delay(850)
                impact.animateTo(0f, tween(180))
                travel.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
            } else impact.animateTo(1f, tween(if (frame.capture) 360 else 110, easing = if (frame.capture) LinearEasing else LinearOutSlowInEasing))
            vm.finishAnimation(frame.id)
        }
    }
    val legal = remember(revision, vm.selected, vm.screen) { game.legal().filter { it.from == vm.selected }.map { it.to }.toSet() }
    val last = frame?.move ?: game.moves.lastOrNull()
    val lastSquares = if (last != null) listOf(last.take(2), last.substring(2,4)) else emptyList()
    val hint = vm.highlighted
    val show = vm.assistance || vm.screen == "lesson"
    val movingFrom = frame?.travels?.map { it.from }.orEmpty()
    fun position(sq: Square): Pair<Int,Int> = if (flipped) (7 - sq.ordinal % 8) to (sq.ordinal / 8) else (sq.ordinal % 8) to (7 - sq.ordinal / 8)
    BoxWithConstraints(modifier.clip(RoundedCornerShape(12.dp)).border(3.dp, Green, RoundedCornerShape(12.dp)).padding(3.dp)) {
        val cell = maxWidth / 8
        Column(Modifier.fillMaxSize()) {
            repeat(8) { row ->
                Row(Modifier.weight(1f)) {
                    repeat(8) { col ->
                        val rank = if (flipped) row else 7 - row
                        val file = if (flipped) 7 - col else col
                        val sq = Square.entries[rank * 8 + file]
                        val p = frame?.before?.get(sq.ordinal) ?: game.board.getPiece(sq)
                        val selected = vm.selected == sq
                        val check = frame == null && p.pieceType == PieceType.KING && p.pieceSide == game.board.sideToMove && game.board.isKingAttacked
                        val recent = sq.name.lowercase() in lastSquares
                        val bg = when { frame?.retry == true && sq == frame.travels.first().to -> Color(0xFFFFCE8A); selected -> Gold; check -> Color(0xFFEFA28C); recent -> Color(0xFFD4D58B); (file + rank) % 2 == 0 -> Color(0xFF87A892); else -> Color(0xFFF0EDD9) }
                        Box(Modifier.weight(1f).fillMaxHeight().background(bg).then(if (selected || check || sq == hint?.from || sq == hint?.to) Modifier.border(3.dp, if (check) Color(0xFF9F3D30) else Green) else Modifier).clickable(enabled = frame == null) { vm.tap(sq) }.semantics { contentDescription = "${sq.name.lowercase()} ${if (p == Piece.NONE) "空格" else p.pieceSide.chinese() + p.chinese()}" }, contentAlignment = Alignment.Center) {
                            if (p != Piece.NONE && sq !in movingFrom && sq != frame?.capturedSquare) PieceArt(p, Modifier.fillMaxSize().padding(3.dp))
                            if (show && frame == null && sq in legal) Box(Modifier.size(if (p == Piece.NONE) 14.dp else 35.dp).clip(RoundedCornerShape(40.dp)).then(if (p == Piece.NONE) Modifier.background(Green.copy(alpha = 0.55f)) else Modifier.border(3.dp, Green, RoundedCornerShape(40.dp))))
                            if (col == 0) Text("${rank + 1}", fontSize = 10.sp, color = Ink, modifier = Modifier.align(Alignment.TopStart).padding(2.dp))
                            if (row == 7) Text(('a' + file).toString(), fontSize = 10.sp, color = Ink, modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp))
                        }
                    }
                }
            }
        }
        if (frame == null && hint != null && vm.screen == "lesson") {
            val (fx, fy) = position(hint.from)
            val (tx, ty) = position(hint.to)
            Canvas(Modifier.fillMaxSize()) {
                val unit = size.width / 8
                val start = Offset((fx + .5f) * unit, (fy + .5f) * unit)
                val end = Offset((tx + .5f) * unit, (ty + .5f) * unit)
                val delta = end - start
                val direction = delta / delta.getDistance()
                val tip = end - direction * unit * .22f
                val tail = tip - direction * unit * .22f
                val side = Offset(-direction.y, direction.x) * unit * .12f
                drawLine(Green, start + direction * unit * .25f, tip, 5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(Green, tip, tail + side, 5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(Green, tip, tail - side, 5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
        // Captured pieces stay visible until contact, then break apart under the arriving piece.
        frame?.capturedSquare?.let { sq ->
            val (x,y) = position(sq)
            val victimModifier = Modifier.offset { IntOffset((cell.toPx()*x).roundToInt(), (cell.toPx()*y).roundToInt()) }.size(cell)
            if (frame.retry) PieceArt(frame.before[sq.ordinal], victimModifier.graphicsLayer { alpha = 1f - travel.value }.padding(3.dp))
            else ShatteringPiece(frame.before[sq.ordinal], impact.value, victimModifier)
        }
        frame?.travels?.forEach { path ->
            val (fx,fy) = position(path.from)
            val (tx,ty) = position(path.to)
            val t = travel.value
            val lift = sin(t * PI).toFloat()
            PieceArt(path.piece, Modifier.offset {
                IntOffset((cell.toPx()*(fx+(tx-fx)*t)).roundToInt(), (cell.toPx()*(fy+(ty-fy)*t) - cell.toPx()*0.06f*lift).roundToInt())
            }.size(cell).graphicsLayer {
                val scale = 1f + 0.07f*lift + 0.035f*sin(impact.value*PI).toFloat()
                scaleX = scale; scaleY = scale
            }.padding(3.dp))
        }
        if (frame?.retry == true && impact.value > 0f) {
            Surface(modifier = Modifier.align(Alignment.TopCenter).padding(8.dp).graphicsLayer { alpha = impact.value },
                color = Color(0xFFFFEBC2), shape = RoundedCornerShape(20.dp)) {
                Text(if (frame.blocked) "↶ 换个格子" else "↶ 再试试", modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                    fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
        }
    }
}

@Composable private fun ShatteringPiece(piece: Piece, progress: Float, modifier: Modifier) {
    val vertices = listOf(0f to 0f, .5f to 0f, 1f to 0f, 1f to .5f, 1f to 1f, .5f to 1f, 0f to 1f, 0f to .5f)
    Box(modifier) {
        if (progress == 0f) PieceArt(piece, Modifier.fillMaxSize().padding(3.dp))
        else {
            vertices.indices.forEach { i ->
                val a = vertices[i]; val b = vertices[(i+1)%vertices.size]
                val dx = (a.first+b.first)/2 - .5f
                val dy = (a.second+b.second)/2 - .5f
                val shard = remember(i) { GenericShape { size, _ ->
                    moveTo(size.width*.5f, size.height*.5f)
                    lineTo(size.width*a.first, size.height*a.second)
                    lineTo(size.width*b.first, size.height*b.second)
                    close()
                } }
                PieceArt(piece, Modifier.fillMaxSize().graphicsLayer {
                    translationX = dx*size.width*progress*1.7f
                    translationY = dy*size.height*progress*1.4f + size.height*.25f*progress*progress
                    rotationZ = (if (i%2==0) 1 else -1)*progress*(15+i*4)
                    alpha = (1f-progress*progress).coerceIn(0f,1f)
                    scaleX = 1f-.25f*progress; scaleY = scaleX
                }.clip(shard).padding(3.dp))
            }
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width/2,size.height/2)
                drawCircle(Gold.copy(alpha = (1-progress)*.6f), size.minDimension*(.2f+.5f*progress), center, style = Stroke(2.dp.toPx()))
                repeat(8) { i ->
                    val angle = i*PI/4
                    val radius = size.minDimension*(.25f+.55f*progress)
                    drawCircle(Gold.copy(alpha = 1-progress), (3.dp.toPx()*(1-progress)).coerceAtLeast(.1f),
                        center + Offset(cos(angle).toFloat()*radius, sin(angle).toFloat()*radius))
                }
            }
        }
    }
}

@Composable private fun PieceArt(piece: Piece, modifier: Modifier) {
    val context = LocalContext.current
    val typeface = remember { Typeface.createFromAsset(context.assets, "chess-symbols.ttf") }
    val symbol = when (piece.pieceType) { PieceType.KING -> "♚"; PieceType.QUEEN -> "♛"; PieceType.ROOK -> "♜"; PieceType.BISHOP -> "♝"; PieceType.KNIGHT -> "♞"; else -> "♟" }
    Canvas(modifier) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface; textSize = size.minDimension * 0.87f; textAlign = android.graphics.Paint.Align.CENTER }
        val bounds = android.graphics.Rect(); paint.getTextBounds(symbol, 0, symbol.length, bounds)
        val y = size.height / 2 - bounds.exactCenterY()
        paint.style = android.graphics.Paint.Style.STROKE; paint.strokeWidth = size.minDimension * 0.035f
        paint.color = if (piece.pieceSide == Side.WHITE) 0xFF213D36.toInt() else 0xFFF9F5DF.toInt()
        drawContext.canvas.nativeCanvas.drawText(symbol, size.width / 2, y, paint)
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = if (piece.pieceSide == Side.WHITE) 0xFFFFFBEA.toInt() else 0xFF213D36.toInt()
        drawContext.canvas.nativeCanvas.drawText(symbol, size.width / 2, y, paint)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Controls(vm: ChessViewModel, revision: Int, onConfigure: () -> Unit, modifier: Modifier) {
    val record = remember(revision, vm.screen) { vm.game.record() }
    var confirm by remember(vm.screen) { mutableStateOf("") }
    var claimChoices by remember { mutableStateOf<Map<String,String>?>(null) }
    val lesson = if (vm.screen == "lesson") vm.attempt?.lesson else null
    var showSource by remember(lesson?.id) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (lesson != null) {
            Surface(color = Pale, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(18.dp)) { Text(lesson.goal, fontSize = 21.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(lesson.intro, fontSize = 17.sp, lineHeight = 26.sp) } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayfulButton(onClick = vm::repeatNarration, enabled = vm.speechEnabled) { Text(if (vm.speaking) "正在朗读 · 重听" else "再听一遍") }
                PlayfulOutlinedButton(onClick = vm::toggleSpeech) { Text(if (vm.speechEnabled) "朗读：开" else "朗读：关") }
            }
            if (vm.speechEnabled && vm.speechMessage != null) Text(vm.speechMessage!!, fontSize = 14.sp, color = Muted)
            if (lesson.question != null && !vm.attempt!!.complete) { Text(lesson.question, fontSize = 19.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("将杀", "逼和", "还可以下").forEach { answer -> PlayfulOutlinedButton(onClick = { vm.answer(answer) }) { Text(answer) } } } }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("亲子辅助", Modifier.weight(1f), fontSize = 17.sp); PlayfulSwitch(vm.assistance, { vm.toggleAssistance() }) }
            Text(if (vm.assistance) "点棋子，再点亮起的格子。可以悔棋，也可以一起找提示。" else "辅助已关闭。按标准规则走棋，不提供提示和悔棋。", color = Muted, fontSize = 15.sp)
        }
        if (vm.feedback.isNotBlank()) Surface(color = Color(0xFFFFEBC2), shape = RoundedCornerShape(16.dp)) { Text(vm.feedback, Modifier.padding(16.dp), fontSize = 17.sp, lineHeight = 25.sp) }
        if (lesson != null && vm.attempt!!.complete) PlayfulButton(onClick = vm::nextLesson, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("★  已完成！下一关", fontSize = 19.sp) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (lesson?.source != null) PlayfulOutlinedButton(onClick = { showSource = true }) { Text("题目出处") }
            if (lesson == null || !vm.attempt!!.complete) PlayfulOutlinedButton(onClick = vm::hint, enabled = lesson != null || (vm.assistance && !vm.thinking && record.result == null && (vm.screen != "bot" || vm.game.board.sideToMove == vm.humanSide))) { Text("给我提示") }
            if (lesson == null) PlayfulOutlinedButton(onClick = vm::undo, enabled = vm.assistance && record.moves.isNotEmpty()) { Text("悔一步") }
            PlayfulOutlinedButton(onClick = vm::flip) { Text("翻转棋盘") }
            if (lesson != null) PlayfulOutlinedButton(onClick = vm::retryLesson) { Text("再试一次") }
            else {
                PlayfulOutlinedButton(onClick = { confirm = "new" }) { Text("重新开始") }
                if (vm.screen == "bot") PlayfulOutlinedButton(onClick = onConfigure) { Text("更换对手") }
                if (record.result == null) {
                    PlayfulOutlinedButton(onClick = { confirm = "resign" }) { Text("认输") }
                    if (vm.screen == "pvp") PlayfulOutlinedButton(onClick = { confirm = "draw" }) { Text("双方和棋") }
                    if (vm.screen != "bot" || vm.game.board.sideToMove == vm.humanSide) PlayfulOutlinedButton(onClick = { if (vm.game.claimReason() != null) vm.claim() else claimChoices = vm.claims() }) { Text("申请规则和棋") }
                }
            }
        }
        if (lesson == null) {
            HorizontalDivider(color = Pale)
            Text("棋谱 · ${record.moves.size} 步", color = Muted, fontSize = 14.sp)
            Text(record.moves.chunked(2).mapIndexed { index, pair -> "${index + 1}. ${pair.joinToString("  ")}" }.takeLast(10).joinToString("   ").ifBlank { "棋盘准备好了，开始吧。" }, fontSize = 14.sp, color = Muted)
        }
    }
    if (showSource) lesson?.source?.let { source ->
        AlertDialog(onDismissRequest = { showSource = false }, title = { Text("第 ${lesson.number} 关的出处") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${source.name} · ${source.puzzleId}")
                Text("原始对局局面，未删减棋子。每条收录解法都走到将杀；两步题已核对所有合法防守。")
                Text("题库数据：${source.license} · 获取于 ${source.retrievedAt}", fontSize = 13.sp, color = Muted)
                PlayfulTextButton(onClick = { uriHandler.openUri(source.url) }) { Text("查看原题（联网）") }
                PlayfulTextButton(onClick = { uriHandler.openUri(source.gameUrl) }) { Text("查看原始对局（联网）") }
            }
        }, confirmButton = { PlayfulTextButton(onClick = { showSource = false }) { Text("回到棋盘") } })
    }
    if (confirm.isNotEmpty()) AlertDialog(onDismissRequest = { confirm = "" }, title = { Text(when(confirm) { "new" -> "开始新的一盘？"; "resign" -> "确认认输？"; else -> "两位都同意和棋吗？" }) }, text = { Text(when(confirm) { "new" -> "当前棋局会被新棋局替换。学习进度会保留。"; "resign" -> "这盘结束后，也可以一起复盘或再来一盘。"; else -> "请把平板交给另一位棋手，由对方点“同意和棋”。" }) }, confirmButton = { PlayfulTextButton(onClick = { when(confirm) { "new" -> vm.newGame(); "resign" -> vm.resign(); else -> vm.agreeDraw() }; confirm = "" }) { Text(if (confirm == "draw") "同意和棋" else "确定") } }, dismissButton = { PlayfulTextButton(onClick = { confirm = "" }) { Text("继续下") } })
    claimChoices?.let { choices -> AlertDialog(onDismissRequest = { claimChoices = null }, title = { Text("申请规则和棋") }, text = {
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            Text(if (choices.isEmpty()) "还未达到三次重复或 50 回合条件，继续下吧。" else "声明准备走以下一步，并据此申请和棋：")
            choices.forEach { (move, reason) -> PlayfulTextButton(onClick = { vm.claim(move); claimChoices = null }) { Text("$move · $reason") } }
        }
    }, confirmButton = { PlayfulTextButton(onClick = { claimChoices = null }) { Text("返回棋局") } }) }
}
