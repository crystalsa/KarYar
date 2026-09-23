package com.example.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.WorkerEntity
import com.example.data.local.entity.WorkplaceFolderEntity
import com.example.ui.components.HairlineCard
import com.example.ui.components.ModernPillSelector
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.util.Formatters
import com.example.util.JalaliCalendar

val EXPENSE_CATEGORIES = listOf(
    "TRANSIT" to "ایاب و ذهاب",
    "ACCOMMODATION" to "اسکان و اقامت",
    "FOOD" to "خوراک و غذا",
    "MEDICAL" to "درمان و بهداشت",
    "OTHER" to "سایر هزینه‌ها"
)

@Composable
fun AddExpenseDialog(
    folder: WorkplaceFolderEntity?,
    workers: List<WorkerEntity>,
    onDismiss: () -> Unit,
    onConfirm: (ExpenseEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("TRANSIT") }
    var selectedScope by remember { mutableStateOf("GROUP") } // GROUP or INDIVIDUAL
    var impactType by remember { mutableStateOf("ALLOWANCE") } // ALLOWANCE (افزایشی) or DEDUCTION (کاهشی)
    var selectedWorker by remember { mutableStateOf(workers.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var accommodationDays by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(JalaliCalendar.todayString()) }
    var workplaceName by remember { mutableStateOf(folder?.name ?: "") }
    var employerName by remember { mutableStateOf(folder?.employerName ?: "") }
    var foremanName by remember { mutableStateOf(folder?.foremanName ?: "") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ثبت هزینه برای کارگاه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scope: Group vs Individual
                Text(
                    text = "نوع هزینه (جمعی یا تکی):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                ModernPillSelector(
                    items = listOf("GROUP", "INDIVIDUAL"),
                    selectedItem = selectedScope,
                    onItemSelected = { selectedScope = it },
                    labelProvider = { if (it == "GROUP") "هزینه جمعی کارگاه" else "هزینه تکی کارگر" }
                )

                // Worker Selector and Impact Type (Allowance vs Deduction) if Individual
                if (selectedScope == "INDIVIDUAL") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "انتخاب کارگر مربوطه:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        workers.forEach { worker ->
                            val isSelected = selectedWorker?.id == worker.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AmberAccent else Slate100,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedWorker = worker }
                            ) {
                                Text(
                                    text = worker.name,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    // Impact Type: Addition (کمک‌هزینه) vs Deduction (کسر از حقوق)
                    Text(
                        text = "تاثیر بر حقوق این کارگر:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate100)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isAllowance = impactType == "ALLOWANCE"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isAllowance) EmeraldAccent else Color.Transparent)
                                .clickable { impactType = "ALLOWANCE" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ افزایشی (کمک‌هزینه کارفرما)",
                                fontSize = 11.5.sp,
                                fontWeight = if (isAllowance) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAllowance) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isAllowance) RoseAccent else Color.Transparent)
                                .clickable { impactType = "DEDUCTION" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "- کاهشی (کسر از حقوق کارگر)",
                                fontSize = 11.5.sp,
                                fontWeight = if (!isAllowance) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isAllowance) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips
                Text(
                    text = "دسته‌بندی هزینه:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EXPENSE_CATEGORIES.forEach { (catKey, catTitle) ->
                        val isSelected = selectedCategory == catKey
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AmberAccent else Slate100,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = catKey }
                        ) {
                            Text(
                                text = catTitle,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.5.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان هزینه (مثلاً کرایه مینی‌بوس، خرید ناهار، اقامت)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Amount & Accommodation Days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = Formatters.formatPriceInput(it) },
                        label = { Text("مبلغ (تومان)") },
                        placeholder = { Text("مثلاً ۵۰۰,۰۰۰") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.3f),
                        singleLine = true
                    )
                    if (selectedCategory == "ACCOMMODATION") {
                        OutlinedTextField(
                            value = accommodationDays,
                            onValueChange = { accommodationDays = it },
                            label = { Text("مدت اسکان (روز)") },
                            placeholder = { Text("مثلاً ۳") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("تاریخ (شمسی)") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AmberAccent) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Workplace
                OutlinedTextField(
                    value = workplaceName,
                    onValueChange = { workplaceName = it },
                    label = { Text("نام محل کار / پروژه") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyanAccent) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Employer & Foreman
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = employerName,
                        onValueChange = { employerName = it },
                        label = { Text("نام کارفرما") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AmberAccent) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = foremanName,
                        onValueChange = { foremanName = it },
                        label = { Text("نام سرکارگر") },
                        leadingIcon = { Icon(Icons.Default.Engineering, contentDescription = null, tint = EmeraldAccent) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("توضیحات و جزئیات فاکتور") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button
                Button(
                    onClick = {
                        val parsedAmount = Formatters.parsePrice(amount)
                        if (title.isNotBlank() && parsedAmount > 0) {
                            val w = if (selectedScope == "INDIVIDUAL") selectedWorker else null
                            val entity = ExpenseEntity(
                                folderId = folder?.id ?: 1L,
                                title = title.trim(),
                                category = selectedCategory,
                                scope = selectedScope,
                                impactType = if (selectedScope == "INDIVIDUAL") impactType else "ALLOWANCE",
                                workerId = w?.id,
                                workerName = if (selectedScope == "GROUP") "تمام کارگران (جمعی)" else w?.name,
                                amount = parsedAmount,
                                accommodationDays = Formatters.parseInt(accommodationDays),
                                date = date.trim(),
                                workplaceName = workplaceName.trim(),
                                employerName = employerName.trim(),
                                foremanName = foremanName.trim(),
                                notes = notes.trim()
                            )
                            onConfirm(entity)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_expense_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text(
                        text = "ثبت نهایی هزینه",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
