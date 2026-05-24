package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.ScanRecord
import com.example.ui.ScanViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.TypeBadge
import com.example.ui.theme.DarkPurple
import com.example.ui.theme.ElectricBlue
import com.example.utils.ParsedQR
import com.example.utils.QRParser
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeResult by viewModel.activeScanResult.collectAsState()
    val isFlashlightOn by viewModel.isFlashlightOn.collectAsState()
    val isVibeEnabled by viewModel.isVibrationEnabled.collectAsState()
    val isAutoOpenEnabled by viewModel.isAutomaticOpenEnabled.collectAsState()
    val galleryError by viewModel.galleryScanError.collectAsState()

    // 1. Check Camera Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // 2. Photo picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanImageUri(context, uri)
        }
    }

    // Handle gallery errors
    LaunchedEffect(galleryError) {
        galleryError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearGalleryError()
        }
    }

    // Automatic Open logic if enabled
    LaunchedEffect(activeResult) {
        activeResult?.let { record ->
            if (isAutoOpenEnabled && record.qrType == "LINK") {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(record.rawContent))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No se pudo abrir el enlace automáticamente", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialColorBackground()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasCameraPermission) {
                // Permission request screen
                PermissionRequestLayout {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            } else {
                // Cameras running
                CameraScannerLayout(
                    viewModel = viewModel,
                    isFlashlightOn = isFlashlightOn,
                    onGalleryClick = { galleryLauncher.launch("image/*") }
                )

                // Top Controls (Flashlight, Vibe, Auto-Open)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flashlight button
                    ControlIconButton(
                        icon = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flashlight",
                        isActive = isFlashlightOn,
                        onToggle = { viewModel.toggleFlashlight() }
                    )

                    // Floating Glass controller for toggles
                    GlassCard(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        cornerRadius = 16.dp,
                        elevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Vibration control
                            Row(
                                modifier = Modifier
                                    .clickable { viewModel.toggleVibration() }
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isVibeEnabled) Icons.Default.Vibration else Icons.Default.VolumeMute,
                                    contentDescription = "Vibe",
                                    tint = if (isVibeEnabled) ElectricBlue else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Vibrar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVibeEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                                )
                            }

                            // Dynamic auto open toggle
                            Row(
                                modifier = Modifier
                                    .clickable { viewModel.toggleAutomaticOpen() }
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAutoOpenEnabled) Icons.Default.Launch else Icons.Default.DoNotDisturbOn,
                                    contentDescription = "Auto Open",
                                    tint = if (isAutoOpenEnabled) DarkPurple else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Auto-abrir",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAutoOpenEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                                )
                            }
                        }
                    }

                    // Gallery scan button
                    ControlIconButton(
                        icon = Icons.Default.Image,
                        contentDescription = "Scan Gallery Image",
                        isActive = false,
                        onToggle = { galleryLauncher.launch("image/*") }
                    )
                }

                // Beautiful viewfinder overlay centered
                CameraViewfinder()

                // Bottom Scan detail overlay if a code is actively scanned
                AnimatedVisibility(
                    visible = activeResult != null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    ),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    activeResult?.let { record ->
                        ResultDetailsCard(
                            record = record,
                            onDismiss = { viewModel.dismissActiveScan() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    val containerColor = if (isActive) Color(0xFFFFD600) else Color(0x33000000)
    val iconColor = if (isActive) Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun MaterialColorBackground(): Color {
    return MaterialTheme.colorScheme.background
}

@Composable
fun PermissionRequestLayout(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Futuristic QR Icon with gradient background
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(DarkPurple, ElectricBlue, DarkPurple)
                    )
                )
                .padding(2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Logo",
                tint = DarkPurple,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Acceso a Cámara Requerido",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "BerMatScanner necesita permisos de tu cámara para poder detectar códigos QR en tiempo real de forma instantánea.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        GradientButton(
            text = "Conceder Permiso",
            onClick = onRequest,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("grant_permission_button")
        )
    }
}

@Composable
fun CameraScannerLayout(
    viewModel: ScanViewModel,
    isFlashlightOn: Boolean,
    onGalleryClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Set up Preview
    val preview = remember { Preview.Builder().build() }
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var activeCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    LaunchedEffect(key1 = isFlashlightOn) {
        try {
            activeCamera?.cameraControl?.enableTorch(isFlashlightOn)
        } catch (e: Exception) {
            Log.e("Scanner", "Error settings torch: ${e.localizedMessage}")
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    val scanner = BarcodeScanning.getClient()
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            if (barcodes.isNotEmpty()) {
                                barcodes[0].rawValue?.let { raw ->
                                    viewModel.onQRDetected(raw)
                                }
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                activeCamera = camera
                camera.cameraControl.enableTorch(isFlashlightOn)
            } catch (e: Exception) {
                Log.e("ScannerScreen", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraExecutor.shutdown()
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Embed PreviewView inside AndroidView
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun CameraViewfinder() {
    val infiniteTransition = rememberInfiniteTransition(label = "viewfinder")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Standard Scanning frame indicator
        Box(
            modifier = Modifier
                .size(260.dp)
                .border(2.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                .padding(2.dp)
        ) {
            // Live scanning glowing framing corner marks
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Animated red/purple line shifting up and down
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.015f)
                        .align(Alignment.TopCenter)
                        .offset(y = (240 * laserYOffset).dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, ElectricBlue, DarkPurple, ElectricBlue, Color.Transparent)
                            )
                        )
                )

                // Render elegant corner brackets
                CornerBrackets()
            }
        }

        // Action instructions printed above viewfinder
        Text(
            text = "Encuadra el código QR para escanear",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-160).dp)
                .background(Color(0x99000000), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CornerBrackets() {
    // Design sharp high tech corners using small custom vectors or nested borders is elegant
    // We can draw beautiful high tech corners inside a Box container
    Box(modifier = Modifier.fillMaxSize()) {
        val cornerSize = 24.dp
        val strokeWidth = 4.dp
        val cornerColor = ElectricBlue

        // Top Left
        Box(modifier = Modifier
            .size(cornerSize)
            .align(Alignment.TopStart)
            .border(
                BorderStroke(strokeWidth, cornerColor),
                shape = RoundedCornerShape(topStart = 12.dp)
            )
            .clip(RoundedCornerShape(topStart = 12.dp))
        )
        // Top Right
        Box(modifier = Modifier
            .size(cornerSize)
            .align(Alignment.TopEnd)
            .border(
                BorderStroke(strokeWidth, cornerColor),
                shape = RoundedCornerShape(topEnd = 12.dp)
            )
            .clip(RoundedCornerShape(topEnd = 12.dp))
        )
        // Bottom Left
        Box(modifier = Modifier
            .size(cornerSize)
            .align(Alignment.BottomStart)
            .border(
                BorderStroke(strokeWidth, cornerColor),
                shape = RoundedCornerShape(bottomStart = 12.dp)
            )
            .clip(RoundedCornerShape(bottomStart = 12.dp))
        )
        // Bottom Right
        Box(modifier = Modifier
            .size(cornerSize)
            .align(Alignment.BottomEnd)
            .border(
                BorderStroke(strokeWidth, cornerColor),
                shape = RoundedCornerShape(bottomEnd = 12.dp)
            )
            .clip(RoundedCornerShape(bottomEnd = 12.dp))
        )
    }
}

@Composable
fun ResultDetailsCard(
    record: ScanRecord,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val parsedResult = remember(record.rawContent) { QRParser.parse(record.rawContent) }
    val formattedTime = remember(record.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        sdf.format(Date(record.timestamp))
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag("result_details_card"),
        cornerRadius = 24.dp,
        elevation = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypeBadge(type = record.qrType)
            
            // Closing X
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.61f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content Display Zone
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp)
                .verticalScroll(rememberScrollState())
        ) {
            renderParsedResult(parsedResult = parsedResult, rawContent = record.rawContent)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Action Suite
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Copy Action
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(record.rawContent))
                    Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("copy_result_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            // Share Action
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, record.rawContent)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir resultado"))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            // Primary Target Action depending on QR category
            val primaryActionLabel = when (parsedResult) {
                is ParsedQR.Link -> "Abrir Link"
                is ParsedQR.Wifi -> "Copiar Clave" // Or copy SSID/Settings easily
                is ParsedQR.Whatsapp -> "Enviar WA"
                is ParsedQR.Yape -> "Yapear"
                is ParsedQR.Phone -> "Llamar"
                is ParsedQR.Email -> "Redactar"
                else -> "Copiar Todo"
            }

            Button(
                onClick = {
                    performPrimaryAction(context, parsedResult, record.rawContent, clipboardManager)
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(44.dp)
                    .testTag("primary_action_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                val icon = when (parsedResult) {
                    is ParsedQR.Link -> Icons.Default.OpenInBrowser
                    is ParsedQR.Wifi -> Icons.Default.WifiPassword
                    is ParsedQR.Whatsapp -> Icons.Default.Message
                    is ParsedQR.Yape -> Icons.Default.AccountBalanceWallet
                    is ParsedQR.Phone -> Icons.Default.Call
                    is ParsedQR.Email -> Icons.Default.Email
                    else -> Icons.Default.Check
                }
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(primaryActionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Escaneado: $formattedTime",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun renderParsedResult(parsedResult: ParsedQR, rawContent: String) {
    when (parsedResult) {
        is ParsedQR.Yape -> {
            Column {
                Text(
                    text = "Código de Pago Yape",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rawContent,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Detectado pago rápido para billetera Yape Perú. Pulsa el botón principal para abrir o realizar transferencia.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
        is ParsedQR.Wifi -> {
            Column {
                Text(
                    text = "Configuración de Red Wi-Fi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Text("Red (SSID): ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                    Text(parsedResult.ssid, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text("Contraseña: ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                    Text(parsedResult.pass.ifEmpty { "Pública / Ninguna" }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text("Seguridad: ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                    Text(parsedResult.security.ifEmpty { "Ninguna" }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        is ParsedQR.Whatsapp -> {
            Column {
                Text(
                    text = "Contacto Directo WhatsApp",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Número: ${parsedResult.number}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                parsedResult.message?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mensaje: \"$it\"",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        is ParsedQR.Link -> {
            Column {
                Text(
                    text = "Dirección Web Detectada",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = parsedResult.url,
                    fontSize = 14.sp,
                    color = DarkPurple,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        is ParsedQR.Phone -> {
            Column {
                Text(
                    text = "Número de Teléfono",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = parsedResult.number,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkPurple
                )
            }
        }
        is ParsedQR.Email -> {
            Column {
                Text(
                    text = "Correo Electrónico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text("Destinatario: ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                    Text(parsedResult.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                parsedResult.subject?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        Text("Asunto: ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                        Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        is ParsedQR.Text -> {
            Column {
                Text(
                    text = "Texto Detectado",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = parsedResult.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

fun performPrimaryAction(
    context: Context,
    parsedResult: ParsedQR,
    rawContent: String,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    try {
        when (parsedResult) {
            is ParsedQR.Link -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(parsedResult.url))
                context.startActivity(intent)
            }
            is ParsedQR.Wifi -> {
                clipboardManager.setText(AnnotatedString(parsedResult.pass))
                Toast.makeText(context, "Contraseña copiada al portapapeles", Toast.LENGTH_SHORT).show()
            }
            is ParsedQR.Whatsapp -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawContent))
                context.startActivity(intent)
            }
            is ParsedQR.Yape -> {
                // If it is a URL, open it; otherwise look for generic payment triggers
                val trimmed = rawContent.trim()
                if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed))
                    context.startActivity(intent)
                } else {
                    // Start generic action view for Peruvian payment integration, or copy
                    clipboardManager.setText(AnnotatedString(trimmed))
                    Toast.makeText(context, "Información de pago copiada", Toast.LENGTH_SHORT).show()
                }
            }
            is ParsedQR.Phone -> {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${parsedResult.number}"))
                context.startActivity(intent)
            }
            is ParsedQR.Email -> {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${parsedResult.address}")).apply {
                    parsedResult.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                    parsedResult.body?.let { putExtra(Intent.EXTRA_TEXT, it) }
                }
                context.startActivity(intent)
            }
            is ParsedQR.Text -> {
                clipboardManager.setText(AnnotatedString(parsedResult.content))
                Toast.makeText(context, "Texto copiado al portapapeles", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "No hay ninguna aplicación instalada para procesar esta acción.", Toast.LENGTH_LONG).show()
    }
}
