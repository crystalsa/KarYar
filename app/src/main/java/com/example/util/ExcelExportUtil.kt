package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.WorkerEntity
import com.example.domain.model.WorkerPerformance
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ExcelExportUtil {

    /**
     * Exports full project records to an Excel-compatible CSV file with UTF-8 BOM
     */
    fun exportToExcelCsv(
        context: Context,
        workers: List<WorkerEntity>,
        attendanceList: List<AttendanceEntity>,
        expenses: List<ExpenseEntity>,
        performances: List<WorkerPerformance>,
        projectName: String = "پروژه کارگاهی"
    ): File {
        val fileName = "گزارش_کارگران_${System.currentTimeMillis()}.csv"
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM so Microsoft Excel opens Persian characters cleanly
            fos.write(0xEF)
            fos.write(0xBB)
            fos.write(0xBF)

            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Title
                writer.append("سامانه مدیریت جامع کارگران و هزینه‌ها - $projectName\n")
                writer.append("تاریخ خروجی,${JalaliCalendar.todayString()}\n\n")

                // Section 1: Performance Summary
                writer.append("=== خلاصه کارکرد و تسویه حساب کارگران ===\n")
                writer.append("شناسه,نام کارگر,شغل / تخصص,تعداد شیفت,ساعات عادی,اضافه کاری (ساعت),ترک زودتر (دقیقه),دستمزد پایه (تومان),اضافه کاری (تومان),پاداش (تومان),کسورات (تومان),هزینه تکی (تومان),سهم هزینه جمعی (تومان),خالص دریافتی (تومان)\n")
                for (p in performances) {
                    writer.append("${p.worker.id},")
                    writer.append("\"${p.worker.name}\",")
                    writer.append("\"${p.worker.role}\",")
                    writer.append("${p.totalShifts},")
                    writer.append("${p.regularHours},")
                    writer.append("${p.overtimeHours},")
                    writer.append("${p.earlyDepartureMinutes},")
                    writer.append("${p.baseWageTotal},")
                    writer.append("${p.overtimePayTotal},")
                    writer.append("${p.bonusTotal},")
                    writer.append("${p.earlyDepartureDeduction},")
                    writer.append("${p.individualExpensesTotal},")
                    writer.append("${p.groupExpenseShare},")
                    writer.append("${p.netPayout}\n")
                }
                writer.append("\n")

                // Section 2: Attendance Logs
                writer.append("=== گزارش ثبت ورود و خروج و شیفت‌ها ===\n")
                writer.append("تاریخ,نام کارگر,ساعت ورود,ساعت خروج,ساعت عادی,اضافه کاری (ساعت),ترک زودتر (دقیقه),دلیل ترک زودتر,دستمزد روزانه,دستمزد ساعتی,پاداش,محل کار,کارفرما,سرکارگر,توضیحات\n")
                val workerMap = workers.associateBy { it.id }
                for (att in attendanceList) {
                    val wName = workerMap[att.workerId]?.name ?: "کارگر #${att.workerId}"
                    writer.append("${att.date},")
                    writer.append("\"$wName\",")
                    writer.append("${att.entryTime},")
                    writer.append("${att.exitTime},")
                    writer.append("${att.regularHours},")
                    writer.append("${att.overtimeHours},")
                    writer.append("${att.earlyDepartureMinutes},")
                    writer.append("\"${att.earlyDepartureReason}\",")
                    writer.append("${att.dailyWage},")
                    writer.append("${att.hourlyWage},")
                    writer.append("${att.bonus},")
                    writer.append("\"${att.workplaceName}\",")
                    writer.append("\"${att.employerName}\",")
                    writer.append("\"${att.foremanName}\",")
                    writer.append("\"${att.notes}\"\n")
                }
                writer.append("\n")

                // Section 3: Expenses (Individual & Group)
                writer.append("=== گزارش تفکیکی هزینه‌ها (ایاب و ذهاب، اسکان، خوراک، درمان) ===\n")
                writer.append("تاریخ,عنوان هزینه,دسته‌بندی,نوع (جمعی/تکی),مربوط به کارگر,مبلغ (تومان),مدت اسکان (روز),محل کار,کارفرما,سرکارگر,توضیحات\n")
                for (exp in expenses) {
                    val catName = when (exp.category) {
                        "TRANSIT" -> "ایاب و ذهاب"
                        "ACCOMMODATION" -> "اسکان"
                        "FOOD" -> "خوراک"
                        "MEDICAL" -> "درمان"
                        else -> "سایر"
                    }
                    val scopeName = if (exp.scope == "GROUP") "جمعی" else "تکی"
                    val wName = exp.workerName ?: "-"
                    writer.append("${exp.date},")
                    writer.append("\"${exp.title}\",")
                    writer.append("\"$catName\",")
                    writer.append("\"$scopeName\",")
                    writer.append("\"$wName\",")
                    writer.append("${exp.amount},")
                    writer.append("${exp.accommodationDays},")
                    writer.append("\"${exp.workplaceName}\",")
                    writer.append("\"${exp.employerName}\",")
                    writer.append("\"${exp.foremanName}\",")
                    writer.append("\"${exp.notes}\"\n")
                }

                writer.flush()
            }
        }
        return file
    }

    /**
     * Share exported file via standard Android share sheet
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    }
}
