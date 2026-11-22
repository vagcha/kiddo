package gr.kiddo.app

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/** Plays the Artlist owl animation, then goes to the home screen. */
@Composable
fun IntroScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(600)
        Speaker.say("Γεια σου! Είμαι η Σοφούλα. Πάμε να παίξουμε και να μάθουμε μαζί!")
        // Safety net in case the video cannot play on this device.
        delay(9000)
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1035), Color(0xFF3A1F5D)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            Text(
                "Καλώς ήρθες στο Kiddo!",
                fontSize = 34.sp, fontWeight = FontWeight.Black,
                color = KColors.Yellow, textAlign = TextAlign.Center
            )
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse("android.resource://${ctx.packageName}/${R.raw.intro}"))
                        setOnPreparedListener { mp ->
                            mp.setVolume(0f, 0f)
                            start()
                        }
                        setOnCompletionListener { onDone() }
                        setOnErrorListener { _, _, _ -> onDone(); true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(36.dp))
            )
            BigButton("Πάμε! ▶", KColors.Orange, Modifier.fillMaxWidth(0.7f), fontSize = 26) { onDone() }
        }
    }
}
