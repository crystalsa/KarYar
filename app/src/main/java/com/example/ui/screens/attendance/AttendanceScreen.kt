package com.example.ui.screens.attendance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DateFolderEntity
import com.example.data.local.entity.WorkerEntity
import com.example.ui.WorkerViewModel
import com.example.ui.components.HairlineCard
import com.example.ui.components.IconicsBox
import com.example.ui.components.IconicsSize
import com.example.ui.components.LoadingButton
import com.example.ui.components.StatusBadge
import com.example.ui.screens.workers.AddEditDateFolderDialog
import com.example.ui.screens.workers.AddEditWorkerDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.util.Formatters
import com.example.util.JalaliCalendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttendanceScreen(
    viewModel: WorkerViewModel,
    modifier: Modifier = Modifier
) {
    val folder by viewModel.currentFolder.collectAsState()
    val dateFolders by viewModel.dateFolders.collectAsState()
    val selectedDateFolder by viewModel.selectedDateFolder.collectAsState()
    val allWorkers by viewModel.workers.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()

    var isAddingWorker by remember { mutableStateOf(false) }
    var editingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var deletingWorkerFromAttendance by remember { mutableStateOf<WorkerEntity?>(null) }
    var copyingWorkerFromAttendance by remember { mutableStateOf<WorkerEntity?>(null) }
    var isCreatingNextDay by remember { mutableStateOf(false) }

    // Day Management States (Long-press to edit/delete)
    var longPressedDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }
    var editingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }
    var deletingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }

    // Active day folder logic
    val activeDayFolder: DateFolderEntity? = selectedDateFolder ?: dateFolders.firstOrNull()
    val targetDate = activeDayFolder?.date ?: JalaliCalendar.todayString()

    // Workers for the active day: if date folder is selected, filter by date folder if assigned, or show workplace workers
    val dayWorkers: List<WorkerEntity> = if (activeDayFolder != null) {
        val specific = allWorkers.filter { it.dateFolderId == activeDayFolder.id || it.workDate == activeDayFolder.date }
        if (specific.isNotEmpty()) specific else allWorkers
    } else {
        allWorkers
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp)
        ) {
            // Day Folders Selector Header (پوشه‌های روزهای هفته)
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
                                .testTag("create_next_day_button")
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
                            sortedDateFolders.forEach { df ->
                                val isSelected = (activeDayFolder?.id == df.id)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AmberAccent else Slate100,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = { viewModel.selectDateFolder(df) },
                                            onLongClick = { longPressedDateFolder = df }
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

            // Quick Attendance Checkbox Table
            item {
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Header row: Folder icon, Day of week and date right next to each other
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconicsBox(
                                    icon = Icons.Default.Folder,
                                    color = AmberAccent,
                                    size = IconicsSize.TINY
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${activeDayFolder?.dayOfWeek ?: "روز کاری"} ${Formatters.toPersianDigits(targetDate)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            StatusBadge(
                                text = "${Formatters.toPersianDigits(dayWorkers.size)} نفر",
                                dotColor = AmberAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (dayWorkers.isEmpty()) {
                            Text(
                                text = "هنوز کارگری در این روز کاری تعریف نشده است. با دکمه + می‌توانید کارگر جدید اضافه کنید.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            // Table Header Row with 3 columns: حضور, نصف روز, غیبت
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Slate100)
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "نام کارگر",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // 1. Full Day (تمام روز)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.width(42.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "تمام روز",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent
                                        )
                                    }

                                    // 2. Half Day (نصف روز)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.width(42.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "نصف روز",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberAccent
                                        )
                                    }

                                    // 3. Absent (غیبت)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.width(36.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "غیبت",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoseAccent
                                        )
                                    }

                                    // Spacer for 3-dots menu button width
                                    Spacer(modifier = Modifier.width(28.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Worker Rows
                            dayWorkers.forEach { worker ->
                                val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == targetDate }
                                val isFullDay = (att != null && att.regularHours >= 8.0 && att.notes != "غیبت" && att.notes != "نصف روز")
                                val isHalfDay = (att != null && att.regularHours == 4.0 && att.notes == "نصف روز")
                                val isAbsent = (att != null && (att.regularHours == 0.0 || att.notes == "غیبت"))
                                val otHours = if (att != null && att.overtimeHours > 0.0) att.overtimeHours else worker.overtimeHours
                                val hasOvertime = otHours > 0.0

                                val hHours = if (att != null && att.hourlyHours > 0.0) att.hourlyHours else worker.hourlyHours
                                val hasHourly = hHours > 0.0

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp, horizontal = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Worker avatar + name + role (Clickable to edit profile)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { editingWorker = worker }
                                            .padding(vertical = 2.dp, horizontal = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(worker.colorTag)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = worker.name.take(1),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = worker.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val statusDetails = buildList {
                                                add(worker.role)
                                                if (isFullDay) add("تمام روز")
                                                else if (isHalfDay) add("نصف روز")
                                                else if (isAbsent) add("غایب")
                                                if (hasHourly) add("${Formatters.toPersianDigits(hHours.toString().removeSuffix(".0"))}س ساعتی")
                                                if (hasOvertime) add("+${Formatters.toPersianDigits(otHours.toString().removeSuffix(".0"))}س اضافه")
                                            }.joinToString(" • ")

                                            Text(
                                                text = statusDetails,
                                                fontSize = 9.5.sp,
                                                color = if (isAbsent) RoseAccent else if (isHalfDay) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Action Checkboxes: Full Day, Half Day, Absent + 3-dots Menu
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // 1. Full Day Presence Checkbox
                                        Box(
                                            modifier = Modifier.width(42.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Checkbox(
                                                checked = isFullDay,
                                                onCheckedChange = {
                                                    viewModel.setAttendanceStatus(worker, targetDate, "FULL")
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = EmeraldAccent,
                                                    checkmarkColor = Color.White,
                                                    uncheckedColor = EmeraldAccent.copy(alpha = 0.45f)
                                                ),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // 2. Half Day Presence Checkbox
                                        Box(
                                            modifier = Modifier.width(42.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Checkbox(
                                                checked = isHalfDay,
                                                onCheckedChange = {
                                                    viewModel.setAttendanceStatus(worker, targetDate, "HALF")
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = AmberAccent,
                                                    checkmarkColor = Color.White,
                                                    uncheckedColor = AmberAccent.copy(alpha = 0.45f)
                                                ),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // 3. Absence Checkbox
                                        Box(
                                            modifier = Modifier.width(36.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Checkbox(
                                                checked = isAbsent,
                                                onCheckedChange = {
                                                    viewModel.setAttendanceStatus(worker, targetDate, "ABSENT")
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = RoseAccent,
                                                    checkmarkColor = Color.White,
                                                    uncheckedColor = RoseAccent.copy(alpha = 0.45f)
                                                ),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // 3-dots Menu Button (سه نقطه با گزینه حذف و کپی)
                                        var menuExpanded by remember { mutableStateOf(false) }
                                        Box(
                                            modifier = Modifier.width(28.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = { menuExpanded = true },
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .testTag("worker_menu_${worker.id}")
                                            ) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "گزینه‌های کارگر",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = menuExpanded,
                                                onDismissRequest = { menuExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = "کپی",
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.ContentCopy,
                                                            contentDescription = null,
                                                            tint = AmberAccent,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    },
                                                    onClick = {
                                                        menuExpanded = false
                                                        copyingWorkerFromAttendance = worker
                                                    }
                                                )

                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = "حذف",
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = RoseAccent
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = null,
                                                            tint = RoseAccent,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    },
                                                    onClick = {
                                                        menuExpanded = false
                                                        deletingWorkerFromAttendance = worker
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = Slate200.copy(alpha = 0.5f)
                                )
                            }

                            // مجموع پرداختی روز: محاسبه دقیق با لحاظ نصف‌روز، غیبت، و کم و زیاد شدن ایاب و ذهاب، مسکن و غیره
                            val dayTotalPayout = dayWorkers.sumOf { worker ->
                                val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == targetDate }
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

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(thickness = 0.8.dp, color = Slate200)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مجموع پرداختی روز:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Formatters.formatCurrency(dayTotalPayout),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB to add a worker directly into this day and workplace
        FloatingActionButton(
            onClick = { isAddingWorker = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .testTag("add_worker_fab"),
            containerColor = AmberAccent,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "افزودن کارگر")
        }
    }

    // Dialog: Add new worker using AddEditWorkerDialog with auto-created date folder
    if (isAddingWorker) {
        AddEditWorkerDialog(
            initialWorker = null,
            initialDate = activeDayFolder?.date ?: JalaliCalendar.todayString(),
            initialDayOfWeek = activeDayFolder?.dayOfWeek ?: JalaliCalendar.getDayOfWeek(activeDayFolder?.date ?: JalaliCalendar.todayString()),
            onDismiss = { isAddingWorker = false },
            onConfirm = { newWorker ->
                viewModel.addWorkerWithDate(
                    worker = newWorker,
                    workDate = newWorker.workDate.ifBlank { JalaliCalendar.todayString() },
                    dayOfWeek = newWorker.dayOfWeek.ifBlank { JalaliCalendar.getDayOfWeek(newWorker.workDate.ifBlank { JalaliCalendar.todayString() }) }
                )
                isAddingWorker = false
            }
        )
    }

    // Dialog: Edit existing worker's profile when clicking on worker's name
    if (editingWorker != null) {
        AddEditWorkerDialog(
            initialWorker = editingWorker,
            onDismiss = { editingWorker = null },
            onConfirm = { updatedWorker ->
                viewModel.updateWorker(updatedWorker)
                editingWorker = null
            }
        )
    }

    // Dialog: Create Next Day
    if (isCreatingNextDay) {
        val baseDateForNext = dateFolders.lastOrNull()?.date ?: activeDayFolder?.date
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

    // Dialog: Delete Worker Confirmation (با دو گزینه حذف و انصراف و کلمه حذف قرمز رنگ)
    if (deletingWorkerFromAttendance != null) {
        val workerToDelete = deletingWorkerFromAttendance!!
        AlertDialog(
            onDismissRequest = { deletingWorkerFromAttendance = null },
            title = {
                Text(
                    text = "حذف",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "آیا از حذف «${workerToDelete.name}» اطمینان دارید؟",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWorker(workerToDelete)
                        deletingWorkerFromAttendance = null
                    }
                ) {
                    Text(
                        text = "حذف",
                        color = RoseAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingWorkerFromAttendance = null }) {
                    Text(
                        text = "انصراف",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    // Dialog: Copy Worker Confirmation (تکثیر کارگر با دو گزینه تکثیر و انصراف)
    if (copyingWorkerFromAttendance != null) {
        val workerToCopy = copyingWorkerFromAttendance!!
        AlertDialog(
            onDismissRequest = { copyingWorkerFromAttendance = null },
            title = {
                Text(
                    text = "تکثیر",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "آیا از تکثیر «${workerToCopy.name}» اطمینان دارید؟",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.duplicateWorker(
                            worker = workerToCopy,
                            targetDate = targetDate,
                            targetDateFolderId = activeDayFolder?.id,
                            targetDayOfWeek = activeDayFolder?.dayOfWeek
                        )
                        copyingWorkerFromAttendance = null
                    }
                ) {
                    Text(
                        text = "تکثیر",
                        color = AmberAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { copyingWorkerFromAttendance = null }) {
                    Text(
                        text = "انصراف",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}
