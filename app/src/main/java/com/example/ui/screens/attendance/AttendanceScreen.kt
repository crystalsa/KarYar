package com.example.ui.screens.attendance

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

    var isAddingDateFolder by remember { mutableStateOf(false) }
    var isAddingWorker by remember { mutableStateOf(false) }
    var editingWorker by remember { mutableStateOf<WorkerEntity?>(null) }

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

                        // Add new date folder button
                        LoadingButton(
                            text = "روز جدید",
                            icon = Icons.Default.Add,
                            onClick = { isAddingDateFolder = true },
                            containerColor = AmberAccent,
                            height = 32.dp,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Date folder horizontal chips
                    if (dateFolders.isEmpty()) {
                        HairlineCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            backgroundColor = Slate100
                        ) {
                            Text(
                                text = "هنوز پوشه روز کاری ایجاد نشده است. با دکمه «روز جدید» روزهای هفته را ثبت کنید.",
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
                            dateFolders.forEach { df ->
                                val isSelected = (activeDayFolder?.id == df.id)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AmberAccent else Slate100,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.selectDateFolder(df) }
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
                                    text = "نام کارگر (لمس جهت ویرایش)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // 1. Full Day (حضور)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.width(42.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "حضور",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent
                                        )
                                    }

                                    // 2. Half Day (نصف روز)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.width(46.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "نصف روز",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberAccent
                                        )
                                    }

                                    // 3. Absent (غیبت)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.width(40.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "غیبت",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoseAccent
                                        )
                                    }
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
                                        Column {
                                            Text(
                                                text = worker.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = worker.role,
                                                    fontSize = 10.sp,
                                                    color = AmberAccent,
                                                    maxLines = 1
                                                )
                                                if (isHalfDay) {
                                                    Text(
                                                        text = "• نصف روز",
                                                        fontSize = 9.5.sp,
                                                        color = AmberAccent,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                } else if (isAbsent) {
                                                    Text(
                                                        text = "• غایب",
                                                        fontSize = 9.5.sp,
                                                        color = RoseAccent,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                if (hasHourly) {
                                                    Text(
                                                        text = "• ${Formatters.toPersianDigits(hHours.toString().removeSuffix(".0"))}س ساعتی",
                                                        fontSize = 9.5.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                if (hasOvertime) {
                                                    Text(
                                                        text = "• +${Formatters.toPersianDigits(otHours.toString().removeSuffix(".0"))}س اضافه",
                                                        fontSize = 9.5.sp,
                                                        color = EmeraldAccent,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Action Checkboxes: Full Day, Half Day, Absent
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
                                            modifier = Modifier.width(46.dp),
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
                                            modifier = Modifier.width(40.dp),
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
                                    }
                                }
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = Slate200.copy(alpha = 0.5f)
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
            Icon(Icons.Default.PersonAdd, contentDescription = "افزودن کارگر به این روز")
        }
    }

    // Dialog: Add new date/day folder
    if (isAddingDateFolder) {
        AddEditDateFolderDialog(
            initialDateFolder = null,
            onDismiss = { isAddingDateFolder = false },
            onConfirm = { date, dayOfWeek, title ->
                viewModel.addDateFolder(date, dayOfWeek, title)
                isAddingDateFolder = false
            }
        )
    }

    // Dialog: Add new worker using identical AddEditWorkerDialog as Workers screen
    if (isAddingWorker) {
        val currentWorkplace = folder
        AddEditWorkerDialog(
            initialWorker = null,
            onDismiss = { isAddingWorker = false },
            onConfirm = { newWorker ->
                val workerToAdd = newWorker.copy(
                    folderId = currentWorkplace?.id ?: 0L,
                    dateFolderId = activeDayFolder?.id ?: 0L,
                    workDate = targetDate,
                    dayOfWeek = activeDayFolder?.dayOfWeek ?: ""
                )
                viewModel.addWorker(workerToAdd)
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
}
