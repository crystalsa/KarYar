package com.example.ui.screens.workers

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.DateFolderEntity
import com.example.data.local.entity.WorkerEntity
import com.example.ui.WorkerViewModel
import com.example.ui.components.HairlineCard
import com.example.ui.components.IconicsBox
import com.example.ui.components.IconicsSize
import com.example.ui.components.LoadingButton
import com.example.ui.components.LoadingOutlinedButton
import com.example.ui.components.ModernPillSelector
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.screens.attendance.CreateNextDayDialog
import com.example.ui.screens.attendance.DayActionOptionsDialog
import com.example.ui.screens.attendance.EditDateFolderDialog
import com.example.ui.screens.attendance.DeleteDayConfirmDialog
import com.example.util.Formatters
import com.example.util.JalaliCalendar
import com.example.util.WageCalculator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkersScreen(
    viewModel: WorkerViewModel,
    modifier: Modifier = Modifier
) {
    val folder by viewModel.currentFolder.collectAsState()
    val allWorkers by viewModel.workers.collectAsState()
    val performances by viewModel.workerPerformances.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()

    // Date folder hierarchy states
    val dateFolders by viewModel.dateFolders.collectAsState()
    val selectedDateFolder by viewModel.selectedDateFolder.collectAsState()
    val workersInDateFolder by viewModel.workersInDateFolder.collectAsState()
    val searchQuery by viewModel.workerSearchQuery.collectAsState()

    // Dialog states
    var isCreatingNextDay by remember { mutableStateOf(false) }
    var isDashboardExpanded by rememberSaveable { mutableStateOf(false) }
    var isAddingWorker by remember { mutableStateOf(false) }
    var showMustCreateDayDialog by remember { mutableStateOf(false) }
    var longPressedDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }
    var editingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }
    var deletingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }

    var editingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var viewingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var viewingAttendance by remember { mutableStateOf<AttendanceEntity?>(null) }
    var deletingWorker by remember { mutableStateOf<WorkerEntity?>(null) }

    var statusFilter by remember { mutableStateOf("همه") }

    // Active day folder logic: synchronized with attendance and date folder creation
    val activeDayFolder: DateFolderEntity? = selectedDateFolder ?: dateFolders.lastOrNull() ?: dateFolders.firstOrNull()
    val targetDayFolder = activeDayFolder
    val targetDate = targetDayFolder?.date ?: JalaliCalendar.todayString()
    val targetDayOfWeek = targetDayFolder?.dayOfWeek ?: JalaliCalendar.todayDayOfWeek()

    // Always show all workers in workplace, with their specific day status and payouts
    val activeWorkersList = allWorkers

    val filteredWorkers = activeWorkersList.filter { worker ->
        val matchesSearch = worker.name.contains(searchQuery, ignoreCase = true) ||
                worker.role.contains(searchQuery, ignoreCase = true) ||
                worker.phone.contains(searchQuery)
        val matchesStatus = when (statusFilter) {
            "فعال" -> worker.isActive
            "غیرفعال" -> !worker.isActive
            else -> true
        }
        matchesSearch && matchesStatus
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
        ) {
            // Horizontal Day Folders Header (شبیه قسمت ورود و خروج، روزها کنار هم از راست به چپ)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconicsBox(
                            icon = Icons.Default.CalendarMonth,
                            color = AmberAccent,
                            size = IconicsSize.SMALL
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "روزهای کاری",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Date folder horizontal chips (از راست به چپ)
                    val sortedDateFolders = remember(dateFolders) {
                        dateFolders.sortedWith(compareBy({ it.date }, { it.id }))
                    }

                    if (sortedDateFolders.isEmpty()) {
                        HairlineCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            backgroundColor = Slate100
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "هنوز روز کاری ثبت نشده است",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "برای شروع ثبت کارگران و ورود و خروج، ابتدا یک روز کاری ثبت نمایید.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LoadingButton(
                                    text = "ایجاد اولین روز کاری",
                                    icon = Icons.Default.Add,
                                    onClick = { isCreatingNextDay = true },
                                    containerColor = AmberAccent,
                                    height = 36.dp,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Day chips in chronological order (از راست به چپ)
                            sortedDateFolders.forEach { df ->
                                val isSelected = (activeDayFolder?.id == df.id)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AmberAccent else Slate100,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = {
                                                viewModel.selectDateFolder(df)
                                            },
                                            onLongClick = {
                                                longPressedDateFolder = df
                                            }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = df.dayOfWeek,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.5.sp
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = Formatters.toPersianDigits(df.date),
                                            color = if (isSelected) Color.White.copy(alpha = 0.95f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // گزارش هزینه‌ها و پرداخت‌های فقط آن روز (داشبورد جامع حساب و کتاب روز داخل کادر متمایز و پیش‌فرض مخفی)
            if (targetDayFolder != null) {
                item {
                    val dayWorkers = allWorkers
                    var dayPresentCount = 0
                    var dayHalfDayCount = 0
                    var dayHourlyCount = 0
                    var dayAbsentCount = 0

                    var dayBaseWages = 0L
                    var dayHourlyPay = 0L
                    var dayHourlyHours = 0.0
                    var dayOvertimePay = 0L
                    var dayOvertimeHours = 0.0

                    var transitAllowance = 0L
                    var transitDeduction = 0L
                    var foodAllowance = 0L
                    var foodDeduction = 0L
                    var accommodationAllowance = 0L
                    var accommodationDeduction = 0L
                    var medicalAllowance = 0L
                    var medicalDeduction = 0L

                    for (worker in dayWorkers) {
                        val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == targetDayFolder.date }
                        val isHourly = WageCalculator.isHourly(worker, att)
                        val isAbsent = WageCalculator.isAbsent(worker, att)
                        val isHalfDay = WageCalculator.isHalfDay(worker, att)
                        val isFullDay = WageCalculator.isFullDay(worker, att)

                        when {
                            isAbsent -> dayAbsentCount++
                            isHourly -> {
                                dayHourlyCount++
                                val hHours = if (att != null && att.hourlyHours > 0) att.hourlyHours else (if (worker.hourlyHours > 0) worker.hourlyHours else 0.0)
                                val hRate = if (att != null && att.hourlyWageRate > 0) att.hourlyWageRate else (if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage)
                                if (hHours > 0 && hRate > 0) {
                                    dayHourlyHours += hHours
                                    dayHourlyPay += (hHours * hRate).toLong()
                                }
                            }
                            isHalfDay -> {
                                dayHalfDayCount++
                                val wage = if (att?.dailyWage != null && att.dailyWage > 0) att.dailyWage else worker.baseDailyWage / 2
                                dayBaseWages += wage
                            }
                            isFullDay -> {
                                dayPresentCount++
                                val wage = if (att?.dailyWage != null && att.dailyWage > 0) att.dailyWage else worker.baseDailyWage
                                dayBaseWages += wage
                            }
                            else -> {
                                // Unmarked
                            }
                        }

                        if (!isAbsent) {
                            val otHours = if (att != null && att.overtimeHours > 0) att.overtimeHours else (if (worker.isOvertimeEnabled) worker.overtimeHours else 0.0)
                            val otRate = if (att != null && att.overtimeRate > 0) att.overtimeRate else worker.overtimeRate
                            if (otHours > 0) {
                                dayOvertimeHours += otHours
                                val pay = if (otRate > 0) (otHours * otRate).toLong()
                                          else if (att != null && att.hourlyWage > 0) (otHours * att.hourlyWage * 1.4).toLong()
                                          else 0L
                                dayOvertimePay += pay
                            }

                            // ایاب و ذهاب
                            if (worker.transitAllowance > 0) {
                                if (worker.transitImpact == "ALLOWANCE") {
                                    transitAllowance += worker.transitAllowance
                                } else {
                                    transitDeduction += worker.transitAllowance
                                }
                            }

                            // خوراک و ناهار
                            if (worker.foodAllowance > 0) {
                                if (worker.foodImpact == "ALLOWANCE") {
                                    foodAllowance += worker.foodAllowance
                                } else {
                                    foodDeduction += worker.foodAllowance
                                }
                            }

                            // اسکان و مسکن
                            if (worker.accommodationAllowance > 0) {
                                if (worker.accommodationImpact == "ALLOWANCE") {
                                    accommodationAllowance += worker.accommodationAllowance
                                } else {
                                    accommodationDeduction += worker.accommodationAllowance
                                }
                            }

                            // بیمه و درمان
                            if (worker.medicalAllowance > 0) {
                                if (worker.medicalImpact == "ALLOWANCE") {
                                    medicalAllowance += worker.medicalAllowance
                                } else {
                                    medicalDeduction += worker.medicalAllowance
                                }
                            }
                        }
                    }

                    val totalAllowances = transitAllowance + foodAllowance + accommodationAllowance + medicalAllowance
                    val totalDeductions = transitDeduction + foodDeduction + accommodationDeduction + medicalDeduction
                    val dayAllowancesNet = totalAllowances - totalDeductions

                    val totalDayPayroll = dayWorkers.sumOf { worker ->
                        val att = attendanceList.firstOrNull { a -> a.workerId == worker.id && a.date == targetDayFolder.date }
                        WageCalculator.calculateDayPayout(worker, att)
                    }
                    val totalDayGrandCost = totalDayPayroll

                    // کادر متمایز داشبورد جامع با حاشیه برجسته و پس‌زمینه اختصاصی
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, AmberAccent.copy(alpha = 0.5f)),
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // ۱. تیتر گزارش: متن گزارش مالی و پرداخت‌های امروز
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isDashboardExpanded = !isDashboardExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconicsBox(
                                        icon = Icons.Default.ReceiptLong,
                                        color = AmberAccent,
                                        size = IconicsSize.SMALL
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "گزارش مالی و پرداخت‌ها",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ۲. زیرش: مجموع خالص پرداختی امروز با مبلغش به صورت پیش‌فرض + دکمه کوچک فلش سمت چپ کلمه تومان
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isDashboardExpanded = !isDashboardExpanded },
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldAccent.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "( مجموع خالص پرداختی )",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // دکمه کوچک فلش در سمت چپ کلمه تومان
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = EmeraldAccent.copy(alpha = 0.2f),
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .clickable { isDashboardExpanded = !isDashboardExpanded }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isDashboardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = if (isDashboardExpanded) "بستن گزارش" else "مشاهده جزئیات گزارش",
                                                    tint = EmeraldAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = Formatters.formatCurrency(totalDayGrandCost),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent
                                        )
                                    }
                                }
                            }

                            // ۳. جزئیات فقط با زدن فلش و باز شدن کادر به صورت متحرک نمایش داده می‌شوند
                            AnimatedVisibility(
                                visible = isDashboardExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    // مجموع دستمزد ناخالص
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Slate100,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "( مجموع دست مزد ناخالص )",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = Formatters.formatCurrency(dayBaseWages),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // تفکیک اضافه کاری و ساعتی به همراه درج ساعت در جلوی مبلغ
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // دستمزد ساعتی
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Slate100
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "دستمزد ساعتی:",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${Formatters.formatCurrency(dayHourlyPay)} (${Formatters.toPersianDigits(dayHourlyHours)} ساعت)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CyanAccent
                                                )
                                            }
                                        }

                                        // اضافه کاری
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Slate100
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "اضافه‌کاری:",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${Formatters.formatCurrency(dayOvertimePay)} (${Formatters.toPersianDigits(dayOvertimeHours)} ساعت)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AmberAccent
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(thickness = 0.6.dp, color = Slate200)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // مزایا و کسورات با رنگ سبز (+) و قرمز (-) و فرمول مثبت/منفی
                                    Text(
                                        text = "مزایا و کسورات تفکیکی روز:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // ایاب و ذهاب
                                    DayAllowanceDeductionItem(
                                        title = "ایاب و ذهاب",
                                        allowance = transitAllowance,
                                        deduction = transitDeduction
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // خوراک و ناهار
                                    DayAllowanceDeductionItem(
                                        title = "خوراک و ناهار",
                                        allowance = foodAllowance,
                                        deduction = foodDeduction
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // اسکان و مسکن
                                    DayAllowanceDeductionItem(
                                        title = "اسکان و مسکن",
                                        allowance = accommodationAllowance,
                                        deduction = accommodationDeduction
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // بیمه و درمان
                                    DayAllowanceDeductionItem(
                                        title = "بیمه و درمان",
                                        allowance = medicalAllowance,
                                        deduction = medicalDeduction
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(thickness = 0.6.dp, color = Slate200)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // وضعیت پرسنل روز
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "وضعیت پرسنل روز:",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        StatusBadge(text = "${Formatters.toPersianDigits(dayPresentCount)} تمام روز", dotColor = EmeraldAccent)
                                        if (dayHourlyCount > 0) {
                                            StatusBadge(text = "${Formatters.toPersianDigits(dayHourlyCount)} ساعتی", dotColor = CyanAccent)
                                        }
                                        if (dayHalfDayCount > 0) {
                                            StatusBadge(text = "${Formatters.toPersianDigits(dayHalfDayCount)} نصف روز", dotColor = AmberAccent)
                                        }
                                        if (dayAbsentCount > 0) {
                                            StatusBadge(text = "${Formatters.toPersianDigits(dayAbsentCount)} غایب", dotColor = RoseAccent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Status Filter Pill
            item {
                ModernPillSelector(
                    items = listOf("همه", "فعال", "غیرفعال"),
                    selectedItem = statusFilter,
                    onItemSelected = { statusFilter = it },
                    labelProvider = { it }
                )
            }

            // Count indicator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (targetDayFolder != null)
                            "کارگران ${targetDayFolder.dayOfWeek} (${Formatters.toPersianDigits(filteredWorkers.size)} نفر)"
                        else
                            "کارگران (${Formatters.toPersianDigits(filteredWorkers.size)} نفر)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Empty State
            if (filteredWorkers.isEmpty()) {
                item {
                    HairlineCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (dateFolders.isEmpty())
                                    "هنوز روز کاری ثبت نشده است. ابتدا یک روز کاری ایجاد نمایید و سپس کارگران را اضافه کنید."
                                else if (targetDayFolder != null)
                                    "کارگری برای ${targetDayFolder.dayOfWeek} یافت نشد"
                                else
                                    "کارگری یافت نشد",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center
                            )
                            if (dateFolders.isEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LoadingButton(
                                    text = "ایجاد روز کاری",
                                    icon = Icons.Default.Add,
                                    onClick = { isCreatingNextDay = true },
                                    containerColor = AmberAccent,
                                    height = 36.dp,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Worker Cards
            items(filteredWorkers, key = { it.id }) { worker ->
                val perf = performances.find { it.worker.id == worker.id }
                val cardTargetDate = targetDayFolder?.date ?: JalaliCalendar.todayString()
                val cardTargetDayOfWeek = targetDayFolder?.dayOfWeek ?: JalaliCalendar.getDayOfWeek(cardTargetDate)
                val att = if (cardTargetDate.isNotBlank()) {
                    attendanceList.firstOrNull { it.workerId == worker.id && it.date == cardTargetDate }
                } else null
                WorkerItemCard(
                    worker = worker,
                    attendance = att,
                    performance = perf,
                    targetDate = cardTargetDate,
                    targetDayOfWeek = cardTargetDayOfWeek,
                    onClick = {
                        viewingWorker = worker
                        viewingAttendance = att
                    },
                    onEdit = { editingWorker = worker },
                    onDelete = { deletingWorker = worker }
                )
            }
        }
    }

    // Dialog: Add new worker
    if (isAddingWorker && activeDayFolder != null) {
        AddEditWorkerDialog(
            initialWorker = null,
            initialDate = activeDayFolder.date,
            initialDayOfWeek = activeDayFolder.dayOfWeek,
            onDismiss = { isAddingWorker = false },
            onConfirm = { newWorker ->
                viewModel.addWorkerWithDate(
                    worker = newWorker,
                    workDate = activeDayFolder.date,
                    dayOfWeek = activeDayFolder.dayOfWeek
                )
                isAddingWorker = false
            }
        )
    }

    // Dialog: Must Create Day First before adding worker
    if (showMustCreateDayDialog) {
        AlertDialog(
            onDismissRequest = { showMustCreateDayDialog = false },
            icon = {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(28.dp))
            },
            title = {
                Text(
                    text = "تعریف روز کاری الزامی است",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "قبل از افزودن کارگر، ابتدا باید یک روز کاری ثبت نمایید تا وضعیت کارکرد و حساب‌کتاب کارگر در آن روز ثبت شود.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                LoadingButton(
                    text = "ساخت روز کاری",
                    icon = Icons.Default.Add,
                    onClick = {
                        showMustCreateDayDialog = false
                        isCreatingNextDay = true
                    },
                    containerColor = AmberAccent,
                    height = 38.dp,
                    fontSize = 12.sp
                )
            },
            dismissButton = {
                LoadingOutlinedButton(
                    text = "انصراف",
                    onClick = { showMustCreateDayDialog = false },
                    height = 38.dp,
                    fontSize = 12.sp
                )
            }
        )
    }

    // Dialog: Create Next Day
    if (isCreatingNextDay) {
        val baseDateForNext = dateFolders.lastOrNull()?.date ?: selectedDateFolder?.date
        CreateNextDayDialog(
            baseDate = baseDateForNext,
            onDismiss = { isCreatingNextDay = false },
            onConfirm = { date, dayOfWeek ->
                viewModel.addDateFolder(
                    date = date,
                    dayOfWeek = dayOfWeek,
                    title = "روز کاری"
                )
                isCreatingNextDay = false
            }
        )
    }

    // Dialog: Long-press Day Options (ویرایش و حذف روز)
    if (longPressedDateFolder != null) {
        DayActionOptionsDialog(
            dateFolder = longPressedDateFolder!!,
            onDismiss = { longPressedDateFolder = null },
            onEdit = {
                editingDateFolder = longPressedDateFolder
                longPressedDateFolder = null
            },
            onDelete = {
                deletingDateFolder = longPressedDateFolder
                longPressedDateFolder = null
            }
        )
    }

    // Dialog: Edit Day
    if (editingDateFolder != null) {
        EditDateFolderDialog(
            dateFolder = editingDateFolder!!,
            onDismiss = { editingDateFolder = null },
            onConfirm = { date, dayOfWeek, title ->
                viewModel.updateDateFolder(
                    editingDateFolder!!.copy(
                        date = date,
                        dayOfWeek = dayOfWeek,
                        title = title
                    )
                )
                editingDateFolder = null
            }
        )
    }

    // Dialog: Delete Day Confirmation
    if (deletingDateFolder != null) {
        DeleteDayConfirmDialog(
            dateFolder = deletingDateFolder!!,
            onDismiss = { deletingDateFolder = null },
            onConfirm = {
                deletingDateFolder?.let { viewModel.deleteDateFolder(it) }
                deletingDateFolder = null
            }
        )
    }

    // Dialog: Edit Worker
    if (editingWorker != null) {
        AddEditWorkerDialog(
            initialWorker = editingWorker,
            onDismiss = { editingWorker = null },
            onConfirm = {
                viewModel.updateWorker(it)
                editingWorker = null
            }
        )
    }

    // Dialog: View Worker Details
    if (viewingWorker != null) {
        val perf = performances.find { it.worker.id == viewingWorker?.id }
        val currentAtt = viewingAttendance ?: (targetDayFolder?.date ?: selectedDateFolder?.date)?.let { d ->
            attendanceList.firstOrNull { it.workerId == viewingWorker?.id && it.date == d }
        }
        WorkerDetailDialog(
            worker = viewingWorker!!,
            attendance = currentAtt,
            performance = perf,
            onDismiss = {
                viewingWorker = null
                viewingAttendance = null
            },
            onEdit = {
                editingWorker = viewingWorker
                viewingWorker = null
                viewingAttendance = null
            },
            onDelete = {
                deletingWorker = viewingWorker
                viewingWorker = null
                viewingAttendance = null
            }
        )
    }

    // Dialog: Delete Worker
    if (deletingWorker != null) {
        AlertDialog(
            onDismissRequest = { deletingWorker = null },
            title = { Text("حذف کارگر", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = { Text("آیا از حذف ${deletingWorker?.name} اطمینان دارید؟", fontSize = 12.5.sp) },
            confirmButton = {
                LoadingButton(
                    text = "حذف",
                    onClick = {
                        deletingWorker?.let { viewModel.deleteWorker(it) }
                        deletingWorker = null
                    },
                    containerColor = RoseAccent,
                    height = 38.dp,
                    fontSize = 12.sp
                )
            },
            dismissButton = {
                LoadingOutlinedButton(
                    text = "انصراف",
                    onClick = { deletingWorker = null },
                    height = 38.dp,
                    fontSize = 12.sp
                )
            }
        )
    }
}

@Composable
private fun DateFolderCard(
    dateFolder: DateFolderEntity,
    workers: List<WorkerEntity>,
    attendanceList: List<AttendanceEntity>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val totalWages = workers.sumOf { worker ->
        val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == dateFolder.date }
        val isHourly = WageCalculator.isHourly(worker, att)
        val isAbsent = WageCalculator.isAbsent(worker, att)
        val isHalfDay = WageCalculator.isHalfDay(worker, att)
        when {
            isAbsent -> 0L
            isHourly -> {
                val hHours = if (att != null && att.hourlyHours > 0) att.hourlyHours else (if (worker.hourlyHours > 0) worker.hourlyHours else 0.0)
                val hRate = if (att != null && att.hourlyWageRate > 0) att.hourlyWageRate else (if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage)
                (hHours * hRate).toLong()
            }
            isHalfDay -> if (att?.dailyWage != null && att.dailyWage > 0) att.dailyWage else worker.baseDailyWage / 2
            att != null && att.dailyWage > 0 -> att.dailyWage
            else -> worker.baseDailyWage
        }
    }
    val totalTransit = workers.filter { worker ->
        val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == dateFolder.date }
        !WageCalculator.isAbsent(worker, att)
    }.sumOf { it.transitAllowance }
    val totalAccommodation = workers.sumOf { it.accommodationAllowance }
    val totalFood = workers.filter { worker ->
        val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == dateFolder.date }
        !WageCalculator.isAbsent(worker, att)
    }.sumOf { it.foodAllowance }
    val totalMedical = workers.sumOf { it.medicalAllowance }
    val totalOthers = totalFood + totalMedical

    val folderTotalDayPayout = workers.sumOf { worker ->
        val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == dateFolder.date }
        WageCalculator.calculateDayPayout(worker, att)
    }

    HairlineCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("date_folder_${dateFolder.id}"),
        backgroundColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconicsBox(
                        icon = Icons.Default.Folder,
                        color = AmberAccent,
                        size = IconicsSize.MEDIUM
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dateFolder.dayOfWeek,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Formatters.toPersianDigits(dateFolder.date),
                                fontSize = 12.5.sp,
                                color = AmberAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (dateFolder.title.isNotBlank()) {
                            Text(
                                text = dateFolder.title,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${Formatters.toPersianDigits(workers.size)} کارگر ثبت‌شده",
                            fontSize = 11.sp,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Totals breakdown under each day's folder
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(Slate200.copy(alpha = 0.6f))
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مجموع حقوق: ${Formatters.formatCurrency(totalWages)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ایاب و ذهاب: ${Formatters.formatCurrency(totalTransit)}",
                    fontSize = 10.5.sp,
                    color = AmberAccent,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "حق مسکن: ${Formatters.formatCurrency(totalAccommodation)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (totalOthers > 0) {
                    Text(
                        text = "سایر مزایا: ${Formatters.formatCurrency(totalOthers)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }

            // مجموع پرداختی روز در کارت پوشه تاریخ
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(thickness = 0.8.dp, color = Slate200.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مجموع پرداختی روز:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Formatters.formatCurrency(folderTotalDayPayout),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldAccent
                )
            }
        }
    }
}

@Composable
private fun WorkerItemCard(
    worker: WorkerEntity,
    attendance: AttendanceEntity? = null,
    performance: com.example.domain.model.WorkerPerformance?,
    targetDate: String? = null,
    targetDayOfWeek: String? = null,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isHourly = WageCalculator.isHourly(worker, attendance)
    val isAbsent = WageCalculator.isAbsent(worker, attendance)
    val isHalfDay = WageCalculator.isHalfDay(worker, attendance)
    val isFullDay = WageCalculator.isFullDay(worker, attendance)
    var showPhone by remember(worker.id) { mutableStateOf(false) }

    HairlineCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("worker_card_${worker.id}"),
        backgroundColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPhone = !showPhone }
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(worker.colorTag)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = worker.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = worker.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val roleDisplay = if (worker.nationalId.length >= 4) {
                            "${worker.role} • کد: ${Formatters.toPersianDigits(worker.nationalId.takeLast(4))}"
                        } else {
                            worker.role
                        }
                        Text(
                            text = roleDisplay,
                            fontSize = 11.sp,
                            color = AmberAccent,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val statusBadgeInfo: Pair<String, Color> = when {
                        isAbsent -> Pair("غایب", RoseAccent)
                        isHourly -> {
                            val hHours = if (attendance != null && attendance.hourlyHours > 0) attendance.hourlyHours
                                         else (if (worker.hourlyHours > 0) worker.hourlyHours else 0.0)
                            if (hHours > 0) {
                                Pair("${Formatters.toPersianDigits(hHours.toString().removeSuffix(".0"))} ساعت کار", CyanAccent)
                            } else {
                                Pair("ساعتی", CyanAccent)
                            }
                        }
                        isHalfDay -> Pair("نصف روز", AmberAccent)
                        isFullDay -> Pair("تمام روز", EmeraldAccent)
                        else -> Pair("تمام روز", EmeraldAccent)
                    }

                    StatusBadge(
                        text = statusBadgeInfo.first,
                        dotColor = statusBadgeInfo.second
                    )

                    // نمایش شماره تماس زیر نشانگر وضعیت کار هنگام کلیک روی نام کارگر
                    if (showPhone && !worker.phone.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val context = LocalContext.current
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate100,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${worker.phone}"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "شماره تماس",
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = Formatters.toPersianDigits(worker.phone),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = androidx.compose.ui.text.TextStyle(
                                        textDirection = TextDirection.Ltr
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Wage & Allowances breakdown - مستقیماً مرتبط با ورود و خروج
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val isHourly = worker.isHourlyEnabled ||
                                   ((attendance?.hourlyWageRate ?: 0L) > 0L && (attendance?.dailyWage ?: 0L) == 0L)

                    // محاسبه دقیق مبلغ پرداختی نهایی ابتدا انجام می‌شود تا در بالای دستمزدها قرار گیرد
                    val dailyWage = if (isHourly) 0L else when {
                        isAbsent -> 0L
                        isHalfDay -> if (attendance?.dailyWage != null && attendance.dailyWage > 0) attendance.dailyWage else worker.baseDailyWage / 2
                        isFullDay -> if (attendance != null && attendance.dailyWage > 0) attendance.dailyWage else worker.baseDailyWage
                        attendance != null && attendance.dailyWage > 0 -> attendance.dailyWage
                        else -> worker.baseDailyWage
                    }
                    val hHours = if (attendance != null && attendance.hourlyHours > 0) attendance.hourlyHours else (if (worker.hourlyHours > 0) worker.hourlyHours else 0.0)
                    val hRate = if (attendance != null && attendance.hourlyWageRate > 0) attendance.hourlyWageRate else (if (attendance != null && attendance.hourlyWage > 0) attendance.hourlyWage else (if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage))
                    val hourlyPay = if (isHourly && !isAbsent && hHours > 0) (hHours * hRate).toLong() else 0L

                    val otHours = if (attendance != null && attendance.overtimeHours > 0) attendance.overtimeHours else worker.overtimeHours
                    val otRate = if (attendance != null && attendance.overtimeRate > 0) attendance.overtimeRate else worker.overtimeRate
                    val overtimePay = if (!isAbsent && otHours > 0) (otHours * otRate).toLong() else 0L

                    val transitVal = if (!isAbsent) (if (worker.transitImpact == "ALLOWANCE") worker.transitAllowance else -worker.transitAllowance) else 0L
                    val accomVal = if (worker.accommodationImpact == "ALLOWANCE") worker.accommodationAllowance else -worker.accommodationAllowance
                    val foodVal = if (!isAbsent) (if (worker.foodImpact == "ALLOWANCE") worker.foodAllowance else -worker.foodAllowance) else 0L
                    val medVal = if (worker.medicalImpact == "ALLOWANCE") worker.medicalAllowance else -worker.medicalAllowance

                    val netDayPayout = WageCalculator.calculateDayPayout(worker, attendance)

                    // ۱. مبلغ پرداختی نهایی در بالای دستمزدها با قرارگیری عدد درست جلوش بدون فاصله
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مبلغ پرداختی نهایی: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAbsent) RoseAccent else EmeraldAccent
                        )
                        Text(
                            text = if (isAbsent) "۰ تومان (غیبت)" else Formatters.formatCurrency(netDayPayout),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isAbsent) RoseAccent else EmeraldAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ۲. ریز دستمزدها و اضافه کار زیر مبلغ پرداختی نهایی
                    if (isAbsent) {
                        // در صورت غیبت: مبلغی ثبت نمی‌شود و کلمه غیبت با رنگ قرمز به جای مبلغ نوشته می‌شود
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isHourly) "دستمزد ساعتی: " else "دستمزد روزانه: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "غیبت",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseAccent
                            )
                        }
                    } else if (isHourly) {
                        val hourlyTotal = (hHours * hRate).toLong()
                        Text(
                            text = if (hHours > 0) {
                                "دستمزد ساعتی: ${Formatters.toPersianDigits(hHours.toString().removeSuffix(".0"))} ساعت (هر ساعت ${Formatters.formatCurrency(hRate)}) = ${Formatters.formatCurrency(hourlyTotal)}"
                            } else {
                                "دستمزد ساعتی: (هر ساعت ${Formatters.formatCurrency(hRate)}) = ۰ تومان"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = CyanAccent
                        )
                    } else if (isHalfDay) {
                        Text(
                            text = "دستمزد روزانه (نصف روز): ${Formatters.formatCurrency(dailyWage)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AmberAccent
                        )
                    } else if (isFullDay) {
                        Text(
                            text = "دستمزد روزانه: ${Formatters.formatCurrency(dailyWage)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (worker.baseDailyWage > 0) {
                        Text(
                            text = "دستمزد روزانه: ${Formatters.formatCurrency(worker.baseDailyWage)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isAbsent && otHours > 0) {
                        val otTotal = (otHours * otRate).toLong()
                        Text(
                            text = "اضافه کار: ${Formatters.toPersianDigits(otHours.toString().removeSuffix(".0"))} ساعت (هر ساعت ${Formatters.formatCurrency(otRate)}) = ${Formatters.formatCurrency(otTotal)}",
                            fontSize = 10.5.sp,
                            color = AmberAccent
                        )
                    }
                }
            }

            // گزینه های ایاب و ذهاب و غیره زیر مبلغ پرداختی نهایی
            // قانون غیبت: افزایش ایاب و ذهاب و غیره شامل غایب نمی‌شود، اما فقط کسورات شامل می‌شود
            val financialItems = buildList {
                if (worker.transitAllowance > 0) {
                    val isAllowance = worker.transitImpact == "ALLOWANCE"
                    if (!isAbsent || !isAllowance) {
                        add(Triple("ایاب و ذهاب", worker.transitAllowance, isAllowance))
                    }
                }
                if (worker.accommodationAllowance > 0) {
                    val isAllowance = worker.accommodationImpact == "ALLOWANCE"
                    if (!isAbsent || !isAllowance) {
                        add(Triple("حق مسکن", worker.accommodationAllowance, isAllowance))
                    }
                }
                if (worker.foodAllowance > 0) {
                    val isAllowance = worker.foodImpact == "ALLOWANCE"
                    if (!isAbsent || !isAllowance) {
                        add(Triple("خوراک", worker.foodAllowance, isAllowance))
                    }
                }
                if (worker.medicalAllowance > 0) {
                    val isAllowance = worker.medicalImpact == "ALLOWANCE"
                    if (!isAbsent || !isAllowance) {
                        add(Triple("درمان", worker.medicalAllowance, isAllowance))
                    }
                }
            }

                if (financialItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        financialItems.forEach { (label, amount, isIncrease) ->
                            val color = if (isIncrease) EmeraldAccent else RoseAccent
                            val bg = if (isIncrease) EmeraldAccent.copy(alpha = 0.12f) else RoseAccent.copy(alpha = 0.12f)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = bg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$label: ",
                                        fontSize = 10.sp,
                                        color = color,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isIncrease) "+${Formatters.formatThousandsPersian(amount)}" else "-${Formatters.formatThousandsPersian(amount)}",
                                        fontSize = 10.sp,
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

            if (worker.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "یادداشت: ${worker.notes}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DayAllowanceDeductionItem(
    title: String,
    allowance: Long,
    deduction: Long
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Slate100,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (allowance == 0L && deduction == 0L) {
                    Text(
                        text = "۰ تومان",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else {
                    if (allowance > 0L) {
                        Text(
                            text = "+ ${Formatters.formatCurrency(allowance)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent
                        )
                    }
                    if (allowance > 0L && deduction > 0L) {
                        Text(
                            text = "|",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                    if (deduction > 0L) {
                        Text(
                            text = "- ${Formatters.formatCurrency(deduction)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseAccent
                        )
                    }
                }
            }
        }
    }
}

