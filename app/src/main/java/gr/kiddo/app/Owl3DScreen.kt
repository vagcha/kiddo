package gr.kiddo.app

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

/** The Artlist 3D owl, shown with <model-viewer> inside a local WebView. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Owl3DScreen(onBack: () -> Unit) {
    val hint = "Γύρνα τη Σοφούλα με το δάχτυλο! Με δύο δάχτυλα κάνεις ζουμ."
    LaunchedEffect(Unit) { Speaker.say("Γεια! Είμαι η Σοφούλα σε τρεις διαστάσεις! $hint") }
    KiddoPage("Σοφούλα 3D", KColors.Red, onBack, speakText = hint) {
        Text(
            hint, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KColors.Ink,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        AndroidView(
            factory = { ctx ->
                val loader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                    .build()
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView, request: WebResourceRequest
                        ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)
                    }
                    loadUrl("https://appassets.androidplatform.net/assets/owl.html")
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(32.dp))
        )
    }
}
