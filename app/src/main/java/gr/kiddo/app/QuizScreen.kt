package gr.kiddo.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Question(
    val prompt: String,
    val options: List<String>,
    val answer: Int,
    val image: Int? = null,
    val swatch: Color? = null,
    val bigText: String? = null,
    val speech: String = prompt,
)

private enum class Category(val title: String, val emoji: String, val color: Color) {
    Math("Μαθηματικά", "➕", KColors.Blue),
    Animals("Ζώα", "🐾", KColors.Green),
    Colors("Χρώματα", "🎨", KColors.Pink),
    Letters("Γράμματα", "🔤", KColors.Orange),
}

private const val QUESTIONS_PER_ROUND = 8

/** Builds a question from the right answer plus distinct wrong ones, shuffled. */
private fun choice(
    prompt: String, correct: String, pool: List<String>, count: Int = 3,
    image: Int? = null, swatch: Color? = null, bigText: String? = null, speech: String = prompt,
): Question {
    val wrong = pool.filter { it != correct }.distinct().shuffled().take(count - 1)
    val opts = (wrong + correct).shuffled()
    return Question(prompt, opts, opts.indexOf(correct), image, swatch, bigText, speech)
}

private fun mathQuestion(older: Boolean): Question {
    if (!older) {
        val a = Random.nextInt(0, 6)
        val b = Random.nextInt(0, 6)
        val r = a + b
        val pool = (0..10).map { it.toString() }
        return choice("Πόσο κάνει;", r.toString(), pool, bigText = "$a + $b = ?",
            speech = "Πόσο κάνει ${NUMBER_WORDS[a]} συν ${NUMBER_WORDS[b]};")
    }
    return when (Random.nextInt(3)) {
        0 -> {
            val a = Random.nextInt(10, 60); val b = Random.nextInt(5, 40)
            val r = a + b
            choice("Πόσο κάνει;", "$r", listOf(r - 10, r - 1, r + 1, r + 10, r + 2).map { "$it" },
                bigText = "$a + $b = ?", speech = "Πόσο κάνει $a συν $b;")
        }
        1 -> {
            val a = Random.nextInt(20, 99); val b = Random.nextInt(5, a)
            val r = a - b
            choice("Πόσο κάνει;", "$r", listOf(r - 10, r - 1, r + 1, r + 10, r + 2).filter { it >= 0 }.map { "$it" },
                bigText = "$a − $b = ?", speech = "Πόσο κάνει $a μείον $b;")
        }
        else -> {
            val a = Random.nextInt(2, 11); val b = Random.nextInt(2, 11)
            val r = a * b
            choice("Πόσο κάνει;", "$r", listOf(r - a, r + a, r - b, r + b, r + 1).map { "$it" },
                bigText = "$a × $b = ?", speech = "Πόσο κάνει $a επί $b;")
        }
    }
}

private fun animalQuestion(older: Boolean): Question {
    val a = ANIMALS.random()
    val names = ANIMALS.map { it.name }
    return if (!older || Random.nextBoolean()) {
        choice("Ποιο ζωάκι είναι αυτό;", a.name, names, image = a.res)
    } else {
        choice("Ποιο ζωάκι λέει «${a.sound}»;", a.name, names, count = 4,
            bigText = "«${a.sound}!»", speech = "Ποιο ζωάκι λέει ${a.sound};")
    }
}

private val COLORS = listOf(
    "Κόκκινο" to Color(0xFFE53935), "Μπλε" to Color(0xFF1E5BFF), "Πράσινο" to Color(0xFF2EAD4B),
    "Κίτρινο" to Color(0xFFFFD600), "Πορτοκαλί" to Color(0xFFFF8A00), "Μωβ" to Color(0xFF8E44EC),
    "Ροζ" to Color(0xFFFF7EB3), "Καφέ" to Color(0xFF8D5524), "Μαύρο" to Color(0xFF111111),
    "Άσπρο" to Color(0xFFFFFFFF), "Γκρι" to Color(0xFF9E9E9E),
)

