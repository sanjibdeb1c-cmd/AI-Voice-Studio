package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ClonedVoice
import com.example.data.TtsHistoryItem
import com.example.ui.SpeechEngine
import com.example.ui.SpeechState
import com.example.ui.StockVoice
import com.example.ui.TtsViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TtsApp()
            }
        }
    }
}

// Pages Enum
enum class StudioPage(val title: String, val icon: ImageVector) {
    TEXT_TO_SPEECH("Text to Speech", Icons.Default.KeyboardVoice),
    VOICE_CLONING("Voice Cloning", Icons.Default.SettingsVoice),
    VOICE_ENHANCEMENT("Voice Enhancement", Icons.Default.Hearing),
    STOCK_VOICES("Stock Voices", Icons.Default.Storefront),
    MY_VOICES("My Voices", Icons.Default.FolderShared),
    AUDIO_HISTORY("Audio History", Icons.Default.History),
    EXPORT("Export Studio", Icons.Default.Download),
    USER_PROFILE("My Profile", Icons.Default.Person),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsApp(viewModel: TtsViewModel = viewModel()) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val themeColorName by viewModel.themeColor.collectAsState()

    // Determine Theme Colors dynamically
    val primaryAccent = when (themeColorName) {
        "Neon Cyber" -> Color(0xFF06B6D4) // Cyan
        "Ocean Deep" -> Color(0xFF64FFDA) // Seafoam
        "Emerald Rich" -> Color(0xFF10B981) // Emerald
        else -> Color(0xFF8B5CF6) // Violet (Slate Studio default)
    }

    val secondaryAccent = when (themeColorName) {
        "Neon Cyber" -> Color(0xFFEC4899) // Pink
        "Ocean Deep" -> Color(0xFF0A192F) // Navy dark
        "Emerald Rich" -> Color(0xFF047857) // Dark green
        else -> Color(0xFFC084FC) // Lilac
    }

    val darkBackground = when (themeColorName) {
        "Ocean Deep" -> Color(0xFF050B14)
        "Emerald Rich" -> Color(0xFF060A0C)
        else -> Color(0xFF080C14) // Deep charcoal
    }

    val containerBackground = when (themeColorName) {
        "Ocean Deep" -> Color(0xFF0A1424)
        "Emerald Rich" -> Color(0xFF0E1618)
        else -> Color(0xFF111827) // Slate panel
    }

    val cardBorderColor = Color(0xFF1F2937)

