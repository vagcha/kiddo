package gr.kiddo.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Activity(val screen: Screen, val title: String, val icon: Int, val color: Color, val speech: String)

private val ACTIVITIES = listOf(
    Activity(Screen.Draw, "Ζωγραφική", R.drawable.ic_draw, KColors.Pink, "Ζωγραφική"),
    Activity(Screen.Music, "Μουσική", R.drawable.ic_music, KColors.Purple, "Μουσική"),
    Activity(Screen.Quiz, "Κουίζ", R.drawable.ic_quiz, KColors.Blue, "Κουίζ"),
    Activity(Screen.Puzzle, "Παζλ", R.drawable.ic_puzzle, KColors.Green, "Παζλ"),
    Activity(Screen.Memory, "Μνήμη", R.drawable.ic_memory, KColors.Teal, "Παιχνίδι μνήμης"),
    Activity(Screen.Numbers, "Μετράμε", R.drawable.ic_numbers, KColors.Orange, "Μετράμε"),
    Activity(Screen.Owl3D, "Σοφούλα 3D", R.drawable.mascot, KColors.Red, "Η Σοφούλα σε τρεις διαστάσεις"),
    Activity(Screen.Intro, "Βίντεο", R.drawable.mascot, KColors.Yellow, "Βίντεο"),
)

@Composable
fun HomeScreen(onOpen: (Screen) -> Unit) {
    val app = LocalAppState.current
    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.bg_home), null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
        )
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 14.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Kiddo", fontSize = 40.sp, fontWeight = FontWeight.Black,
                    color = KColors.Orange, modifier = Modifier.weight(1f)
                )
                RoundIconButton(
                    if (app.musicOn) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff,
                    if (app.musicOn) KColors.Purple else Color.Gray,
                    { app.updateMusic(!app.musicOn) }
                )
                Spacer(Modifier.width(8.dp))
                StarChip(app.stars)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                BouncingMascot(
                    Modifier.size(120.dp).clickable {
                        Synth.pop()
                        Speaker.say("Γεια σου! Τι θα παίξουμε σήμερα;")
                    }
                )
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        "Γεια σου! Τι θα παίξουμε σήμερα;",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KColors.Ink
                    )
                }
            }
            AgeSelector(app.older) { app.updateOlder(it) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(ACTIVITIES) { a ->
                    ActivityCard(a) {
                        Synth.pop()
                        Speaker.say(a.speech)
                        onOpen(a.screen)
                    }
                }
            }
        }
    }
}

@Composable
private fun AgeSelector(older: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.85f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Ηλικία:", Modifier.padding(horizontal = 10.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KColors.Ink)
        AgeChip("5–7", !older) { onChange(false) }
        AgeChip("8–10", older) { onChange(true) }
    }
}

@Composable
private fun AgeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) KColors.Orange else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        fontSize = 18.sp, fontWeight = FontWeight.Bold,
        color = if (selected) Color.White else KColors.Ink
    )
}

@Composable
private fun ActivityCard(a: Activity, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(10.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(a.color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(a.icon), null, Modifier.fillMaxSize().padding(8.dp))
            }
            Text(
                a.title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = a.color,
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 10.dp)
            )
        }
    }
}
