package gr.kiddo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Speaker.init(applicationContext)
        BgMusic.init(applicationContext)
        val appState = AppState(applicationContext)
        setContent { KiddoApp(appState) }
    }

    override fun onStart() {
        super.onStart()
        BgMusic.onForeground(true)
    }

    override fun onStop() {
        super.onStop()
        BgMusic.onForeground(false)
        Speaker.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            BgMusic.release()
            Speaker.shutdown()
        }
    }
}
