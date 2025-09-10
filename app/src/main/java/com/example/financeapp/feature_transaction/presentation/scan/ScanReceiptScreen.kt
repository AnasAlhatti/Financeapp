package com.example.financeapp.feature_transaction.presentation.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resumeWithException

// ---------- Data ----------
data class ParsedReceipt(
    val merchant: String? = null,
    val dateMillis: Long? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val categorySuggestion: String? = null,
    val rawLines: List<String> = emptyList()
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    onBack: () -> Unit,
    onParsed: (ParsedReceipt) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // CameraX state
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // UI state
    var flashEnabled by remember { mutableStateOf(false) }
    var gridEnabled by remember { mutableStateOf(false) }

    var isCapturing by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var capturedFile by remember { mutableStateOf<File?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var parsedPreview by remember { mutableStateOf<ParsedReceipt?>(null) } // dialog preview

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Camera permission
    val cameraPermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermissionGranted.value = granted }

    // Pick from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val temp = File(context.cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { out -> input.copyTo(out) }
            }
            capturedFile = temp // reuse file handling
        }
    }

    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    // Ask permission if not granted
    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted.value) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Bind camera when permission granted and preview ready
    LaunchedEffect(cameraPermissionGranted.value, previewView) {
        if (!cameraPermissionGranted.value || previewView == null) return@LaunchedEffect
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView!!.surfaceProvider
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(previewView!!.display.rotation)
            .build()
        val selector = CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
        boundCamera = camera
        runCatching { boundCamera?.cameraControl?.enableTorch(flashEnabled) }

        imageCapture = capture
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Receipt", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Gallery import
                    IconButton(
                        enabled = capturedFile == null,
                        onClick = { galleryLauncher.launch("image/*") }
                    ) {
                        Icon(Icons.Outlined.InsertPhoto, contentDescription = "Import")
                    }
                    // Grid toggle
                    IconButton(onClick = { gridEnabled = !gridEnabled }) {
                        Icon(
                            if (gridEnabled) Icons.Filled.GridOn else Icons.Outlined.GridOff,
                            contentDescription = "Grid"
                        )
                    }
                    // Flash toggle
                    val hasTorch = boundCamera?.cameraInfo?.hasFlashUnit() == true
                    IconButton(
                        enabled = cameraPermissionGranted.value && capturedFile == null && hasTorch,
                        onClick = {
                            flashEnabled = !flashEnabled
                            boundCamera?.cameraControl?.enableTorch(flashEnabled)
                        }
                    ) {
                        Icon(
                            if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Flash"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    )
                )
            )
        },
        bottomBar = {
            BottomActionBarSimple(
                hasPhoto = capturedFile != null,
                isProcessing = isProcessing,
                onCapture = {
                    val cap = imageCapture ?: return@BottomActionBarSimple
                    val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                    isCapturing = true
                    cap.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                isCapturing = false
                                capturedFile = file
                            }
                            override fun onError(exception: ImageCaptureException) {
                                isCapturing = false
                                errorMsg = exception.message ?: "Capture failed."
                            }
                        }
                    )
                },
                onRetake = {
                    capturedFile = null
                    errorMsg = null
                },
                onExtract = {
                    val f: File = capturedFile ?: return@BottomActionBarSimple
                    scope.launch {
                        isProcessing = true
                        errorMsg = null
                        try {
                            val parsed = withContext(Dispatchers.IO) {
                                extractReceiptFromFile(context, f)
                            }
                            parsedPreview = parsed
                        } catch (e: Exception) {
                            errorMsg = e.message ?: "Failed to process receipt."
                        }
                        isProcessing = false
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Camera preview
            if (cameraPermissionGranted.value) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission is required.")
                }
            }

            // Overlay guides
            ReceiptOverlay()

            // 3×3 Grid
            if (gridEnabled && capturedFile == null) {
                GridOverlay()
            }

            // Captured preview (EXIF upright)
            AnimatedVisibility(
                visible = capturedFile != null,
                enter = fadeIn(), exit = fadeOut()
            ) {
                val bmp = remember(capturedFile) {
                    capturedFile?.let { f -> decodeBitmapRespectExif(f) }
                }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Processing or capture indicator
            if (isCapturing || isProcessing) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isProcessing) "Reading receipt…" else "Capturing…",
                            color = Color.White
                        )
                    }
                }
            }

            // Error
            errorMsg?.let {
                Box(Modifier.fillMaxSize()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 92.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }

    // OCR preview dialog
    parsedPreview?.let { parsed ->
        AlertDialog(
            onDismissRequest = { parsedPreview = null },
            title = { Text("Receipt detected") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fun f(v: Any?) = v?.toString() ?: "—"
                    Text("Merchant: ${f(parsed.merchant)}")
                    Text("Date: ${
                        parsed.dateMillis?.let {
                            val df = android.text.format.DateFormat.getMediumDateFormat(context)
                            df.format(java.util.Date(it))
                        } ?: "—"
                    }")
                    Text("Amount: ${f(parsed.amount)} ${parsed.currency ?: ""}")
                    parsed.categorySuggestion?.let { Text("Suggested category: $it") }
                    Spacer(Modifier.height(8.dp))
                    Text("Detected text:", style = MaterialTheme.typography.labelMedium)
                    ElevatedCard {
                        Column(Modifier.padding(8.dp)) {
                            parsed.rawLines.take(12).forEach { l -> Text(l) }
                            if (parsed.rawLines.size > 12) Text("…")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    parsedPreview = null
                    onParsed(parsed)
                }) { Text("Use") }
            },
            dismissButton = {
                TextButton(onClick = { parsedPreview = null }) { Text("Close") }
            }
        )
    }
}

