package gr.kiddo.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

private sealed interface Mark
private data class Line(val points: List<Offset>, val color: Color, val width: Float) : Mark
private data class Sticker(val animal: Int, val at: Offset, val size: Float) : Mark

private val PALETTE = listOf(
    Color(0xFFF2453D), Color(0xFFFF8A00), Color(0xFFFFC928), Color(0xFF3DBE5B),
    Color(0xFF1BA39C), Color(0xFF3D8BFF), Color(0xFF8E44EC), Color(0xFFFF5C8A),
    Color(0xFF8D5524), Color(0xFF2B2140), Color(0xFF9E9E9E)
)

private enum class Tool { Brush, Eraser, Sticker }

@Composable
fun DrawScreen(onBack: () -> Unit) {
    val marks = remember { mutableStateListOf<Mark>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var color by remember { mutableStateOf(PALETTE[5]) }
    var width by remember { mutableFloatStateOf(22f) }
    var tool by remember { mutableStateOf(Tool.Brush) }
    var sticker by remember { mutableStateOf(0) }

    val stickerImages: List<ImageBitmap> = ANIMALS.map { ImageBitmap.imageResource(it.res) }

    val curColor by rememberUpdatedState(if (tool == Tool.Eraser) Color.White else color)
    val curWidth by rememberUpdatedState(if (tool == Tool.Eraser) width * 2f else width)
    val curTool by rememberUpdatedState(tool)
    val curSticker by rememberUpdatedState(sticker)

    KiddoPage("Ζωγραφική", KColors.Pink, onBack, speakText = "Ζωγράφισε με το δάχτυλο! Διάλεξε χρώμα ή βάλε ζωάκια.") {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .shadow(6.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
        ) {
            Canvas(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            if (curTool == Tool.Sticker) {
                                marks.add(Sticker(curSticker, pos, 220f))
                                Synth.pop()
                            } else {
                                marks.add(Line(listOf(pos, pos + Offset(0.1f, 0.1f)), curColor, curWidth))
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { if (curTool != Tool.Sticker) current = listOf(it) },
                            onDrag = { change, _ ->
                                if (curTool != Tool.Sticker) current = current + change.position
                            },
                            onDragEnd = {
                                if (current.size > 1) marks.add(Line(current, curColor, curWidth))
                                current = emptyList()
                            },
                            onDragCancel = { current = emptyList() }
                        )
                    }
            ) {
                clipRect {
                    fun drawStroke(points: List<Offset>, c: Color, w: Float) {
                        if (points.isEmpty()) return
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val p0 = points[i - 1]
                                val p1 = points[i]
                                quadraticTo(p0.x, p0.y, (p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                            }
                            lineTo(points.last().x, points.last().y)
                        }
                        drawPath(path, c, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    marks.forEach { m ->
                        when (m) {
                            is Line -> drawStroke(m.points, m.color, m.width)
                            is Sticker -> {
                                val img = stickerImages[m.animal]
                                val s = m.size.toInt()
                                drawImage(
                                    img,
                                    srcOffset = IntOffset.Zero,
                                    srcSize = IntSize(img.width, img.height),
                                    dstOffset = IntOffset((m.at.x - s / 2).toInt(), (m.at.y - s / 2).toInt()),
                                    dstSize = IntSize(s, s)
                                )
                            }
                        }
                    }
                    drawStroke(current, curColor, curWidth)
                }
            }
        }

        // Tools row
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(Icons.Rounded.Brush, tool == Tool.Brush, KColors.Blue) { tool = Tool.Brush }
            ToolButton(Icons.Rounded.CleaningServices, tool == Tool.Eraser, KColors.Teal) { tool = Tool.Eraser }
            listOf(10f, 22f, 40f).forEach { w ->
                Box(
                    Modifier.size(52.dp).clip(CircleShape)
                        .background(if (width == w) KColors.Yellow else Color.White)
                        .clickable { width = w },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size((w / 1.6f).dp).clip(CircleShape).background(KColors.Ink))
                }
            }
            RoundIconButton(Icons.AutoMirrored.Rounded.Undo, KColors.Orange, { if (marks.isNotEmpty()) marks.removeAt(marks.lastIndex) })
            RoundIconButton(Icons.Rounded.Delete, KColors.Red, { marks.clear(); Synth.pop() })
        }

        // Colours
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PALETTE.forEach { c ->
                val selected = tool == Tool.Brush && color == c
                Box(
                    Modifier.size(if (selected) 52.dp else 44.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (selected) 4.dp else 0.dp, Color.White, CircleShape)
                        .clickable { color = c; tool = Tool.Brush }
                )
            }
        }

        // Stickers
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ANIMALS.forEachIndexed { i, a ->
                val selected = tool == Tool.Sticker && sticker == i
                Image(
                    painterResource(a.res), a.name,
                    Modifier.size(58.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (selected) KColors.Yellow else Color.White)
                        .clickable { sticker = i; tool = Tool.Sticker; Speaker.say(a.name) }
                        .padding(4.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, color: Color, onClick: () -> Unit) {
    RoundIconButton(icon, if (selected) color else Color.LightGray, onClick)
}
