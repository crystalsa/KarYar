package com.karyar.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.karyar.app.R
import com.karyar.app.data.local.entity.ExpenseEntity
import com.karyar.app.domain.model.DashboardAnalytics
import com.karyar.app.domain.model.WorkerPerformance
import java.io.File
import java.io.FileOutputStream

object PdfExportUtil {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 30f
    private const val MARGIN_RIGHT = 565f
    private const val CONTENT_WIDTH = 535
    private const val MAX_CONTENT_Y = 760f

    /**
     * Generates a multi-page, professional PDF report with embedded Persian font,
     * proper RTL text shaping via StaticLayout, no record truncation, and complete financial reconciliation.
     */
    fun exportToPdf(
        context: Context,
        projectName: String,
        employerName: String,
        foremanName: String,
        analytics: DashboardAnalytics,
        performances: List<WorkerPerformance>,
        expenses: List<ExpenseEntity> = emptyList()
    ): File {
        var fontLoadFailed = false
        val vazirTypeface: Typeface = try {
            val tf = ResourcesCompat.getFont(context, R.font.vazirmatn)
            if (tf != null) {
                tf
            } else {
                fontLoadFailed = true
                android.util.Log.e("PdfExportUtil", "خطا در بارگذاری فونت وزیزمتن: فایل فونت R.font.vazirmatn یافت نشد.")
                Typeface.DEFAULT
            }
        } catch (e: Exception) {
            fontLoadFailed = true
            android.util.Log.e("PdfExportUtil", "خطا در بارگذاری فونت وزیزمتن: ${e.message}", e)
            Typeface.DEFAULT
        }

        val pdfDoc = PdfDocument()
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        // 1. First Page Header
        drawFirstPageHeader(
            canvas = canvas,
            typeface = vazirTypeface,
            projectName = projectName,
            employerName = employerName,
            foremanName = foremanName,
            analytics = analytics
        )

        // 2. Financial Summary Cards on First Page
        drawFinancialSummaryCards(canvas, vazirTypeface, analytics)

        // 3. Workers Table Title
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = vazirTypeface
            textSize = 12.5f
            isFakeBoldText = true
            color = Color.parseColor("#0F172A")
        }
        drawRtlText(canvas, "صورت‌جلسه کارکرد و تسویه حساب هر کارگر (تفکیک به ازای شناسه کارگر):", MARGIN_LEFT, 245f, textPaint, CONTENT_WIDTH)

        // Table Header
        var currentY = 265f
        drawTableHeader(canvas, vazirTypeface, currentY)
        currentY += 22f

        // Draw Workers Rows (All workers, no truncation, separate row per worker ID)
        val rowHeight = 22f
        for ((index, p) in performances.withIndex()) {
            if (currentY + rowHeight > MAX_CONTENT_Y) {
                // Finish current page
                drawPageFooter(canvas, vazirTypeface, pageNumber)
                pdfDoc.finishPage(page)

                // Start next page
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas

                // Compact Header for continuation page
                drawContinuationHeader(canvas, vazirTypeface, projectName)
                currentY = 60f
                drawTableHeader(canvas, vazirTypeface, currentY)
                currentY += 22f
            }

            drawWorkerRow(canvas, vazirTypeface, index, p, currentY)
            currentY += rowHeight
        }

        // Summary and Signatures: if not enough room on current page, create a final page
        val summaryNeededHeight = 180f
        if (currentY + summaryNeededHeight > MAX_CONTENT_Y) {
            drawPageFooter(canvas, vazirTypeface, pageNumber)
            pdfDoc.finishPage(page)

            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDoc.startPage(pageInfo)
            canvas = page.canvas

            drawContinuationHeader(canvas, vazirTypeface, projectName)
            currentY = 60f
        }

        currentY += 12f
        drawSummaryAndExpensesBox(canvas, vazirTypeface, currentY, analytics, expenses)
        currentY += 95f