private val MIXES = listOf(
    Triple("Κόκκινο", "Κίτρινο", "Πορτοκαλί"),
    Triple("Μπλε", "Κίτρινο", "Πράσινο"),
    Triple("Κόκκινο", "Μπλε", "Μωβ"),
    Triple("Κόκκινο", "Άσπρο", "Ροζ"),
    Triple("Μαύρο", "Άσπρο", "Γκρι"),
)

private fun colorQuestion(older: Boolean): Question {
    val names = COLORS.map { it.first }
    if (older && Random.nextBoolean()) {
        val (a, b, r) = MIXES.random()
        return choice("Αν ανακατέψω $a και $b, τι χρώμα βγαίνει;", r, names, count = 4,
            bigText = "$a + $b = ?")
    }
    val (name, c) = COLORS.random()
    return choice("Τι χρώμα είναι αυτό;", name, names, count = if (older) 4 else 3, swatch = c)
}

private data class Word(val word: String, val emoji: String, val image: Int? = null)

private val WORDS = listOf(
    Word("ΓΑΤΑ", "", R.drawable.animal_cat), Word("ΣΚΥΛΟΣ", "", R.drawable.animal_dog),
    Word("ΕΛΕΦΑΝΤΑΣ", "", R.drawable.animal_elephant), Word("ΛΙΟΝΤΑΡΙ", "", R.drawable.animal_lion),
    Word("ΒΑΤΡΑΧΟΣ", "", R.drawable.animal_frog), Word("ΓΟΥΡΟΥΝΙ", "", R.drawable.animal_pig),
    Word("ΜΗΛΟ", "🍎"), Word("ΗΛΙΟΣ", "☀️"), Word("ΨΑΡΙ", "🐟"), Word("ΣΠΙΤΙ", "🏠"),
    Word("ΔΕΝΤΡΟ", "🌳"), Word("ΑΣΤΕΡΙ", "⭐"), Word("ΜΕΛΙΣΣΑ", "🐝"), Word("ΧΕΛΩΝΑ", "🐢"),
    Word("ΜΠΑΛΑ", "⚽"), Word("ΦΕΓΓΑΡΙ", "🌙"), Word("ΚΑΡΔΙΑ", "❤️"), Word("ΠΑΠΑΚΙ", "🦆"),
    Word("ΤΡΕΝΟ", "🚂"), Word("ΟΜΠΡΕΛΑ", "☂️"), Word("ΝΕΡΟ", "💧"), Word("ΖΑΧΑΡΗ", "🍬"),
)

private const val ALPHABET = "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ"

private fun letterQuestion(older: Boolean): Question {
    val w = WORDS.random()
    val letters = ALPHABET.map { it.toString() }
    if (!older) {
        val first = w.word.first().toString()
        return choice("Από ποιο γράμμα αρχίζει;", first, letters, image = w.image,
            bigText = if (w.image == null) w.emoji else null,
            speech = "Από ποιο γράμμα αρχίζει η λέξη ${w.word.lowercase()};")
    }
    val idx = Random.nextInt(w.word.length)
    val missing = w.word[idx].toString()
    val shown = w.word.replaceRange(idx, idx + 1, "_")
    return choice("Ποιο γράμμα λείπει;", missing, letters, count = 4, image = w.image,
        bigText = if (w.image == null) "${w.emoji}  $shown" else shown,
        speech = "Ποιο γράμμα λείπει από τη λέξη;")
}

private fun buildRound(cat: Category, older: Boolean): List<Question> {
    val out = mutableListOf<Question>()
    var guard = 0
    while (out.size < QUESTIONS_PER_ROUND && guard++ < 200) {
        val q = when (cat) {
            Category.Math -> mathQuestion(older)
            Category.Animals -> animalQuestion(older)
            Category.Colors -> colorQuestion(older)
            Category.Letters -> letterQuestion(older)
        }
        // Avoid the same question twice in a row.
        if (out.lastOrNull()?.let { it.bigText == q.bigText && it.image == q.image && it.swatch == q.swatch } == true) continue
        out.add(q)
    }
    return out
}

@Composable
fun QuizScreen(onBack: () -> Unit) {
    var category by remember { mutableStateOf<Category?>(null) }
    val cat = category
    if (cat == null) {
        CategoryPicker(onBack) { category = it }
    } else {
        QuizRound(cat, onBack = { category = null }, onHome = onBack)
    }
}

