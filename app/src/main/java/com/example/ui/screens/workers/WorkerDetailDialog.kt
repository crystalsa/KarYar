package com.example.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.WorkerEntity
import com.example.domain.model.WorkerPerformance
import com.example.ui.components.HairlineCard
import com.example.ui.components.IconicsBox
import com.example.ui.components.IconicsSize
import com.example.ui.components.LoadingButton
import com.example.ui.components.LoadingOutlinedButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate100
import com.example.util.Formatters

@Composable
fun WorkerDetailDialog(
    worker: WorkerEntity,
    attendance: AttendanceEntity? = null,
    performance: WorkerPerformance? = null,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Android-Iconics styled avatar container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(worker.colorTag))
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = worker.name.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = worker.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = worker.role,
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberAccent,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status & Contact tightly displayed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        text = if (worker.isActive) "مشغول به کار" else "غیرفعال / مرخصی",
                        dotColor = if (worker.isActive) EmeraldAccent else RoseAccent
                    )
                    if (worker.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconicsBox(
                                icon = Icons.Default.Phone,
                                color = EmeraldAccent,
                                size = IconicsSize.TINY
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Formatters.toPersianDigits(worker.phone),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (worker.nationalId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconicsBox(
                            icon = Icons.Default.Badge,
                            color = CyanAccent,
                            size = IconicsSize.TINY
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "کد ملی: ${Formatters.toPersianDigits(worker.nationalId)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Financial Overview Card (Tight Rows)
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Slate100,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "خلاصه مالی و کارکرد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val isAbsent = attendance != null && (attendance.regularHours == 0.0 || attendance.notes == "غیبت")
                        val isHalfDay = attendance != null && (attendance.regularHours == 4.0 || attendance.notes == "نصف روز")
                        val isFullDay = attendance != null && (attendance.regularHours >= 8.0 && attendance.notes != "غیبت" && attendance.notes != "نصف روز")

                        if (attendance != null) {
                            val attStatusText = when {
                                isAbsent -> "غیبت"
                                isHalfDay -> "نصف روز (۴ ساعت)"
                                isFullDay -> "تمام روز (۸ ساعت)"
                                else -> "ثبت شده"
                            }
                            val attStatusColor = when {
                                isAbsent -> RoseAccent
                                isHalfDay -> AmberAccent
                                else -> EmeraldAccent
                            }
                            CompactDetailRow(
                                label = "وضعیت تردد این روز:",
                                value = attStatusText,
                                valueColor = attStatusColor
                            )
                        }

                        if (isAbsent) {
                            CompactDetailRow(
                                label = "دستمزد روزانه این روز:",
                                value = "غیبت (مبلغ ثبت نشد)",
                                valueColor = RoseAccent
                            )
                        } else if (isHalfDay) {
                            val halfWage = if (attendance?.dailyWage != null && attendance.dailyWage > 0) attendance.dailyWage else worker.baseDailyWage / 2
                            CompactDetailRow(
                                label = "دستمزد روزانه (نصف روز):",
                                value = Formatters.formatCurrency(halfWage),
                                valueColor = AmberAccent
                            )
                        } else if (worker.baseDailyWage > 0) {
                            val wageToDisplay = if (attendance != null && attendance.dailyWage > 0) attendance.dailyWage else worker.baseDailyWage
                            CompactDetailRow("دستمزد روزانه:", Formatters.formatCurrency(wageToDisplay))
                        }

                        if (worker.isHourlyEnabled || worker.hourlyHours > 0 || worker.hourlyWageRate > 0) {
                            val hRate = if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage
                            CompactDetailRow(
                                "دستمزد ساعتی:",
                                "${Formatters.toPersianDigits(worker.hourlyHours.toString().removeSuffix(".0"))} ساعت × ${Formatters.formatCurrency(hRate)}"
                            )
                        }

                        if (worker.isOvertimeEnabled || worker.overtimeHours > 0 || worker.overtimeRate > 0) {
                            CompactDetailRow(
                                "اضافه کاری:",
                                "${Formatters.toPersianDigits(worker.overtimeHours.toString().removeSuffix(".0"))} ساعت × ${Formatters.formatCurrency(worker.overtimeRate)}"
                            )
                        }

                        if (performance != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            CompactDetailRow("تعداد شیفت‌ها:", "${Formatters.toPersianDigits(performance.totalShifts)} شیفت (${Formatters.toPersianDigits(performance.regularHours)} ساعت)")

                            if (performance.hourlyPayTotal > 0) {
                                CompactDetailRow(
                                    label = "مجموع کارکرد ساعتی:",
                                    value = "${Formatters.toPersianDigits(performance.hourlyHours)} ساعت (${Formatters.formatCurrency(performance.hourlyPayTotal)})",
                                    valueColor = CyanAccent
                                )
                            }

                            if (performance.overtimePayTotal > 0) {
                                CompactDetailRow(
                                    label = "مجموع اضافه کاری:",
                                    value = "${Formatters.toPersianDigits(performance.overtimeHours)} ساعت (${Formatters.formatCurrency(performance.overtimePayTotal)})",
                                    valueColor = AmberAccent
                                )
                            }

                            if (performance.bonusTotal > 0) {
                                CompactDetailRow("پاداش منظور شده:", Formatters.formatCurrency(performance.bonusTotal))
                            }

                            if (performance.totalAllowances > 0) {
                                CompactDetailRow(
                                    label = "(+) مجموع کمک‌هزینه‌ها:",
                                    value = "+${Formatters.formatCurrency(performance.totalAllowances)}",
                                    valueColor = EmeraldAccent
                                )
                            }

                            if (performance.totalDeductions > 0) {
                                CompactDetailRow(
                                    label = "(-) مجموع کسورات هزینه‌ها:",
                                    value = "-${Formatters.formatCurrency(performance.totalDeductions)}",
                                    valueColor = RoseAccent
                                )
                            }

                            if (performance.earlyDepartureMinutes > 0) {
                                CompactDetailRow(
                                    label = "(-) کسر تعجیل در خروج:",
                                    value = "-${Formatters.formatCurrency(performance.earlyDepartureDeduction)} (${Formatters.toPersianDigits(performance.earlyDepartureMinutes)} دقیقه)",
                                    valueColor = RoseAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "خالص دریافتی نهایی:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Formatters.formatCurrency(performance.netPayout),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = EmeraldAccent
                                )
                            }
                        }
                    }
                }

                // Personal Description / Notes
                if (worker.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "یادداشت: ${worker.notes}",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions using LoadingButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LoadingOutlinedButton(
                        text = "بستن",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 38.dp,
                        fontSize = 12.sp
                    )
                    LoadingButton(
                        text = "ویرایش",
                        icon = Icons.Default.Edit,
                        onClick = onEdit,
                        modifier = Modifier.weight(1.2f),
                        containerColor = AmberAccent,
                        height = 38.dp,
                        fontSize = 12.sp
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(38.dp)
                            .background(RoseAccent.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RoseAccent, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