    val studioColorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = primaryAccent,
            secondary = secondaryAccent,
            background = darkBackground,
            surface = containerBackground,
            onBackground = Color.White,
            onSurface = Color(0xFFE5E7EB),
            surfaceVariant = Color(0xFF1F2937)
        )
    } else {
        lightColorScheme(
            primary = primaryAccent,
            secondary = secondaryAccent,
            background = Color(0xFFF3F4F6),
            surface = Color.White,
            onBackground = Color(0xFF111827),
            onSurface = Color(0xFF374151),
            surfaceVariant = Color(0xFFE5E7EB)
        )
    }

    var activePage by remember { mutableStateOf(StudioPage.TEXT_TO_SPEECH) }
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 720

    // Custom Theme wrapper
    MaterialTheme(
        colorScheme = studioColorScheme,
        typography = MaterialTheme.typography
    ) {
        Scaffold(
            bottomBar = {
                if (!isWideScreen) {
                    ScrollableTabRow(
                        selectedTabIndex = StudioPage.values().indexOf(activePage),
                        edgePadding = 0.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = {} // No visible bar indicator for clean mobile style
                    ) {
                        StudioPage.values().forEach { page ->
                            val isSelected = activePage == page
                            val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            Tab(
                                selected = isSelected,
                                onClick = { activePage = page },
                                text = { Text(page.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = color) },
                                icon = { Icon(page.icon, contentDescription = page.title, tint = color, modifier = Modifier.size(20.dp)) }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Wide Screen Side Navigation Panel
                if (isWideScreen) {
                    Column(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, cardBorderColor, RoundedCornerShape(0.dp))
                            .padding(16.dp)
                    ) {
                        // Title / Logo Area
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(end = 8.dp)
                            )
                            Text(
                                text = "Voice Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Navigation Items
                        StudioPage.values().forEach { page ->
                            val isSelected = activePage == page
                            val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                            val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgColor)
                                    .clickable { activePage = page }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = page.icon,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = page.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Quick info footer
                        Text(
                            text = "Premium Studio v3.5",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                // Main Section Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Crossfade(targetState = activePage) { page ->
                        when (page) {
                            StudioPage.TEXT_TO_SPEECH -> TextToSpeechPage(viewModel, primaryAccent, secondaryAccent)
                            StudioPage.VOICE_CLONING -> VoiceCloningPage(viewModel, primaryAccent)
                            StudioPage.VOICE_ENHANCEMENT -> VoiceEnhancementPage(viewModel, primaryAccent)
                            StudioPage.STOCK_VOICES -> StockVoicesPage(viewModel, primaryAccent)
                            StudioPage.MY_VOICES -> MyVoicesPage(viewModel, primaryAccent)
                            StudioPage.AUDIO_HISTORY -> AudioHistoryPage(viewModel, primaryAccent)
                            StudioPage.EXPORT -> ExportPage(viewModel, primaryAccent)
                            StudioPage.USER_PROFILE -> UserProfilePage(viewModel, primaryAccent)
                            StudioPage.SETTINGS -> SettingsPage(viewModel, primaryAccent)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. TEXT TO SPEECH PAGE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToSpeechPage(viewModel: TtsViewModel, primaryAccent: Color, secondaryAccent: Color) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val inputText by viewModel.inputText.collectAsState()
    val selectedEngine by viewModel.selectedEngine.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val volume by viewModel.volume.collectAsState()

    val stability by viewModel.stability.collectAsState()
    val similarity by viewModel.similarity.collectAsState()
    val styleExaggeration by viewModel.styleExaggeration.collectAsState()
    val selectedEmotion by viewModel.selectedEmotion.collectAsState()

    val selectedLocale by viewModel.selectedLocale.collectAsState()
    val availableLocales by viewModel.availableLocales.collectAsState()
    val selectedGeminiVoice by viewModel.selectedGeminiVoice.collectAsState()
    val clonedVoices by viewModel.clonedVoicesList.collectAsState()
    val selectedClonedVoiceId by viewModel.selectedClonedVoiceId.collectAsState()

    val speechState by viewModel.speechState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showLocaleDropdown by remember { mutableStateOf(false) }
    var showVoiceDropdown by remember { mutableStateOf(false) }
    var showEmotionDropdown by remember { mutableStateOf(false) }

    val geminiVoices = remember {
        listOf(
            "Kore" to "Kore (Female - Clear)",
            "Aoede" to "Aoede (Female - Warm)",
            "Puck" to "Puck (Male - Expressive)",
            "Charon" to "Charon (Male - Deep)",
            "Fenrir" to "Fenrir (Male - Dramatic)"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Card
        Text(
            text = "AI Voice Studio",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Generate professional speech from text using high-fidelity pre-trained models.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Engine Selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                SpeechEngine.GEMINI to "AI Speech (Gemini)",
                SpeechEngine.NATIVE to "Native Engine",
                SpeechEngine.CLONED to "My Cloned Voices"
            ).forEach { (engine, label) ->
                val isSelected = selectedEngine == engine
                OutlinedButton(
                    onClick = { viewModel.setEngine(engine) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF374151))
                ) {
                    Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        // Text Editor Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.updateInputText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    placeholder = { Text("Type or paste your content here... (e.g. scripts, blogs, narration)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy button
                    IconButton(onClick = {
                        if (inputText.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(inputText))
                            Toast.makeText(context, "Copied text to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy text")
                    }

                    // Count Displays
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val wordCount = inputText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                        Text(
                            text = "$wordCount words",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${inputText.length} characters",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("character_count")
                        )
                    }
                }
            }
        }

        // Configuration Sliders Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Voice Configuration Parameters", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))

                // Conditionally render options based on Engine
                if (selectedEngine == SpeechEngine.GEMINI) {
                    // Gemini prebuilt voices drop down
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = showVoiceDropdown,
                            onExpandedChange = { showVoiceDropdown = !showVoiceDropdown }
                        ) {
                            OutlinedTextField(
                                value = geminiVoices.find { it.first == selectedGeminiVoice }?.second ?: selectedGeminiVoice,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("AI Stock Voice Target") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showVoiceDropdown) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = showVoiceDropdown,
                                onDismissRequest = { showVoiceDropdown = false }
                            ) {
                                geminiVoices.forEach { (id, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setGeminiVoice(id)
                                            showVoiceDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedEngine == SpeechEngine.NATIVE) {
                    // Native Language Selector
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = showLocaleDropdown,
                            onExpandedChange = { showLocaleDropdown = !showLocaleDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedLocale.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Language / Accent (Native)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLocaleDropdown) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = showLocaleDropdown,
                                onDismissRequest = { showLocaleDropdown = false }
                            ) {
                                availableLocales.forEach { locale ->
                                    DropdownMenuItem(
                                        text = { Text(locale.displayName) },
                                        onClick = {
                                            viewModel.setLocale(locale)
                                            showLocaleDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Custom Cloned Voices Selector
                    if (clonedVoices.isEmpty()) {
                        Text(
                            text = "No cloned voices created yet. Go to 'Voice Cloning' page to record/train a custom voice!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        var showClonedDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = showClonedDropdown,
                                onExpandedChange = { showClonedDropdown = !showClonedDropdown }
                            ) {
                                val selectedName = clonedVoices.find { it.id == selectedClonedVoiceId }?.name ?: "Select Custom Cloned Voice"
                                OutlinedTextField(
                                    value = selectedName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("My Cloned Voices List") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showClonedDropdown) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = showClonedDropdown,
                                    onDismissRequest = { showClonedDropdown = false }
                                ) {
                                    clonedVoices.forEach { voice ->
                                        DropdownMenuItem(
                                            text = { Text("${voice.name} (Quality: ${voice.qualityScore}%)") },
                                            onClick = {
                                                viewModel.setSelectedClonedVoiceId(voice.id)
                                                showClonedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Fine controls
                if (selectedEngine == SpeechEngine.NATIVE) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Speed: ${(speed * 100).toInt()}%", modifier = Modifier.weight(1f))
                            Slider(value = speed, onValueChange = { viewModel.setSpeed(it) }, valueRange = 0.5f..2.0f, modifier = Modifier.weight(3f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pitch: ${(pitch * 100).toInt()}%", modifier = Modifier.weight(1f))
                            Slider(value = pitch, onValueChange = { viewModel.setPitch(it) }, valueRange = 0.5f..2.0f, modifier = Modifier.weight(3f))
                        }
                    }
                } else {
                    // ElevenLabs Premium Sliders
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Stability", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("${(stability * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(value = stability, onValueChange = { viewModel.setStability(it) }, valueRange = 0.0f..1.0f)
                        }

                        Column {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Clarity / Similarity", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("${(similarity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(value = similarity, onValueChange = { viewModel.setSimilarity(it) }, valueRange = 0.0f..1.0f)
                        }

                        Column {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Style Exaggeration", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("${(styleExaggeration * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(value = styleExaggeration, onValueChange = { viewModel.setStyleExaggeration(it) }, valueRange = 0.0f..1.0f)
                        }

                        // Emotion Selection
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ExposedDropdownMenuBox(
                                expanded = showEmotionDropdown,
                                onExpandedChange = { showEmotionDropdown = !showEmotionDropdown }
                            ) {
                                OutlinedTextField(
                                    value = selectedEmotion,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Performance Emotion") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEmotionDropdown) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = showEmotionDropdown,
                                    onDismissRequest = { showEmotionDropdown = false }
                                ) {
                                    listOf("Neutral", "Excited", "Professional", "Whisper", "Dramatic").forEach { emot ->
                                        DropdownMenuItem(
                                            text = { Text(emot) },
                                            onClick = {
                                                viewModel.setEmotion(emot)
                                                showEmotionDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Volume slider
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Master Volume: ${(volume * 100).toInt()}%", modifier = Modifier.weight(1.5f), fontSize = 13.sp)
                    Slider(value = volume, onValueChange = { viewModel.setVolume(it) }, valueRange = 0.0f..1.0f, modifier = Modifier.weight(3f))
                }
            }
        }

        // Bouncing Waveforms Visualizer when playing
        if (speechState == SpeechState.SPEAKING) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Synthesizing Stream Active", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    BouncingWavesAnim()
                }
            }
        }

        // Error message overlay
        errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Action Trigger Button Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Synthesize Speech action
            Button(
                onClick = { viewModel.startSpeaking() },
                modifier = Modifier.weight(1.5f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Synthesize Speech", fontWeight = FontWeight.Bold)
            }

            // Pause / Stop controls
            if (speechState == SpeechState.SPEAKING) {
                Button(
                    onClick = { viewModel.pausePlayback() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Text("Pause")
                }
            } else if (speechState == SpeechState.PAUSED) {
                Button(
                    onClick = { viewModel.resumePlayback() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Resume")
                }
            }

            Button(
                onClick = { viewModel.stopPlayback() },
                modifier = Modifier.weight(0.8f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Text("Stop")
            }
        }
    }
}

// Custom Waveform Micro-Animation
@Composable
fun BouncingWavesAnim() {
    val infiniteTransition = rememberInfiniteTransition()
    val heights = listOf(
        infiniteTransition.animateFloat(initialValue = 10f, targetValue = 40f, animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse)),
        infiniteTransition.animateFloat(initialValue = 5f, targetValue = 35f, animationSpec = infiniteRepeatable(animation = tween(300, easing = LinearEasing), repeatMode = RepeatMode.Reverse)),
        infiniteTransition.animateFloat(initialValue = 15f, targetValue = 55f, animationSpec = infiniteRepeatable(animation = tween(500, easing = LinearEasing), repeatMode = RepeatMode.Reverse)),
        infiniteTransition.animateFloat(initialValue = 8f, targetValue = 45f, animationSpec = infiniteRepeatable(animation = tween(350, easing = LinearEasing), repeatMode = RepeatMode.Reverse)),
        infiniteTransition.animateFloat(initialValue = 12f, targetValue = 50f, animationSpec = infiniteRepeatable(animation = tween(450, easing = LinearEasing), repeatMode = RepeatMode.Reverse))
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(60.dp)
    ) {
        heights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(height.value.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// ==========================================
// 2. VOICE CLONING PAGE
// ==========================================
@Composable
fun VoiceCloningPage(viewModel: TtsViewModel, primaryAccent: Color) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()
    val recordingAmplitude by viewModel.recordingAmplitude.collectAsState()
    val cloneProgress by viewModel.cloneProgress.collectAsState()
    val clonedVoices by viewModel.clonedVoicesList.collectAsState()

    var nameInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var cleanNoise by remember { mutableStateOf(true) }
    var removeSilence by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(context, "Microphone access is needed for recording speech samples.", Toast.LENGTH_LONG).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("AI Voice Cloning", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = primaryAccent)
        Text("Train a custom clone voice with just 10 seconds of high-quality sample recording.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Step 1: Record Voice Sample", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (isRecording) "Recording clip: $recordingSeconds s" else "Status: Microphone Ready", fontWeight = FontWeight.Bold)
                        Text("Recommended duration: 10-15 seconds", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }

                    Button(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopRecording()
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.startRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) MaterialTheme.colorScheme.error else primaryAccent
                        )
                    ) {
                        Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(if (isRecording) "Stop" else "Record Voice")
                    }
                }

                // Amplitude level bar
                if (isRecording) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Volume meter:", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                        LinearProgressIndicator(
                            progress = { recordingAmplitude },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = primaryAccent
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Step 2: Voice Attributes & Training", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Voice Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("Description (e.g. Energetic youth, calm deep narrator)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // Advanced clean options
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Checkbox(checked = cleanNoise, onCheckedChange = { cleanNoise = it })
                    Text("Apply Background Noise Cleaner", fontSize = 14.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    Checkbox(checked = removeSilence, onCheckedChange = { removeSilence = it })
                    Text("Silence/Pause Removal Gating", fontSize = 14.sp)
                }

                if (cloneProgress != null) {
                    LinearProgressIndicator(
                        progress = { (cloneProgress ?: 0) / 100f },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Text("Training Voice: $cloneProgress%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = primaryAccent, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Button(
                        onClick = {
                            if (nameInput.isBlank()) {
                                Toast.makeText(context, "Please enter a Voice Name.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.trainVoice(nameInput, descriptionInput, cleanNoise, removeSilence)
                                nameInput = ""
                                descriptionInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Train/Save Cloned Voice", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active clones list
        Text("Your Custom Clones", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 12.dp))
        if (clonedVoices.isEmpty()) {
            Text("No custom cloned voices recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        } else {
            clonedVoices.forEach { voice ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(voice.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(voice.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Quality Score: ${voice.qualityScore}% • Status: Stable", fontSize = 11.sp, color = primaryAccent, fontWeight = FontWeight.Bold)
                        }

                        Row {
                            IconButton(onClick = { viewModel.toggleClonedVoiceFavorite(voice.id) }) {
                                Icon(
                                    imageVector = if (voice.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (voice.isFavorite) primaryAccent else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.deleteClonedVoice(voice.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. VOICE ENHANCEMENT PAGE
// ==========================================
@Composable
fun VoiceEnhancementPage(viewModel: TtsViewModel, primaryAccent: Color) {
    val isEnhancing by viewModel.isEnhancing.collectAsState()
    val clonedVoices by viewModel.clonedVoicesList.collectAsState()
    val selectedClonedVoiceId by viewModel.selectedClonedVoiceId.collectAsState()

    val cleanNoise by viewModel.enhanceNoiseRemoval.collectAsState()
    val echoRemoval by viewModel.enhanceEchoRemoval.collectAsState()
    val breathReduction by viewModel.enhanceBreathReduction.collectAsState()
    val volumeNorm by viewModel.enhanceVolumeNormalization.collectAsState()
    val speechCleanup by viewModel.enhanceSpeechCleanup.collectAsState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("AI Audio Studio Enhancement", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = primaryAccent)
        Text("Clean, balance, and polish audio samples using advanced Studio DSP.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Target Clip for Studio Enhancement", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                if (clonedVoices.isEmpty()) {
                    Text("No custom recorded audio clips available to enhance. Please record a voice sample in 'Voice Cloning' first.", color = primaryAccent)
                } else {
                    clonedVoices.forEach { voice ->
                        val isSelected = selectedClonedVoiceId == voice.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) primaryAccent.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.setSelectedClonedVoiceId(voice.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { viewModel.setSelectedClonedVoiceId(voice.id) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(voice.name, fontWeight = FontWeight.Bold)
                                Text("Raw file path: " + voice.filePath.takeLast(25), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI DSP Cleanup Modules", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Checkbox(checked = cleanNoise, onCheckedChange = { viewModel.setEnhanceNoiseRemoval(it) })
                    Column {
                        Text("AI Background Noise Removal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Intelligently eliminates surrounding environment hiss and hum.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Checkbox(checked = echoRemoval, onCheckedChange = { viewModel.setEnhanceEchoRemoval(it) })
                    Column {
                        Text("Echo / Reverb Suppression", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Dampens bounce-back sounds from hard surfaces.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Checkbox(checked = breathReduction, onCheckedChange = { viewModel.setEnhanceBreathReduction(it) })
                    Column {
                        Text("Breath & Mouth Click Gate", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Suppresses deep breathing spikes and clicks.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Checkbox(checked = volumeNorm, onCheckedChange = { viewModel.setEnhanceVolumeNormalization(it) })
                    Column {
                        Text("Auto Volume Normalization", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Ensures consistent loud and clear voice output across the clip.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Checkbox(checked = speechCleanup, onCheckedChange = { viewModel.setEnhanceSpeechCleanup(it) })
                    Column {
                        Text("AI Studio Restoration & Bandpass Filter", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Simulates multi-band condensated studio quality.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                if (isEnhancing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Applying Studio Enhancements...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = primaryAccent, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Button(
                        onClick = { viewModel.enhanceAudioClip() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedClonedVoiceId != null
                    ) {
                        Icon(Icons.Default.Hearing, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Process & Enhance Audio", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. STOCK AI VOICES PAGE
// ==========================================
@Composable
fun StockVoicesPage(viewModel: TtsViewModel, primaryAccent: Color) {
    val stockVoicesList by viewModel.stockVoices.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf(
        "All", "Narrator", "Documentary", "Podcast", "Storytelling", "Audiobook", "Meditation", "News", "Cinematic Trailer", "Motivation", "Male", "Female", "British", "American", "Indian", "Robot"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Stock Voice Marketplace", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = primaryAccent)
        Text("Browse premium pre-trained high fidelity marketplace AI voices.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))

        // Categories list horizontal scroll row
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = Color.Transparent
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = { Text(cat, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        // Voice Card Grids
        val filteredVoices = if (selectedCategory == "All") {
            stockVoicesList
        } else {
            stockVoicesList.filter { it.categories.contains(selectedCategory) }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(280.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredVoices) { voice ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFF1F2937))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(voice.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("${voice.gender} • ${voice.age}", fontSize = 11.sp, color = primaryAccent, fontWeight = FontWeight.Bold)
                            }

                            Row {
                                IconButton(onClick = { viewModel.toggleStockVoiceFavorite(voice.id) }) {
                                    Icon(
                                        imageVector = if (voice.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (voice.isFavorite) primaryAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(voice.description, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Set engine to gemini and choose voice, then synthesize text
                                    viewModel.setEngine(SpeechEngine.GEMINI)
                                    viewModel.setGeminiVoice(voice.name)
                                    viewModel.updateInputText(voice.sampleText)
                                    viewModel.startSpeaking()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.padding(end = 4.dp).size(16.dp))
                                Text("Preview Voice", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.setEngine(SpeechEngine.GEMINI)
                                    viewModel.setGeminiVoice(voice.name)
                                    Toast.makeText(viewModel.getApplication(), "Selected stock voice: ${voice.name}!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("Select", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. MY VOICES PAGE
// ==========================================
@Composable
fun MyVoicesPage(viewModel: TtsViewModel, primaryAccent: Color) {
    val clonedVoices by viewModel.clonedVoicesList.collectAsState()
    val stockVoicesList by viewModel.stockVoices.collectAsState()

    val favoriteStockVoices = stockVoicesList.filter { it.isFavorite }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Saved Voice Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = primaryAccent)
        Text("Manage all your custom trained clone voices and saved marketplace stock favorites.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))

        Text("Custom Cloned Voices", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

        if (clonedVoices.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("No cloned voices trained yet. Record sound clips to clone your vocal parameters.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            clonedVoices.forEach { voice ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFF1F2937))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(voice.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(voice.description, style = MaterialTheme.typography.bodySmall)
                            Text("Cloning Quality Score: ${voice.qualityScore}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryAccent)
                        }

                        Row {
                            IconButton(onClick = { viewModel.toggleClonedVoiceFavorite(voice.id) }) {
                                Icon(
                                    imageVector = if (voice.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (voice.isFavorite) primaryAccent else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.deleteClonedVoice(voice.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Marketplace Star Favorites", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

        if (favoriteStockVoices.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Browse Stock Marketplace voices and star them to save inside favorites list.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            favoriteStockVoices.forEach { voice ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFF1F2937))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(voice.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(voice.description, style = MaterialTheme.typography.bodySmall)
                        }

                        IconButton(onClick = { viewModel.toggleStockVoiceFavorite(voice.id) }) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = primaryAccent)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. AUDIO HISTORY PAGE
// ==========================================
@Composable
fun AudioHistoryPage(viewModel: TtsViewModel, primaryAccent: Color) {
    val historyList by viewModel.historyList.collectAsState()
    val searchQuery by viewModel.historySearchQuery.collectAsState()
    val filterEngine by viewModel.historyFilterEngine.collectAsState()
    val sortBy by viewModel.historySortBy.collectAsState()
    val favoritesOnly by viewModel.favoritesFilterOnly.collectAsState()

    val filteredHistory = historyList.filter { item ->
        val matchSearch = item.text.contains(searchQuery, ignoreCase = true) || item.voiceName.contains(searchQuery, ignoreCase = true)
        val matchEngine = filterEngine == "All" || item.engine == filterEngine
        val matchFavorites = !favoritesOnly || item.isFavorite
        matchSearch && matchEngine && matchFavorites
    }.sortedWith(Comparator { a, b ->
        when (sortBy) {
            "Oldest" -> a.timestamp.compareTo(b.timestamp)
            "Length" -> b.text.length.compareTo(a.text.length)
            "Alphabetical" -> a.text.compareTo(b.text)
            else -> b.timestamp.compareTo(a.timestamp) // Newest
        }
    })

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Synthesized Audio History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = primaryAccent)
        Text("Replay, favorite, download or clear your generated voice history logs.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))

        // Search & Filter Box
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setHistorySearchQuery(it) },
                    placeholder = { Text("Search text scripts or voice names...") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var showEngineDropdown by remember { mutableStateOf(false) }
                    var showSortDropdown by remember { mutableStateOf(false) }

                    // Engine Filter
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showEngineDropdown = !showEngineDropdown },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Engine: $filterEngine", fontSize = 12.sp)
                        }
                        DropdownMenu(expanded = showEngineDropdown, onDismissRequest = { showEngineDropdown = false }) {
                            listOf("All", "NATIVE", "GEMINI", "CLONED").forEach { eng ->
                                DropdownMenuItem(text = { Text(eng) }, onClick = { viewModel.setHistoryFilterEngine(eng); showEngineDropdown = false })
                            }
                        }
                    }

                    // Sort Filter
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showSortDropdown = !showSortDropdown },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sort: $sortBy", fontSize = 12.sp)
                        }
                        DropdownMenu(expanded = showSortDropdown, onDismissRequest = { showSortDropdown = false }) {
                            listOf("Newest", "Oldest", "Length", "Alphabetical").forEach { s ->
                                DropdownMenuItem(text = { Text(s) }, onClick = { viewModel.setHistorySortBy(s); showSortDropdown = false })
                            }
                        }
                    }

                    // Favorites filter icon toggle
                    IconButton(
                        onClick = { viewModel.setFavoritesFilterOnly(!favoritesOnly) },
                        modifier = Modifier.background(
                            if (favoritesOnly) primaryAccent.copy(alpha = 0.2f) else Color.Transparent,
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = if (favoritesOnly) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Show favorites",
                            tint = if (favoritesOnly) primaryAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Records Found: ${filteredHistory.size}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            if (filteredHistory.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("Clear All Logs", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Lazy History List
        if (filteredHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No synthesized audio logs match criteria.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredHistory) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFF1F2937))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (item.engine == "NATIVE") Icons.Default.PhoneAndroid else Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (item.engine == "NATIVE") primaryAccent else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(item.voiceName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("(${item.format})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }

                                Row {
                                    IconButton(onClick = { viewModel.toggleHistoryFavorite(item.id) }, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = null,
                                            tint = if (item.isFavorite) primaryAccent else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { viewModel.deleteHistoryItem(item.id) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )

                                Button(
                                    onClick = { viewModel.playHistoryItem(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryAccent.copy(alpha = 0.15f), contentColor = primaryAccent),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play Back", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. EXPORT PAGE
// ==========================================
@Composable
fun ExportPage(viewModel: TtsViewModel, primaryAccent: Color) {
    val inputText by viewModel.inputText.collectAsState()
    val exportFormat by viewModel.exportFormat.collectAsState()
    val exportQuality by viewModel.exportQuality.collectAsState()
    val speechState by viewModel.speechState.collectAsState()

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Studio Speech Export", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = primaryAccent)
        Text("Export generated scripts to high quality, downloadable sound clips.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Sound Export Container", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("MP3", "WAV", "FLAC", "OGG").forEach { format ->
                        val isSelected = exportFormat == format
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setExportFormat(format) }
                                .border(1.dp, if (isSelected) primaryAccent else Color(0xFF374151), RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) primaryAccent.copy(alpha = 0.15f) else Color.Transparent
                            )
                        ) {
                            Text(
                                text = format,
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) primaryAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Audio Bitrate / Sample Quality", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("128 kbps", "192 kbps", "256 kbps", "320 kbps", "Lossless").forEach { q ->
                        val isSelected = exportQuality == q
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) primaryAccent.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable { viewModel.setExportQuality(q) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { viewModel.setExportQuality(q) })
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(q, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Content Script Review", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    text = if (inputText.isBlank()) "No script prepared. Write some text in the 'Text to Speech' panel first!" else inputText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (inputText.isBlank()) primaryAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (speechState == SpeechState.LOADING) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
        } else {
            Button(
                onClick = {
                    if (inputText.isBlank()) {
                        Toast.makeText(context, "No text available to export. Go back and write some script.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.exportSpeechAudio(inputText)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Export High Quality File", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 8. SETTINGS PAGE
// ==========================================
@Composable
fun SettingsPage(viewModel: TtsViewModel, primaryAccent: Color) {
    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val activeColorTheme by viewModel.themeColor.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()

    var showNotifications by remember { mutableStateOf(true) }
    var geminiApiKeyInput by remember { mutableStateOf("Stored Securely in Secrets Panel") }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Platform Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = primaryAccent)
        Text("Configure UI preferences, clear caches, and secure your API credentials.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("User Interface Prefs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Display Theme", fontWeight = FontWeight.Bold)
                        Text("Switch dark slate and energy efficient views.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(checked = isDarkTheme, onCheckedChange = { viewModel.setDarkMode(it) })
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Studio Accent Colors", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = primaryAccent)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Slate Studio", "Neon Cyber", "Ocean Deep", "Emerald Rich").forEach { themeName ->
                        val isSelected = activeColorTheme == themeName
                        Button(
                            onClick = { viewModel.setThemeColor(themeName) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) primaryAccent else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(themeName.take(10), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("API & Secrets Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                OutlinedTextField(
                    value = geminiApiKeyInput,
                    onValueChange = { geminiApiKeyInput = it },
                    label = { Text("Gemini API Key Status") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryAccent) }
                )
                Text(
                    text = "Note: For optimal security, API keys must be set inside the AI Studio Secrets panel. Do not paste keys inside configuration files.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Studio Cache & Storage", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Cache Occupied:", fontWeight = FontWeight.Bold)
                        Text(cacheSize, style = MaterialTheme.typography.headlineSmall, color = primaryAccent, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.clearStudioCache()
                            Toast.makeText(context, "All voice cache cleaned up!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clean Cache")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Studio System Notifications", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Vocal Generation Push Alerts", fontWeight = FontWeight.Bold)
                        Text("Alert when exporting operations finish in background.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(checked = showNotifications, onCheckedChange = { showNotifications = it })
                }
            }
        }
    }
}

// ==========================================
// 9. USER PROFILE PAGE
// ==========================================
@Composable
fun UserProfilePage(viewModel: TtsViewModel, primaryAccent: Color) {
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userSubscription by viewModel.userSubscription.collectAsState()
    val charactersLimit by viewModel.charactersLimit.collectAsState()
    val charactersUsed by viewModel.charactersUsed.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(userName) }
    var editEmail by remember { mutableStateOf(userEmail) }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("My Profile Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = primaryAccent)
        Text("Manage credentials, check studio character quota consumption, and review subscription benefits.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar Icon Placeholder styled beautifully
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(primaryAccent, MaterialTheme.colorScheme.secondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(userSubscription, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = primaryAccent) },
                            border = BorderStroke(1.dp, primaryAccent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!isEditing) {
                    Button(
                        onClick = {
                            editName = userName
                            editEmail = userEmail
                            isEditing = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Personal Details")
                    }
                } else {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (editName.isNotBlank() && editEmail.isNotBlank()) {
                                    viewModel.updateProfile(editName, editEmail)
                                    isEditing = false
                                    Toast.makeText(context, "Profile details updated successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Fields cannot be blank.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        // Quota Progress Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Studio Character Quota Usage", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
                Text("Characters consumed are accumulated from Text to Speech conversions.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 16.dp))

                val fraction = if (charactersLimit > 0) charactersUsed.toFloat() / charactersLimit.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = primaryAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${String.format("%,d", charactersUsed)} / ${String.format("%,d", charactersLimit)} used", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${String.format("%.1f", (1f - fraction) * 100)}% remaining", color = primaryAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Plan & Subscription Billing Details
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Subscription & Billing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Tier: $userSubscription", fontWeight = FontWeight.Bold)
                        Text("Next billing renewal: Aug 15, 2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }

                    Text("$19.99/mo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = primaryAccent)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Benefits Included in Your Plan:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = primaryAccent)
                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    "High fidelity vocal outputs with Gemini Engine",
                    "Custom voice training parameters",
                    "Unlimited MP3, WAV, FLAC file exports",
                    "History cloud sync across workspace sessions",
                    "Noise cancellation, echo filter enhancement tools"
                ).forEach { benefit ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(benefit, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
