package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.Student
import com.example.data.Exam
import com.example.data.Submission
import com.example.qr.QrGenerator
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

object PdfGenerator {

    // Generates the official electronic Exam Permit/Convocation as an elegant A4 PDF
    fun generateConvocationPdf(
        context: Context,
        student: Student,
        exams: List<Exam>
    ): File? {
        val destFile = File(context.cacheDir, "استدعاء_${student.name.replace(" ", "_")}.pdf")
        
        // standard A4 dimensions are 595 x 842 points
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Set up paints
        val textPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(13, 71, 161) // Deep blue
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = android.graphics.Color.rgb(13, 71, 161)
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        val fillPaint = Paint().apply {
            color = android.graphics.Color.rgb(240, 244, 248)
            style = Paint.Style.FILL
        }

        // Draw double border around page
        canvas.drawRect(20f, 20f, 575f, 822f, borderPaint)
        borderPaint.strokeWidth = 1f
        canvas.drawRect(25f, 25f, 570f, 817f, borderPaint)

        // Headers (Right aligned for Arabic RTL)
        canvas.drawText("الجمهورية الجزائرية الديمقراطية الشعبية", 360f, 50f, headerPaint)
        canvas.drawText("وزارة التعليم العالي والبحث العلمي", 380f, 70f, textPaint)
        canvas.drawText("منصة ســنـد للخدمات التعليمية", 400f, 88f, textPaint)

        // Left Header: Date
        val currentDate = "التاريخ: 2026-05-22"
        canvas.drawText(currentDate, 50f, 50f, textPaint)
        canvas.drawText("الحالة: مقبول إلكترونياً", 50f, 70f, textPaint)

        // Title line
        canvas.drawLine(40f, 110f, 555f, 110f, borderPaint)
        canvas.drawText("الاستدعاء الرسمي للاختبارات النهائية", 185f, 145f, titlePaint)
        canvas.drawLine(150f, 155f, 445f, 155f, borderPaint)

        // Candidate Info Block
        canvas.drawRect(40f, 180f, 555f, 320f, fillPaint)
        canvas.drawRect(40f, 180f, 555f, 320f, borderPaint)

        val colRight = 380f
        val colLeft = 70f
        canvas.drawText("الاسم واللقب الكامـل:  ${student.name}", colRight, 210f, headerPaint)
        canvas.drawText("الرقم الوطني الموحّد:  ${student.nationalId}", colRight, 240f, textPaint)
        canvas.drawText("القسم / المستوى:      ${student.classRating}", colRight, 270f, textPaint)
        canvas.drawText("رقم الاستدعاء:        ${student.invitationCode}", colRight, 300f, textPaint)

        canvas.drawText("رقم المقعد الدراسي:    ${student.seatNumber}", colLeft, 210f, headerPaint)
        canvas.drawText("قاعة الاختبار المحددة:  ${student.hallNumber}", colLeft, 240f, textPaint)
        canvas.drawText("الترميز الرقمي للفرز:    STU_REG_2026", colLeft, 270f, textPaint)

        // Scheduled Exams Section
        canvas.drawText("جدول مواعيد الاختبارات المسجلة للترشح:", 370f, 355f, headerPaint)
        
        var currentY = 380f
        exams.forEach { exam ->
            canvas.drawRect(40f, currentY, 555f, currentY + 60f, fillPaint)
            canvas.drawRect(40f, currentY, 555f, currentY + 60f, borderPaint)
            
            canvas.drawText("المادة: ${exam.title}", 280f, currentY + 25f, headerPaint)
            canvas.drawText("التاريخ: ${exam.date} | الوقت: ${exam.time}", 280f, currentY + 48f, textPaint)
            canvas.drawText("المدة: ${exam.durationMinutes} دقيقة | الدرجة: ${exam.totalPoints}ن", 50f, currentY + 25f, textPaint)
            canvas.drawText("الأستاذ المشرف: أحمد المحمود", 50f, currentY + 48f, textPaint)
            
            currentY += 75f
        }

        // Guidelines Notice
        canvas.drawText("تنبيهات هامة للمترشح:", 445f, currentY + 20f, headerPaint)
        textPaint.textSize = 10f
        canvas.drawText("1. يجب الحضور قبل انطلاق الامتحان بـ 15 دقيقة مصحوباً ببطاقة الهوية والطلب المطبوع.", 172f, currentY + 40f, textPaint)
        canvas.drawText("2. يمنع منعاً باتاً إدخال الهواتف المحمولة أو المبرمجات الذكية لقاعة الامتحان الكبرى.", 184f, currentY + 55f, textPaint)
        canvas.drawText("3. سيتم تفعيل الباركود/QR في مدخل القاعة عبر جهاز الفحص الإلكتروني لتسجيل الحضور.", 148f, currentY + 70f, textPaint)

        // QR Code embedding (Bottom Right Corner)
        val qrBitmap = QrGenerator.generateQrBitmap(student.barcodeHash, 140)
        canvas.drawBitmap(qrBitmap, 410f, currentY + 95f, null)
        textPaint.textSize = 9f
        canvas.drawText("رمز المصادقة والتحقق إلكترونياً", 415f, currentY + 245f, textPaint)

        // Signature area
        val signaturePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("ختم عمادة الامتحانات الموحدة", 70f, currentY + 120f, signaturePaint)
        val sigLinePaint = Paint().apply {
            color = android.graphics.Color.GRAY
            strokeWidth = 1f
        }
        canvas.drawLine(50f, currentY + 180f, 210f, currentY + 180f, sigLinePaint)
        canvas.drawText("إمضاء المنظم الإلكتروني التلقائي", 67f, currentY + 195f, textPaint)

        // Finish the page
        pdfDocument.finishPage(page)

        try {
            val outputStream = FileOutputStream(destFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()
            return destFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    // Generates the Exam result spreadsheet as a CSV file to open inside Microsoft Excel
    fun exportScoresExcel(
        context: Context,
        examTitle: String,
        submissions: List<Submission>,
        studentNamesMap: Map<Int, String>
    ): File? {
        val destFile = File(context.cacheDir, "نتائج_${examTitle.replace(" ", "_")}.csv")
        try {
            val writer = FileWriter(destFile)
            // Arabic UTF-8 BOM so Excel opens it correctly with Arabic encoding
            writer.write('\ufeff'.toInt())

            // Writing headers
            writer.append("رقم الطالب,الاسم المترشح,علامة الامتحان الكاملة,نقاط الطالب المستحقة,حالة التصحيح,تاريخ التسليم,معايير الـ Rubrics المفصلة\n")

            submissions.forEach { sub ->
                val stdName = studentNamesMap[sub.studentId] ?: "طالب مجهول"
                val cleanedAnswers = sub.rubricGradesJson.replace(",", " | ").replace("\n", " ")
                writer.append("${sub.studentId},${stdName},20.0,${sub.score},\"${if(sub.status == "GRADED") "تم التصحيح" else "قيد التصحيح"}\",${sub.submissionDate},\"$cleanedAnswers\"\n")
            }

            writer.flush()
            writer.close()
            return destFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
