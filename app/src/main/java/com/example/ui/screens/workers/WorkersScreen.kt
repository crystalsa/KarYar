package com.example.ui.screens.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.util.Formatters

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

    // Dialog states
    var isAddingDateFolder by remember { mutableStateOf(false) }
    var editingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }
    var deletingDateFolder by remember { mutableStateOf<DateFolderEntity?>(null) }

    var isAddingWorker by remember { mutableStateOf(false) }
    var editingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var viewingWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var deletingWorker by remember { mutableStateOf<WorkerEntity?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("همه") }

    val activeWorkersList = if (selectedDateFolder != null) workersInDateFolder else allWorkers

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
            // LEVEL 1: DATE FOLDERS LIST (When no specific date folder is selected)
            if (selectedDateFolder == null) {
                item {
                    HairlineCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Slate100,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
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
                                        text = "پوشه‌های تاریخ و روزهای هفته",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${Formatters.toPersianDigits(dateFolders.size)} پوشه",
                                    fontSize = 11.5.sp,
                                    color = AmberAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "ابتدا یک پوشه روز بسازید و سپس کارگران را داخل آن ثبت کنید.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (dateFolders.isEmpty()) {
                    item {
                        HairlineCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            backgroundColor = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconicsBox(
                                    icon = Icons.Default.CalendarMonth,
                                    color = AmberAccent,
                                    size = IconicsSize.HERO
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "هنوز پوشه تاریخی برای این کارگاه ایجاد نشده است.",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "با زدن دکمه + پایین، اولین پوشه روز (مثلاً شنبه) را ایجاد کنید.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(dateFolders, key = { it.id }) { dateFolder ->
                        val folderWorkers = allWorkers.filter { it.dateFolderId == dateFolder.id }
                        DateFolderCard(
                            dateFolder = dateFolder,
                            workers = folderWorkers,
                            attendanceList = attendanceList,
                            onClick = { viewModel.selectDateFolder(dateFolder) },
                            onEdit = { editingDateFolder = dateFolder },
                            onDelete = { deletingDateFolder = dateFolder }
                        )
                    }
                }
            } else {
                // LEVEL 2: INSIDE A SPECIFIC DATE FOLDER (Workers in this Day/Date)
                item {
                    val currentDF = selectedDateFolder!!
                    HairlineCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = AmberAccent.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.selectDateFolder(null) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "بازگشت به پوشه‌ها",
                                            tint = AmberAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text(
                                            text = "پوشه: ${currentDF.dayOfWeek} ${Formatters.toPersianDigits(currentDF.date)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = AmberAccent
                                        )
                                        if (currentDF.title.isNotBlank()) {
                                            Text(
                                                text = currentDF.title,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                LoadingButton(
                                    text = "بازگشت به روزها",
                                    onClick = { viewModel.selectDateFolder(null) },
                                    containerColor = AmberAccent,
                                    height = 30.dp,
                                    fontSize = 10.5.sp
                                )
                            }

                            // Day Totals Breakdown (مجموع حقوق، ایاب و ذهاب، حق مسکن و بقیه) مرتبط مستقیم با ورود و خروج
                            val currentFolderWorkers = allWorkers.filter { it.dateFolderId == currentDF.id }
                            val cTotalWages = currentFolderWorkers.sumOf { worker ->
                                val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == currentDF.date }
                                when {
                                    att != null && (att.regularHours == 0.0 || att.notes == "غیبت") -> 0L
                                    att != null && (att.regularHours == 4.0 || att.notes == "نصف روز") -> if (att.dailyWage > 0) att.dailyWage else worker.baseDailyWage / 2
                                    att != null && att.dailyWage > 0 -> att.dailyWage
                                    else -> worker.baseDailyWage
                                }
                            }
                            val cTotalTransit = currentFolderWorkers.filter { worker ->
                                val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == currentDF.date }
                                !(att != null && (att.regularHours == 0.0 || att.notes == "غیبت"))
                            }.sumOf { it.transitAllowance }
                            val cTotalAccom = currentFolderWorkers.sumOf { it.accommodationAllowance }
                            val cTotalFood = currentFolderWorkers.filter { worker ->
                                val att = attendanceList.firstOrNull { it.workerId == worker.id && it.date == currentDF.date }
                                !(att != null && (att.regularHours == 0.0 || att.notes == "غیبت"))
                            }.sumOf { it.foodAllowance }
                            val cTotalMed = currentFolderWorkers.sumOf { it.medicalAllowance }
                            val cTotalOthers = cTotalFood + cTotalMed

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "مجموع حقوق: ${Formatters.formatCurrency(cTotalWages)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ایاب و ذهاب: ${Formatters.formatCurrency(cTotalTransit)}",
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "حق مسکن: ${Formatters.formatCurrency(cTotalAccom)}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (cTotalOthers > 0) {
                                    Text(
                                        text = "سایر مزایا: ${Formatters.formatCurrency(cTotalOthers)}",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جستجوی نام کارگر، شغل یا شماره تلفن...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AmberAccent) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_workers_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
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
                            text = "کارگران این روز (${Formatters.toPersianDigits(filteredWorkers.size)} نفر)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "برای جزئیات روی کارت بزنید",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Empty State in Date Folder
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
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (workersInDateFolder.isEmpty())
                                        "هنوز کارگری در این پوشه روز (${selectedDateFolder?.dayOfWeek}) ثبت نشده است."
                                    else
                                        "هیچ کارگری با این فیلتر یافت نشد",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "با زدن دکمه + پایین، کارگران حاضر در این تاریخ را اضافه کنید.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Worker Cards
                items(filteredWorkers, key = { it.id }) { worker ->
                    val perf = performances.find { it.worker.id == worker.id }
                    val targetDate = selectedDateFolder?.date
                    val att = targetDate?.let { d ->
                        attendanceList.firstOrNull { it.workerId == worker.id && it.date == d }
                    }
                    WorkerItemCard(
                        worker = worker,
                        attendance = att,
                        performance = perf,
                        onClick = { viewingWorker = worker },
                        onEdit = { editingWorker = worker },
                        onDelete = { deletingWorker = worker }
                    )
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                if (selectedDateFolder == null) {
                    isAddingDateFolder = true
                } else {
                    isAddingWorker = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .testTag("add_action_fab"),
            containerColor = AmberAccent,
            contentColor = Color.White
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = if (selectedDateFolder == null) "ایجاد پوشه تاریخ و روز" else "افزودن کارگر به این روز"
            )
        }
    }

    // Dialog: Add Date Folder
    if (isAddingDateFolder && folder != null) {
        AddEditDateFolderDialog(
            initialDateFolder = null,
            onDismiss = { isAddingDateFolder = false },
            onConfirm = { date, dayOfWeek, title ->
                viewModel.createDateFolder(folder!!.id, date, dayOfWeek, title)
                isAddingDateFolder = false
            }
        )
    }

    // Dialog: Edit Date Folder
    if (editingDateFolder != null) {
        AddEditDateFolderDialog(
            initialDateFolder = editingDateFolder,
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

    // Dialog: Delete Date Folder
    if (deletingDateFolder != null) {
        AlertDialog(
            onDismissRequest = { deletingDateFolder = null },
            title = { Text("حذف پوشه تاریخ و روز") },
            text = { Text("آیا از حذف پوشه ${deletingDateFolder?.dayOfWeek} ${Formatters.toPersianDigits(deletingDateFolder?.date ?: "")} اطمینان دارید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingDateFolder?.let { viewModel.deleteDateFolder(it) }
                        deletingDateFolder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                ) {
                    Text("حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingDateFolder = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog: Add Worker into currently selected Date Folder
    if (isAddingWorker) {
        AddEditWorkerDialog(
            initialWorker = null,
            onDismiss = { isAddingWorker = false },
            onConfirm = {
                viewModel.addWorker(it)
                isAddingWorker = false
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
            text = { Text("آیا از حذف ${deletingWorker?.name} اطمینان دارید؟ تمام ترددهای ثبت‌شده برای ایشان نیز حذف خواهد شد.", fontSize = 12.5.sp) },
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
        }
    }
}

@Composable
private fun WorkerItemCard(
    worker: WorkerEntity,
    attendance: AttendanceEntity? = null,
    performance: com.example.domain.model.WorkerPerformance?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAbsent = attendance != null && (attendance.regularHours == 0.0 || attendance.notes == "غیبت")
    val isHalfDay = attendance != null && (attendance.regularHours == 4.0 || attendance.notes == "نصف روز")
    val isFullDay = attendance != null && (attendance.regularHours >= 8.0 && attendance.notes != "غیبت" && attendance.notes != "نصف روز")

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
                    modifier = Modifier.weight(1f)
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

                StatusBadge(
                    text = when {
                        isAbsent -> "غایب"
                        isHalfDay -> "نصف روز"
                        isFullDay -> "حاضر"
                        worker.isActive -> "فعال"
                        else -> "مرخصی"
                    },
                    dotColor = when {
                        isAbsent -> RoseAccent
                        isHalfDay -> AmberAccent
                        isFullDay -> EmeraldAccent
                        worker.isActive -> EmeraldAccent
                        else -> RoseAccent
                    }
                )
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
                    } else if (worker.baseDailyWage > 0) {
                        val wageToDisplay = if (attendance != null && attendance.dailyWage > 0) attendance.dailyWage else worker.baseDailyWage
                        Text(
                            text = "دستمزد روزانه: ${Formatters.formatCurrency(wageToDisplay)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
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

                    if (performance != null) {
                        Text(
                            text = "کارکرد: ${Formatters.toPersianDigits(performance.totalShifts)} شیفت | خالص: ${Formatters.formatCurrency(performance.netPayout)}",
                            fontSize = 10.5.sp,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

            // Quick display of active individual allowances / deductions (فقط در صورتی که غایب نباشد)
            if (!isAbsent) {
                val activeAllowances = mutableListOf<String>()
                if (worker.transitAllowance > 0) {
                    val sign = if (worker.transitImpact == "ALLOWANCE") "+" else "-"
                    activeAllowances.add("ایاب‌ذهاب: $sign${Formatters.formatThousandsPersian(worker.transitAllowance)}")
                }
                if (worker.accommodationAllowance > 0) {
                    val sign = if (worker.accommodationImpact == "ALLOWANCE") "+" else "-"
                    activeAllowances.add("مسکن: $sign${Formatters.formatThousandsPersian(worker.accommodationAllowance)}")
                }
                if (worker.foodAllowance > 0) {
                    val sign = if (worker.foodImpact == "ALLOWANCE") "+" else "-"
                    activeAllowances.add("خوراک: $sign${Formatters.formatThousandsPersian(worker.foodAllowance)}")
                }
                if (worker.medicalAllowance > 0) {
                    val sign = if (worker.medicalImpact == "ALLOWANCE") "+" else "-"
                    activeAllowances.add("درمان: $sign${Formatters.formatThousandsPersian(worker.medicalAllowance)}")
                }

                if (activeAllowances.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeAllowances.joinToString(" | "),
                        fontSize = 10.sp,
                        color = AmberAccent,
                        fontWeight = FontWeight.Normal
                    )
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
