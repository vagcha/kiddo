package gr.kiddo.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/* ------------------------------------------------------------------ */
/* Hub                                                                 */
/* ------------------------------------------------------------------ */

private data class QuickGame(val screen: Screen, val key: String, val title: String, val emoji: String, val color: Color)

private val QUICK_GAMES = listOf(
    QuickGame(Screen.Balloons, "balloons", "Φούσκες", "🎈", KColors.Red),
    QuickGame(Screen.Moles, "moles", "Βρες το ζωάκι", "🐸", KColors.Green),
    QuickGame(Screen.Catch, "catch", "Πιάσε τα φρούτα", "🍎", KColors.Orange),
    QuickGame(Screen.Simon, "simon", "Θυμήσου τη σειρά", "🎵", KColors.Purple),
)

@Composable
fun GamesHub(onBack: () -> Unit, onOpen: (Screen) -> Unit) {
    val app = LocalAppState.current
    LaunchedEffect(Unit) { Speaker.say("Διάλεξε παιχνίδι!") }
    KiddoPage("Γρήγορα παιχνίδια", KColors.Red, onBack, speakText = "Διάλεξε παιχνίδι!") {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QUICK_GAMES.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { g ->
                        Surface(
                            shape = RoundedCornerShape(26.dp), color = g.color, shadowElevation = 6.dp,
                            modifier = Modifier.weight(1f).aspectRatio(1f).clickable {
                                Synth.pop(); Speaker.say(g.title); onOpen(g.screen)
                            }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(g.emoji, fontSize = 60.sp)
                                Text(g.title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                                Text("Ρεκόρ: ${app.best(g.key)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Shared game pieces                                                   */
/* ------------------------------------------------------------------ */

@Composable
private fun Pill(text: String, warn: Boolean = false) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (warn) KColors.Red else Color.White)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        fontSize = 20.sp, fontWeight = FontWeight.Black,
        color = if (warn) Color.White else KColors.Ink
    )
}

@Composable
private fun Hud(timeLeft: Int?, score: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
        if (timeLeft != null) Pill("⏱ $timeLeft", warn = timeLeft <= 5)
        Pill(score)
    }
}

@Composable
private fun Banner(text: String, color: Color = KColors.Ink) {
    Text(
        text, Modifier.fillMaxWidth().padding(vertical = 6.dp), textAlign = TextAlign.Center,
        fontSize = 24.sp, fontWeight = FontWeight.Black, color = color
    )
}

/** Instruction card with "Ξεκίνα!" and a 3-2-1 countdown. */
@Composable
private fun BoxScope.StartOverlay(text: String, onStart: () -> Unit) {
    var count by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { Speaker.say(text) }
    LaunchedEffect(count) {
        if (count > 0) {
            Synth.note(Synth.C5, 250)
            delay(650)
            if (count == 1) {
                Synth.note(Synth.C6, 400)
                onStart()
            } else count--
        }
    }
    Box(
        Modifier.matchParentSize().background(Color.White.copy(alpha = 0.75f)).clickable(enabled = true) {},
        contentAlignment = Alignment.Center
    ) {
        if (count == 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                Text(text, fontSize = 22.sp, fontWeight = FontWeight.Black, color = KColors.Ink, textAlign = TextAlign.Center)
                BigButton("Ξεκίνα!", KColors.Green, Modifier.padding(top = 16.dp).width(220.dp), fontSize = 24) {
                    Speaker.stop(); count = 3
                }
            }
        } else {
            Text("$count", fontSize = 110.sp, fontWeight = FontWeight.Black, color = KColors.Orange)
        }
    }
}

/** Ticks once a second while [running]; calls [onEnd] at zero. */
@Composable
private fun Countdown(running: Boolean, timeLeft: Int, onTick: (Int) -> Unit, onEnd: () -> Unit) {
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var t = timeLeft
        while (t > 0) {
            delay(1000)
            t--
            onTick(t)
            if (t in 1..5) Synth.note(Synth.G5, 120)
        }
        onEnd()
    }
}

private class Result(val stars: Int, val message: String)

private fun finish(app: AppState, game: String, score: Int, t2: Int, t3: Int, message: String): Result {
    val record = app.saveBest(game, score)
    val stars = if (score >= t3) 3 else if (score >= t2) 2 else 1
    app.addStars(stars)
    return Result(stars, message + if (record && score > 0) " Νέο ρεκόρ!" else "")
}

private class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val color: Color)

