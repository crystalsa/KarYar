package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.domain.model.DashboardAnalytics
import com.example.domain.model.WorkerPerformance
import java.io.File
import java.io.FileOutputStream

object PdfExportUtil {

    /**
     * Generates a modern, clean PDF report of the worker management system
     */
    fun exportToPdf(
        context: Context,
        projectName: String,
        employerName: String,
        foremanName: String,
        analytics: DashboardAnalytics,
        performances: List<WorkerPerformance>
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawPdfContent(
            canvas = canvas,
            projectName = projectName,
            employerName = employerName,
            foremanName = foremanName,
            analytics = analytics,
            performances = performances
        )

        pdfDoc.finishPage(page)

        val fileName = "گزارش_کارگاه_${System.currentTimeMillis()}.pdf"
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        return file
    }

    private fun drawPdfContent(
        canvas: Canvas,
        projectName: String,
        employerName: String,
        foremanName: String,
        analytics: DashboardAnalytics,
        performances: List<WorkerPerformance>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header background banner
        paint.color = Color.parseColor("#0F172A") // Deep slate navy
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Header Gold accent line
        paint.color = Color.parseColor("#F59E0B")
        canvas.drawRect(0f, 90f, 595f, 94f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("گزارش جامع عملکرد، حضور و غیاب و هزینه‌های کارگران", 30f, 40f, paint)

        // Header Subtitle
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 11f
        paint.isFakeBoldText = false
        val todayStr = JalaliCalendar.todayString()
        canvas.drawText("پروژه: $projectName | تاریخ صدور: $todayStr", 30f, 65f, paint)

        // 2. Metadata Box
        paint.color = Color.parseColor("#F1F5F9")
        val metaRect = RectF(30f, 110f, 565f, 155f)
        canvas.drawRoundRect(metaRect, 8f, 8f, paint)

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("کارفرما: $employerName", 45f, 135f, paint)
        canvas.drawText("سرکارگر: $foremanName", 220f, 135f, paint)
        canvas.drawText("تعداد کارگران: ${analytics.activeWorkersCount} نفر فعال", 390f, 135f, paint)

        // 3. Financial Summary 4 Cards
        val cardWidth = 125f
        val cardHeight = 55f
        val startY = 170f

        drawStatCard(canvas, 30f, startY, cardWidth, cardHeight, "مجموع دستمزد", Formatters.formatCurrency(analytics.totalWagesPaid), "#10B981")
        drawStatCard(canvas, 168f, startY, cardWidth, cardHeight, "اضافه کاری و پاداش", Formatters.formatCurrency(analytics.totalOvertimePaid + analytics.totalBonusesPaid), "#F59E0B")
        drawStatCard(canvas, 306f, startY, cardWidth, cardHeight, "مجموع هزینه‌ها", Formatters.formatCurrency(analytics.grandTotalExpenses), "#EF4444")
        drawStatCard(canvas, 444f, startY, cardWidth, cardHeight, "هزینه کل پروژه", Formatters.formatCurrency(analytics.grandTotalProjectCost), "#6366F1")

        // 4. Section Title: Workers Performance Table
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("صورت‌جلسه کارکرد و خالص دریافتی هر کارگر", 30f, 255f, paint)

        // Table Header
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(30f, 268f, 565f, 290f, paint)

        paint.color = Color.parseColor("#334155")
        paint.textSize = 9.5f
        paint.isFakeBoldText = true

        canvas.drawText("ردیف", 35f, 283f, paint)
        canvas.drawText("نام کارگر", 70f, 283f, paint)
        canvas.drawText("تخصص / شغل", 180f, 283f, paint)
        canvas.drawText("شیفت / ساعت", 270f, 283f, paint)
        canvas.drawText("اضافه کاری", 345f, 283f, paint)
        canvas.drawText("هزینه تکی", 410f, 283f, paint)
        canvas.drawText("خالص دریافتی", 485f, 283f, paint)

        // Table Rows
        var rowY = 308f
        paint.isFakeBoldText = false
        performances.take(12).forEachIndexed { index, p ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(30f, rowY - 14f, 565f, rowY + 8f, paint)
            }

            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 9f
            canvas.drawText("${index + 1}", 38f, rowY, paint)
            canvas.drawText(p.worker.name, 70f, rowY, paint)
            canvas.drawText(p.worker.role, 180f, rowY, paint)
            canvas.drawText("${p.totalShifts} شیفت (${p.regularHours}h)", 270f, rowY, paint)
            canvas.drawText("${p.overtimeHours}h", 350f, rowY, paint)
            canvas.drawText(Formatters.formatCurrency(p.individualExpensesTotal, ""), 410f, rowY, paint)

            paint.color = Color.parseColor("#047857")
            paint.isFakeBoldText = true
            canvas.drawText(Formatters.formatCurrency(p.netPayout), 485f, rowY, paint)
            paint.isFakeBoldText = false

            rowY += 22f
        }

        // 5. Cost Distribution Breakdown Box
        rowY += 15f
        paint.color = Color.parseColor("#F8FAFC")
        val expBox = RectF(30f, rowY, 565f, rowY + 95f)
        canvas.drawRoundRect(expBox, 8f, 8f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("تفکیک هزینه‌های کارگاه (جمعی و تکی):", 45f, rowY + 22f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 9.5f
        paint.isFakeBoldText = false

        canvas.drawText("• ایاب و ذهاب: ${Formatters.formatCurrency(analytics.totalTransitExpenses)}", 45f, rowY + 45f, paint)
        canvas.drawText("• اسکان (${analytics.totalAccommodationDays} روز): ${Formatters.formatCurrency(analytics.totalAccommodationExpenses)}", 45f, rowY + 68f, paint)

        canvas.drawText("• خوراک و پذیرایی: ${Formatters.formatCurrency(analytics.totalFoodExpenses)}", 260f, rowY + 45f, paint)
        canvas.drawText("• درمان و بهداشت: ${Formatters.formatCurrency(analytics.totalMedicalExpenses)}", 260f, rowY + 68f, paint)

        canvas.drawText("• هزینه‌های جمعی: ${Formatters.formatCurrency(analytics.totalGroupExpenses)}", 430f, rowY + 45f, paint)
        canvas.drawText("• هزینه‌های تکی: ${Formatters.formatCurrency(analytics.totalIndividualExpenses)}", 430f, rowY + 68f, paint)

        // 6. Signature Lines at bottom
        val sigY = 760f
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawLine(50f, sigY, 200f, sigY, paint)
        canvas.drawLine(395f, sigY, 545f, sigY, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 9.5f
        canvas.drawText("امضاء و تأیید سرکارگر: $foremanName", 55f, sigY + 18f, paint)
        canvas.drawText("امضاء و تأیید کارفرما: $employerName", 400f, sigY + 18f, paint)
    }

    private fun drawStatCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        accentHex: String
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#F8FAFC")
        val rect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(rect, 6f, 6f, paint)

        // Left accent indicator
        paint.color = Color.parseColor(accentHex)
        val leftBar = RectF(x, y, x + 4f, y + height)
        canvas.drawRoundRect(leftBar, 2f, 2f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 8.5f
        canvas.drawText(label, x + 10f, y + 20f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 9.5f
        paint.isFakeBoldText = true
        canvas.drawText(value, x + 10f, y + 42f, paint)
    }
}
