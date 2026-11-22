package gr.kiddo.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val COLORING_PAGES = listOf(
    R.drawable.color_owl to "Η Σοφούλα",
    R.drawable.color_cat to "Γατούλα",
    R.drawable.color_elephant to "Ελεφαντάκι",
    R.drawable.color_sea to "Θάλασσα",
    R.drawable.color_rocket to "Πύραυλος",
    R.drawable.color_house to "Σπιτάκι",
)

private val FILL_COLORS = listOf(
    0xFFF2453D, 0xFFFF8A00, 0xFFFFC928, 0xFFFFF176, 0xFF3DBE5B, 0xFFA5D6A7,
    0xFF1BA39C, 0xFF3D8BFF, 0xFF90CAF9, 0xFF8E44EC, 0xFFFF5C8A, 0xFFF8BBD0,
    0xFF8D5524, 0xFFFFCC99, 0xFF9E9E9E, 0xFFFFFFFF,
).map { Color(it) }

private const val LINE = 0xFF000000.toInt()

/** Scanline flood fill. Black outline pixels are walls. Returns false if nothing changed. */
fun floodFill(px: IntArray, w: Int, h: Int, sx: Int, sy: Int, newColor: Int): Boolean {
    // If the child tapped right on a line, look for the nearest fillable pixel.
    var x0 = sx
    var y0 = sy
    if (px[y0 * w + x0] == LINE) {
        var found = false
        search@ for (r in 1..8) {
            for (dy in -r..r) for (dx in -r..r) {
                val x = sx + dx
                val y = sy + dy
                if (x in 0 until w && y in 0 until h && px[y * w + x] != LINE) {
                    x0 = x; y0 = y; found = true; break@search
                }
            }
        }
        if (!found) return false
    }
    val target = px[y0 * w + x0]
    if (target == newColor) return false
    val stack = ArrayDeque<Int>()
    stack.addLast(y0 * w + x0)
    while (stack.isNotEmpty()) {
        val idx = stack.removeLast()
        if (px[idx] != target) continue
        val y = idx / w
        var x = idx % w
        while (x > 0 && px[y * w + x - 1] == target) x--
        var spanUp = false
        var spanDown = false
        while (x < w && px[y * w + x] == target) {
            px[y * w + x] = newColor
            if (y > 0) {
                val up = (y - 1) * w + x
                if (px[up] == target) {
                    if (!spanUp) { stack.addLast(up); spanUp = true }
                } else spanUp = false
            }
            if (y < h - 1) {
                val dn = (y + 1) * w + x
                if (px[dn] == target) {
                    if (!spanDown) { stack.addLast(dn); spanDown = true }
                } else spanDown = false
            }
            x++
        }
    }
    return true
}