private fun MutableList<Particle>.burst(x: Float, y: Float, color: Color) {
    repeat(14) {
        val a = Random.nextFloat() * 2f * PI.toFloat()
        val v = 220f + Random.nextFloat() * 380f
        add(Particle(x, y, cos(a) * v, sin(a) * v, 0.6f, color))
    }
}

private fun DrawScope.drawParticles(parts: MutableList<Particle>, dt: Float) {
    val it = parts.iterator()
    while (it.hasNext()) {
        val p = it.next()
        p.life -= dt
        if (p.life <= 0f) { it.remove(); continue }
        p.x += p.vx * dt; p.y += p.vy * dt; p.vy += 800f * dt
        drawCircle(p.color.copy(alpha = (p.life / 0.6f).coerceIn(0f, 1f)), 12f, Offset(p.x, p.y))
    }
}

/** Shared frame loop: calls [step] with the frame time while the composable is shown. */
@Composable
private fun FrameLoop(step: (Float) -> Unit) {
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                step(dt)
            }
        }
    }
}

/** Wraps a game so "Ξανά" restarts it cleanly. */
@Composable
private fun Replayable(content: @Composable (restart: () -> Unit) -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    key(round) { content { round++ } }
}

/* ------------------------------------------------------------------ */
/* Balloons                                                            */
/* ------------------------------------------------------------------ */

private val BALLOON_COLORS = listOf(
    "κόκκινες" to Color(0xFFF2453D), "μπλε" to Color(0xFF3D8BFF), "κίτρινες" to Color(0xFFFFC928),
    "πράσινες" to Color(0xFF2FAE52), "μωβ" to Color(0xFF8E44EC),
)

private class Balloon(val x: Float, var y: Float, val r: Float, val vy: Float, var wob: Float, val c: Int, val n: Int?) {
    var drawX = x
}

private data class MathTarget(val a: Int, val b: Int, val op: Int) {
    val answer = when (op) { 0 -> a + b; 1 -> a - b; else -> a * b }
    val text = "$a ${listOf("+", "−", "×")[op]} $b"
    val speech = "$a ${listOf("συν", "μείον", "επί")[op]} $b"
}

private fun newMath(): MathTarget = when (Random.nextInt(3)) {
    0 -> MathTarget(Random.nextInt(2, 13), Random.nextInt(2, 13), 0)
    1 -> { val a = Random.nextInt(8, 21); MathTarget(a, Random.nextInt(1, a), 1) }
    else -> MathTarget(Random.nextInt(2, 7), Random.nextInt(2, 7), 2)
}

