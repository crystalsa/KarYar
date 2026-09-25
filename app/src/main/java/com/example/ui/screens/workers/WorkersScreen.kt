package com.example.ui.screens.workers

import android.content.Intent
import android.net.Uri
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
    val expenses by viewModel.expenses.collectAsState()
    val searchQuery by viewModel.workerSearchQuery.collectAsState()

    // Dialog states
    var isCreatingNextDay by remember { mutableStateOf(false) }
    var longPressedDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }
    var editingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }
    var deletingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }

    var editingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var viewingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconicsBox(
                                icon = Icons.Default.CalendarMonth,
                                color = AmberAccent,
                                size = IconicsSize.SMALL
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "روزهای کاری و شیفت‌ها",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isCreatingNextDay = true }
                                .testTag("create_next_day_button_workers")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "ساخت روز بعد",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "روز بعد",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
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
                            Text(
                                text = "هنوز روز کاری ثبت نشده است",
                                modifier = Modifier.padding(10.dp),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

            // گزارش هزینه‌ها و پرداخت‌های فقط آن روز (زیر روزها و بالای لیست کارگران)
            if (targetDayFolder != null) {
                item {
                    val dayWorkers = allWorkers
                    var dayPresentCount = 0
                    var dayHalfDayCount = 0
                    var dayAbsentCount = 0

                    var dayBaseWages = 0L
                    var dayHourlyPay = 0L
                    var dayOvertimePay = 0L
                    var dayBonuses = 0L
                    var dayAllowancesNet = 0L

                    for (worker in dayWorkers) {
                        val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == targetDayFolder.date }
                        val isAbsent = (att != null && (att.regularHours == 0.0 || att.notes == "غیبت"))
                        val isHalfDay = (att != null && (att.regularHours == 4.0 || att.notes == "نصف روز"))
                        val isFullDay = (att != null && (att.regularHours >= 8.0 && att.notes != "غیبت" && att.notes != "نصف روز"))

                        when {
                            isAbsent -> dayAbsentCount++
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

                        if (isFullDay || isHalfDay) {
                            val hHours = if (att != null && att.hourlyHours > 0) att.hourlyHours else 0.0
                            val hRate = if (att != null && att.hourlyWageRate > 0) att.hourlyWageRate else (if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage)
                            if (hHours > 0 && hRate > 0) dayHourlyPay += (hHours * hRate).toLong()

                            val otHours = if (att != null && att.overtimeHours > 0) att.overtimeHours else 0.0
                            val otRate = if (att != null && att.overtimeRate > 0) att.overtimeRate else worker.overtimeRate
                            if (otHours > 0 && otRate > 0) dayOvertimePay += (otHours * otRate).toLong()

                            dayBonuses += (att?.bonus ?: 0L)

                            val transit = if (worker.transitImpact == "ALLOWANCE") worker.transitAllowance else -worker.transitAllowance
                            val accom = if (worker.accommodationImpact == "ALLOWANCE") worker.accommodationAllowance else -worker.accommodationAllowance
                            val food = if (worker.foodImpact == "ALLOWANCE") worker.foodAllowance else -worker.foodAllowance
                            val med = if (worker.medicalImpact == "ALLOWANCE") worker.medicalAllowance else -worker.medicalAllowance
                            dayAllowancesNet += (transit + accom + food + med)
                        }
                    }

                    val dayExpenses = expenses.filter { it.date == targetDayFolder.date }
                    val dayExpensesTotal = dayExpenses.sumOf { it.amount }

                    val totalDayPayroll = dayBaseWages + dayHourlyPay + dayOvertimePay + dayBonuses + dayAllowancesNet
                    val totalDayGrandCost = (totalDayPayroll + dayExpensesTotal).coerceAtLeast(0L)

                    HairlineCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        backgroundColor = Slate100,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                    Column {
                                        Text(
                                            text = "گزارش مالی و پرداخت‌های فقط این روز",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${targetDayFolder.dayOfWeek} (${Formatters.toPersianDigits(targetDayFolder.date)})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AmberAccent
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AmberAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "فقط این روز",
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberAccent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Grand Total Row
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldAccent.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "مجموع کل پرداختی و هزینه روز:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(totalDayGrandCost),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldAccent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "دستمزد روزانه:",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(dayBaseWages),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "اضافه‌کاری و ساعتی:",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(dayHourlyPay + dayOvertimePay),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "مزایا و کسورات:",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(dayAllowancesNet),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberAccent
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "هزینه‌های متفرقه روز:",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(dayExpensesTotal),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dayExpensesTotal > 0) RoseAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(thickness = 0.6.dp, color = Slate200)
                            Spacer(modifier = Modifier.height(6.dp))

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
                                text = if (targetDayFolder != null)
                                    "کارگری برای ${targetDayFolder.dayOfWeek} یافت نشد"
                                else
                                    "کارگری یافت نشد",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }

            // Worker Cards
            items(filteredWorkers, key = { it.id }) { worker ->
                val perf = performances.find { it.worker.id == worker.id }
                val cardTargetDate = targetDayFolder?.date ?: worker.workDate
                val cardTargetDayOfWeek = targetDayFolder?.dayOfWeek ?: worker.dayOfWeek
                val att = if (cardTargetDate.isNotBlank()) {
                    attendanceList.firstOrNull { it.workerId == worker.id && it.date == cardTargetDate }
                } else null
                WorkerItemCard(
                    worker = worker,
                    attendance = att,
                    performance = perf,
                    targetDate = cardTargetDate,
                    targetDayOfWeek = cardTargetDayOfWeek,
                    onClick = { viewingWorker = worker },
                    onEdit = { editingWorker = worker },
                    onDelete = { deletingWorker = worker }
                )
            }
        }
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
                    title = "شیفت کاری"
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
        val targetDate = selectedDateFolder?.date
        val att = targetDate?.let { d ->
            attendanceList.firstOrNull { it.workerId == viewingWorker?.id && it.date == d }
        }
        WorkerDetailDialog(
            worker = viewingWorker!!,
            attendance = att,
            performance = perf,
            onDismiss = { viewingWorker = null },
            onEdit = {
                editingWorker = viewingWorker
                viewingWorker = null
            },
            onDelete = {
                deletingWorker = viewingWorker
                viewingWorker = null
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
        when {
            att != null && (att.regularHours == 0.0 || att.notes == "غیبت") -> 0L
            att != null && (att.regularHours == 4.0 || att.notes == "نصف روز") -> if (att.dailyWage > 0) att.dailyWage else worker.baseDailyWage / 2
            att != null && att.dailyWage > 0 -> att.dailyWage
            else -> worker.baseDailyWage
        }
    }
    val totalTransit = workers.filter { worker ->
        val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == dateFolder.date }
        !(att != null && (att.regularHours == 0.0 || att.notes == "غیبت"))
    }.sumOf { it.transitAllowance }
    val totalAccommodation = workers.sumOf { it.accommodationAllowance }
    val totalFood = workers.filter { worker ->
        val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == dateFolder.date }
        !(att != null && (att.regularHours == 0.0 || att.notes == "غیبت"))
    }.sumOf { it.foodAllowance }
    val totalMedical = workers.sumOf { it.medicalAllowance }
    val totalOthers = totalFood + totalMedical

    val folderTotalDayPayout = workers.sumOf { worker ->
        val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == dateFolder.date }
        val isAbsent = (att != null && (att.regularHours == 0.0 || att.notes == "غیبت"))
        val isHalfDay = (att != null && (att.regularHours == 4.0 || att.notes == "نصف روز"))
        if (isAbsent) {
            0L
        } else {
            val baseWage = when {
                isHalfDay -> if (att?.dailyWage != null && att.dailyWage > 0) att.dailyWage else worker.baseDailyWage / 2
                att != null && att.dailyWage > 0 -> att.dailyWage
                else -> worker.baseDailyWage
            }
            val hHours = if (att != null && att.hourlyHours > 0) att.hourlyHours else worker.hourlyHours
            val hRate = if (att != null && att.hourlyWageRate > 0) att.hourlyWageRate else (if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage)
            val hourlyPay = (hHours * hRate).toLong()

            val otHours = if (att != null && att.overtimeHours > 0) att.overtimeHours else worker.overtimeHours
            val otRate = if (att != null && att.overtimeRate > 0) att.overtimeRate else worker.overtimeRate
            val otPay = (otHours * otRate).toLong()

            val transit = if (worker.transitImpact == "ALLOWANCE") worker.transitAllowance else -worker.transitAllowance
            val accom = if (worker.accommodationImpact == "ALLOWANCE") worker.accommodationAllowance else -worker.accommodationAllowance
            val food = if (worker.foodImpact == "ALLOWANCE") worker.foodAllowance else -worker.foodAllowance
            val med = if (worker.medicalImpact == "ALLOWANCE") worker.medicalAllowance else -worker.medicalAllowance

            (baseWage + hourlyPay + otPay + transit + accom + food + med).coerceAtLeast(0L)
        }
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = AmberAccent, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = RoseAccent, modifier = Modifier.size(16.dp))
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
    val isAbsent = attendance != null && (attendance.regularHours == 0.0 || attendance.notes == "غیبت")
    val isHalfDay = attendance != null && (attendance.regularHours == 4.0 || attendance.notes == "نصف روز")
    val isFullDay = attendance != null && (attendance.regularHours >= 8.0 && attendance.notes != "غیبت" && attendance.notes != "نصف روز")
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
                        Text(
                            text = worker.role,
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
                    StatusBadge(
                        text = when {
                            isAbsent -> "غایب"
                            isHalfDay -> "نصف روز"
                            isFullDay -> "تمام روز"
                            worker.isActive -> if (attendance != null) "ثبت‌شده" else "ثبت‌نشده"
                            else -> "مرخصی"
                        },
                        dotColor = when {
                            isAbsent -> RoseAccent
                            isHalfDay -> AmberAccent
                            isFullDay -> EmeraldAccent
                            worker.isActive -> if (attendance != null) EmeraldAccent else Slate400
                            else -> RoseAccent
                        }
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

            // نمایش تاریخ و روز کاری روی کارت کارگر (این تاریخ و روز هم در قسمت کارگران هم نمایش داده بشه)
            val displayDate = targetDate?.ifBlank { null } ?: worker.workDate.ifBlank { attendance?.date ?: "" }
            val displayDow = targetDayOfWeek?.ifBlank { null } ?: worker.dayOfWeek.ifBlank { if (displayDate.isNotBlank()) JalaliCalendar.getDayOfWeek(displayDate) else "" }
            if (displayDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AmberAccent.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$displayDow ${Formatters.toPersianDigits(displayDate)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Wage & Allowances breakdown - مستقیماً مرتبط با ورود و خروج
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isAbsent) {
                        // در صورت غیبت: مبلغی ثبت نمی‌شود و کلمه غیبت با رنگ قرمز به جای مبلغ نوشته می‌شود
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "دستمزد روزانه: ",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "غیبت",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseAccent
                            )
                        }
                    } else if (isHalfDay) {
                        // در صورت نصف روز: نصف دستمزد روزانه محاسبه و نمایش داده می‌شود
                        val halfWage = if (attendance?.dailyWage != null && attendance.dailyWage > 0) {
                            attendance.dailyWage
                        } else {
                            worker.baseDailyWage / 2
                        }
                        Text(
                            text = "دستمزد روزانه (نصف روز): ${Formatters.formatCurrency(halfWage)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent
                        )
                    } else if (isFullDay) {
                        val wageToDisplay = if (attendance != null && attendance.dailyWage > 0) attendance.dailyWage else worker.baseDailyWage
                        Text(
                            text = "دستمزد روزانه: ${Formatters.formatCurrency(wageToDisplay)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else if (worker.baseDailyWage > 0) {
                        Text(
                            text = "دستمزد پایه: ${Formatters.formatCurrency(worker.baseDailyWage)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isAbsent && (worker.isHourlyEnabled || (attendance?.hourlyHours ?: 0.0) > 0 || worker.hourlyHours > 0)) {
                        val hHours = if (attendance != null && attendance.hourlyHours > 0) attendance.hourlyHours else worker.hourlyHours
                        val hRate = if (attendance != null && attendance.hourlyWageRate > 0) attendance.hourlyWageRate else (if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage)
                        if (hHours > 0) {
                            Text(
                                text = "ساعتی: ${Formatters.toPersianDigits(hHours.toString().removeSuffix(".0"))} ساعت (${Formatters.formatCurrency(hRate)})",
                                fontSize = 10.5.sp,
                                color = CyanAccent
                            )
                        }
                    }

                    if (!isAbsent && (worker.isOvertimeEnabled || (attendance?.overtimeHours ?: 0.0) > 0 || worker.overtimeHours > 0)) {
                        val otHours = if (attendance != null && attendance.overtimeHours > 0) attendance.overtimeHours else worker.overtimeHours
                        val otRate = if (attendance != null && attendance.overtimeRate > 0) attendance.overtimeRate else worker.overtimeRate
                        if (otHours > 0) {
                            Text(
                                text = "اضافه کار: ${Formatters.toPersianDigits(otHours.toString().removeSuffix(".0"))} ساعت (${Formatters.formatCurrency(otRate)})",
                                fontSize = 10.5.sp,
                                color = AmberAccent
                            )
                        }
                    }

                    // محاسبه دقیق مبلغ پرداختی نهایی
                    val dailyWage = when {
                        isAbsent -> 0L
                        isHalfDay -> if (attendance?.dailyWage != null && attendance.dailyWage > 0) attendance.dailyWage else worker.baseDailyWage / 2
                        isFullDay -> if (attendance != null && attendance.dailyWage > 0) attendance.dailyWage else worker.baseDailyWage
                        attendance != null && attendance.dailyWage > 0 -> attendance.dailyWage
                        else -> worker.baseDailyWage
                    }
                    val hHours = if (attendance != null && attendance.hourlyHours > 0) attendance.hourlyHours else worker.hourlyHours
                    val hRate = if (attendance != null && attendance.hourlyWageRate > 0) attendance.hourlyWageRate else (if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage)
                    val hourlyPay = if (!isAbsent && hHours > 0) (hHours * hRate).toLong() else 0L

                    val otHours = if (attendance != null && attendance.overtimeHours > 0) attendance.overtimeHours else worker.overtimeHours
                    val otRate = if (attendance != null && attendance.overtimeRate > 0) attendance.overtimeRate else worker.overtimeRate
                    val overtimePay = if (!isAbsent && otHours > 0) (otHours * otRate).toLong() else 0L

                    val transitVal = if (!isAbsent) (if (worker.transitImpact == "ALLOWANCE") worker.transitAllowance else -worker.transitAllowance) else 0L
                    val accomVal = if (worker.accommodationImpact == "ALLOWANCE") worker.accommodationAllowance else -worker.accommodationAllowance
                    val foodVal = if (!isAbsent) (if (worker.foodImpact == "ALLOWANCE") worker.foodAllowance else -worker.foodAllowance) else 0L
                    val medVal = if (worker.medicalImpact == "ALLOWANCE") worker.medicalAllowance else -worker.medicalAllowance

                    val netDayPayout = if (isAbsent) 0L else (dailyWage + hourlyPay + overtimePay + transitVal + accomVal + foodVal + medVal).coerceAtLeast(0L)

                    // مبلغ پرداختی نهایی با رنگ سبز - کلمه شیفت ۱ به طور کامل حذف شد
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مبلغ پرداختی نهایی: ",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent
                        )
                        Text(
                            text = if (isAbsent) "۰ تومان (غیبت)" else Formatters.formatCurrency(netDayPayout),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAbsent) RoseAccent else EmeraldAccent
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = AmberAccent, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = RoseAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // گزینه های ایاب و ذهاب و غیره زیر مبلغ پرداختی نهایی
            // اگر افزایشی بود با رنگ سبز و اگر کاهشی بود با رنگ قرمز
            if (!isAbsent) {
                val financialItems = buildList {
                    if (worker.transitAllowance > 0) {
                        add(Triple("ایاب و ذهاب", worker.transitAllowance, worker.transitImpact == "ALLOWANCE"))
                    }
                    if (worker.accommodationAllowance > 0) {
                        add(Triple("حق مسکن", worker.accommodationAllowance, worker.accommodationImpact == "ALLOWANCE"))
                    }
                    if (worker.foodAllowance > 0) {
                        add(Triple("خوراک", worker.foodAllowance, worker.foodImpact == "ALLOWANCE"))
                    }
                    if (worker.medicalAllowance > 0) {
                        add(Triple("درمان", worker.medicalAllowance, worker.medicalImpact == "ALLOWANCE"))
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
