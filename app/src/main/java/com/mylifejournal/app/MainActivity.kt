package com.mylifejournal.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import org.json.JSONObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class UiPalette(
    val background: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val primaryDark: Color,
    val ink: Color,
    val muted: Color,
    val primarySoft: Color,
    val warm: Color,
    val warmInk: Color,
    val line: Color
)

private data class TaskRow(val id: Int, val name: String, val done: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            MyLifeJournalApp(JournalDb(this), loadPalette())
        }
    }

    private fun loadPalette(): UiPalette {
        val json = try {
            assets.open("ui.json").bufferedReader().use { JSONObject(it.readText()) }
        } catch (_: Exception) { JSONObject() }

        fun color(key: String, fallback: String): Color {
            return try { Color(AndroidColor.parseColor(json.optString(key, fallback))) }
            catch (_: Exception) { Color(AndroidColor.parseColor(fallback)) }
        }

        return UiPalette(
            background = color("background", "#F3F6F5"),
            surfaceAlt = color("surfaceAlt", "#EAF0EE"),
            primary = color("accent", "#176B63"),
            primaryDark = color("accentDark", "#104D48"),
            ink = color("text", "#172A2C"),
            muted = color("muted", "#718080"),
            primarySoft = color("accentSoft", "#DDEFEA"),
            warm = color("warm", "#F7ECDD"),
            warmInk = color("warmInk", "#8A6032"),
            line = color("line", "#E0E8E5")
        )
    }
}

@Composable
private fun MyLifeJournalApp(db: JournalDb, p: UiPalette) {
    var tab by rememberSaveable { mutableStateOf("Home") }
    var selectedMood by rememberSaveable { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tasks = remember(refresh) { readTasks(db) }

    val colors = lightColorScheme(
        primary = p.primary,
        onPrimary = Color.White,
        primaryContainer = p.primarySoft,
        onPrimaryContainer = p.primaryDark,
        secondary = p.warmInk,
        onSecondary = Color.White,
        secondaryContainer = p.warm,
        onSecondaryContainer = p.warmInk,
        background = p.background,
        onBackground = p.ink,
        surface = Color.White,
        onSurface = p.ink,
        surfaceVariant = p.surfaceAlt,
        onSurfaceVariant = p.muted,
        outline = p.line,
        outlineVariant = p.line
    )

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = p.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = p.ink,
                    tonalElevation = 5.dp,
                    windowInsets = NavigationBarDefaults.windowInsets
                ) {
                    val destinations = listOf(
                        Triple("Home", Icons.Filled.Home, "Home"),
                        Triple("Journal", Icons.Filled.Edit, "Journal"),
                        Triple("Tasks", Icons.Filled.CheckCircle, "Tasks"),
                        Triple("Weekly", Icons.Filled.DateRange, "Weekly")
                    )
                    destinations.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item.first,
                            onClick = { tab = item.first },
                            icon = { Icon(item.second, contentDescription = item.third) },
                            label = { Text(item.third, fontSize = 10.sp, maxLines = 1) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = p.primaryDark,
                                selectedTextColor = p.primaryDark,
                                indicatorColor = p.primarySoft,
                                unselectedIconColor = p.muted,
                                unselectedTextColor = p.muted
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Crossfade(
                targetState = tab,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                animationSpec = tween(180),
                label = "screen-transition"
            ) { current ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .background(p.background)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    AppHeader(p, current)
                    Spacer(Modifier.height(20.dp))
                    when (current) {
                        "Home" -> HomeScreen(db, p, selectedMood, onMood = {
                            selectedMood = it
                            db.mood(it)
                            scope.launch { snackbar.showSnackbar("Mood saved: $it") }
                        }, onGo = { tab = it })
                        "Journal" -> JournalScreen(db, p, onSaved = {
                            refresh++
                            scope.launch { snackbar.showSnackbar("Reflection saved on this phone") }
                        })
                        "Tasks" -> TasksScreen(db, p, tasks, onChanged = { refresh++ }, onMessage = {
                            scope.launch { snackbar.showSnackbar(it) }
                        })
                        else -> WeeklyScreen(db, p)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun readTasks(db: JournalDb): List<TaskRow> {
    val array = db.tasks()
    return (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        TaskRow(item.optInt("id"), item.optString("name"), item.optInt("done") == 1)
    }
}

@Composable
private fun AppHeader(p: UiPalette, page: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(15.dp), color = p.primary) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Favorite, contentDescription = "Journal", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(if (page == "Home") "My Life Journal" else page, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = p.ink)
            Text(
                when (page) {
                    "Home" -> "YOUR PERSONAL SPACE"
                    "Journal" -> "A SPACE FOR YOUR THOUGHTS"
                    "Tasks" -> "PLAN ONE STEP AT A TIME"
                    else -> "NOTICE YOUR PROGRESS"
                },
                fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp, color = p.muted
            )
        }
        Surface(shape = RoundedCornerShape(15.dp), color = p.primarySoft, modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("G", color = p.primaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    db: JournalDb,
    p: UiPalette,
    selectedMood: String,
    onMood: (String) -> Unit,
    onGo: (String) -> Unit
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    HeroCard(p, greeting, date)
    Spacer(Modifier.height(21.dp))
    SectionEyebrow("DAILY CHECK-IN", p)
    Spacer(Modifier.height(8.dp))
    Panel {
        Text("How are you feeling today?", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
        Spacer(Modifier.height(4.dp))
        Text("Choose what feels closest. No judgement.", fontSize = 12.sp, color = p.muted)
        Spacer(Modifier.height(15.dp))
        val moods = listOf("😞" to "Low", "😕" to "Meh", "😐" to "Okay", "🙂" to "Good", "😊" to "Great")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            moods.forEach { (emoji, label) ->
                MoodChoice(emoji, label, selectedMood == label, p, Modifier.weight(1f)) { onMood(label) }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    SectionEyebrow("TODAY'S MOMENTUM", p)
    Spacer(Modifier.height(8.dp))
    val done = db.done()
    val total = db.total()
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Your little wins", modifier = Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
            Surface(shape = RoundedCornerShape(30.dp), color = p.primarySoft) {
                Text("$done / $total DONE", modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = p.primaryDark)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(if (total == 0) "Add one small task to get started." else "Keep going. Every completed step counts.", fontSize = 12.sp, color = p.muted)
        Spacer(Modifier.height(13.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(20.dp)),
            color = p.primary, trackColor = p.surfaceAlt
        )
    }
    Spacer(Modifier.height(8.dp))
    SectionEyebrow("QUICK ACTIONS", p)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ActionCard(p, "Write a journal", "Clear your thoughts", Icons.Filled.Edit, p.primarySoft, Modifier.weight(1f)) { onGo("Journal") }
        ActionCard(p, "Add a task", "One step at a time", Icons.Filled.Add, p.warm, Modifier.weight(1f)) { onGo("Tasks") }
    }
    Spacer(Modifier.height(14.dp))
    Surface(shape = RoundedCornerShape(18.dp), color = p.surfaceAlt, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp)) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = p.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("You don't need a perfect day. Just a little progress.", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = p.ink)
        }
    }
}

@Composable
private fun HeroCard(p: UiPalette, greeting: String, date: String) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(p.primaryDark, p.primary, Color(0xFF328579))))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White.copy(alpha = .15f), shape = RoundedCornerShape(30.dp)) {
                    Text("YOUR DAILY RESET", modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, color = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text(date, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = .88f))
            }
            Spacer(Modifier.height(18.dp))
            Text("$greeting, Gaurav.", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("Make space for what matters.", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = .96f))
            Spacer(Modifier.height(7.dp))
            Text("Small, steady steps are still progress. Start with one thing today.", fontSize = 12.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = .82f))
        }
    }
}