@Composable
fun BalloonsGame(onBack: () -> Unit, onHome: () -> Unit) = Replayable { restart ->
    val app = LocalAppState.current
    val older = app.older
    val measurer = rememberTextMeasurer()
    val balloons = remember { mutableListOf<Balloon>() }
    val parts = remember { mutableListOf<Particle>() }
    var frame by remember { mutableLongStateOf(0L) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var running by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(45) }
    var score by remember { mutableIntStateOf(0) }
    var colorTarget by remember { mutableIntStateOf(Random.nextInt(BALLOON_COLORS.size)) }
    var mathTarget by remember { mutableStateOf(newMath()) }
    var result by remember { mutableStateOf<Result?>(null) }
    var spawnT by remember { mutableStateOf(0f) }
    var elapsed by remember { mutableStateOf(0f) }
    var hits by remember { mutableIntStateOf(0) }

    fun speech() = if (older) "Σπάσε τη φούσκα με το αποτέλεσμα του ${mathTarget.speech}" else "Σπάσε τις ${BALLOON_COLORS[colorTarget].first} φούσκες!"

    fun nextTarget() {
        if (older) mathTarget = newMath()
        else {
            var t: Int
            do { t = Random.nextInt(BALLOON_COLORS.size) } while (t == colorTarget)
            colorTarget = t
        }
    }

    FrameLoop { dt ->
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        if (w <= 0f) return@FrameLoop
        if (running) {
            elapsed += dt
            spawnT -= dt
            if (spawnT <= 0f) {
                spawnT = if (older) 0.5f else 0.62f
                val r = (w / 9f).coerceIn(70f, 120f)
                val c: Int
                var n: Int? = null
                if (!older) {
                    c = if (Random.nextFloat() < 0.42f) colorTarget else Random.nextInt(BALLOON_COLORS.size)
                } else {
                    c = Random.nextInt(BALLOON_COLORS.size)
                    n = if (Random.nextFloat() < 0.38f) mathTarget.answer
                    else max(0, mathTarget.answer + listOf(-3, -2, -1, 1, 2, 3, 5).random())
                }
                val speed = (150f + Random.nextFloat() * 120f) * (1f + elapsed / 45f) * (if (older) 1.2f else 1f)
                balloons.add(Balloon(r + Random.nextFloat() * (w - 2 * r), h + r * 1.4f, r, speed, Random.nextFloat() * 6f, c, n))
            }
        }
        val it = balloons.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.y -= b.vy * dt
            b.wob += dt * 2f
            b.drawX = b.x + sin(b.wob) * 16f
            if (b.y < -b.r * 2.5f) it.remove()
        }
        frame++
        lastDt = dt
    }

    Countdown(running, timeLeft, { timeLeft = it }) {
        running = false
        result = finish(app, "balloons", score, 8, 15, "Έσπασες $score φούσκες!")
    }

    Box(Modifier.fillMaxSize()) {
        KiddoPage("Φούσκες", KColors.Red, onBack, speakText = speech()) {
            Hud(timeLeft, "⭐ $score")
            if (older) Banner("Βρες τη φούσκα: ${mathTarget.text} = ?")
            else Banner("Σπάσε τις ${BALLOON_COLORS[colorTarget].first.greekCaps()}!", BALLOON_COLORS[colorTarget].second)
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(12.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFBFE6FF), KColors.Cream)))
            ) {
                Canvas(
                    Modifier.fillMaxSize()
                        .onSizeChanged { size = it }
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = { pos ->
                                if (!running) return@detectTapGestures
                                val hit = balloons.lastOrNull { b ->
                                    val dx = (pos.x - b.drawX) / b.r
                                    val dy = (pos.y - b.y) / (b.r * 1.2f)
                                    dx * dx + dy * dy <= 1.3f
                                } ?: return@detectTapGestures
                                balloons.remove(hit)
                                parts.burst(hit.drawX, hit.y, BALLOON_COLORS[hit.c].second)
                                Synth.pop()
                                val good = if (older) hit.n == mathTarget.answer else hit.c == colorTarget
                                if (good) {
                                    score++; hits++
                                    Synth.correct()
                                    if (older || hits % 5 == 0) {
                                        nextTarget()
                                        Speaker.say(if (older) mathTarget.speech else "Τώρα σπάσε τις ${BALLOON_COLORS[colorTarget].first}!")
                                    }
                                } else Synth.wrong()
                            })
                        }
                ) {
                    if (frame < 0) return@Canvas
                    balloons.forEach { b -> drawBalloon(b, measurer) }
                    drawParticles(parts, lastDt)
                }
                if (!running && result == null) {
                    StartOverlay(
                        if (older) "Σπάσε τη φούσκα με το σωστό αποτέλεσμα! Έχεις 45 δευτερόλεπτα."
                        else "Σπάσε τις φούσκες με το χρώμα που λέω! Έχεις 45 δευτερόλεπτα."
                    ) {
                        running = true
                        Speaker.say(speech())
                    }
                }
            }
        }
        result?.let { r -> Celebration(r.stars, r.message, onAgain = restart, onHome = onHome) }
    }
}

private var lastDt = 0.016f

/** Greek all-caps drop the accents: "κόκκινες" -> "ΚΟΚΚΙΝΕΣ". */
private fun String.greekCaps(): String =
    java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").uppercase()

private fun DrawScope.drawBalloon(b: Balloon, measurer: TextMeasurer) {
    val col = BALLOON_COLORS[b.c].second
    val x = b.drawX
    val y = b.y
    val string = Path().apply {
        moveTo(x, y + b.r * 1.2f)
        quadraticTo(x + 20f, y + b.r * 1.7f, x, y + b.r * 2.2f)
    }
    drawPath(string, Color(0xFF8A8199), style = Stroke(4f))
    val knot = Path().apply {
        moveTo(x - 14f, y + b.r * 1.3f); lineTo(x + 14f, y + b.r * 1.3f); lineTo(x, y + b.r * 1.14f); close()
    }
    drawPath(knot, col)
    drawOval(
        Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.8f), 0.25f to col, 1f to col,
            center = Offset(x - b.r * 0.35f, y - b.r * 0.45f), radius = b.r * 1.6f
        ),
        topLeft = Offset(x - b.r, y - b.r * 1.2f), size = Size(b.r * 2, b.r * 2.4f)
    )
    b.n?.let { n ->
        val layout = measurer.measure(
            "$n", TextStyle(color = Color.White, fontSize = (b.r * 0.8f / density).sp, fontWeight = FontWeight.Black)
        )
        drawText(layout, topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f))
    }
}

