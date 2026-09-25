package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DateFolderEntity
import com.example.ui.WorkerViewModel
import com.example.ui.components.GlowStatCard
import com.example.ui.components.HairlineCard
import com.example.ui.components.IconicsBox
import com.example.ui.components.IconicsSize
import com.example.ui.components.LoadingButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.util.Formatters
import com.example.util.JalaliCalendar

@Composable
fun DashboardScreen(
    viewModel: WorkerViewModel,
    onChangeFolder: () -> Unit = {},
    onNavigateToTab: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val folder by viewModel.currentFolder.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val recentAttendance by viewModel.attendanceList.collectAsState()
    val dailyBookkeeping by viewModel.dailyBookkeeping.collectAsState()
    val selectedDailyDate by viewModel.selectedDailyDate.collectAsState()
    val dateFolders by viewModel.dateFolders.collectAsState()

    // 1. When app starts fresh without any folder selected:
    // User requested: "صفحه اول کار وقتی شروع میشه باید خالی باشه"
    if (folder == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            HairlineCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconicsBox(
                        icon = Icons.Default.Business,
                        color = AmberAccent,
                        size = IconicsSize.HERO
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هیچ پوشه کاری انتخاب نشده است",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "برای شروع حساب و کتاب، لطفاً ابتدا یک پوشه برای محل کار یا پروژه خود ایجاد کنید.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    LoadingButton(
                        text = "مدیریت پوشه‌های محل کار",
                        icon = Icons.Default.FolderOpen,
                        onClick = onChangeFolder,
                        containerColor = AmberAccent,
                        height = 42.dp,
                        fontSize = 13.sp
                    )
                }
            }
        }
        return
    }

    // 2. Normal Dashboard when a workplace folder is selected
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
    ) {
        // 1. DAILY BOOKKEEPING SECTION (حساب کتاب در صفحه اصلی به صورت روزانه باشه و اعداد رو نشون بده)
        item {
            HairlineCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header with Date Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconicsBox(
                                icon = Icons.Default.CalendarToday,
                                color = AmberAccent,
                                size = IconicsSize.TINY
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حساب و کتاب روزانه",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${dailyBookkeeping.dayOfWeek} ${Formatters.toPersianDigits(dailyBookkeeping.date)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date navigation chips - synchronized with dateFolders
                    val todayStr = JalaliCalendar.todayString()
                    val sortedDateFolders = remember(dateFolders) {
                        dateFolders.sortedWith(compareBy({ it.date }, { it.id }))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // All Date Folders (روزهای کاری تعریف‌شده در پروژه)
                        sortedDateFolders.forEach { df ->
                            val isSelected = (selectedDailyDate == df.date)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AmberAccent else Slate100,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.selectDailyDate(df.date)
                                        viewModel.selectDateFolder(df)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = df.dayOfWeek,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = Formatters.toPersianDigits(df.date),
                                        fontSize = 10.5.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.95f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // If today is not in sortedDateFolders, show today chip
                        if (sortedDateFolders.none { it.date == todayStr }) {
                            val isSelected = (selectedDailyDate == todayStr)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AmberAccent else Slate100,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectDailyDate(todayStr) }
                            ) {
                                Text(
                                    text = "امروز (${Formatters.toPersianDigits(todayStr)})",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big Daily Total Card
                    HairlineCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = AmberAccent.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مجموع هزینه این روز:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "دستمزدها + اضافه کاری + هزینه‌های جاری",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Formatters.formatCurrency(dailyBookkeeping.grandDailyCost),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Daily Breakdown Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DailyMetricCard(
                            title = "دستمزد کارگران امروز",
                            value = Formatters.formatCurrency(dailyBookkeeping.totalDailyWages + dailyBookkeeping.totalHourlyPay + dailyBookkeeping.totalOvertimePay + dailyBookkeeping.totalBonuses),
                            subtitle = "${Formatters.toPersianDigits(dailyBookkeeping.workersPresent)} کارگر | ${Formatters.toPersianDigits(dailyBookkeeping.totalHours)} ساعت",
                            accentColor = EmeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        DailyMetricCard(
                            title = "هزینه‌های جاری امروز",
                            value = Formatters.formatCurrency(dailyBookkeeping.dailyExpenses),
                            subtitle = "ایاب‌ذهاب، اسکان، غذا، درمان",
                            accentColor = RoseAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Detailed breakdown numbers for the day
                    Text(
                        text = "ریز ارقام و مبالغ امروز:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    DailyDetailItem(
                        title = "دستمزد روزانه عادی",
                        amount = dailyBookkeeping.totalDailyWages,
                        color = EmeraldAccent
                    )
                    if (dailyBookkeeping.totalHourlyPay > 0) {
                        DailyDetailItem(
                            title = "دستمزد کارکرد ساعتی",
                            amount = dailyBookkeeping.totalHourlyPay,
                            color = CyanAccent
                        )
                    }
                    if (dailyBookkeeping.totalOvertimePay > 0) {
                        DailyDetailItem(
                            title = "دستمزد اضافه کاری",
                            amount = dailyBookkeeping.totalOvertimePay,
                            color = AmberAccent
                        )
                    }
                    if (dailyBookkeeping.totalBonuses > 0) {
                        DailyDetailItem(
                            title = "پاداش و مساعده",
                            amount = dailyBookkeeping.totalBonuses,
                            color = CyanAccent
                        )
                    }
                    if (dailyBookkeeping.transitCost > 0) {
                        DailyDetailItem(
                            title = "هزینه ایاب و ذهاب",
                            amount = dailyBookkeeping.transitCost,
                            color = CyanAccent
                        )
                    }
                    if (dailyBookkeeping.accommodationCost > 0) {
                        DailyDetailItem(
                            title = "هزینه مسکن و اسکان",
                            amount = dailyBookkeeping.accommodationCost,
                            color = IndigoAccent
                        )
                    }
                    if (dailyBookkeeping.foodCost > 0) {
                        DailyDetailItem(
                            title = "هزینه خوراک و غذا",
                            amount = dailyBookkeeping.foodCost,
                            color = AmberAccent
                        )
                    }
                    if (dailyBookkeeping.medicalCost > 0) {
                        DailyDetailItem(
                            title = "هزینه درمان و دارو",
                            amount = dailyBookkeeping.medicalCost,
                            color = RoseAccent
                        )
                    }
                }
            }
        }

        // 3. Overall Project Totals (مجموع کل پروژه)
        item {
            HairlineCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "مجموع هزینه‌های کل پروژه تاکنون",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlowStatCard(
                            title = "هزینه کل پروژه",
                            value = Formatters.formatCurrency(analytics.grandTotalProjectCost),
                            subtitle = "مجموع کل دستمزدها و هزینه‌ها",
                            icon = Icons.Default.AttachMoney,
                            accentColor = AmberAccent,
                            modifier = Modifier.weight(1f)
                        )
                        GlowStatCard(
                            title = "پرسنل پروژه",
                            value = "${Formatters.toPersianDigits(analytics.totalWorkersCount)} نفر",
                            subtitle = "${Formatters.toPersianDigits(analytics.activeWorkersCount)} نفر شاغل و فعال",
                            icon = Icons.Default.People,
                            accentColor = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlowStatCard(
                            title = "کل روزهای کاری",
                            value = "${Formatters.toPersianDigits(dateFolders.size)} روز",
                            subtitle = "روزهای ثبت‌شده در پروژه",
                            icon = Icons.Default.CalendarMonth,
                            accentColor = EmeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        GlowStatCard(
                            title = "مجموع دستمزدها",
                            value = Formatters.formatCurrency(analytics.totalWagesPaid + analytics.totalHourlyPaid + analytics.totalOvertimePaid + analytics.totalBonusesPaid),
                            subtitle = "مجموع پرداختی تمام روزها",
                            icon = Icons.Default.AttachMoney,
                            accentColor = IndigoAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Recent Shifts in this Project
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخرین کارکردهای ثبت‌شده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${Formatters.toPersianDigits(recentAttendance.size)} رکورد",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val workerMap = workers.associateBy { it.id }
        if (recentAttendance.isEmpty()) {
            item {
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هنوز هیچ تردد یا کارکردی در این پروژه ثبت نشده است.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentAttendance.take(5)) { att ->
                val worker = workerMap[att.workerId]
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(worker?.colorTag ?: 0xFFD97706L).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (worker?.name?.take(1) ?: "ک"),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(worker?.colorTag ?: 0xFFD97706L)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = worker?.name ?: "کارگر",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${Formatters.toPersianDigits(att.date)} | ورود: ${Formatters.toPersianDigits(att.entryTime)} - خروج: ${Formatters.toPersianDigits(att.exitTime)}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "محل: ${att.workplaceName} (سرکارگر: ${att.foremanName})",
                                    fontSize = 11.sp,
                                    color = CyanAccent
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Formatters.formatCurrency(att.dailyWage),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                            if (att.overtimeHours > 0) {
                                Text(
                                    text = "+${Formatters.toPersianDigits(att.overtimeHours)}h اضافه کار",
                                    fontSize = 11.sp,
                                    color = AmberAccent
                                )
                            }
                            if (att.earlyDepartureMinutes > 0) {
                                Text(
                                    text = "-${Formatters.toPersianDigits(att.earlyDepartureMinutes)}m تعجیل",
                                    fontSize = 11.sp,
                                    color = RoseAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    HairlineCard(
        modifier = modifier,
        backgroundColor = Slate100,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DailyDetailItem(
    title: String,
    amount: Long,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = Formatters.formatCurrency(amount),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
