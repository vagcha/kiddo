package gr.kiddo.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** Tiny synthesizer: xylophone-like notes and feedback jingles, no audio files needed. */
object Synth {
    private const val RATE = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = HashMap<String, ShortArray>()

    const val C5 = 523.25
    const val D5 = 587.33
    const val E5 = 659.25
    const val F5 = 698.46
    const val G5 = 783.99
    const val A5 = 880.00
    const val B5 = 987.77
    const val C6 = 1046.50

    fun note(freq: Double, durationMs: Int = 900) {
        scope.launch { playNow(freq, durationMs) }
    }

    fun sequence(freqs: List<Double>, gapMs: Long, noteMs: Int = 500) {
        scope.launch {
            for (f in freqs) {
                launch { playNow(f, noteMs) }
                delay(gapMs)
            }
        }
    }

    fun correct() = sequence(listOf(C5, E5, G5, C6), 90, 400)
    fun wrong() = sequence(listOf(330.0, 262.0), 160, 350)
    fun win() = sequence(listOf(C5, E5, G5, C6, G5, C6), 130, 500)
    fun pop() = note(G5 * 1.5, 200)

    private suspend fun playNow(freq: Double, ms: Int) {
        val data = synchronized(cache) {
            cache.getOrPut("$freq-$ms") { render(freq, ms) }
        }
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(data.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } catch (e: Exception) {
            return
        }
        try {
            track.write(data, 0, data.size)
            track.play()
            delay(ms + 80L)
        } catch (_: Exception) {
        } finally {
            track.release()
        }
    }

    private fun render(freq: Double, ms: Int): ShortArray {
        val n = RATE * ms / 1000
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val attack = min(1.0, i / 60.0)
            val env = attack * exp(-t * 6.0)
            val s = sin(2 * PI * freq * t) +
                0.35 * sin(2 * PI * freq * 3.0 * t) * exp(-t * 18.0) +
                0.15 * sin(2 * PI * freq * 4.2 * t) * exp(-t * 30.0)
            out[i] = (s * env * 0.55 * Short.MAX_VALUE / 1.5).toInt().toShort()
        }
        return out
    }
}

/** Looping background track (generated with Artlist). */
object BgMusic {
    private var player: MediaPlayer? = null
    private var wanted = false
    private var foreground = true

    fun init(context: Context) {
        if (player != null) return
        player = MediaPlayer.create(context, R.raw.bg_music)?.apply {
            isLooping = true
            setVolume(0.3f, 0.3f)
        }
    }

    fun setWanted(value: Boolean) {
        wanted = value
        apply()
    }

    fun onForeground(value: Boolean) {
        foreground = value
        apply()
    }

    private fun apply() {
        val p = player ?: return
        try {
            if (wanted && foreground) {
                if (!p.isPlaying) p.start()
            } else if (p.isPlaying) {
                p.pause()
            }
        } catch (_: IllegalStateException) {
        }
    }

    fun release() {
        player?.release()
        player = null
    }
}

/** Greek text-to-speech, so children who cannot read yet can still play. */
object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val greek = Locale("el", "GR")
                val res = tts?.setLanguage(greek)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                tts?.setSpeechRate(0.9f)
                tts?.setPitch(1.15f)
                ready = true
            }
        }
    }

    fun say(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