/* ------------------------------------------------------------------ */
/* Whack-an-animal                                                     */
/* ------------------------------------------------------------------ */

private class HoleState(val animal: Animal) {
    var hit by mutableStateOf(false)
}

@Composable
fun MolesGame(onBack: () -> Unit, onHome: () -> Unit) = Replayable { restart ->
    val app = LocalAppState.current
    val older = app.older
    val scope = rememberCoroutineScope()
    val holes = remember { mutableStateListOf<HoleState?>().apply { repeat(9) { add(null) } } }
    var target by remember { mutableStateOf(ANIMALS.random()) }
    var running by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(40) }
    var score by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<Result?>(null) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var elapsed = 0f
        while (running) {
            val free = holes.indices.filter { holes[it] == null }
            if (free.isNotEmpty()) {
                val i = free.random()
                val st = HoleState(if (Random.nextFloat() < 0.45f) target else ANIMALS.random())
                holes[i] = st
                val stay = max(if (older) 650f else 950f, (if (older) 1150f else 1600f) - elapsed * 15f).toLong()
                scope.launch {
                    delay(stay)
                    if (holes[i] === st && !st.hit) holes[i] = null
                }
            }
            val gap = max(if (older) 280f else 420f, (if (older) 650f else 900f) - elapsed * 10f)
            delay(gap.toLong())
            elapsed += gap / 1000f
        }
    }

    Countdown(running, timeLeft, { timeLeft = it }) {
        running = false
        for (i in holes.indices) holes[i] = null
        result = finish(app, "moles", score, 6, 12, "Βρήκες $score ζωάκια!")
    }

    Box(Modifier.fillMaxSize()) {
        KiddoPage("Βρες το ζωάκι", KColors.Green, onBack, speakText = "Πάτα ${target.acc}!") {
            Hud(timeLeft, "⭐ $score")
            Banner("Πάτα ${target.acc}!")
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(12.dp)
                    .clip(RoundedCornerShape(26.dp)).background(Color(0xFFD8F3C2))
            ) {
                Column(
                    Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                ) {
                    for (r in 0 until 3) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (c in 0 until 3) {
                                val i = r * 3 + c
                                Hole(holes[i], Modifier.weight(1f)) {
                                    val st = holes[i]
                                    if (!running || st == null || st.hit) return@Hole
                                    if (st.animal == target) {
                                        score++; hits++
                                        st.hit = true
                                        Synth.correct()
                                        scope.launch { delay(250); if (holes[i] === st) holes[i] = null }
                                        if (hits % 3 == 0) {
                                            var t: Animal
                                            do { t = ANIMALS.random() } while (t == target)
                                            target = t
                                            Speaker.say("Τώρα πάτα ${t.acc}!")
                                        }
                                    } else Synth.wrong()
                                }
                            }
                        }
                    }
                }
                if (!running && result == null) {
                    StartOverlay("Τα ζωάκια πετάγονται από τις τρύπες. Πάτα μόνο αυτό που σου λέω! Έχεις 40 δευτερόλεπτα.") {
                        running = true
                        Speaker.say("Πάτα ${target.acc}!")
                    }
                }
            }
        }
        result?.let { r -> Celebration(r.stars, r.message, onAgain = restart, onHome = onHome) }
    }
}

