package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisBackgroundDark
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceVariantDark
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun GenerativeStudioScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedStudioTab by remember { mutableIntStateOf(0) }
    val isProcessing by viewModel.isProcessing.collectAsState()
    val creations by viewModel.creations.collectAsState()

    val studioTabs = listOf("MUSIC", "IMAGES", "VEO VIDEO", "TRANSCRIBE", "GALLERY")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackgroundDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STARK GENERATIVE MEDIA MATRIX",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisCyan
                )
                Text(
                    text = "LYRIA • GEMINI 3.1 • VEO 3 • TRANSCRIBE",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSecondary
                )
            }

            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = JarvisCyan,
                    strokeWidth = 2.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Studio Tabs
        TabRow(
            selectedTabIndex = selectedStudioTab,
            containerColor = JarvisSurfaceDark,
            contentColor = JarvisCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedStudioTab]),
                    color = JarvisCyan
                )
            }
        ) {
            studioTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedStudioTab == index,
                    onClick = { selectedStudioTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedStudioTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedStudioTab == index) JarvisCyan else JarvisTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("studio_tab_$title")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedStudioTab) {
            0 -> MusicStudioPane(viewModel)
            1 -> ImageStudioPane(viewModel)
            2 -> VeoVideoStudioPane(viewModel)
            3 -> AudioTranscribePane(viewModel)
            4 -> CreationsGalleryPane(viewModel)
        }
    }
}

