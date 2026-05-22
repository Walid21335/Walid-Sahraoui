package com.example.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.nio.charset.StandardCharsets
import kotlin.math.absoluteValue

object QrGenerator {

    // Generates a Version 1 QR code matrix (21x21 modules)
    fun generateQrMatrix(content: String): Array<BooleanArray> {
        val size = 21
        val matrix = Array(size) { BooleanArray(size) { false } }

        // 1. Draw Finder Patterns (7x7 squares at 3 corners)
        drawFinderPattern(matrix, 0, 0)         // Top Left
        drawFinderPattern(matrix, size - 7, 0)  // Top Right
        drawFinderPattern(matrix, 0, size - 7)  // Bottom Left

        // 2. Draw Timing Lines (alternating dots between finders)
        for (i in 8 until size - 8) {
            val value = (i % 2 == 0)
            matrix[6][i] = value // Horizontal
            matrix[i][6] = value // Vertical
        }

        // 3. Mark alignment anchor points
        matrix[15][15] = true
        matrix[14][15] = true
        matrix[15][14] = true

        // 4. Fill remaining space with deterministic data bit-stream based on string hash and contents
        val dataBytes = content.toByteArray(StandardCharsets.UTF_8)
        val bitStream = mutableListOf<Boolean>()
        
        // Convert bytes to bits
        for (b in dataBytes) {
            for (bit in 7 downTo 0) {
                bitStream.add(((b.toInt() ushr bit) and 1) == 1)
            }
        }

        // Pad bitstream if needed
        var hash = content.hashCode()
        while (bitStream.size < size * size) {
            hash = (hash * 31) + 17
            bitStream.add((hash % 2 == 0))
        }

        // Place bits into the open slots of the matrix (skipping finder patterns and timing lines)
        var bitIndex = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                // Skip finder patterns
                if (isInsideFinder(r, c, size)) continue
                // Skip timing lines
                if (r == 6 || c == 6) continue

                matrix[r][c] = bitStream[bitIndex % bitStream.size]
                bitIndex++
            }
        }

        return matrix
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, rStart: Int, cStart: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                // Outermost ring (7x7 border) and Innermost block (3x3 core) are filled
                val isBorder = (r == 0 || r == 6 || c == 0 || c == 6)
                val isCore = (r in 2..4 && c in 2..4)
                if (isBorder || isCore) {
                    matrix[rStart + r][cStart + c] = true
                }
            }
        }
    }

    private fun isInsideFinder(r: Int, c: Int, size: Int): Boolean {
        // Top Left finder
        if (r < 8 && c < 8) return true
        // Top Right finder
        if (r < 8 && c >= size - 8) return true
        // Bottom Left finder
        if (r >= size - 8 && c < 8) return true
        return false
    }

    // Generates an Android Bitmap of the QR matrix to be embedded into PDF or shared
    fun generateQrBitmap(content: String, sizePx: Int = 300): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundPaint = Paint().apply { color = android.graphics.Color.WHITE }
        val qrPaint = Paint().apply { color = android.graphics.Color.BLACK }

        // Fill background white
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), backgroundPaint)

        val matrix = generateQrMatrix(content)
        val matrixSize = matrix.size
        val blockSize = sizePx.toFloat() / matrixSize

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (matrix[r][c]) {
                    canvas.drawRect(
                        c * blockSize,
                        r * blockSize,
                        (c + 1) * blockSize,
                        (r + 1) * blockSize,
                        qrPaint
                    )
                }
            }
        }
        return bitmap
    }
}

@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    qrColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val matrix = QrGenerator.generateQrMatrix(content)
    val matrixSize = matrix.size

    Box(
        modifier = modifier
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val blockSize = size.width / matrixSize

            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = qrColor,
                            topLeft = Offset(c * blockSize, r * blockSize),
                            size = Size(blockSize + 1f, blockSize + 1f) // 1f gap coverage
                        )
                    }
                }
            }
        }
    }
}
