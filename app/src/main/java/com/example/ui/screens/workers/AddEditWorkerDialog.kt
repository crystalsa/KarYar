package com.example.ui.screens.workers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.WorkerEntity
import com.example.ui.components.HairlineCard
import com.example.ui.components.IconicsBox
import com.example.ui.components.IconicsSize
import com.example.ui.components.LoadingButton
import com.example.ui.components.LoadingOutlinedButton
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.util.Formatters
import com.example.util.JalaliCalendar

private val PRESET_ROLES = listOf(
    "کارگر ساده", "بنا", "گچ‌کار", "جوشکار", "برق‌کار", "لوله‌کش", "آرماتوربند", "قالب‌بند", "نقاش", "سرکارگر"
)

private val COLOR_OPTIONS = listOf(
    0xFFD97706L, // Amber
    0xFF0284C7L, // Blue
    0xFF059669L, // Emerald
    0xFFE11D48L, // Rose
    0xFF7C3AEDL  // Violet
)

@Composable
fun AddEditWorkerDialog(
    initialWorker: WorkerEntity? = null,
    initialDate: String? = null,
    initialDayOfWeek: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (WorkerEntity) -> Unit
) {
    var workDate by remember {
        mutableStateOf(
            if (!initialWorker?.workDate.isNullOrBlank()) initialWorker!!.workDate
            else if (!initialDate.isNullOrBlank()) initialDate!!
            else JalaliCalendar.todayString()
        )
    }
    var dayOfWeek by remember {
        mutableStateOf(
            if (!initialWorker?.dayOfWeek.isNullOrBlank()) initialWorker!!.dayOfWeek
            else if (!initialDayOfWeek.isNullOrBlank()) initialDayOfWeek!!
            else JalaliCalendar.getDayOfWeek(workDate)
        )
    }

    var name by remember { mutableStateOf(initialWorker?.name ?: "") }
    var role by remember { mutableStateOf(initialWorker?.role ?: "کارگر ساده") }
    var isPhoneEnabled by remember {
        mutableStateOf(initialWorker?.phone?.isNotBlank() == true)
    }
    var phone by remember { mutableStateOf(initialWorker?.phone ?: "") }

    var isNationalIdEnabled by remember {
        mutableStateOf(initialWorker?.nationalId?.isNotBlank() == true)
    }
    var nationalId by remember { mutableStateOf(initialWorker?.nationalId ?: "") }

    var baseDailyWage by remember {
        mutableStateOf(
            if (initialWorker != null && initialWorker.baseDailyWage > 0)
                Formatters.formatThousandsPersian(initialWorker.baseDailyWage)
            else ""
        )
    }

    // Hourly Wage State
    var isHourlyEnabled by remember {
        mutableStateOf(
            initialWorker?.isHourlyEnabled == true ||
            (initialWorker?.hourlyHours ?: 0.0) > 0.0 ||
            (initialWorker?.hourlyWageRate ?: 0L) > 0L
        )
    }
    var hourlyWageRate by remember {
        mutableStateOf(
            if (initialWorker != null && (initialWorker.hourlyWageRate > 0 || initialWorker.baseHourlyWage > 0)) {
                val rate = if (initialWorker.hourlyWageRate > 0) initialWorker.hourlyWageRate else initialWorker.baseHourlyWage
                Formatters.formatThousandsPersian(rate)
            } else ""
        )
    }
    var hourlyHours by remember {
        mutableStateOf(
            if (initialWorker != null && initialWorker.hourlyHours > 0.0)
                initialWorker.hourlyHours.toString().removeSuffix(".0")
            else ""
        )
    }

    // Overtime State
    var isOvertimeEnabled by remember {
        mutableStateOf(
            initialWorker?.isOvertimeEnabled == true ||
            (initialWorker?.overtimeHours ?: 0.0) > 0.0 ||
            (initialWorker?.overtimeRate ?: 0L) > 0L
        )
    }
    var overtimeRate by remember {
        mutableStateOf(
            if (initialWorker != null && initialWorker.overtimeRate > 0)
                Formatters.formatThousandsPersian(initialWorker.overtimeRate)
            else ""
        )
    }
    var overtimeHours by remember {
        mutableStateOf(
            if (initialWorker != null && initialWorker.overtimeHours > 0.0)
                initialWorker.overtimeHours.toString().removeSuffix(".0")
            else ""
        )
    }

    var isActive by remember { mutableStateOf(initialWorker?.isActive ?: true) }
    var notes by remember { mutableStateOf(initialWorker?.notes ?: "") }
    var selectedColor by remember { mutableStateOf(initialWorker?.colorTag ?: 0xFFD97706L) }

    // Allowances & Deductions
    var transitEnabled by remember {
        mutableStateOf(initialWorker != null && initialWorker.transitAllowance > 0)
    }
    var transitAllowance by remember {
        mutableStateOf(
            if (initialWorker != null && initialWorker.transitAllowance > 0)
                Formatters.formatThousandsPersian(initialWorker.transitAllowance)
            else ""
        )
    }
    var transitImpact by remember { mutableStateOf(initialWorker?.transitImpact ?: "ALLOWANCE") }

    var foodEnabled by remember {
        mutableStateOf(initialWorker != null && initialWorker.foodAllowance > 0)
    }
    var foodAllowance by remember {
        mutableStateOf(
            if (initialWorker != null && initialWorker.foodAllowance > 0)
                Formatters.formatThousandsPersian(initialWorker.foodAllowance)
            else ""
        )
    }
    var foodImpact by remember { mutableStateOf(initialWorker?.foodImpact ?: "ALLOWANCE") }

    var accommodationEnabled by remember {
        mutableStateOf(initialWorker != null && initialWorker.accommodationAllowance > 0)
    }
    var accommodationAllowance by remember {
        mutableStateOf(
            if (initialWorker != null && initialWorker.accommodationAllowance > 0)
                Formatters.formatThousandsPersian(initialWorker.accommodationAllowance)
            else ""
        )
    }
    var accommodationImpact by remember { mutableStateOf(initialWorker?.accommodationImpact ?: "DEDUCTION") }

    var medicalEnabled by remember {
        mutableStateOf(initialWorker != null && initialWorker.medicalAllowance > 0)
    }
    var medicalAllowance by remember {
        mutableStateOf(
            if (initialWorker != null && initialWorker.medicalAllowance > 0)
                Formatters.formatThousandsPersian(initialWorker.medicalAllowance)
            else ""
        )
    }
    var medicalImpact by remember { mutableStateOf(initialWorker?.medicalImpact ?: "ALLOWANCE") }

    val initialDailyWage = remember {
        if (initialWorker != null && initialWorker.baseDailyWage > 0)
            Formatters.formatThousandsPersian(initialWorker.baseDailyWage)
        else ""
    }
    val initialHourlyRate = remember {
        if (initialWorker != null && (initialWorker.hourlyWageRate > 0 || initialWorker.baseHourlyWage > 0)) {
            val r = if (initialWorker.hourlyWageRate > 0) initialWorker.hourlyWageRate else initialWorker.baseHourlyWage
            Formatters.formatThousandsPersian(r)
        } else ""
    }
    val initialHourlyHours = remember {
        if (initialWorker != null && initialWorker.hourlyHours > 0.0)
            initialWorker.hourlyHours.toString().removeSuffix(".0")
        else ""
    }
    val initialOvertimeRate = remember {
        if (initialWorker != null && initialWorker.overtimeRate > 0)
            Formatters.formatThousandsPersian(initialWorker.overtimeRate)
        else ""
    }
    val initialOvertimeHours = remember {
        if (initialWorker != null && initialWorker.overtimeHours > 0.0)
            initialWorker.overtimeHours.toString().removeSuffix(".0")
        else ""
    }

    val isModified = (name != (initialWorker?.name ?: "")) ||
            (role != (initialWorker?.role ?: "کارگر ساده")) ||
            (isPhoneEnabled != (initialWorker?.phone?.isNotBlank() == true)) ||
            (phone != (initialWorker?.phone ?: "")) ||
            (isNationalIdEnabled != (initialWorker?.nationalId?.isNotBlank() == true)) ||
            (nationalId != (initialWorker?.nationalId ?: "")) ||
            (baseDailyWage != initialDailyWage) ||
            (isHourlyEnabled != (initialWorker?.isHourlyEnabled == true)) ||
            (hourlyWageRate != initialHourlyRate) ||
            (hourlyHours != initialHourlyHours) ||
            (isOvertimeEnabled != (initialWorker?.isOvertimeEnabled == true)) ||
            (overtimeRate != initialOvertimeRate) ||
            (overtimeHours != initialOvertimeHours) ||
            (notes != (initialWorker?.notes ?: "")) ||
            (transitEnabled != (initialWorker != null && initialWorker.transitAllowance > 0)) ||
            (foodEnabled != (initialWorker != null && initialWorker.foodAllowance > 0)) ||
            (accommodationEnabled != (initialWorker != null && initialWorker.accommodationAllowance > 0)) ||
            (medicalEnabled != (initialWorker != null && initialWorker.medicalAllowance > 0))

    var showUnsavedAlert by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    fun handleRequestClose() {
        if (isModified) {
            showUnsavedAlert = true
        } else {
            onDismiss()
        }
    }

    fun saveWorker() {
        if (name.isNotBlank()) {
            isSaving = true
            val parsedHourlyRate = if (isHourlyEnabled) Formatters.parsePrice(hourlyWageRate) else 0L
            val parsedHourlyHours = if (isHourlyEnabled) Formatters.parseDouble(hourlyHours) else 0.0
            val parsedOvertimeRate = if (isOvertimeEnabled) Formatters.parsePrice(overtimeRate) else 0L
            val parsedOvertimeHours = if (isOvertimeEnabled) Formatters.parseDouble(overtimeHours) else 0.0

            val finalPhone = if (isPhoneEnabled) phone.trim() else ""
            val finalNationalId = if (isNationalIdEnabled) nationalId.trim() else ""

            val entity = (initialWorker ?: WorkerEntity(name = name, role = role)).copy(
                workDate = workDate,
                dayOfWeek = dayOfWeek,
                name = name.trim(),
                role = role.trim(),
                phone = finalPhone,
                nationalId = finalNationalId,
                baseDailyWage = Formatters.parsePrice(baseDailyWage),
                baseHourlyWage = parsedHourlyRate,
                isHourlyEnabled = isHourlyEnabled,
                hourlyWageRate = parsedHourlyRate,
                hourlyHours = parsedHourlyHours,
                isOvertimeEnabled = isOvertimeEnabled,
                overtimeRate = parsedOvertimeRate,
                overtimeHours = parsedOvertimeHours,
                isActive = isActive,
                notes = notes.trim(),
                colorTag = selectedColor,
                transitAllowance = if (transitEnabled) Formatters.parsePrice(transitAllowance) else 0L,
                transitImpact = transitImpact,
                foodAllowance = if (foodEnabled) Formatters.parsePrice(foodAllowance) else 0L,
                foodImpact = foodImpact,
                accommodationAllowance = if (accommodationEnabled) Formatters.parsePrice(accommodationAllowance) else 0L,
                accommodationImpact = accommodationImpact,
                medicalAllowance = if (medicalEnabled) Formatters.parsePrice(medicalAllowance) else 0L,
                medicalImpact = medicalImpact
            )
            onConfirm(entity)
        }
    }

    BackHandler(enabled = true) {
        handleRequestClose()
    }

    Dialog(
        onDismissRequest = { handleRequestClose() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Android-Iconics styled container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconicsBox(
                            icon = if (initialWorker == null) Icons.Default.PersonAdd else Icons.Default.Engineering,
                            color = AmberAccent,
                            size = IconicsSize.SMALL
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (initialWorker == null) "افزودن کارگر" else "ویرایش کارگر",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { handleRequestClose() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Date & Day Display Card: Day first, then Date
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = AmberAccent.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconicsBox(
                            icon = Icons.Default.CalendarMonth,
                            color = AmberAccent,
                            size = IconicsSize.TINY
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val cleanDay = dayOfWeek.ifBlank { JalaliCalendar.getDayOfWeek(workDate) }
                            .replace("روز", "")
                            .trim()
                        Text(
                            text = "$cleanDay  ${Formatters.toPersianDigits(workDate)}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Name & Role compactly stacked
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام کارگر", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Role Presets Chips - compact scrollable row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PRESET_ROLES.forEach { preset ->
                        val isSelected = role == preset
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) AmberAccent else Slate100,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { role = preset }
                        ) {
                            Text(
                                text = preset,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Custom Role Text Field
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("عنوان شغل", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Engineering, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                // --- 1. Phone Section: Toggling preserves phone text ---
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Slate100,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isPhoneEnabled,
                                onCheckedChange = { isPhoneEnabled = it },
                                colors = CheckboxDefaults.colors(checkedColor = EmeraldAccent),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconicsBox(
                                icon = Icons.Default.Phone,
                                color = EmeraldAccent,
                                size = IconicsSize.TINY
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "شماره تماس",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isPhoneEnabled) {
                            Spacer(modifier = Modifier.height(3.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { input ->
                                    val digitsOnly = Formatters.toEnglishDigits(input).filter { it.isDigit() }.take(11)
                                    phone = digitsOnly
                                },
                                label = { Text("شماره تماس", fontSize = 11.sp) },
                                placeholder = { Text("09123456789", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("worker_phone_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldAccent,
                                    unfocusedBorderColor = Slate200,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // --- 2. National ID Section: Toggling preserves nationalId text ---
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Slate100,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isNationalIdEnabled,
                                onCheckedChange = { isNationalIdEnabled = it },
                                colors = CheckboxDefaults.colors(checkedColor = IndigoAccent),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconicsBox(
                                icon = Icons.Default.Badge,
                                color = IndigoAccent,
                                size = IconicsSize.TINY
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "کد ملی",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isNationalIdEnabled) {
                            Spacer(modifier = Modifier.height(3.dp))
                            OutlinedTextField(
                                value = nationalId,
                                onValueChange = { input ->
                                    val digitsOnly = Formatters.toEnglishDigits(input).filter { it.isDigit() }.take(10)
                                    nationalId = digitsOnly
                                },
                                label = { Text("کد ملی", fontSize = 11.sp) },
                                placeholder = { Text("0012345678", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(14.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("worker_national_id_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndigoAccent,
                                    unfocusedBorderColor = Slate200,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Base Daily Wage
                OutlinedTextField(
                    value = baseDailyWage,
                    onValueChange = { baseDailyWage = Formatters.formatPriceInput(it) },
                    label = { Text("دستمزد روزانه (تومان)", fontSize = 12.sp) },
                    placeholder = { Text("مثلاً ۱,۲۰۰,۰۰۰", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                // --- SECTION: Hourly Wage ---
                val parsedHourlyRateVal = Formatters.parsePrice(hourlyWageRate)
                val parsedHourlyHoursVal = Formatters.parseDouble(hourlyHours)
                val hourlyTotal = if (isHourlyEnabled && parsedHourlyRateVal > 0 && parsedHourlyHoursVal > 0) {
                    (parsedHourlyRateVal * parsedHourlyHoursVal).toLong()
                } else 0L

                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Slate100,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isHourlyEnabled,
                                    onCheckedChange = { isHourlyEnabled = it },
                                    colors = CheckboxDefaults.colors(checkedColor = CyanAccent),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "دستمزد ساعتی",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (hourlyTotal > 0) {
                                Text(
                                    text = "جمع: ${Formatters.formatCurrency(hourlyTotal)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                        }

                        if (isHourlyEnabled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = hourlyWageRate,
                                    onValueChange = { hourlyWageRate = Formatters.formatPriceInput(it) },
                                    label = { Text("مبلغ هر ساعت (تومان)", fontSize = 11.sp) },
                                    placeholder = { Text("۱۵۰,۰۰۰", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = hourlyHours,
                                    onValueChange = {
                                        hourlyHours = Formatters.toEnglishDigits(it).filter { ch -> ch.isDigit() || ch == '.' }
                                    },
                                    label = { Text("ساعت کار", fontSize = 11.sp) },
                                    placeholder = { Text("۲", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(0.8f),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // --- SECTION: Overtime ---
                val parsedOtRateVal = Formatters.parsePrice(overtimeRate)
                val parsedOtHoursVal = Formatters.parseDouble(overtimeHours)
                val overtimeTotal = if (isOvertimeEnabled && parsedOtRateVal > 0 && parsedOtHoursVal > 0) {
                    (parsedOtRateVal * parsedOtHoursVal).toLong()
                } else 0L

                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Slate100,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isOvertimeEnabled,
                                    onCheckedChange = { isOvertimeEnabled = it },
                                    colors = CheckboxDefaults.colors(checkedColor = AmberAccent),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "اضافه کاری",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (overtimeTotal > 0) {
                                Text(
                                    text = "جمع: ${Formatters.formatCurrency(overtimeTotal)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberAccent
                                )
                            }
                        }

                        if (isOvertimeEnabled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = overtimeRate,
                                    onValueChange = { overtimeRate = Formatters.formatPriceInput(it) },
                                    label = { Text("نرخ اضافه کار (تومان)", fontSize = 11.sp) },
                                    placeholder = { Text("نرخ هر ساعت", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = overtimeHours,
                                    onValueChange = {
                                        overtimeHours = Formatters.toEnglishDigits(it).filter { ch -> ch.isDigit() || ch == '.' }
                                    },
                                    label = { Text("ساعت اضافه", fontSize = 11.sp) },
                                    placeholder = { Text("۲", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(0.8f),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // --- SECTION: Individual Costs ---
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Slate100,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "هزینه‌های فردی",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "+ اضافه / - کسر",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        // 1. Transit
                        ExpenseSettingRow(
                            title = "ایاب و ذهاب",
                            icon = Icons.Default.DirectionsBus,
                            iconColor = CyanAccent,
                            enabled = transitEnabled,
                            onEnabledChange = { transitEnabled = it },
                            amount = transitAllowance,
                            onAmountChange = { transitAllowance = it },
                            impact = transitImpact,
                            onImpactChange = { transitImpact = it }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 2. Accommodation
                        ExpenseSettingRow(
                            title = "مسکن",
                            icon = Icons.Default.Home,
                            iconColor = IndigoAccent,
                            enabled = accommodationEnabled,
                            onEnabledChange = { accommodationEnabled = it },
                            amount = accommodationAllowance,
                            onAmountChange = { accommodationAllowance = it },
                            impact = accommodationImpact,
                            onImpactChange = { accommodationImpact = it }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. Food
                        ExpenseSettingRow(
                            title = "خوراک",
                            icon = Icons.Default.LocalDining,
                            iconColor = AmberAccent,
                            enabled = foodEnabled,
                            onEnabledChange = { foodEnabled = it },
                            amount = foodAllowance,
                            onAmountChange = { foodAllowance = it },
                            impact = foodImpact,
                            onImpactChange = { foodImpact = it }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 4. Medical
                        ExpenseSettingRow(
                            title = "درمان",
                            icon = Icons.Default.MedicalServices,
                            iconColor = RoseAccent,
                            enabled = medicalEnabled,
                            onEnabledChange = { medicalEnabled = it },
                            amount = medicalAllowance,
                            onAmountChange = { medicalAllowance = it },
                            impact = medicalImpact,
                            onImpactChange = { medicalImpact = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Android Settings-like On/Off Switch for Active/Inactive status & Color palette
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Android settings style Switch with status label
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldAccent,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Slate300
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isActive) "کارگر فعال" else "کارگر غیرفعال",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) EmeraldAccent else Slate500
                        )
                    }

                    // Color palette
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        COLOR_OPTIONS.forEach { colorVal ->
                            val isSelected = selectedColor == colorVal
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .clickable { selectedColor = colorVal },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // LoadingButtonAndroid-inspired Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoadingOutlinedButton(
                        text = "انصراف",
                        onClick = { handleRequestClose() },
                        modifier = Modifier.weight(1f),
                        height = 42.dp
                    )
                    LoadingButton(
                        text = if (initialWorker == null) "ثبت" else "ذخیره",
                        icon = if (initialWorker == null) Icons.Default.PersonAdd else Icons.Default.Save,
                        onClick = { saveWorker() },
                        isLoading = isSaving,
                        modifier = Modifier.weight(1.3f),
                        containerColor = AmberAccent,
                        height = 42.dp,
                        testTag = "submit_worker_button"
                    )
                }
            }
        }
    }

    if (showUnsavedAlert) {
        AlertDialog(
            onDismissRequest = { showUnsavedAlert = false },
            title = {
                Text(
                    text = "تغییرات ذخیره‌نشده",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "اطلاعات ذخیره نشده است. خارج می‌شوید؟",
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                LoadingButton(
                    text = "ذخیره",
                    onClick = {
                        showUnsavedAlert = false
                        saveWorker()
                    },
                    containerColor = AmberAccent,
                    height = 38.dp,
                    fontSize = 12.sp
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedAlert = false
                    onDismiss()
                }) {
                    Text("انصراف", color = RoseAccent, fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
private fun ExpenseSettingRow(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    impact: String,
    onImpactChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = CheckboxDefaults.colors(checkedColor = AmberAccent),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconicsBox(
                    icon = icon,
                    color = iconColor,
                    size = IconicsSize.TINY
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (enabled) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (impact == "ALLOWANCE") EmeraldAccent else Slate100,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onImpactChange("ALLOWANCE") }
                    ) {
                        Text(
                            text = "+ اضافه",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (impact == "ALLOWANCE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (impact == "DEDUCTION") RoseAccent else Slate100,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onImpactChange("DEDUCTION") }
                    ) {
                        Text(
                            text = "- کسر",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (impact == "DEDUCTION") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (enabled) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { onAmountChange(Formatters.formatPriceInput(it)) },
                label = { Text("مبلغ روزانه (تومان)", fontSize = 11.sp) },
                placeholder = { Text("۵۰,۰۰۰", fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}