@Composable
fun ColoringScreen(onBack: () -> Unit) {
    var page by remember { mutableStateOf<Int?>(null) }
    val p = page
    if (p == null) {
        LaunchedEffect(Unit) { Speaker.say("Διάλεξε μια σελίδα για χρωματισμό!") }
        KiddoPage("Χρωματισμός", KColors.Yellow, onBack, speakText = "Διάλεξε μια σελίδα για χρωματισμό!") {
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                COLORING_PAGES.indices.chunked(2).forEach { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { i ->
                            Surface(
                                shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 5.dp,
                                modifier = Modifier.weight(1f).fillMaxSize().clickable {
                                    Synth.pop(); Speaker.say(COLORING_PAGES[i].second); page = i
                                }
                            ) {
                                Image(
                                    painterResource(COLORING_PAGES[i].first), COLORING_PAGES[i].second,
                                    Modifier.fillMaxSize().padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        ColoringBoard(COLORING_PAGES[p].first, onBack = { page = null }, onHome = onBack)
    }
}

@Composable
private fun ColoringBoard(res: Int, onBack: () -> Unit, onHome: () -> Unit) {
    val app = LocalAppState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val base = remember(res) {
        BitmapFactory.decodeResource(
            context.resources, res,
            BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }
    val w = base.width
    val h = base.height
    val original = remember(res) {
        IntArray(w * h).also { base.getPixels(it, 0, w, 0, 0, w, h) }.also { arr ->
            // Normalise to pure black lines / white areas.
            for (i in arr.indices) {
                val c = arr[i]
                val lum = ((c shr 16 and 0xFF) + (c shr 8 and 0xFF) + (c and 0xFF)) / 3
                arr[i] = if (lum < 128) LINE else 0xFFFFFFFF.toInt()
            }
        }
    }
    val pixels = remember(res) { original.copyOf() }
    val bitmap = remember(res) { Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { setPixels(pixels, 0, w, 0, 0, w, h) } }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val history = remember(res) { ArrayDeque<IntArray>() }
    var version by remember { mutableIntStateOf(0) }
    var color by remember { mutableStateOf(FILL_COLORS[2]) }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    fun refresh() {
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        version++
    }

    LaunchedEffect(res) { Speaker.say("Διάλεξε χρώμα και πάτα μέσα στη ζωγραφιά!") }

    Box(Modifier.fillMaxSize()) {
        KiddoPage("Χρωματισμός", KColors.Yellow, onBack, speakText = "Διάλεξε χρώμα και πάτα μέσα στη ζωγραφιά!") {
            Box(
                Modifier.fillMaxWidth().padding(12.dp).aspectRatio(1f)
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                Canvas(
                    Modifier.fillMaxSize().pointerInput(res) {
                        detectTapGestures { pos ->
                            if (busy) return@detectTapGestures
                            val x = (pos.x / size.width * w).toInt().coerceIn(0, w - 1)
                            val y = (pos.y / size.height * h).toInt().coerceIn(0, h - 1)
                            busy = true
                            scope.launch {
                                val snapshot = pixels.copyOf()
                                val changed = withContext(Dispatchers.Default) {
                                    floodFill(pixels, w, h, x, y, color.toArgb())
                                }
                                if (changed) {
                                    history.addLast(snapshot)
                                    if (history.size > 15) history.removeFirst()
                                    refresh()
                                    Synth.pop()
                                }
                                busy = false
                            }
                        }
                    }
                ) {
                    if (version < 0) return@Canvas // reading version redraws when pixels change
                    drawImage(
                        image,
                        srcOffset = IntOffset.Zero, srcSize = IntSize(w, h),
                        dstOffset = IntOffset.Zero, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        filterQuality = FilterQuality.Medium
                    )
                }
            }
            // Palette 2 rows x 8
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FILL_COLORS.chunked(8).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { c ->
                            val sel = c == color
                            Box(
                                Modifier.size(if (sel) 42.dp else 36.dp)
                                    .shadow(if (sel) 6.dp else 2.dp, CircleShape)
                                    .clip(CircleShape).background(c)
                                    .border(if (sel) 4.dp else 1.dp, if (sel) KColors.Ink else Color.LightGray, CircleShape)
                                    .clickable { color = c; Synth.pop() }
                            )
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(Icons.AutoMirrored.Rounded.Undo, KColors.Orange, {
                    history.removeLastOrNull()?.let { prev ->
                        prev.copyInto(pixels)
                        refresh()
                    }
                }, 60.dp)
                RoundIconButton(Icons.Rounded.Refresh, KColors.Red, {
                    history.addLast(pixels.copyOf())
                    original.copyInto(pixels)
                    refresh()
                }, 60.dp)
                RoundIconButton(Icons.Rounded.Check, KColors.Green, {
                    app.addStars(2)
                    done = true
                }, 72.dp)
            }
        }
        if (done) {
            Celebration(3, "Τι ωραία ζωγραφιά!", onAgain = { done = false; original.copyInto(pixels); history.clear(); refresh() }, onHome = onHome)
        }
    }
}
