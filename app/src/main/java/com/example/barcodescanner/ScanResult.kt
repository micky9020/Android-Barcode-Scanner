package com.example.barcodescanner

import com.google.mlkit.vision.barcode.common.Barcode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Resultado de una lectura de código. */
data class ScanResult(
    val value: String,
    val format: String,
    val isUrl: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    val timeLabel: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    companion object {
        fun from(barcode: Barcode): ScanResult {
            val value = barcode.rawValue.orEmpty()
            val isUrl = barcode.valueType == Barcode.TYPE_URL ||
                value.startsWith("http://", true) || value.startsWith("https://", true)
            return ScanResult(value, formatName(barcode.format), isUrl)
        }

        fun formatName(format: Int): String = when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "Aztec"
            else -> "Desconocido"
        }
    }
}