@Composable
private fun SectionEyebrow(value: String, p: UiPalette) {
    Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp, color = p.primary)
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(17.dp), content = content)
    }
}

@Composable
private fun MoodChoice(
    emoji: String,
    label: String,
    selected: Boolean,
    p: UiPalette,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (selected) p.primarySoft else p.surfaceAlt, animationSpec = tween(160), label = "mood-color")
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = bg,
        border = if (selected) BorderStroke(1.dp, p.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 21.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) p.primaryDark else p.muted, maxLines = 1)
        }
    }
}

@Composable
private fun ActionCard(
    p: UiPalette,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, modifier = modifier.height(116.dp), shape = RoundedCornerShape(20.dp), color = tint) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = p.primaryDark, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(9.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.ink)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun JournalScreen(db: JournalDb, p: UiPalette, onSaved: () -> Unit) {
    var good by rememberSaveable { mutableStateOf("") }
    var hard by rememberSaveable { mutableStateOf("") }
    var tomorrow by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    PageIntro("DAILY REFLECTION", "Put it into words.", "A few honest lines can help you understand your day.", p)
    Panel {
        JournalField("01  What went well today?", "Something I'm grateful for…", good, { good = it }, p, 2)
        Spacer(Modifier.height(14.dp))
        JournalField("02  What felt difficult?", "Something that's on my mind…", hard, { hard = it }, p, 3)
        Spacer(Modifier.height(14.dp))
        JournalField("03  One step for tomorrow", "A small thing I want to try…", tomorrow, { tomorrow = it }, p, 2)
        Spacer(Modifier.height(15.dp))
        Button(
            onClick = {
                if (good.isBlank() && hard.isBlank() && tomorrow.isBlank()) return@Button
                db.journal(good.trim(), hard.trim(), tomorrow.trim())
                good = ""; hard = ""; tomorrow = ""
                keyboard?.hide()
                onSaved()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = p.primary)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Save my reflection", fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(7.dp))
    Panel {
        Text("YOUR MOST RECENT NOTE", fontSize = 10.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold, color = p.primary)
        Spacer(Modifier.height(8.dp))
        Text(db.latest().ifBlank { "Your saved reflections will appear here." }, fontSize = 12.sp, lineHeight = 19.sp, color = p.muted)
    }
}

@Composable
private fun PageIntro(eyebrow: String, title: String, description: String, p: UiPalette) {
    SectionEyebrow(eyebrow, p)
    Spacer(Modifier.height(7.dp))
    Text(title, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = p.ink)
    Spacer(Modifier.height(5.dp))
    Text(description, fontSize = 13.sp, lineHeight = 19.sp, color = p.muted)
    Spacer(Modifier.height(17.dp))
}

@Composable
private fun JournalField(
    label: String,
    hint: String,
    value: String,
    onValue: (String) -> Unit,
    p: UiPalette,
    lines: Int
) {
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(hint, fontSize = 13.sp, color = p.muted) },
            minLines = lines,
            shape = RoundedCornerShape(15.dp)
        )
    }
}

