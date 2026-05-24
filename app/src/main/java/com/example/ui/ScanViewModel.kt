package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ScanDatabase
import com.example.data.ScanRecord
import com.example.data.ScanRepository
import com.example.utils.ParsedQR
import com.example.utils.QRParser
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScanRepository
    val historyRecords: StateFlow<List<ScanRecord>>

    init {
        val db = ScanDatabase.getDatabase(application)
        repository = ScanRepository(db.scanDao())
        historyRecords = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // UI Configuration & Control States
    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val _isVibrationEnabled = MutableStateFlow(true)
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    private val _isAutomaticOpenEnabled = MutableStateFlow(false)
    val isAutomaticOpenEnabled: StateFlow<Boolean> = _isAutomaticOpenEnabled.asStateFlow()

    // Current Active Scanned Result (Null if none or dismissed)
    private val _activeScanResult = MutableStateFlow<ScanRecord?>(null)
    val activeScanResult: StateFlow<ScanRecord?> = _activeScanResult.asStateFlow()

    // Processing image feedback
    private val _galleryScanError = MutableStateFlow<String?>(null)
    val galleryScanError: StateFlow<String?> = _galleryScanError.asStateFlow()

    // Tracking last processed QR to prevent continuous double triggering on the same frame loop
    private var lastScannedRaw: String? = null
    private var lastScannedTimestamp: Long = 0L

    fun toggleFlashlight() {
        _isFlashlightOn.value = !_isFlashlightOn.value
    }

    fun setFlashlight(on: Boolean) {
        _isFlashlightOn.value = on
    }

    fun toggleVibration() {
        _isVibrationEnabled.value = !_isVibrationEnabled.value
    }

    fun toggleAutomaticOpen() {
        _isAutomaticOpenEnabled.value = !_isAutomaticOpenEnabled.value
    }

    fun dismissActiveScan() {
        _activeScanResult.value = null
        // Reset throttle tracking slightly later so if they scan again they can, but not immediately
        lastScannedRaw = null
    }

    fun clearGalleryError() {
        _galleryScanError.value = null
    }

    /**
     * Handles live camera frames or gallery findings
     */
    fun onQRDetected(rawContent: String) {
        val currentTime = System.currentTimeMillis()
        
        // Anti-throttle check: prevent the same code within 2 seconds
        if (rawContent == lastScannedRaw && (currentTime - lastScannedTimestamp) < 2000) {
            return
        }

        lastScannedRaw = rawContent
        lastScannedTimestamp = currentTime

        // Trigger vibration
        triggerVibration()

        // Categorize using QRParser
        val parsed = QRParser.parse(rawContent)
        val typeString = when (parsed) {
            is ParsedQR.Link -> "LINK"
            is ParsedQR.Text -> "TEXT"
            is ParsedQR.Wifi -> "WIFI"
            is ParsedQR.Phone -> "PHONE"
            is ParsedQR.Email -> "EMAIL"
            is ParsedQR.Whatsapp -> "WHATSAPP"
            is ParsedQR.Yape -> "YAPE"
        }

        val record = ScanRecord(
            rawContent = rawContent,
            qrType = typeString,
            timestamp = currentTime
        )

        // Show result dialogue/card
        _activeScanResult.value = record

        // Persist to local Room database in a background thread
        viewModelScope.launch {
            repository.insertRecord(record)
        }
    }

    /**
     * Decode from gallery Uri
     */
    fun scanImageUri(context: Context, uri: Uri) {
        _galleryScanError.value = null
        try {
            val image = InputImage.fromFilePath(context, uri)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val rawValue = barcodes[0].rawValue
                        if (!rawValue.isNullOrEmpty()) {
                            onQRDetected(rawValue)
                        } else {
                            _galleryScanError.value = "No se detectó un código QR válido en esta foto."
                        }
                    } else {
                        _galleryScanError.value = "No se encontró ningún código QR en la imagen."
                    }
                }
                .addOnFailureListener {
                    _galleryScanError.value = "Error al procesar la imagen: ${it.localizedMessage}"
                }
        } catch (e: Exception) {
            _galleryScanError.value = "Error al cargar la imagen seleccionada."
        }
    }

    private fun triggerVibration() {
        if (!_isVibrationEnabled.value) return
        val context = getApplication<Application>().applicationContext
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors on systems lacking it or permission issues
        }
    }

    fun deleteRecord(record: ScanRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
                return ScanViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
