package gr.kiddo.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val ROUNDS = 6

private data class CountRound(val groups: List<Pair<Animal, Int>>, val total: Int, val options: List<Int>)

private fun makeRound(older: Boolean): CountRound {
    val groups = if (!older) {
        listOf(ANIMALS.random() to Random.nextInt(1, 11))
    } else {
        val two = ANIMALS.shuffled().take(2)
        listOf(two[0] to Random.nextInt(3, 11), two[1] to Random.nextInt(3, 11))
    }
    val total = groups.sumOf { it.second }
    val wrong = listOf(total - 2, total - 1, total + 1, total + 2).filter { it > 0 }.shuffled().take(2)
    return CountRound(groups, total, (wrong + total).shuffled())
}

@Composable
fun NumbersScreen(onBack: () -> Unit) {
    val app = LocalAppState.current
    var seed by remember { mutableIntStateOf(0) }
    var roundNo by remember(seed) { mutableIntStateOf(0) }
    var score by remember(seed) { mutableIntStateOf(0) }
    val round = remember(seed, roundNo, app.older) { makeRound(app.older) }
    val tapped = remember(seed, roundNo) { mutableStateListOf<String>() }
    var picked by remember(seed, roundNo) { mutableStateOf<Int?>(null) }
    var finished by remember(seed) { mutableStateOf(false) }

    val question = if (round.groups.size == 1) "Πόσα ζωάκια βλέπεις;" else "Πόσα είναι όλα μαζί;"
    LaunchedEffect(seed, roundNo) { Speaker.say("$question Πάτα τα για να τα μετρήσεις!") }

    LaunchedEffect(picked) {
        val p = picked ?: return@LaunchedEffect
        if (p == round.total) {
            score++
            Synth.correct()
            Speaker.say("Μπράβο! Είναι ${NUMBER_WORDS[round.total]}!")
        } else {
            Synth.wrong()
            Speaker.say("Όχι ακριβώς. Είναι ${NUMBER_WORDS[round.total]}.")
        }
        delay(2000)
        if (roundNo + 1 >= ROUNDS) {
            finished = true
            app.addStars(starsFor(score, ROUNDS))
        } else roundNo++
    }

    Box(Modifier.fillMaxSize()) {
        KiddoPage("Μετράμε", KColors.Orange, onBack, speakText = question) {
            Text(
                "Γύρος ${roundNo + 1} από $ROUNDS",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp, color = KColors.Ink
            )
            Text(
                question, Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center,
                fontSize = 28.sp, fontWeight = FontWeight.Black, color = KColors.Ink
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                round.groups.forEachIndexed { g, (animal, count) ->
                    val perRow = 5
                    Column(
                        Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.8f)).padding(6.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        (0 until count).chunked(perRow).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                row.forEach { k ->
                                    val key = "$g-$k"
                                    val isTapped = key in tapped
                                    val s by animateFloatAsState(if (isTapped) 1.15f else 1f, label = "s")
                                    Box(
                                        Modifier.weight(1f, fill = false).size(if (app.older) 50.dp else 62.dp).padding(2.dp)
                                            .graphicsLayer { scaleX = s; scaleY = s }
                                            .clickable {
                                                if (!isTapped && picked == null) {
                                                    tapped.add(key)
                                                    Synth.note(Synth.C5 * (1 + tapped.size * 0.06), 300)
                                                    Speaker.say(NUMBER_WORDS.getOrElse(tapped.size) { "${tapped.size}" })
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(painterResource(animal.res), animal.name, Modifier.fillMaxSize())
                                        if (isTapped) {
                                            Box(
                                                Modifier.align(Alignment.TopEnd).size(24.dp).clip(CircleShape)
                                                    .background(KColors.Orange),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "${tapped.indexOf(key) + 1}", color = Color.White,
                                                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                round.options.forEach { o ->
                    val color = when {
                        picked == null -> KColors.Orange
                        o == round.total -> KColors.Green
                        o == picked -> KColors.Red
                        else -> Color.LightGray
                    }
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(26.dp))
                            .background(color)
                            .clickable { if (picked == null) picked = o },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$o", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
        if (finished) {
            Celebration(
                starsFor(score, ROUNDS), "Βρήκες $score από $ROUNDS!",
                onAgain = { seed++ }, onHome = onBack
            )
        }
    }
}