@Composable
private fun CategoryPicker(onBack: () -> Unit, onPick: (Category) -> Unit) {
    LaunchedEffect(Unit) { Speaker.say("Διάλεξε κουίζ!") }
    KiddoPage("Κουίζ", KColors.Blue, onBack, speakText = "Διάλεξε κουίζ!") {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Category.entries.forEach { c ->
                Surface(
                    shape = RoundedCornerShape(26.dp), color = c.color, shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().weight(1f).clickable {
                        Synth.pop(); Speaker.say(c.title); onPick(c)
                    }
                ) {
                    Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.emoji, fontSize = 48.sp)
                        Spacer(Modifier.size(16.dp))
                        Text(c.title, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizRound(cat: Category, onBack: () -> Unit, onHome: () -> Unit) {
    val app = LocalAppState.current
    var seed by remember { mutableIntStateOf(0) }
    val questions = remember(seed, app.older) { buildRound(cat, app.older) }
    var index by remember(seed) { mutableIntStateOf(0) }
    var score by remember(seed) { mutableIntStateOf(0) }
    var picked by remember(seed, index) { mutableStateOf<Int?>(null) }
    var finished by remember(seed) { mutableStateOf(false) }
    val q = questions[index.coerceAtMost(questions.lastIndex)]

    LaunchedEffect(seed, index) { if (!finished) Speaker.say(q.speech) }

    LaunchedEffect(picked) {
        val p = picked ?: return@LaunchedEffect
        if (p == q.answer) {
            score++
            Synth.correct()
            Speaker.say(listOf("Μπράβο!", "Σωστά!", "Τέλεια!", "Υπέροχα!").random())
        } else {
            Synth.wrong()
            Speaker.say("Η σωστή απάντηση είναι ${q.options[q.answer]}")
        }
        delay(if (p == q.answer) 1300 else 2300)
        if (index + 1 >= questions.size) {
            finished = true
            app.addStars(starsFor(score, questions.size))
        } else {
            index++
        }
    }

    Box(Modifier.fillMaxSize()) {
        KiddoPage(cat.title, cat.color, onBack, speakText = q.speech) {
            Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = { (index + if (picked != null) 1 else 0).toFloat() / questions.size },
                    modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50)),
                    color = cat.color, trackColor = Color.White
                )
                Text(
                    "Ερώτηση ${index + 1} από ${questions.size}",
                    fontSize = 16.sp, color = KColors.Ink, modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    q.prompt, fontSize = 28.sp, fontWeight = FontWeight.Black, color = KColors.Ink,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp)
                )
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    QuestionVisual(q)
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    q.options.forEachIndexed { i, opt ->
                        val color = when {
                            picked == null -> cat.color
                            i == q.answer -> KColors.Green
                            i == picked -> KColors.Red
                            else -> Color.LightGray
                        }
                        BigButton(opt, color, Modifier.fillMaxWidth(), fontSize = 26) {
                            if (picked == null) picked = i
                        }
                    }
                }
            }
        }
        if (finished) {
            Celebration(
                starsFor(score, questions.size),
                "Βρήκες $score από ${questions.size}!",
                onAgain = { seed++ },
                onHome = onHome
            )
        }
    }
}

@Composable
private fun QuestionVisual(q: Question) {
    when {
        q.image != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(q.image), null, Modifier.size(200.dp))
            if (q.bigText != null) {
                Text(q.bigText, fontSize = 44.sp, fontWeight = FontWeight.Black, color = KColors.Ink, letterSpacing = 4.sp)
            }
        }
        q.swatch != null -> Box(
            Modifier.size(180.dp).clip(RoundedCornerShape(40.dp)).background(Color.LightGray).padding(6.dp)
                .clip(RoundedCornerShape(36.dp)).background(q.swatch)
        )
        q.bigText != null -> Surface(shape = RoundedCornerShape(30.dp), color = Color.White, shadowElevation = 4.dp) {
            Text(
                q.bigText, fontSize = 52.sp, fontWeight = FontWeight.Black, color = KColors.Ink,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)
            )
        }
    }
}
