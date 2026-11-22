package gr.kiddo.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun MemoryScreen(onBack: () -> Unit) {
    val app = LocalAppState.current
    val pairs = if (app.older) 6 else 4
    val cols = if (app.older) 3 else 2
    var seed by remember { mutableIntStateOf(0) }
    val cards = remember(seed, pairs) {
        ANIMALS.shuffled().take(pairs).flatMap { listOf(it, it) }.shuffled()
    }
    val open = remember(seed, pairs) { mutableStateListOf<Int>() }
    val matched = remember(seed, pairs) { mutableStateListOf<Int>() }
    var moves by remember(seed, pairs) { mutableIntStateOf(0) }
    var busy by remember(seed, pairs) { mutableStateOf(false) }
    val won = matched.size == cards.size
    var rewarded by remember(seed, pairs) { mutableStateOf(false) }

    LaunchedEffect(seed) { Speaker.say("Βρες τα ζευγάρια! Γύρνα δύο κάρτες.") }

    LaunchedEffect(open.size) {
        if (open.size == 2) {
            busy = true
            moves++
            val (a, b) = open[0] to open[1]
            if (cards[a] == cards[b]) {
                delay(350)
                Synth.correct()
                Speaker.say(cards[a].name)
                matched.addAll(listOf(a, b))
                open.clear()
            } else {
                delay(1000)
                open.clear()
            }
            busy = false
        }
    }
    LaunchedEffect(won) {
        if (won && !rewarded) {
            rewarded = true
            app.addStars(if (moves <= pairs + 2) 3 else if (moves <= pairs * 2) 2 else 1)
        }
    }

    Box(Modifier.fillMaxSize()) {
        KiddoPage("Μνήμη", KColors.Teal, onBack, speakText = "Βρες τα ζευγάρια! Γύρνα δύο κάρτες.") {
            Text(
                "Κινήσεις: $moves   •   Ζευγάρια: ${matched.size / 2} / $pairs",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KColors.Ink
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val rows = cards.indices.chunked(cols)
                rows.forEach { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { i ->
                            val faceUp = i in open || i in matched
                            MemoryCard(
                                animal = cards[i],
                                faceUp = faceUp,
                                matched = i in matched,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            ) {
                                if (!busy && !faceUp && open.size < 2) {
                                    Synth.pop()
                                    open.add(i)
                                }
                            }
                        }
                    }
                }
            }
            BigButton("🔀 Νέο παιχνίδι", KColors.Orange, Modifier.fillMaxWidth().padding(16.dp)) { seed++ }
        }
        if (won && rewarded) {
            Celebration(
                if (moves <= pairs + 2) 3 else if (moves <= pairs * 2) 2 else 1,
                "Τα βρήκες όλα με $moves κινήσεις!",
                onAgain = { seed++ }, onHome = onBack
            )
        }
    }
}

@Composable
private fun MemoryCard(animal: Animal, faceUp: Boolean, matched: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (faceUp) 180f else 0f, tween(350), label = "flip")
    Box(
        modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .shadow(5.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(KColors.Teal, KColors.Blue))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("⭐", fontSize = 40.sp)
            }
        } else {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .background(if (matched) Color(0xFFDFF7E3) else Color.White)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(animal.res), animal.name, Modifier.fillMaxSize())
            }
        }
    }
}