        drawSignatures(canvas, vazirTypeface, currentY, foremanName, employerName)

        drawPageFooter(canvas, vazirTypeface, pageNumber)
        pdfDoc.finishPage(page)

        // Save to cache/exports
        val fileName = "گزارش_کارگاه_${System.currentTimeMillis()}.pdf"
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        return file
    }

    private fun drawFirstPageHeader(
        canvas: Canvas,
        typeface: Typeface,
        projectName: String,
        employerName: String,
        foremanName: String,
        analytics: DashboardAnalytics
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Banner background
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 85f, paint)

        // Gold accent line
        paint.color = Color.parseColor("#F59E0B")
        canvas.drawRect(0f, 85f, PAGE_WIDTH.toFloat(), 88f, paint)

        // Header Title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 15f
            isFakeBoldText = true
            color = Color.WHITE
        }
        drawRtlText(canvas, "گزارش جامع عملکرد، حضور و غیاب و تسویه کارگران", MARGIN_LEFT, 20f, titlePaint, CONTENT_WIDTH)

        // Subtitle
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 10f
            color = Color.parseColor("#94A3B8")
        }
        val todayStr = JalaliCalendar.todayString()
        drawRtlText(canvas, "پروژه: $projectName   |   تاریخ صدور: $todayStr", MARGIN_LEFT, 52f, subPaint, CONTENT_WIDTH)

        // Metadata Box
        paint.color = Color.parseColor("#F1F5F9")
        val metaRect = RectF(MARGIN_LEFT, 98f, MARGIN_RIGHT, 140f)
        canvas.drawRoundRect(metaRect, 6f, 6f, paint)

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 9.5f
            isFakeBoldText = true
            color = Color.parseColor("#1E293B")
        }
        drawRtlText(canvas, "کارفرما: $employerName", 40f, 115f, metaPaint, 160)
        drawRtlText(canvas, "سرکارگر: $foremanName", 215f, 115f, metaPaint, 160)
        drawRtlText(canvas, "پرسنل فعال: ${Formatters.toPersianDigits(analytics.activeWorkersCount)} نفر", 400f, 115f, metaPaint, 150)
    }

    private fun drawContinuationHeader(canvas: Canvas, typeface: Typeface, projectName: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 40f, paint)

        paint.color = Color.parseColor("#F59E0B")
        canvas.drawRect(0f, 40f, PAGE_WIDTH.toFloat(), 42f, paint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 11f
            isFakeBoldText = true
            color = Color.WHITE
        }
        drawRtlText(canvas, "پروژه: $projectName  |  ادامه صورت‌جلسه کارکرد و تسویه حساب پرسنل", MARGIN_LEFT, 15f, textPaint, CONTENT_WIDTH)
    }

    private fun drawFinancialSummaryCards(canvas: Canvas, typeface: Typeface, analytics: DashboardAnalytics) {
        val cardWidth = 124f
        val cardHeight = 52f
        val startY = 152f

        drawStatCard(canvas, typeface, 30f, startY, cardWidth, cardHeight, "مجموع دستمزد پایه", Formatters.formatCurrency(analytics.totalWagesPaid), "#10B981")
        drawStatCard(canvas, typeface, 166f, startY, cardWidth, cardHeight, "اضافه کاری و ساعتی", Formatters.formatCurrency(analytics.totalOvertimePaid + analytics.totalHourlyPaid), "#F59E0B")
        drawStatCard(canvas, typeface, 302f, startY, cardWidth, cardHeight, "هزینه‌های کارگاه", Formatters.formatCurrency(analytics.grandTotalExpenses), "#EC4899")
        drawStatCard(canvas, typeface, 438f, startY, cardWidth, cardHeight, "کل مخارج پروژه", Formatters.formatCurrency(analytics.grandTotalProjectCost), "#6366F1")
    }

    private fun drawTableHeader(canvas: Canvas, typeface: Typeface, y: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 20f, paint)

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f
            isFakeBoldText = true
            color = Color.parseColor("#334155")
        }

        drawRtlText(canvas, "ردیف", 34f, y + 4f, headerPaint, 25)
        drawRtlText(canvas, "نام کارگر (شناسه)", 65f, y + 4f, headerPaint, 115)
        drawRtlText(canvas, "تخصص / شغل", 185f, y + 4f, headerPaint, 95)
        drawRtlText(canvas, "کارکرد عادی", 285f, y + 4f, headerPaint, 75)
        drawRtlText(canvas, "اضافه کار", 365f, y + 4f, headerPaint, 55)
        drawRtlText(canvas, "مزایا/کسورات/سهم", 425f, y + 4f, headerPaint, 65)
        drawRtlText(canvas, "خالص دریافتی", 495f, y + 4f, headerPaint, 70)
    }

    private fun drawWorkerRow(
        canvas: Canvas,
        typeface: Typeface,
        index: Int,
        p: WorkerPerformance,
        y: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (index % 2 == 0) {
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRect(MARGIN_LEFT, y - 2f, MARGIN_RIGHT, y + 19f, paint)
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f
            color = Color.parseColor("#1E293B")
        }

        // 1. Index
        drawRtlText(canvas, Formatters.toPersianDigits(index + 1), 34f, y + 3f, textPaint, 25)

        // 2. Name with ID (guarantees separate distinction if two workers share the same name)
        val nameWithId = "${p.worker.name} (#${p.worker.id})"
        drawRtlText(canvas, nameWithId, 65f, y + 3f, textPaint, 115)

        // 3. Role
        drawRtlText(canvas, p.worker.role, 185f, y + 3f, textPaint, 95)

        // 4. Work days / Regular hours
        val workStr = if (p.hourlyHours > 0) {
            "${Formatters.toPersianDigits(p.totalShifts)}ر (${Formatters.toPersianDigits(p.hourlyHours)}ساعتی)"
        } else {
            "${Formatters.toPersianDigits(p.totalShifts)}ر (${Formatters.toPersianDigits(p.regularHours)}س)"
        }
        drawRtlText(canvas, workStr, 285f, y + 3f, textPaint, 75)

        // 5. Overtime
        val otStr = if (p.overtimeHours > 0) "${Formatters.toPersianDigits(p.overtimeHours)}س" else "-"
        drawRtlText(canvas, otStr, 365f, y + 3f, textPaint, 55)

        // 6. Net allowances / deductions / group share
        val netAdjust = p.totalAllowances - p.totalDeductions - p.groupExpenseShare
        val adjustStr = if (netAdjust > 0L) "+${Formatters.formatCurrency(netAdjust)}" else if (netAdjust < 0L) Formatters.formatCurrency(netAdjust) else "۰"
        val adjustPaint = TextPaint(textPaint).apply {
            color = if (netAdjust > 0L) Color.parseColor("#059669") else if (netAdjust < 0L) Color.parseColor("#E11D48") else Color.parseColor("#64748B")
        }
        drawRtlText(canvas, adjustStr, 425f, y + 3f, adjustPaint, 65)

        // 7. Net Payout
        val payoutPaint = TextPaint(textPaint).apply {
            isFakeBoldText = true
            color = Color.parseColor("#047857")
        }
        drawRtlText(canvas, Formatters.formatCurrency(p.netPayout), 495f, y + 3f, payoutPaint, 70)
    }

    private fun drawSummaryAndExpensesBox(
        canvas: Canvas,
        typeface: Typeface,
        y: Float,
        analytics: DashboardAnalytics,
        expenses: List<ExpenseEntity>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#F8FAFC")
        val expBox = RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + 80f)
        canvas.drawRoundRect(expBox, 8f, 8f, paint)

        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(expBox, 8f, 8f, paint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 9.5f
            isFakeBoldText = true
            color = Color.parseColor("#0F172A")
        }
        drawRtlText(canvas, "خلاصه آمار عملکرد و هزینه‌های این کارگاه:", MARGIN_LEFT + 12f, y + 10f, titlePaint, CONTENT_WIDTH - 24)

        val itemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f
            color = Color.parseColor("#334155")
        }

        // Line 1
        drawRtlText(canvas, "• کل ساعات کارکرد عادی: ${Formatters.toPersianDigits(analytics.totalWorkHours)} ساعت", MARGIN_LEFT + 12f, y + 30f, itemPaint, 240)
        drawRtlText(canvas, "• کل اضافه کاری: ${Formatters.toPersianDigits(analytics.totalOvertimeHours)} ساعت", MARGIN_LEFT + 270f, y + 30f, itemPaint, 240)

        // Line 2: Note totalWorkDaysCount used correctly
        drawRtlText(canvas, "• مجموع روزهای کاری ثبت‌شده: ${Formatters.toPersianDigits(analytics.totalWorkDaysCount)} روز", MARGIN_LEFT + 12f, y + 48f, itemPaint, 240)
        drawRtlText(canvas, "• دستمزد ساعتی پرداختی: ${Formatters.formatCurrency(analytics.totalHourlyPaid)}", MARGIN_LEFT + 270f, y + 48f, itemPaint, 240)

        // Line 3: Expenses breakdown matching Dashboard
        drawRtlText(canvas, "• کل هزینه‌های جانبی کارگاه: ${Formatters.formatCurrency(analytics.grandTotalExpenses)}", MARGIN_LEFT + 12f, y + 64f, itemPaint, 240)
        val grandTotalPaint = TextPaint(itemPaint).apply {
            isFakeBoldText = true
            color = Color.parseColor("#4338CA")
        }
        drawRtlText(canvas, "• هزینه کل نهایی پروژه: ${Formatters.formatCurrency(analytics.grandTotalProjectCost)}", MARGIN_LEFT + 270f, y + 64f, grandTotalPaint, 240)
    }

    private fun drawSignatures(canvas: Canvas, typeface: Typeface, y: Float, foremanName: String, employerName: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 1f
        canvas.drawLine(50f, y, 210f, y, paint)
        canvas.drawLine(380f, y, 540f, y, paint)

        val sigPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 9f
            color = Color.parseColor("#475569")
        }
        drawRtlText(canvas, "امضاء و تأیید سرکارگر: $foremanName", 50f, y + 8f, sigPaint, 170)
        drawRtlText(canvas, "امضاء و تأیید کارفرما: $employerName", 380f, y + 8f, sigPaint, 170)
    }

    private fun drawPageFooter(canvas: Canvas, typeface: Typeface, pageNumber: Int) {
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f
            color = Color.parseColor("#94A3B8")
        }
        val footerText = "سامانه مدیریت کارگران   |   صفحه ${Formatters.toPersianDigits(pageNumber)}"
        drawRtlText(canvas, footerText, MARGIN_LEFT, 815f, footerPaint, CONTENT_WIDTH, Layout.Alignment.ALIGN_CENTER)
    }

    private fun drawStatCard(
        canvas: Canvas,
        typeface: Typeface,
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

        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(rect, 6f, 6f, paint)

        // Accent indicator bar
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor(accentHex)
        val leftBar = RectF(x, y, x + 3.5f, y + height)
        canvas.drawRoundRect(leftBar, 2f, 2f, paint)

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 7.5f
            color = Color.parseColor("#64748B")
        }
        drawRtlText(canvas, label, x + 7f, y + 8f, labelPaint, (width - 10).toInt())

        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 8.5f
            isFakeBoldText = true
            color = Color.parseColor("#0F172A")
        }
        drawRtlText(canvas, value, x + 7f, y + 26f, valuePaint, (width - 10).toInt())
    }

    /**
     * Renders Persian / Arabic text with proper letter shaping (اتصال حروف) and RTL layout direction.
     */
    private fun drawRtlText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: TextPaint,
        width: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ) {
        if (text.isBlank()) return
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(align)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}