@Composable
private fun Hole(state: HoleState?, modifier: Modifier, onTap: () -> Unit) {
    val up by animateFloatAsState(if (state != null) 0f else 1.1f, tween(160), label = "up")
    var shown by remember { mutableStateOf<Animal?>(null) }
    if (state != null) shown = state.animal
    val hitScale by animateFloatAsState(if (state?.hit == true) 1.15f else 1f, label = "hit")
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomStartPercent = 14, bottomEndPercent = 14))
            .background(Brush.verticalGradient(listOf(Color(0xFFA8E07A), Color(0xFF7CC85A))))
            .pointerInput(Unit) { detectTapGestures(onPress = { onTap() }) }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawOval(Color(0xFF5B3A1E), topLeft = Offset(size.width * 0.16f, size.height * 0.62f), size = Size(size.width * 0.68f, size.height * 0.36f))
        }
        shown?.let { a ->
            Image(
                painterResource(a.res), a.name,
                Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, bottom = 6.dp, top = 4.dp)
                    .graphicsLayer {
                        translationY = up * size.height
                        scaleX = hitScale; scaleY = hitScale
                    }
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Catch the fruit                                                     */
/* ------------------------------------------------------------------ */

private val FRUITS = listOf("🍎", "🍌", "🍓", "🍊", "🍇", "🍉", "🍐", "🍒")

private class Falling(val x: Float, var y: Float, val vy: Float, val emoji: String, val bee: Boolean, var rot: Float)

@Composable
fun CatchGame(onBack: () -> Unit, onHome: () -> Unit) = Replayable { restart ->
    val app = LocalAppState.current
    val older = app.older
    val measurer = rememberTextMeasurer()
    val items = remember { mutableListOf<Falling>() }
    val parts = remember { mutableListOf<Particle>() }
    var frame by remember { mutableLongStateOf(0L) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var running by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(40) }
    var score by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<Result?>(null) }
    var bx by remember { mutableStateOf(0.5f) }
    var spawnT by remember { mutableStateOf(0f) }
    var elapsed by remember { mutableStateOf(0f) }
    var flash by remember { mutableStateOf(0f) }

    FrameLoop { dt ->
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        if (w <= 0f) return@FrameLoop
        val s = (w / 9f).coerceIn(90f, 140f)
        val basketY = h - s * 1.1f
        val basketX = bx * w
        if (running) {
            elapsed += dt
            spawnT -= dt
            if (spawnT <= 0f) {
                spawnT = max(0.38f, 0.85f - elapsed * 0.01f)
                val bee = older && Random.nextFloat() < 0.25f
                items.add(
                    Falling(
                        s / 2 + Random.nextFloat() * (w - s), -s,
                        (330f + Random.nextFloat() * 220f) * (1f + elapsed / 50f),
                        if (bee) "🐝" else FRUITS.random(), bee, Random.nextFloat() * 6f
                    )
                )
            }
        }
        val it = items.iterator()
        while (it.hasNext()) {
            val f = it.next()
            f.y += f.vy * dt
            f.rot += dt * (if (f.bee) 10f else 1.5f)
            if (f.y > h + s) { it.remove(); continue }
            if (running && f.y > basketY - s * 0.6f && f.y < basketY + s * 0.3f && abs(f.x - basketX) < s * 1.1f) {
                it.remove()
                if (f.bee) {
                    score = max(0, score - 2); Synth.wrong(); flash = 0.35f
                    parts.burst(f.x, f.y, KColors.Yellow)
                } else {
                    score++
                    Synth.note(Synth.C5 * (1 + (score % 8) * 0.12), 250)
                    parts.burst(f.x, f.y, KColors.Orange)
                }
            }
        }
        if (flash > 0f) flash = max(0f, flash - dt)
        frame++
        lastDt = dt
    }

    Countdown(running, timeLeft, { timeLeft = it }) {
        running = false
        result = finish(app, "catch", score, 10, 20, "Μάζεψες $score φρούτα!")
    }

    Box(Modifier.fillMaxSize()) {
        KiddoPage(
            "Πιάσε τα φρούτα", KColors.Orange, onBack,
            speakText = if (older) "Πιάσε τα φρούτα με το καλάθι. Πρόσεχε τις μέλισσες!" else "Πιάσε τα φρούτα με το καλάθι!"
        ) {
            Hud(timeLeft, "🧺 $score")
            Text(
                "Σύρε το δάχτυλο δεξιά–αριστερά", Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KColors.Ink.copy(alpha = 0.7f)
            )
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(12.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(0f to Color(0xFFBFE6FF), 0.7f to Color(0xFFE8F7FF), 0.7f to Color(0xFF9AD77A), 1f to Color(0xFF9AD77A)))
            ) {
                Canvas(
                    Modifier.fillMaxSize()
                        .onSizeChanged { size = it }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val e = awaitPointerEvent()
                                    e.changes.firstOrNull()?.let { ch ->
                                        if (ch.pressed) {
                                            bx = (ch.position.x / this.size.width).coerceIn(0.06f, 0.94f)
                                            ch.consume()
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    if (frame < 0) return@Canvas
                    val s = (size.width / 9f).coerceIn(90f, 140f)
                    val emojiStyle = TextStyle(fontSize = (s / density).sp)
                    items.forEach { f ->
                        val layout = measurer.measure(f.emoji, emojiStyle)
                        val dx = if (f.bee) sin(f.rot) * 24f else 0f
                        translate(f.x + dx - layout.size.width / 2f, f.y - layout.size.height / 2f) {
                            rotate(if (f.bee) 0f else sin(f.rot) * 17f, pivot = Offset(layout.size.width / 2f, layout.size.height / 2f)) {
                                drawText(layout)
                            }
                        }
                    }
                    val basket = measurer.measure("🧺", TextStyle(fontSize = (s * 1.7f / density).sp))
                    drawText(basket, topLeft = Offset(bx * size.width - basket.size.width / 2f, size.height - s * 1.1f - basket.size.height / 2f))
                    if (flash > 0f) drawRect(KColors.Red.copy(alpha = flash))
                    drawParticles(parts, lastDt)
                }
                if (!running && result == null) {
                    StartOverlay(
                        if (older) "Πιάσε τα φρούτα με το καλάθι. Πρόσεχε τις μέλισσες, σου παίρνουν πόντους!"
                        else "Σύρε το καλάθι και πιάσε όσα φρούτα μπορείς!"
                    ) { running = true }
                }
            }
        }
        result?.let { r -> Celebration(r.stars, r.message, onAgain = restart, onHome = onHome) }
    }
}

/* ------------------------------------------------------------------ */
/* Simon                                                               */
/* ------------------------------------------------------------------ */

private val PADS = listOf(
    Triple(KColors.Red, Synth.C5, "🍓"),
    Triple(KColors.Blue, Synth.E5, "🐳"),
    Triple(KColors.Yellow, Synth.G5, "🌞"),
    Triple(KColors.Green, Synth.C6, "🐸"),
)

@Composable
fun SimonGame(onBack: () -> Unit, onHome: () -> Unit) = Replayable { restart ->
    val app = LocalAppState.current
    val older = app.older
    val scope = rememberCoroutineScope()
    val seq = remember { mutableStateListOf<Int>() }
    var pos by remember { mutableIntStateOf(0) }
    var lit by remember { mutableIntStateOf(-1) }
    var accepting by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Κοίτα και άκου…") }
    var result by remember { mutableStateOf<Result?>(null) }

    suspend fun light(i: Int, ms: Long) {
        lit = i
        Synth.note(PADS[i].second, (ms + 200).toInt())
        delay(ms)
        if (lit == i) lit = -1
    }

    fun playNext() {
        seq.add(Random.nextInt(4))
        scope.launch {
            accepting = false
            status = "Κοίτα και άκου…"
            delay(600)
            val ms = max(if (older) 260 else 380, (if (older) 480 else 620) - seq.size * 15).toLong()
            for (i in seq.toList()) {
                light(i, ms); delay(120)
            }
            pos = 0
            accepting = true
            status = "Σειρά σου!"
        }
    }

    fun press(i: Int) {
        if (!accepting) return
        scope.launch { light(i, 220) }
        if (i != seq[pos]) {
            accepting = false
            Synth.wrong()
            status = "Ωχ! Λάθος κουμπί"
            val best = seq.size - 1
            scope.launch {
                delay(700)
                result = finish(app, "simon", best, if (older) 4 else 3, if (older) 7 else 5, "Θυμήθηκες $best στη σειρά!")
            }
            return
        }
        pos++
        if (pos >= seq.size) {
            accepting = false
            scope.launch {
                delay(250); Synth.correct(); delay(400)
                playNext()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        KiddoPage("Θυμήσου τη σειρά", KColors.Purple, onBack, speakText = "Κοίτα ποια κουμπιά ανάβουν και πάτα τα με την ίδια σειρά!") {
            Hud(null, "Σειρά: ${max(0, seq.size - 1)}")
            Banner(status)
            Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (r in 0 until 2) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            for (c in 0 until 2) {
                                val i = r * 2 + c
                                val on = lit == i
                                val scale by animateFloatAsState(if (on) 1.05f else 1f, tween(90), label = "pad")
                                Box(
                                    Modifier.weight(1f).fillMaxSize()
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .shadow(if (on) 14.dp else 5.dp, RoundedCornerShape(30.dp))
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(PADS[i].first.copy(alpha = if (on) 1f else 0.55f))
                                        .pointerInput(Unit) { detectTapGestures(onPress = { press(i) }) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(PADS[i].third, fontSize = 48.sp)
                                }
                            }
                        }
                    }
                }
                if (!started) {
                    Box(Modifier.matchParentSize()) {
                        StartOverlay("Κοίτα ποια κουμπιά ανάβουν και πάτα τα με την ίδια σειρά!") {
                            started = true
                            playNext()
                        }
                    }
                }
            }
        }
        result?.let { r -> Celebration(r.stars, r.message, onAgain = restart, onHome = onHome) }
    }
}