// 1. Music Studio (lyria-3-clip-preview / lyria-3-pro-preview)
@Composable
fun MusicStudioPane(viewModel: JarvisViewModel) {
    var musicPrompt by remember { mutableStateOf("Epic cinematic Stark Industries battle theme, pulsing synthesizers, heavy brass, and high-tech percussion") }
    var isProTrack by remember { mutableStateOf(false) } // clip (up to 30s) vs full track
    val latestResult by viewModel.latestGeneration.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisCardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lyria Acoustic Generator",
                            style = MaterialTheme.typography.bodyLarge,
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generates original studio-quality musical compositions using Lyria 3 models.",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Model Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isProTrack,
                            onClick = { isProTrack = false },
                            label = { Text("lyria-3-clip-preview (Clip ≤30s)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isProTrack,
                            onClick = { isProTrack = true },
                            label = { Text("lyria-3-pro-preview (Full Track)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisGold,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = musicPrompt,
                        onValueChange = { musicPrompt = it },
                        label = { Text("Musical Atmosphere & Composition Prompt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_prompt_input"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateMusic(musicPrompt, isProTrack) },
                        enabled = !isProcessing && musicPrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("generate_music_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isProcessing) "SYNTHESIZING TRACK..." else "GENERATE MUSIC VIA LYRIA",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Playback Card if generated
        latestResult?.let { result ->
            if (result.mimeType.startsWith("audio/")) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2236)),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(JarvisGreen)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ACOUSTIC COMPOSITION READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = JarvisCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Synthesized Audio Stream Active", color = JarvisCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. Image Studio (gemini-3.1-flash-image-preview)
@Composable
fun ImageStudioPane(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    var imagePrompt by remember { mutableStateOf("Futuristic holographic Iron Man Mark 85 cybernetic helmet blueprint with glowing neon cyan wireframes") }
    var selectedAspect by remember { mutableStateOf("1:1") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val isProcessing by viewModel.isProcessing.collectAsState()
    val latestResult by viewModel.latestGeneration.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                selectedBitmap = BitmapFactory.decodeStream(inputStream)
            } catch (_: Exception) {}
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisCardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini 3.1 Flash Image Studio",
                            style = MaterialTheme.typography.bodyLarge,
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create new images from text or upload an existing photo to edit it with AI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Aspect ratio chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("1:1" to "Square", "16:9" to "Landscape", "9:16" to "Portrait").forEach { (ratio, label) ->
                            FilterChip(
                                selected = selectedAspect == ratio,
                                onClick = { selectedAspect = ratio },
                                label = { Text("$ratio ($label)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JarvisCyan,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = imagePrompt,
                        onValueChange = { imagePrompt = it },
                        label = { Text(if (selectedBitmap != null) "Edit Instructions for Uploaded Image" else "Image Generation Prompt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_prompt_input"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Optional Image Upload for Editing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder)
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = JarvisCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedBitmap != null) "IMAGE LOADED (EDIT MODE)" else "UPLOAD PHOTO TO EDIT",
                                color = JarvisCyan,
                                fontSize = 11.sp
                            )
                        }

                        if (selectedBitmap != null) {
                            Text(
                                text = "CLEAR",
                                color = JarvisRed,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { selectedBitmap = null }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateOrEditImage(imagePrompt, selectedBitmap, selectedAspect) },
                        enabled = !isProcessing && imagePrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("render_image_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isProcessing) "RENDERING PIXELS..." else if (selectedBitmap != null) "EDIT IMAGE (GEMINI 3.1)" else "CREATE IMAGE (GEMINI 3.1)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Display generated image preview
        latestResult?.let { result ->
            if (result.mimeType.startsWith("image/") && result.mediaData != null) {
                item {
                    val decodedBitmap = remember(result.mediaData) {
                        try {
                            val bytes = Base64.decode(result.mediaData, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    decodedBitmap?.let { bmp ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(JarvisCyan)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "RENDERED VISUAL ARTIFACT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisCyan
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Generated Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. Veo 3 Video Studio (veo-3.1-fast-generate-preview)
@Composable
fun VeoVideoStudioPane(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    var videoPrompt by remember { mutableStateOf("Hyper-realistic flight sequence of Iron Man Mark 85 soaring at supersonic speed above neon cybernetic skyscrapers, cinematic lighting, photorealistic") }
    var selectedAspect by remember { mutableStateOf("16:9") } // "16:9" or "9:16"
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val isProcessing by viewModel.isProcessing.collectAsState()
    val latestResult by viewModel.latestGeneration.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                sourceBitmap = BitmapFactory.decodeStream(inputStream)
            } catch (_: Exception) {}
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisCardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Veo 3 Video Generation",
                            style = MaterialTheme.typography.bodyLarge,
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generate high-definition video from text or animate an uploaded photo using model veo-3.1-fast-generate-preview in 16:9 or 9:16 aspect ratios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Aspect ratio selector (16:9 landscape vs 9:16 portrait)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedAspect == "16:9",
                            onClick = { selectedAspect = "16:9" },
                            label = { Text("16:9 (Landscape)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedAspect == "9:16",
                            onClick = { selectedAspect = "9:16" },
                            label = { Text("9:16 (Portrait)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisGold,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = videoPrompt,
                        onValueChange = { videoPrompt = it },
                        label = { Text(if (sourceBitmap != null) "Motion & Animation Directives for Photo" else "Video Scene Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_prompt_input"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Animate Photo Upload
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder)
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = JarvisCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sourceBitmap != null) "PHOTO LOADED (ANIMATION MODE)" else "UPLOAD PHOTO TO ANIMATE",
                                color = JarvisCyan,
                                fontSize = 11.sp
                            )
                        }

                        if (sourceBitmap != null) {
                            Text(
                                text = "CLEAR",
                                color = JarvisRed,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { sourceBitmap = null }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateVideo(videoPrompt, sourceBitmap, selectedAspect) },
                        enabled = !isProcessing && videoPrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("generate_veo_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isProcessing) "DISPATCHING TO VEO 3..." else if (sourceBitmap != null) "ANIMATE IMAGE (VEO 3)" else "GENERATE VIDEO (VEO 3)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        latestResult?.let { result ->
            if (result.mimeType == "video/mp4") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF102339)),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(JarvisGreen)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "VEO 3 GENERATION INITIATED",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// 4. Audio Transcription (gemini-3.5-transcribe)
@Composable
fun AudioTranscribePane(viewModel: JarvisViewModel) {
    val isProcessing by viewModel.isProcessing.collectAsState()
    val transcriptResult by viewModel.transcriptionResult.collectAsState()
    val isListening by viewModel.speechManager.isListening.collectAsState()
    val liveTranscript by viewModel.speechManager.recognizedTranscript.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisCardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini 3.5 Transcribe Subsystem",
                            style = MaterialTheme.typography.bodyLarge,
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Speak into microphone or capture acoustic telemetry to receive verbatim text transcription via model gemini-3.5-transcribe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (isListening) {
                                viewModel.stopVoiceListening()
                                if (liveTranscript.isNotBlank()) {
                                    val simulatedWavBase64 = Base64.encodeToString(liveTranscript.toByteArray(), Base64.NO_WRAP)
                                    viewModel.transcribeAudioData(simulatedWavBase64)
                                }
                            } else {
                                viewModel.startVoiceListening()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) JarvisRed else JarvisCyan
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("transcribe_mic_button")
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isListening) "STOP & TRANSCRIBE AUDIO" else "RECORD AUDIO FOR TRANSCRIBE",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        transcriptResult?.let { text ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E32)),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(JarvisCyan)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "VERBATIM TRANSCRIPTION (GEMINI 3.5 TRANSCRIBE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = JarvisTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// 5. Saved Creations Gallery
@Composable
fun CreationsGalleryPane(viewModel: JarvisViewModel) {
    val creations by viewModel.creations.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (creations.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved media creations found in cloud database.",
                        color = JarvisTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(creations) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(JarvisCardBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${record.type} // ${record.model}",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = record.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisGreen,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = record.prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisTextPrimary
                        )
                    }
                }
            }
        }
    }
}
