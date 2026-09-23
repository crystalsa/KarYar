package com.example.ui.screens.reports

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.WorkerPerformance
import com.example.ui.WorkerViewModel
import com.example.ui.components.HairlineCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate100
import com.example.util.ExcelExportUtil
import com.example.util.Formatters
import com.example.util.JalaliCalendar
import com.example.util.PdfExportUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportsScreen(
    viewModel: WorkerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val folder by viewModel.currentFolder.collectAsState()
    val performances by viewModel.workerPerformances.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    val activeProject = folder?.name ?: "پروژه کارگاهی"
    val activeEmployer = folder?.employerName ?: "کارفرما"
    val activeForeman = folder?.foremanName ?: "سرکارگر"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // 1. Header & Export Buttons Banner
        item {
            HairlineCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "گزارشات و خروجی رسمی این کارگاه",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "صدور فیش حقوقی، گزارش هزینه‌ها و عملکرد با محاسبه تفکیکی",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(
                            text = "${performances.size} کارگر",
                            dotColor = AmberAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Export Action Buttons (PDF & Excel)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // PDF Export Button
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val file = PdfExportUtil.exportToPdf(
                                            context = context,
                                            projectName = activeProject,
                                            employerName = activeEmployer,
                                            foremanName = activeForeman,
                                            analytics = analytics,
                                            performances = performances
                                        )
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "فایل PDF با موفقیت آماده شد", Toast.LENGTH_SHORT).show()
                                            ExcelExportUtil.shareFile(context, file, "application/pdf", "ارسال گزارش رسمی PDF")
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "خطا در تولید PDF: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("خروجی PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Excel / CSV Export Button
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val file = ExcelExportUtil.exportToExcelCsv(
                                            context = context,
                                            workers = workers,
                                            attendanceList = attendanceList,
                                            expenses = expenses,
                                            performances = performances,
                                            projectName = activeProject
                                        )
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "فایل اکسل با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                                            ExcelExportUtil.shareFile(context, file, "text/csv", "ارسال گزارش اکسل (CSV)")
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "خطا در صدور اکسل: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_excel_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("خروجی اکسل", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 2. Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ریز محاسبات کارکرد و دستمزد هر کارگر",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "اضافه کاری، ترک زودتر، پاداش، افزایشی و کاهشی",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (performances.isEmpty()) {
            item {
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "هیچ کارگری برای محاسبه در این کارگاه وجود ندارد.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Worker Performance Detailed Pay Slips
        items(performances, key = { it.worker.id }) { perf ->
            WorkerPaySlipCard(perf)
        }
    }
}

@Composable
private fun WorkerPaySlipCard(perf: WorkerPerformance) {
    HairlineCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(perf.worker.colorTag)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = perf.worker.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = perf.worker.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = perf.worker.role,
                            fontSize = 11.5.sp,
                            color = AmberAccent,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "خالص دریافتی",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatters.formatCurrency(perf.netPayout),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate100)
                    .padding(10.dp)
            ) {
                ReportRow("تعداد شیفت و ساعت کارکرد عادی:", "${Formatters.toPersianDigits(perf.totalShifts)} شیفت (${Formatters.toPersianDigits(perf.regularHours)} ساعت)")
                ReportRow("دستمزد پایه تجمیعی:", Formatters.formatCurrency(perf.baseWageTotal))

                if (perf.hourlyPayTotal > 0 || perf.hourlyHours > 0) {
                    ReportRow("دستمزد ساعتی (${Formatters.toPersianDigits(perf.hourlyHours)} ساعت):", "+${Formatters.formatCurrency(perf.hourlyPayTotal)}", CyanAccent)
                }
                if (perf.overtimeHours > 0) {
                    ReportRow("اضافه کاری (${Formatters.toPersianDigits(perf.overtimeHours)} ساعت):", "+${Formatters.formatCurrency(perf.overtimePayTotal)}", AmberAccent)
                }
                if (perf.bonusTotal > 0) {
                    ReportRow("پاداش منظور شده:", "+${Formatters.formatCurrency(perf.bonusTotal)}", EmeraldAccent)
                }
                if (perf.totalAllowances > 0) {
                    ReportRow("(+) مجموع کمک‌هزینه‌های افزایشی:", "+${Formatters.formatCurrency(perf.totalAllowances)}", EmeraldAccent)
                }
                if (perf.totalDeductions > 0) {
                    ReportRow("(-) مجموع کسورات هزینه‌ها:", "-${Formatters.formatCurrency(perf.totalDeductions)}", RoseAccent)
                }
                if (perf.earlyDepartureMinutes > 0) {
                    ReportRow("(-) کسر ترک زودتر (${perf.earlyDepartureMinutes} دقیقه):", "-${Formatters.formatCurrency(perf.earlyDepartureDeduction)}", RoseAccent)
                }
                ReportRow("سهم از هزینه‌های جمعی کارگاه:", Formatters.formatCurrency(perf.groupExpenseShare), IndigoAccent)
            }
        }
    }
}

@Composable
private fun ReportRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            fontSize = 11.5.sp,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