@Composable
private fun TasksScreen(
    db: JournalDb,
    p: UiPalette,
    tasks: List<TaskRow>,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit
) {
    var newTask by rememberSaveable { mutableStateOf("") }
    PageIntro("YOUR DAILY FOCUS", "Make today count.", "Keep your list realistic. Small wins build momentum.", p)
    Panel {
        Text("Add a new task", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = newTask,
            onValueChange = { newTask = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("What would you like to get done?") },
            shape = RoundedCornerShape(15.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Spacer(Modifier.height(11.dp))
        Button(
            onClick = {
                if (newTask.isBlank()) onMessage("Please enter a task first")
                else {
                    db.add(newTask.trim())
                    newTask = ""
                    onChanged()
                    onMessage("Task added")
                }
            },
            modifier = Modifier.fillMaxWidth().height(49.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = p.primary)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("Add to my list", fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(6.dp))
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today's checklist", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
            Text(tasks.count { it.done }.toString() + " COMPLETE", color = p.primaryDark, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
        if (tasks.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Nothing here yet. Add one task above to get started.", fontSize = 13.sp, color = p.muted)
        } else {
            Spacer(Modifier.height(8.dp))
            tasks.forEachIndexed { index, task ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = task.done,
                        onCheckedChange = {
                            db.check(task.id, it)
                            onChanged()
                            onMessage(if (it) "Nice work!" else "Task reopened")
                        },
                        colors = CheckboxDefaults.colors(checkedColor = p.primary, uncheckedColor = p.muted)
                    )
                    Text(
                        task.name,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = if (task.done) p.muted else p.ink,
                        textDecoration = if (task.done) TextDecoration.LineThrough else null
                    )
                }
                if (index < tasks.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(color = p.line.copy(alpha = .7f))
                }
            }
        }
    }
}

@Composable
private fun WeeklyScreen(db: JournalDb, p: UiPalette) {
    val journalDays = db.weekJournals()
    val done = db.weekDone()
    val total = db.weekTasks()
    val percent = if (total == 0) 0 else (100f * done / total).toInt()

    PageIntro("YOUR LAST 7 DAYS", "Look how far you've come.", "Use these numbers as information, not as a judgement.", p)
    Panel {
        Text("Your week at a glance", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            StatCard(p, journalDays.toString(), "Journal entries", p.primarySoft, Modifier.weight(1f))
            StatCard(p, done.toString(), "Tasks completed", p.warm, Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            StatCard(p, total.toString(), "Tasks added", p.surfaceAlt, Modifier.weight(1f))
            StatCard(p, "$percent%", "Completion", Color(0xFFE8EDF1), Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(7.dp))
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = p.primary, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(8.dp))
            Text("Your consistency", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
        }
        Spacer(Modifier.height(8.dp))
        Text("You completed $done of $total tasks this week.", fontSize = 12.sp, color = p.muted)
        Spacer(Modifier.height(13.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(20.dp)),
            color = p.primary, trackColor = p.surfaceAlt
        )
    }
    Spacer(Modifier.height(7.dp))
    Surface(shape = RoundedCornerShape(22.dp), color = p.surfaceAlt, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("PAUSE & REFLECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, color = p.primary)
            Spacer(Modifier.height(8.dp))
            Text("What gave you energy this week? What drained it? What's one thing you want to carry into next week?", fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, color = p.ink)
        }
    }
    Spacer(Modifier.height(7.dp))
    Panel {
        Text("A note from you", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
        Spacer(Modifier.height(8.dp))
        Text(db.latest().ifBlank { "Your journal entries will show here as you write them." }, fontSize = 12.sp, lineHeight = 19.sp, color = p.muted)
    }
}

@Composable
private fun StatCard(p: UiPalette, value: String, label: String, tint: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(17.dp), color = tint) {
        Column(Modifier.padding(13.dp)) {
            Text(value, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = p.ink)
            Spacer(Modifier.height(3.dp))
            Text(label, fontSize = 10.sp, lineHeight = 14.sp, color = p.muted)
        }
    }
}
