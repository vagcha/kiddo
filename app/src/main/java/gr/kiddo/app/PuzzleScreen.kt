package gr.kiddo.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PUZZLES = listOf(
    R.drawable.puzzle_farm to "Φάρμα",
    R.drawable.puzzle_ocean to "Βυθός",
    R.drawable.puzzle_space to "Διάστημα",
)

@Composable
fun PuzzleScreen(onBack: () -> Unit) {
    var chosen by remember { mutableStateOf<Int?>(null) }
    val c = chosen
    if (c == null) {
        LaunchedEffect(Unit) { Speaker.say("Διάλεξε μια εικόνα για παζλ!") }
        KiddoPage("Παζλ", KColors.Green, onBack, speakText = "Διάλεξε μια εικόνα για παζλ!") {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PUZZLES.forEachIndexed { i, (res, name) ->
                    Surface(
                        shape = RoundedCornerShape(26.dp), shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth().weight(1f).clickable {
                            Synth.pop(); Speaker.say(name); chosen = i
                        }
                    ) {
                        Box {
                            Image(painterResource(res), name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Text(
                                name,
                                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                                    .clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.9f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                fontSize = 24.sp, fontWeight = FontWeight.Black, color = KColors.Green
                            )
                        }
                    }
                }
            }
        }
    } else {
        PuzzleBoard(PUZZLES[c].first, onBack = { chosen = null }, onHome = onBack)
    }
}

private fun shuffledTiles(n: Int): List<Int> {
    var list: List<Int>
    do {
        list = (0 until n * n).shuffled()
    } while (list.withIndex().count { it.index == it.value } > n) // make sure it is really mixed
    return list
}

@Composable
private fun PuzzleBoard(res: Int, onBack: () -> Unit, onHome: () -> Unit) {
    val app = LocalAppState.current
    val n = if (app.older) 4 else 3
    val image: ImageBitmap = ImageBitmap.imageResource(res)
    var seed by remember { mutableIntStateOf(0) }
    val tiles = remember(seed, n) { mutableStateListOf<Int>().apply { addAll(shuffledTiles(n)) } }
    var selected by remember(seed) { mutableStateOf<Int?>(null) }
    var moves by remember(seed) { mutableIntStateOf(0) }
    var peek by remember { mutableStateOf(false) }
    val solved = tiles.withIndex().all { it.index == it.value }
    var celebrated by remember(seed) { mutableStateOf(false) }

    LaunchedEffect(solved) {
        if (solved && !celebrated) {
            celebrated = true
            app.addStars(3)
        }
    }
    LaunchedEffect(seed) { Speaker.say("Πάτα δύο κομμάτια για να αλλάξουν θέση!") }

    Box(Modifier.fillMaxSize()) {
        KiddoPage("Παζλ", KColors.Green, onBack, speakText = "Πάτα δύο κομμάτια για να αλλάξουν θέση!") {
            Text(
                "Κινήσεις: $moves", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KColors.Ink
            )
            Box(
                Modifier.fillMaxWidth().padding(16.dp).aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp)).background(Color.White).padding(4.dp)
            ) {
                if (peek) {
                    Image(painterResource(res), null, Modifier.fillMaxSize())
                } else {
                    Column(Modifier.fillMaxSize()) {
                        for (r in 0 until n) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                for (col in 0 until n) {
                                    val pos = r * n + col
                                    val tile = tiles[pos]
                                    val isSel = selected == pos
                                    val scale by animateFloatAsState(if (isSel) 0.9f else 1f, label = "t")
                                    Canvas(
                                        Modifier.weight(1f).fillMaxSize().padding(2.dp)
                                            .graphicsLayer { scaleX = scale; scaleY = scale }
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                if (isSel) 4.dp else 0.dp,
                                                KColors.Yellow, RoundedCornerShape(8.dp)
                                            )
                                            .pointerInput(pos, seed) {
                                                detectTapGestures {
                                                    if (solved) return@detectTapGestures
                                                    val s = selected
                                                    if (s == null) {
                                                        selected = pos
                                                        Synth.pop()
                                                    } else {
                                                        if (s != pos) {
                                                            val tmp = tiles[s]
                                                            tiles[s] = tiles[pos]
                                                            tiles[pos] = tmp
                                                            moves++
                                                            if (tiles[pos] == pos || tiles[s] == s) Synth.correct() else Synth.pop()
                                                        }
                                                        selected = null
                                                    }
                                                }
                                            }
                                    ) {
                                        val tw = image.width / n
                                        val th = image.height / n
                                        val sx = (tile % n) * tw
                                        val sy = (tile / n) * th
                                        drawImage(
                                            image,
                                            srcOffset = IntOffset(sx, sy),
                                            srcSize = IntSize(tw, th),
                                            dstOffset = IntOffset.Zero,
                                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.weight(1f).height(64.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (peek) KColors.Teal else KColors.Blue)
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                peek = true
                                tryAwaitRelease()
                                peek = false
                            })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("👀 Κράτα να δεις", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                BigButton("🔀 Ανακάτεμα", KColors.Orange, Modifier.weight(1f), fontSize = 18) { seed++ }
            }
            Text(
                if (app.older) "Επίπεδο: 4 × 4" else "Επίπεδο: 3 × 3",
                Modifier.fillMaxWidth().padding(12.dp), textAlign = TextAlign.Center,
                fontSize = 16.sp, color = KColors.Ink
            )
        }
        if (solved && celebrated) {
            Celebration(3, "Μπράβο! Έλυσες το παζλ με $moves κινήσεις!", onAgain = { seed++ }, onHome = onHome)
        }
    }
}
