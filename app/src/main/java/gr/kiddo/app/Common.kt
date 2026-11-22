package gr.kiddo.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

object KColors {
    val Orange = Color(0xFFFF8A00)
    val Teal = Color(0xFF1BA39C)
    val Purple = Color(0xFF8E44EC)
    val Pink = Color(0xFFFF5C8A)
    val Blue = Color(0xFF3D8BFF)
    val Green = Color(0xFF3DBE5B)
    val Yellow = Color(0xFFFFC928)
    val Red = Color(0xFFF2453D)
    val Ink = Color(0xFF2B2140)
    val Cream = Color(0xFFFFF8EC)
    val Rainbow = listOf(Red, Orange, Yellow, Green, Teal, Blue, Purple, Pink)
}

@Composable
fun KiddoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = KColors.Orange,
            secondary = KColors.Teal,
            tertiary = KColors.Purple,
            background = KColors.Cream,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = KColors.Ink,
            onSurface = KColors.Ink,
        ),
        content = content
    )
}

/** Standard page: coloured header with back button, title, star counter. */
@Composable
fun KiddoPage(
    title: String,
    color: Color,
    onBack: () -> Unit,
    speakText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val app = LocalAppState.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), KColors.Cream)))
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, color, onBack)
                Text(
                    title,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = KColors.Ink
                )
                if (speakText != null) {
                    SpeakButton(speakText, color)
                    Spacer(Modifier.width(8.dp))
                }
                StarChip(app.stars)
            }
            content()
        }
    }
}

@Composable
fun SpeakButton(text: String, color: Color, size: Dp = 48.dp) {
    RoundIconButton(
        Icons.AutoMirrored.Rounded.VolumeUp,
        color,
        { Speaker.say(text) },
        size
    )
}

@Composable
fun RoundIconButton(icon: ImageVector, color: Color, onClick: () -> Unit, size: Dp = 52.dp) {
    Box(
        Modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun StarChip(stars: Int) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Star, null, tint = KColors.Yellow, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(4.dp))
        Text("$stars", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KColors.Ink)
    }
}

@Composable
fun BigButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: Int = 22,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(text, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun BouncingMascot(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "mascot")
    val dy by t.animateFloat(
        0f, -14f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dy"
    )
    val rot by t.animateFloat(
        -4f, 4f,
        infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "rot"
    )
    Image(
        painterResource(R.drawable.mascot), contentDescription = "Σοφούλα",
        modifier = modifier.graphicsLayer { translationY = dy }.rotate(rot)
    )
}

private class Confetti(val x: Float, val y: Float, val speed: Float, val size: Float, val color: Color, val spin: Float)

@Composable
fun ConfettiRain(modifier: Modifier = Modifier) {
    val pieces = remember {
        List(90) {
            Confetti(
                Random.nextFloat(), Random.nextFloat(), 0.6f + Random.nextFloat(),
                8f + Random.nextFloat() * 14f, KColors.Rainbow.random(), Random.nextFloat() * 360f
            )
        }
    }
    val t = rememberInfiniteTransition(label = "confetti")
    val p by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3500, easing = LinearEasing)), label = "p"
    )
    Canvas(modifier) {
        pieces.forEach { c ->
            val y = ((c.y + p * c.speed) % 1f) * size.height
            val x = c.x * size.width + kotlin.math.sin((p * 6.28f + c.spin) * 2f) * 20f
            rotate(c.spin + p * 720f * c.speed, pivot = Offset(x, y)) {
                drawRect(c.color, topLeft = Offset(x, y), size = Size(c.size, c.size * 0.6f))
            }
        }
    }
}

/** Full-screen "well done" overlay with confetti, stars and the owl. */
@Composable
fun BoxScope.Celebration(
    stars: Int,
    message: String,
    onAgain: () -> Unit,
    onHome: () -> Unit,
) {
    LaunchedEffect(Unit) {
        Synth.win()
        Speaker.say(message)
    }
    Box(
        Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)).clickable(enabled = true) {},
        contentAlignment = Alignment.Center
    ) {
        ConfettiRain(Modifier.fillMaxSize())
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BouncingMascot(Modifier.size(150.dp))
                Text(message, fontSize = 30.sp, fontWeight = FontWeight.Black, color = KColors.Orange, textAlign = TextAlign.Center)
                Row {
                    repeat(3) { i ->
                        Icon(
                            Icons.Rounded.Star, null,
                            tint = if (i < stars) KColors.Yellow else Color(0xFFE0E0E0),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTextButton(Icons.Rounded.Refresh, "Ξανά", KColors.Green, onAgain)
                    IconTextButton(Icons.Rounded.Home, "Αρχική", KColors.Blue, onHome)
                }
            }
        }
    }
}

@Composable
fun IconTextButton(icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.height(56.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

fun starsFor(score: Int, total: Int): Int = when {
    score >= total -> 3
    score * 3 >= total * 2 -> 2
    else -> 1
}

data class Animal(val name: String, val res: Int, val sound: String, val article: String)

val ANIMALS = listOf(
    Animal("Γάτα", R.drawable.animal_cat, "Νιάου", "η γάτα"),
    Animal("Σκύλος", R.drawable.animal_dog, "Γαβ γαβ", "ο σκύλος"),
    Animal("Ελέφαντας", R.drawable.animal_elephant, "Πρρρου", "ο ελέφαντας"),
    Animal("Λιοντάρι", R.drawable.animal_lion, "Γρρρ", "το λιοντάρι"),
    Animal("Βάτραχος", R.drawable.animal_frog, "Κουάξ", "ο βάτραχος"),
    Animal("Γουρουνάκι", R.drawable.animal_pig, "Οινκ", "το γουρουνάκι"),
)

val NUMBER_WORDS = listOf(
    "μηδέν", "ένα", "δύο", "τρία", "τέσσερα", "πέντε", "έξι", "επτά", "οκτώ", "εννιά", "δέκα",
    "έντεκα", "δώδεκα", "δεκατρία", "δεκατέσσερα", "δεκαπέντε", "δεκαέξι", "δεκαεπτά", "δεκαοκτώ", "δεκαεννιά", "είκοσι"
)