/** Simple bottom bar without aspect ratio controls. */
@Composable
private fun BottomActionBarSimple(
    hasPhoto: Boolean,
    isProcessing: Boolean,
    onCapture: () -> Unit,
    onRetake: () -> Unit,
    onExtract: () -> Unit
) {
    Surface(tonalElevation = 6.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hasPhoto) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onCapture, enabled = !isProcessing) {
                        Text("Shoot", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            } else {
                OutlinedButton(onClick = onRetake, enabled = !isProcessing) { Text("Retake") }
                Button(onClick = onExtract, enabled = !isProcessing) {
                    Icon(Icons.Outlined.InsertPhoto, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Extract")
                }
            }
        }
    }
}

@Composable
private fun ReceiptOverlay() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun GridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val stroke = 1.dp.toPx()
        val gridColor = Color.White.copy(alpha = 0.35f)

        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), stroke)
        drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), stroke)
        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), stroke)
        drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), stroke)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private suspend fun extractReceiptFromFile(
    context: android.content.Context,
    file: File
): ParsedReceipt {
    val image = com.google.mlkit.vision.common.InputImage.fromFilePath(
        context,
        android.net.Uri.fromFile(file)
    )
    val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
        com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
    )
    val result = recognizer.process(image).await()
    val lines = result.text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    val parsed = parseReceiptLines(lines)
    return parsed.copy(rawLines = lines)
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) { cause, _, _ -> null?.let { it1 -> it1(cause) } } }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }

