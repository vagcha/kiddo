package gr.kiddo.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val NOTES = listOf(
    Synth.C5 to "Ντο", Synth.D5 to "Ρε", Synth.E5 to "Μι", Synth.F5 to "Φα",
    Synth.G5 to "Σολ", Synth.A5 to "Λα", Synth.B5 to "Σι", Synth.C6 to "Ντο"
)

private data class Song(val title: String, val notes: List<Int>, val beats: List<Int>)

// Traditional melodies (public domain). Note indexes into NOTES; beats in half-beats.
private val SONGS = listOf(
    Song(
        "Λαμπύρισε αστεράκι",
        listOf(0, 0, 4, 4, 5, 5, 4, 3, 3, 2, 2, 1, 1, 0),
        listOf(1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2)
    ),
    Song(
        "Αδερφέ Ιάκωβε",
        listOf(0, 1, 2, 0, 0, 1, 2, 0, 2, 3, 4, 2, 3, 4),
        listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2)
    ),
    Song(
        "Η σκάλα",
        listOf(0, 1, 2, 3, 4, 5, 6, 7, 7, 6, 5, 4, 3, 2, 1, 0),
        List(16) { 1 }
    ),
)

@Composable
fun MusicScreen(onBack: () -> Unit) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()
    var lit by remember { mutableIntStateOf(-1) }
    var songIdx by remember { mutableIntStateOf(0) }
    var learnPos by remember { mutableIntStateOf(-1) } // -1 = free play
    var demoJob by remember { mutableStateOf<Job?>(null) }
    var done by remember { mutableStateOf(false) }
    val song = SONGS[songIdx]

    fun flash(i: Int) {
        lit = i
        scope.launch {
            delay(220)
            if (lit == i) lit = -1
        }
    }

    fun tap(i: Int) {
        Synth.note(NOTES[i].first)
        flash(i)
        if (learnPos >= 0) {
            if (i == song.notes[learnPos]) {
                learnPos++
                if (learnPos >= song.notes.size) {
                    learnPos = -1
                    app.addStars(3)
                    done = true
                }
            }
        }
    }

    fun playDemo() {
        demoJob?.cancel()
        learnPos = -1
        demoJob = scope.launch {
            for (k in song.notes.indices) {
                val n = song.notes[k]
                Synth.note(NOTES[n].first)
                lit = n
                delay(song.beats[k] * 330L)
                lit = -1
                delay(40)
            }
        }
    }

    LaunchedEffect(Unit) { Speaker.say("Πάτα τα πλήκτρα του ξυλόφωνου! Ή διάλεξε τραγούδι και μάθε να το παίζεις.") }

    Box(Modifier.fillMaxSize()) {
        KiddoPage("Μουσική", KColors.Purple, onBack, speakText = "Πάτα τα πλήκτρα! Με το Μάθε το, ακολούθησε το πλήκτρο που αναβοσβήνει.") {
            // Song picker
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SONGS.forEachIndexed { i, s ->
                    Text(
                        s.title,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (i == songIdx) KColors.Purple else Color.White)
                            .pointerInput(i) {
                                detectTapGestures {
                                    songIdx = i
                                    learnPos = -1
                                    demoJob?.cancel()
                                    Speaker.say(s.title)
                                }
                            }
                            .padding(10.dp),
                        color = if (i == songIdx) Color.White else KColors.Ink,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BigButton("▶ Άκου", KColors.Blue, Modifier.weight(1f)) { playDemo() }
                BigButton(if (learnPos >= 0) "Πάμε!" else "🎓 Μάθε το", KColors.Green, Modifier.weight(1f)) {
                    demoJob?.cancel()
                    learnPos = 0
                    Speaker.say("Πάτα το πλήκτρο που αναβοσβήνει!")
                }
            }
            if (learnPos >= 0) {
                Text(
                    "Νότα ${learnPos + 1} από ${song.notes.size}",
                    Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KColors.Purple
                )
            }
            // Xylophone: horizontal bars, longest on top
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NOTES.forEachIndexed { i, (_, name) ->
                    val hint = learnPos >= 0 && song.notes[learnPos] == i
                    XyloBar(
                        name = name,
                        color = KColors.Rainbow[i],
                        widthFraction = 1f - i * 0.06f,
                        lit = lit == i,
                        hint = hint,
                        modifier = Modifier.weight(1f)
                    ) { tap(i) }
                }
            }
        }
        if (done) {
            Celebration(3, "Μπράβο! Έπαιξες το τραγούδι!", onAgain = { done = false }, onHome = onBack)
        }
    }
}

@Composable
private fun XyloBar(
    name: String, color: Color, widthFraction: Float, lit: Boolean, hint: Boolean,
    modifier: Modifier, onTap: () -> Unit,
) {
    val scale by animateFloatAsState(if (lit) 0.94f else 1f, tween(90), label = "s")
    val currentOnTap by rememberUpdatedState(onTap)
    val pulse = remember { mutableStateOf(false) }
    LaunchedEffect(hint) {
        while (hint) {
            pulse.value = !pulse.value
            delay(350)
        }
        pulse.value = false
    }
    Box(
        modifier
            .fillMaxWidth(widthFraction)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (lit) 1.dp else 5.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(if (lit || (hint && pulse.value)) Color.White else color)
            .pointerInput(Unit) { detectTapGestures(onPress = { currentOnTap() }) },
        contentAlignment = Alignment.Center
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0x55000000)))
            Text(
                name, Modifier.weight(1f), textAlign = TextAlign.Center,
                fontSize = 22.sp, fontWeight = FontWeight.Black,
                color = if (lit || (hint && pulse.value)) color else Color.White
            )
            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0x55000000)))
        }
    }
}