@RequiresApi(Build.VERSION_CODES.O)
private fun parseReceiptLines(lines: List<String>): ParsedReceipt {
    // Join for global regexes, but keep lines for context-based picks.
    val joined = lines.joinToString("\n")

    // --- DATE: handle TR & EN styles: dd/MM/yyyy, dd.MM.yyyy, yyyy-MM-dd ---
    val dateRegexes = listOf(
        Regex("""\b(\d{1,2})[\/\.](\d{1,2})[\/\.](\d{4})\b"""),       // 01/08/2025 or 01.08.2025
        Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""")                  // 2025-08-01
    )
    val dateMillis: Long? = run {
        for (rx in dateRegexes) {
            val m = rx.find(joined)
            if (m != null) {
                val parts = m.groupValues.drop(1).map { it.toInt() }
                val (y, mth, d) = when (rx) {
                    dateRegexes[1] -> Triple(parts[0], parts[1], parts[2]) // yyyy-MM-dd
                    else -> Triple(parts[2], parts[1], parts[0])           // dd/MM/yyyy or dd.MM.yyyy
                }
                return@run runCatching {
                    val ld = java.time.LocalDate.of(y, mth, d)
                    ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.getOrNull()
            }
        }
        null
    }

    // --- CURRENCY: TRY (₺, TL), EUR (€), USD ($), AED ---
    val currency = when {
        joined.contains("₺") || joined.contains(" TL", true) || joined.contains("TRY", true) -> "TRY"
        joined.contains("€")  || joined.contains("EUR", true)                                 -> "EUR"
        joined.contains("$")  || joined.contains("USD", true)                                 -> "USD"
        joined.contains("AED", true)                                                          -> "AED"
        else -> null
    }

    // --- AMOUNT: Prefer lines near total keywords (TR & EN) ---
    val totalWords = listOf(
        // EN
        "total", "grand total", "subtotal", "amount", "sum", "balance",
        // TR
        "toplam", "genel toplam", "ara toplam", "tutar", "ödeme"
    )
    val taxWords = listOf("tax", "kdv", "vergi")
    val amountRegex = Regex("""(?:TL|₺|\$|€)?\s*\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2})\s*(?:TL|₺|\$|€)?""")

    val totalIdx = lines.indexOfFirst { line ->
        val l = line.lowercase()
        totalWords.any { it in l }
    }

    val amount: Double? = run {
        val candidates = buildList {
            if (totalIdx >= 0) {
                add(lines[totalIdx])
                if (totalIdx + 1 < lines.size) add(lines[totalIdx + 1])
                if (totalIdx - 1 >= 0) add(lines[totalIdx - 1])
            }
            // fallback: scan all lines that don't look like tax-only
            addAll(lines.filterNot { l -> taxWords.any { it in l.lowercase() } })
        }
        candidates.firstNotNullOfOrNull { line ->
            amountRegex.find(line)?.value?.let(::normalizeAmount)
        }
    }

    // --- Merchant: first meaningful line (skip tax/total/keywords) ---
    val merchant = lines.firstOrNull { line ->
        val l = line.lowercase()
        line.length in 3..40 &&
                line.any { it.isLetter() } &&
                totalWords.none { it in l } &&
                taxWords.none { it in l } &&
                !l.contains("fatura") && !l.contains("fiş") &&
                !l.contains("invoice") && !l.contains("receipt")
    }

    // --- Category (very simple heuristics, TR + EN) ---
    val category = when {
        // Food & Grocery
        joined.contains("market", true) || joined.contains("grocery", true) ||
                joined.contains("bim", true) || joined.contains("migros", true) ||
                joined.contains("carrefour", true) || joined.contains("şok", true) -> "Food"

        // Transport
        joined.contains("uber", true) || joined.contains("taxi", true) ||
                joined.contains("dolmuş", true) || joined.contains("otobüs", true) ||
                joined.contains("metro", true) || joined.contains("taksi", true) -> "Transport"

        // Shopping / Retail
        joined.contains("mall", true) || joined.contains("store", true) ||
                joined.contains("avm", true) || joined.contains("mağaza", true)    -> "Shopping"

        else -> null
    }

    return ParsedReceipt(
        merchant = merchant,
        dateMillis = dateMillis,
        amount = amount,
        currency = currency,
        categorySuggestion = category
    )
}

/** Convert "₺1.234,56", "1,234.56 TL", "$12.34", "12,34" → 1234.56 */
private fun normalizeAmount(raw: String): Double? {
    // strip spaces and currency labels; keep separators
    val cleaned = raw
        .replace("TL", "", ignoreCase = true)
        .replace("TRY", "", ignoreCase = true)
        .replace("EUR", "", ignoreCase = true)
        .replace("USD", "", ignoreCase = true)
        .replace("AED", "", ignoreCase = true)
        .replace("₺", "")
        .replace("€", "")
        .replace("$", "")
        .trim()

    // keep only digits and separators
    val numeric = cleaned.replace("[^0-9,\\.]".toRegex(), "")

    // Detect decimal separator by last occurrence
    val lastDot = numeric.lastIndexOf('.')
    val lastComma = numeric.lastIndexOf(',')
    val decimalSep = if (lastComma > lastDot) ',' else '.'

    val normalized = if (decimalSep == ',') {
        // e.g. 1.234,56 -> remove thousands dots, turn comma into dot
        numeric.replace(".", "").replace(',', '.')
    } else {
        // e.g. 1,234.56 -> remove thousands commas, keep dot
        numeric.replace(",", "")
    }

    return normalized.toDoubleOrNull()
}

private fun decodeBitmapRespectExif(file: File, maxSize: Int = 2048): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, opts)
    val (w, h) = opts.outWidth to opts.outHeight
    if (w <= 0 || h <= 0) return null

    var inSample = 1
    while (w / inSample > maxSize || h / inSample > maxSize) inSample *= 2

    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
    val src = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null

    val exif = ExifInterface(file.absolutePath)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
        }
    }
    return if (!matrix.isIdentity) {
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true).also {
            if (it != src) src.recycle()
        }
    } else src
}
