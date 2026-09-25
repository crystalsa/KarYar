package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.DateFolderEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.WorkerEntity
import com.example.data.local.entity.WorkplaceFolderEntity
import com.example.data.repository.WorkerRepository
import com.example.domain.model.DashboardAnalytics
import com.example.domain.model.WorkerPerformance
import com.example.util.JalaliCalendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyBookkeeping(
    val date: String = JalaliCalendar.todayString(),
    val dayOfWeek: String = JalaliCalendar.todayDayOfWeek(),
    val grandDailyCost: Long = 0L,
    val totalDailyWages: Long = 0L,
    val totalHourlyPay: Long = 0L,
    val totalOvertimePay: Long = 0L,
    val totalBonuses: Long = 0L,
    val dailyExpenses: Long = 0L,
    val workersPresent: Int = 0,
    val totalHours: Double = 0.0,
    val transitCost: Long = 0L,
    val accommodationCost: Long = 0L,
    val foodCost: Long = 0L,
    val medicalCost: Long = 0L,
    val attendances: List<AttendanceEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList()
) {
    val totalDailyCost: Long get() = grandDailyCost
    val totalWagesPaid: Long get() = totalDailyWages + totalHourlyPay
    val totalOvertimePaid: Long get() = totalOvertimePay
    val totalBonusesPaid: Long get() = totalBonuses
    val totalExpensesPaid: Long get() = dailyExpenses
    val workersPresentCount: Int get() = workersPresent
    val totalHoursWorked: Double get() = totalHours
}

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkerRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WorkerRepository(
            folderDao = database.folderDao(),
            dateFolderDao = database.dateFolderDao(),
            workerDao = database.workerDao(),
            attendanceDao = database.attendanceDao(),
            expenseDao = database.expenseDao()
        )
        viewModelScope.launch {
            repository.allFolders.collect { folderList ->
                if (folderList.isEmpty()) {
                    repository.loadSampleData()
                } else if (currentFolder.value == null) {
                    currentFolder.value = folderList.first()
                }
            }
        }
    }

    // All Folders (Workplaces)
    val allFolders: StateFlow<List<WorkplaceFolderEntity>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Selected Workplace Folder
    val currentFolder = MutableStateFlow<WorkplaceFolderEntity?>(null)

    // Date/Day Folders for current workplace folder
    val dateFolders: StateFlow<List<DateFolderEntity>> = currentFolder.flatMapLatest { folder ->
        if (folder != null) repository.getDateFoldersByFolder(folder.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Date Folder (inside Workers & Attendance screens)
    val selectedDateFolder = MutableStateFlow<DateFolderEntity?>(null)

    // Workers for selected folder (all workers in workplace)
    val workers: StateFlow<List<WorkerEntity>> = currentFolder.flatMapLatest { folder ->
        if (folder != null) repository.getWorkersByFolder(folder.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance for selected folder
    val attendanceList: StateFlow<List<AttendanceEntity>> = currentFolder.flatMapLatest { folder ->
        if (folder != null) repository.getAttendanceByFolder(folder.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Workers inside the active date folder (همگام با حضور و غیاب و پوشه تاریخ)
    val workersInDateFolder: StateFlow<List<WorkerEntity>> = combine(
        selectedDateFolder,
        workers,
        attendanceList
    ) { dateFolder, workerList, attList ->
        if (dateFolder != null) {
            val attendedIds = attList.filter { it.date == dateFolder.date }.map { it.workerId }.toSet()
            val specific = workerList.filter { worker ->
                worker.id in attendedIds ||
                worker.dateFolderId == dateFolder.id ||
                worker.workDate == dateFolder.date
            }
            if (specific.isNotEmpty()) specific else workerList
        } else {
            workerList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses for selected folder
    val expenses: StateFlow<List<ExpenseEntity>> = currentFolder.flatMapLatest { folder ->
        if (folder != null) repository.getExpensesByFolder(folder.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Date for Daily Bookkeeping on Dashboard
    val selectedDailyDate = MutableStateFlow<String>(JalaliCalendar.todayString())

    // Worker search state (ذره‌بین بالای صفحه کنار سه نقطه)
    val workerSearchQuery = MutableStateFlow("")
    val isWorkerSearchVisible = MutableStateFlow(false)

    fun setWorkerSearchQuery(query: String) {
        workerSearchQuery.value = query
    }

    fun toggleWorkerSearch(visible: Boolean? = null) {
        val next = visible ?: !isWorkerSearchVisible.value
        isWorkerSearchVisible.value = next
        if (!next) {
            workerSearchQuery.value = ""
        }
    }

    // Daily Bookkeeping (حساب و کتاب روزانه در صفحه اصلی)
    val dailyBookkeeping: StateFlow<DailyBookkeeping> = combine(
        selectedDailyDate,
        workers,
        attendanceList,
        expenses,
        dateFolders
    ) { dateStr, workerList, attList, expList, dfList ->
        val dailyExps = expList.filter { it.date == dateStr }
        val targetDf = dfList.find { it.date == dateStr }
        val dayWorkers = if (targetDf != null) {
            val specific = workerList.filter { it.dateFolderId == targetDf.id || it.workDate == targetDf.date }
            if (specific.isNotEmpty()) specific else workerList
        } else {
            val specific = workerList.filter { it.workDate == dateStr }
            if (specific.isNotEmpty()) specific else workerList
        }

        var wages = 0L
        var hourlyPay = 0L
        var overtimePay = 0L
        var bonuses = 0L
        var allowancesNet = 0L
        var hours = 0.0
        var presentCount = 0

        for (worker in dayWorkers) {
            val att = attList.firstOrNull { it.workerId == worker.id && it.date == dateStr }
            val isAbsent = (att != null && (att.regularHours == 0.0 || att.notes == "غیبت"))
            val isHalfDay = (att != null && (att.regularHours == 4.0 || att.notes == "نصف روز"))

            if (!isAbsent) {
                presentCount++
                val baseWage = when {
                    isHalfDay -> if (att?.dailyWage != null && att.dailyWage > 0) att.dailyWage else worker.baseDailyWage / 2
                    att != null && att.dailyWage > 0 -> att.dailyWage
                    else -> worker.baseDailyWage
                }
                wages += baseWage

                val hHours = if (att != null && att.hourlyHours > 0) att.hourlyHours else worker.hourlyHours
                val hRate = if (att != null && att.hourlyWageRate > 0) att.hourlyWageRate else (if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage)
                val hPay = if (hHours > 0 && hRate > 0) (hHours * hRate).toLong() else 0L
                hourlyPay += hPay

                val otHours = if (att != null && att.overtimeHours > 0) att.overtimeHours else worker.overtimeHours
                val otRate = if (att != null && att.overtimeRate > 0) att.overtimeRate else worker.overtimeRate
                val otPaid = if (otHours > 0) {
                    if (otRate > 0) (otHours * otRate).toLong()
                    else if (att != null && att.hourlyWage > 0) (otHours * att.hourlyWage * 1.4).toLong()
                    else 0L
                } else 0L
                overtimePay += otPaid

                bonuses += (att?.bonus ?: 0L)
                val regHours = if (att != null) att.regularHours else (if (isHalfDay) 4.0 else 8.0)
                hours += (regHours + hHours + otHours)

                val transit = if (worker.transitImpact == "ALLOWANCE") worker.transitAllowance else -worker.transitAllowance
                val accom = if (worker.accommodationImpact == "ALLOWANCE") worker.accommodationAllowance else -worker.accommodationAllowance
                val food = if (worker.foodImpact == "ALLOWANCE") worker.foodAllowance else -worker.foodAllowance
                val med = if (worker.medicalImpact == "ALLOWANCE") worker.medicalAllowance else -worker.medicalAllowance
                allowancesNet += (transit + accom + food + med)
            }
        }

        val transitTotal = dailyExps.filter { it.category == "TRANSIT" }.sumOf { it.amount }
        val accTotal = dailyExps.filter { it.category == "ACCOMMODATION" }.sumOf { it.amount }
        val foodTotal = dailyExps.filter { it.category == "FOOD" }.sumOf { it.amount }
        val medTotal = dailyExps.filter { it.category == "MEDICAL" }.sumOf { it.amount }
        val expensesTotal = dailyExps.sumOf { it.amount }
        val grandDailyCost = wages + hourlyPay + overtimePay + bonuses + allowancesNet + expensesTotal

        DailyBookkeeping(
            date = dateStr,
            dayOfWeek = JalaliCalendar.getDayOfWeek(dateStr),
            grandDailyCost = grandDailyCost.coerceAtLeast(0L),
            totalDailyWages = wages,
            totalHourlyPay = hourlyPay,
            totalOvertimePay = overtimePay,
            totalBonuses = bonuses,
            dailyExpenses = expensesTotal,
            workersPresent = presentCount,
            totalHours = hours,
            transitCost = transitTotal,
            accommodationCost = accTotal,
            foodCost = foodTotal,
            medicalCost = medTotal,
            attendances = attList.filter { it.date == dateStr },
            expenses = dailyExps
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyBookkeeping())

    // Reactive Analytics for the current folder - synced with all days & all workers
    val analytics: StateFlow<DashboardAnalytics> = combine(
        workers,
        attendanceList,
        expenses,
        dateFolders
    ) { workerList, attList, expList, dfList ->
        val todayStr = JalaliCalendar.todayString()
        var totalHours = 0.0
        var totalOtHours = 0.0
        var totalEarlyMins = 0
        var totalWages = 0L
        var totalHourlyPaid = 0L
        var totalOtPay = 0L
        var totalBonuses = 0L
        var totalAllowancesNet = 0L
        var todayCount = 0

        val daysToEvaluate = if (dfList.isNotEmpty()) {
            dfList.map { it.date to it.id }.distinctBy { it.first }
        } else {
            val distinctDates = attList.map { it.date }.distinct()
            if (distinctDates.isNotEmpty()) distinctDates.map { it to null }
            else listOf(todayStr to null)
        }

        val processedAttIds = mutableSetOf<Long>()

        for ((dayDate, dayFolderId) in daysToEvaluate) {
            val dayWorkers = if (dayFolderId != null) {
                val specific = workerList.filter { it.dateFolderId == dayFolderId || it.workDate == dayDate }
                if (specific.isNotEmpty()) specific else workerList
            } else {
                val specific = workerList.filter { it.workDate == dayDate }
                if (specific.isNotEmpty()) specific else workerList
            }

            for (worker in dayWorkers) {
                val att = attList.firstOrNull { it.workerId == worker.id && it.date == dayDate }
                if (att != null) processedAttIds.add(att.id)

                val isAbsent = (att != null && (att.regularHours == 0.0 || att.notes == "غیبت"))
                val isHalfDay = (att != null && (att.regularHours == 4.0 || att.notes == "نصف روز"))

                if (!isAbsent) {
                    if (dayDate == todayStr) todayCount++

                    val baseWage = when {
                        isHalfDay -> if (att?.dailyWage != null && att.dailyWage > 0) att.dailyWage else worker.baseDailyWage / 2
                        att != null && att.dailyWage > 0 -> att.dailyWage
                        else -> worker.baseDailyWage
                    }
                    totalWages += baseWage

                    val hHours = if (att != null && att.hourlyHours > 0) att.hourlyHours else worker.hourlyHours
                    val hRate = if (att != null && att.hourlyWageRate > 0) att.hourlyWageRate else (if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage)
                    totalHourlyPaid += if (hHours > 0 && hRate > 0) (hHours * hRate).toLong() else 0L

                    val otHours = if (att != null && att.overtimeHours > 0) att.overtimeHours else worker.overtimeHours
                    val otRate = if (att != null && att.overtimeRate > 0) att.overtimeRate else worker.overtimeRate
                    totalOtHours += otHours
                    totalOtPay += if (otHours > 0) {
                        if (otRate > 0) (otHours * otRate).toLong()
                        else if (att != null && att.hourlyWage > 0) (otHours * att.hourlyWage * 1.4).toLong()
                        else 0L
                    } else 0L

                    totalBonuses += (att?.bonus ?: 0L)
                    val regHours = if (att != null) att.regularHours else (if (isHalfDay) 4.0 else 8.0)
                    totalHours += (regHours + hHours + otHours)
                    if (att != null) totalEarlyMins += att.earlyDepartureMinutes

                    val transit = if (worker.transitImpact == "ALLOWANCE") worker.transitAllowance else -worker.transitAllowance
                    val accom = if (worker.accommodationImpact == "ALLOWANCE") worker.accommodationAllowance else -worker.accommodationAllowance
                    val food = if (worker.foodImpact == "ALLOWANCE") worker.foodAllowance else -worker.foodAllowance
                    val med = if (worker.medicalImpact == "ALLOWANCE") worker.medicalAllowance else -worker.medicalAllowance
                    totalAllowancesNet += (transit + accom + food + med)
                }
            }
        }

        // Account for any remaining standalone attendance records
        for (att in attList) {
            if (att.id !in processedAttIds) {
                val worker = workerList.find { it.id == att.workerId }
                val isAbsent = (att.regularHours == 0.0 || att.notes == "غیبت")
                if (!isAbsent) {
                    totalWages += att.dailyWage
                    totalHours += att.regularHours
                    totalEarlyMins += att.earlyDepartureMinutes
                    val hRate = if (att.hourlyWageRate > 0) att.hourlyWageRate else (worker?.hourlyWageRate ?: 0L)
                    val hHours = if (att.hourlyHours > 0) att.hourlyHours else (worker?.hourlyHours ?: 0.0)
                    totalHourlyPaid += if (hHours > 0 && hRate > 0) (hHours * hRate).toLong() else 0L
                    val otHours = if (att.overtimeHours > 0) att.overtimeHours else (worker?.overtimeHours ?: 0.0)
                    val otRate = if (att.overtimeRate > 0) att.overtimeRate else (worker?.overtimeRate ?: 0L)
                    totalOtHours += otHours
                    totalOtPay += if (otHours > 0 && otRate > 0) (otHours * otRate).toLong() else 0L
                    totalBonuses += att.bonus
                }
            }
        }

        var transitTotal = 0L
        var accTotal = 0L
        var accDays = 0
        var foodTotal = 0L
        var medicalTotal = 0L
        var otherTotal = 0L
        var individualTotal = 0L
        var groupTotal = 0L

        for (exp in expList) {
            when (exp.category) {
                "TRANSIT" -> transitTotal += exp.amount
                "ACCOMMODATION" -> {
                    accTotal += exp.amount
                    accDays += exp.accommodationDays
                }
                "FOOD" -> foodTotal += exp.amount
                "MEDICAL" -> medicalTotal += exp.amount
                else -> otherTotal += exp.amount
            }

            if (exp.scope == "INDIVIDUAL") individualTotal += exp.amount
            else groupTotal += exp.amount
        }

        val totalExpenses = transitTotal + accTotal + foodTotal + medicalTotal + otherTotal
        val grandTotalCost = (totalWages + totalHourlyPaid + totalOtPay + totalBonuses + totalAllowancesNet + totalExpenses).coerceAtLeast(0L)

        DashboardAnalytics(
            totalWorkersCount = workerList.size,
            activeWorkersCount = workerList.count { it.isActive },
            todayAttendanceCount = todayCount,
            totalWorkHours = totalHours,
            totalOvertimeHours = totalOtHours,
            totalEarlyDepartureMinutes = totalEarlyMins,
            totalWagesPaid = totalWages,
            totalHourlyPaid = totalHourlyPaid,
            totalOvertimePaid = totalOtPay,
            totalBonusesPaid = totalBonuses,
            totalTransitExpenses = transitTotal,
            totalAccommodationExpenses = accTotal,
            totalAccommodationDays = accDays,
            totalFoodExpenses = foodTotal,
            totalMedicalExpenses = medicalTotal,
            totalOtherExpenses = otherTotal,
            totalIndividualExpenses = individualTotal,
            totalGroupExpenses = groupTotal,
            grandTotalExpenses = totalExpenses,
            grandTotalProjectCost = grandTotalCost
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardAnalytics())

    // Worker Performance Summaries (with Allowance / Deduction support and Hourly/Overtime calculations)
    val workerPerformances: StateFlow<List<WorkerPerformance>> = combine(
        workers,
        attendanceList,
        expenses
    ) { workerList, attList, expList ->
        val activeCount = workerList.count { it.isActive }.coerceAtLeast(1)
        val groupExpensesTotal = expList.filter { it.scope == "GROUP" }.sumOf { it.amount }
        val sharePerWorker = groupExpensesTotal / activeCount

        workerList.map { worker ->
            val workerAtts = attList.filter { it.workerId == worker.id }
            val workerExps = expList.filter { it.workerId == worker.id && it.scope == "INDIVIDUAL" }

            val shifts = workerAtts.size
            val regHours = workerAtts.sumOf { it.regularHours }
            val otHours = workerAtts.sumOf { if (it.overtimeHours > 0) it.overtimeHours else worker.overtimeHours }
            val hHours = workerAtts.sumOf { if (it.hourlyHours > 0) it.hourlyHours else worker.hourlyHours }
            val earlyMins = workerAtts.sumOf { it.earlyDepartureMinutes }
            val baseWage = workerAtts.sumOf { it.dailyWage }

            val hourlyPay = workerAtts.sumOf {
                val r = if (it.hourlyWageRate > 0) it.hourlyWageRate else worker.hourlyWageRate
                val h = if (it.hourlyHours > 0) it.hourlyHours else worker.hourlyHours
                (h * r).toLong()
            }

            val otPay = workerAtts.sumOf {
                val r = if (it.overtimeRate > 0) it.overtimeRate else worker.overtimeRate
                val h = if (it.overtimeHours > 0) it.overtimeHours else worker.overtimeHours
                if (h > 0) {
                    if (r > 0) (h * r).toLong()
                    else (h * it.hourlyWage * 1.4).toLong()
                } else 0L
            }

            val bonus = workerAtts.sumOf { it.bonus }
            val earlyDeduction = workerAtts.sumOf { ((it.earlyDepartureMinutes / 60.0) * it.hourlyWage).toLong() }

            // Allowances (افزایشی) from explicit expense entries
            val expAllowances = workerExps.filter { it.impactType == "ALLOWANCE" }
            val expTransitAllowance = expAllowances.filter { it.category == "TRANSIT" }.sumOf { it.amount }
            val expFoodAllowance = expAllowances.filter { it.category == "FOOD" }.sumOf { it.amount }
            val expAccAllowance = expAllowances.filter { it.category == "ACCOMMODATION" }.sumOf { it.amount }
            val expMedAllowance = expAllowances.filter { it.category == "MEDICAL" }.sumOf { it.amount }

            // Base allowances set on worker profile
            val profTransitAllowance = if (worker.transitImpact == "ALLOWANCE") worker.transitAllowance * shifts else 0L
            val profFoodAllowance = if (worker.foodImpact == "ALLOWANCE") worker.foodAllowance * shifts else 0L
            val profAccAllowance = if (worker.accommodationImpact == "ALLOWANCE") worker.accommodationAllowance * shifts else 0L
            val profMedAllowance = if (worker.medicalImpact == "ALLOWANCE") worker.medicalAllowance * shifts else 0L

            val transitAllowanceTotal = expTransitAllowance + profTransitAllowance
            val foodAllowanceTotal = expFoodAllowance + profFoodAllowance
            val accommodationAllowanceTotal = expAccAllowance + profAccAllowance
            val medicalAllowanceTotal = expMedAllowance + profMedAllowance
            val totalAllowances = transitAllowanceTotal + foodAllowanceTotal + accommodationAllowanceTotal + medicalAllowanceTotal

            // Deductions (کاهشی) from explicit expense entries
            val expDeductions = workerExps.filter { it.impactType == "DEDUCTION" }
            val expTransitDeduction = expDeductions.filter { it.category == "TRANSIT" }.sumOf { it.amount }
            val expFoodDeduction = expDeductions.filter { it.category == "FOOD" }.sumOf { it.amount }
            val expAccDeduction = expDeductions.filter { it.category == "ACCOMMODATION" }.sumOf { it.amount }
            val expMedDeduction = expDeductions.filter { it.category == "MEDICAL" }.sumOf { it.amount }

            // Base deductions set on worker profile
            val profTransitDeduction = if (worker.transitImpact == "DEDUCTION") worker.transitAllowance * shifts else 0L
            val profFoodDeduction = if (worker.foodImpact == "DEDUCTION") worker.foodAllowance * shifts else 0L
            val profAccDeduction = if (worker.accommodationImpact == "DEDUCTION") worker.accommodationAllowance * shifts else 0L
            val profMedDeduction = if (worker.medicalImpact == "DEDUCTION") worker.medicalAllowance * shifts else 0L

            val transitDeductionTotal = expTransitDeduction + profTransitDeduction
            val foodDeductionTotal = expFoodDeduction + profFoodDeduction
            val accommodationDeductionTotal = expAccDeduction + profAccDeduction
            val medicalDeductionTotal = expMedDeduction + profMedDeduction
            val totalDeductions = transitDeductionTotal + foodDeductionTotal + accommodationDeductionTotal + medicalDeductionTotal

            // Net Payout Calculation:
            // Base Wage + Hourly Pay + Overtime + Bonus + Allowances (+) - Deductions (-) - Early Departure Penalty
            val netPayout = baseWage + hourlyPay + otPay + bonus + totalAllowances - totalDeductions - earlyDeduction

            WorkerPerformance(
                worker = worker,
                totalShifts = shifts,
                regularHours = regHours,
                hourlyHours = hHours,
                overtimeHours = otHours,
                earlyDepartureMinutes = earlyMins,
                baseWageTotal = baseWage,
                hourlyPayTotal = hourlyPay,
                overtimePayTotal = otPay,
                bonusTotal = bonus,
                earlyDepartureDeduction = earlyDeduction,
                totalAllowances = totalAllowances,
                transitAllowanceTotal = transitAllowanceTotal,
                foodAllowanceTotal = foodAllowanceTotal,
                accommodationAllowanceTotal = accommodationAllowanceTotal,
                medicalAllowanceTotal = medicalAllowanceTotal,
                totalDeductions = totalDeductions,
                transitDeductionTotal = transitDeductionTotal,
                foodDeductionTotal = foodDeductionTotal,
                accommodationDeductionTotal = accommodationDeductionTotal,
                medicalDeductionTotal = medicalDeductionTotal,
                groupExpenseShare = sharePerWorker,
                netPayout = netPayout
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection Handlers
    fun selectFolder(folder: WorkplaceFolderEntity?) {
        currentFolder.value = folder
        selectedDateFolder.value = null
    }

    fun selectDateFolder(dateFolder: DateFolderEntity?) {
        selectedDateFolder.value = dateFolder
    }

    fun selectDailyDate(dateStr: String) {
        selectedDailyDate.value = dateStr
    }

    // Workplace Folder Actions
    fun addFolder(name: String, foremanName: String, employerName: String, colorTag: Long, notes: String = "") {
        viewModelScope.launch {
            val newId = repository.insertFolder(
                WorkplaceFolderEntity(
                    name = name,
                    foremanName = foremanName,
                    employerName = employerName,
                    colorTag = colorTag,
                    notes = notes
                )
            )
            currentFolder.value = WorkplaceFolderEntity(
                id = newId,
                name = name,
                foremanName = foremanName,
                employerName = employerName,
                colorTag = colorTag,
                notes = notes
            )
        }
    }

    fun createFolder(name: String, foremanName: String, employerName: String, notes: String = "", colorTag: Long = 0xFFD97706L) {
        addFolder(name = name, foremanName = foremanName, employerName = employerName, colorTag = colorTag, notes = notes)
    }

    fun updateFolder(folder: WorkplaceFolderEntity) {
        viewModelScope.launch {
            repository.updateFolder(folder)
            if (currentFolder.value?.id == folder.id) {
                currentFolder.value = folder
            }
        }
    }

    fun duplicateFolder(folder: WorkplaceFolderEntity) {
        viewModelScope.launch {
            repository.duplicateFolder(folder)
        }
    }

    fun deleteFolder(folder: WorkplaceFolderEntity) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            if (currentFolder.value?.id == folder.id) {
                currentFolder.value = null
            }
        }
    }

    // Date/Day Folder Actions
    fun addDateFolder(date: String, dayOfWeek: String, title: String, notes: String = "") {
        val folder = currentFolder.value ?: return
        viewModelScope.launch {
            val newId = repository.insertDateFolder(
                DateFolderEntity(
                    folderId = folder.id,
                    date = date,
                    dayOfWeek = dayOfWeek,
                    title = title,
                    notes = notes
                )
            )
            selectedDateFolder.value = DateFolderEntity(
                id = newId,
                folderId = folder.id,
                date = date,
                dayOfWeek = dayOfWeek,
                title = title,
                notes = notes
            )
        }
    }

    fun createDateFolder(folderId: Long, date: String, dayOfWeek: String, title: String, notes: String = "") {
        addDateFolder(date = date, dayOfWeek = dayOfWeek, title = title, notes = notes)
    }

    fun updateDateFolder(dateFolder: DateFolderEntity) {
        viewModelScope.launch {
            repository.updateDateFolder(dateFolder)
            if (selectedDateFolder.value?.id == dateFolder.id) {
                selectedDateFolder.value = dateFolder
            }
        }
    }

    fun deleteDateFolder(dateFolder: DateFolderEntity) {
        viewModelScope.launch {
            repository.deleteDateFolder(dateFolder)
            if (selectedDateFolder.value?.id == dateFolder.id) {
                selectedDateFolder.value = null
            }
        }
    }

    // Worker Actions
    fun addWorkerWithDate(
        worker: WorkerEntity,
        workDate: String,
        dayOfWeek: String,
        title: String = "شیفت کاری"
    ) {
        val folder = currentFolder.value ?: return
        viewModelScope.launch {
            val existingDf = repository.getDateFolderByDate(folder.id, workDate)
            val dateFolder = if (existingDf != null) {
                existingDf
            } else {
                val newDfId = repository.insertDateFolder(
                    DateFolderEntity(
                        folderId = folder.id,
                        date = workDate,
                        dayOfWeek = dayOfWeek,
                        title = title
                    )
                )
                DateFolderEntity(
                    id = newDfId,
                    folderId = folder.id,
                    date = workDate,
                    dayOfWeek = dayOfWeek,
                    title = title
                )
            }
            selectedDateFolder.value = dateFolder

            val finalWorker = worker.copy(
                folderId = folder.id,
                dateFolderId = dateFolder.id,
                workDate = workDate,
                dayOfWeek = dayOfWeek
            )
            val workerId = repository.insertWorker(finalWorker)

            repository.insertAttendance(
                AttendanceEntity(
                    folderId = folder.id,
                    workerId = workerId,
                    date = workDate,
                    dailyWage = finalWorker.baseDailyWage,
                    hourlyWage = if (finalWorker.hourlyWageRate > 0) finalWorker.hourlyWageRate else finalWorker.baseHourlyWage,
                    hourlyWageRate = finalWorker.hourlyWageRate,
                    hourlyHours = finalWorker.hourlyHours,
                    overtimeHours = finalWorker.overtimeHours,
                    overtimeRate = finalWorker.overtimeRate,
                    workplaceName = folder.name,
                    foremanName = folder.foremanName,
                    employerName = folder.employerName,
                    regularHours = 8.0,
                    notes = "تمام روز"
                )
            )
        }
    }

    fun addWorker(worker: WorkerEntity) {
        val folder = currentFolder.value ?: return
        val activeDateFolder = selectedDateFolder.value
        viewModelScope.launch {
            val finalWorker = worker.copy(
                folderId = folder.id,
                dateFolderId = activeDateFolder?.id ?: worker.dateFolderId,
                workDate = activeDateFolder?.date ?: worker.workDate,
                dayOfWeek = activeDateFolder?.dayOfWeek ?: worker.dayOfWeek
            )
            val workerId = repository.insertWorker(finalWorker)

            // If registered in a specific date folder, automatically create a default attendance entry for this day
            if (activeDateFolder != null) {
                repository.insertAttendance(
                    AttendanceEntity(
                        folderId = folder.id,
                        workerId = workerId,
                        date = activeDateFolder.date,
                        dailyWage = finalWorker.baseDailyWage,
                        hourlyWage = if (finalWorker.hourlyWageRate > 0) finalWorker.hourlyWageRate else finalWorker.baseHourlyWage,
                        hourlyWageRate = finalWorker.hourlyWageRate,
                        hourlyHours = finalWorker.hourlyHours,
                        overtimeHours = finalWorker.overtimeHours,
                        overtimeRate = finalWorker.overtimeRate,
                        workplaceName = folder.name,
                        foremanName = folder.foremanName,
                        employerName = folder.employerName,
                        notes = "تمام روز"
                    )
                )
            }
        }
    }

    fun duplicateWorker(
        worker: WorkerEntity,
        targetDate: String? = null,
        targetDateFolderId: Long? = null,
        targetDayOfWeek: String? = null
    ) {
        val folder = currentFolder.value ?: return
        val date = targetDate ?: worker.workDate.ifBlank { JalaliCalendar.todayString() }
        val dow = targetDayOfWeek ?: worker.dayOfWeek.ifBlank { JalaliCalendar.getDayOfWeek(date) }
        val dfId = targetDateFolderId ?: worker.dateFolderId

        viewModelScope.launch {
            val duplicated = worker.copy(
                id = 0,
                name = "${worker.name} (کپی)",
                folderId = folder.id,
                dateFolderId = dfId,
                workDate = date,
                dayOfWeek = dow
            )
            val newWorkerId = repository.insertWorker(duplicated)

            repository.insertAttendance(
                AttendanceEntity(
                    folderId = folder.id,
                    workerId = newWorkerId,
                    date = date,
                    dailyWage = duplicated.baseDailyWage,
                    hourlyWage = if (duplicated.hourlyWageRate > 0) duplicated.hourlyWageRate else duplicated.baseHourlyWage,
                    hourlyWageRate = duplicated.hourlyWageRate,
                    hourlyHours = duplicated.hourlyHours,
                    overtimeHours = duplicated.overtimeHours,
                    overtimeRate = duplicated.overtimeRate,
                    workplaceName = folder.name,
                    foremanName = folder.foremanName,
                    employerName = folder.employerName,
                    regularHours = 8.0,
                    notes = "تمام روز"
                )
            )
        }
    }

    fun updateWorker(worker: WorkerEntity) {
        viewModelScope.launch {
            repository.updateWorker(worker)
            // Synchronize existing attendance records for this worker with updated rates
            val atts = attendanceList.value.filter { it.workerId == worker.id }
            for (att in atts) {
                repository.updateAttendance(
                    att.copy(
                        dailyWage = if (att.dailyWage > 0) worker.baseDailyWage else 0L,
                        hourlyWage = if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage,
                        hourlyWageRate = worker.hourlyWageRate,
                        hourlyHours = worker.hourlyHours,
                        overtimeHours = worker.overtimeHours,
                        overtimeRate = worker.overtimeRate
                    )
                )
            }
        }
    }

    fun deleteWorker(worker: WorkerEntity) {
        viewModelScope.launch { repository.deleteWorker(worker) }
    }

    // Attendance Actions
    fun addAttendance(attendance: AttendanceEntity) {
        val folder = currentFolder.value ?: return
        viewModelScope.launch {
            repository.insertAttendance(
                attendance.copy(
                    folderId = folder.id,
                    workplaceName = folder.name,
                    foremanName = folder.foremanName,
                    employerName = folder.employerName
                )
            )
        }
    }

    fun updateAttendance(attendance: AttendanceEntity) {
        viewModelScope.launch { repository.updateAttendance(attendance) }
    }

    fun setAttendanceStatus(worker: WorkerEntity, dateStr: String, status: String) {
        val folder = currentFolder.value ?: return
        viewModelScope.launch {
            val existing = attendanceList.value.firstOrNull { it.workerId == worker.id && it.date == dateStr }
            if (existing != null) {
                when (status) {
                    "FULL" -> {
                        if (existing.regularHours >= 8.0 && existing.notes != "غیبت" && existing.notes != "نصف روز") {
                            // Already marked full present: toggle off (delete)
                            repository.deleteAttendance(existing)
                        } else {
                            repository.updateAttendance(
                                existing.copy(
                                    regularHours = 8.0,
                                    dailyWage = worker.baseDailyWage,
                                    hourlyWageRate = worker.hourlyWageRate,
                                    hourlyHours = worker.hourlyHours,
                                    overtimeHours = worker.overtimeHours,
                                    overtimeRate = worker.overtimeRate,
                                    notes = "تمام روز"
                                )
                            )
                        }
                    }
                    "HALF" -> {
                        if (existing.regularHours == 4.0 && existing.notes == "نصف روز") {
                            // Already marked half day: toggle off (delete)
                            repository.deleteAttendance(existing)
                        } else {
                            repository.updateAttendance(
                                existing.copy(
                                    regularHours = 4.0,
                                    dailyWage = worker.baseDailyWage / 2,
                                    hourlyWageRate = worker.hourlyWageRate,
                                    hourlyHours = worker.hourlyHours,
                                    overtimeHours = worker.overtimeHours,
                                    overtimeRate = worker.overtimeRate,
                                    notes = "نصف روز"
                                )
                            )
                        }
                    }
                    "ABSENT" -> {
                        if (existing.regularHours == 0.0 || existing.notes == "غیبت") {
                            // Already marked absent: toggle off (delete)
                            repository.deleteAttendance(existing)
                        } else {
                            repository.updateAttendance(
                                existing.copy(
                                    regularHours = 0.0,
                                    overtimeHours = 0.0,
                                    hourlyHours = 0.0,
                                    dailyWage = 0L,
                                    notes = "غیبت"
                                )
                            )
                        }
                    }
                }
            } else {
                when (status) {
                    "FULL" -> {
                        repository.insertAttendance(
                            AttendanceEntity(
                                folderId = folder.id,
                                workerId = worker.id,
                                date = dateStr,
                                regularHours = 8.0,
                                dailyWage = worker.baseDailyWage,
                                hourlyWage = if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage,
                                hourlyWageRate = worker.hourlyWageRate,
                                hourlyHours = worker.hourlyHours,
                                overtimeHours = worker.overtimeHours,
                                overtimeRate = worker.overtimeRate,
                                workplaceName = folder.name,
                                foremanName = folder.foremanName,
                                employerName = folder.employerName,
                                notes = "تمام روز"
                            )
                        )
                    }
                    "HALF" -> {
                        repository.insertAttendance(
                            AttendanceEntity(
                                folderId = folder.id,
                                workerId = worker.id,
                                date = dateStr,
                                regularHours = 4.0,
                                dailyWage = worker.baseDailyWage / 2,
                                hourlyWage = if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage,
                                hourlyWageRate = worker.hourlyWageRate,
                                hourlyHours = worker.hourlyHours,
                                overtimeHours = worker.overtimeHours,
                                overtimeRate = worker.overtimeRate,
                                workplaceName = folder.name,
                                foremanName = folder.foremanName,
                                employerName = folder.employerName,
                                notes = "نصف روز"
                            )
                        )
                    }
                    "ABSENT" -> {
                        repository.insertAttendance(
                            AttendanceEntity(
                                folderId = folder.id,
                                workerId = worker.id,
                                date = dateStr,
                                regularHours = 0.0,
                                dailyWage = 0L,
                                hourlyWage = 0L,
                                hourlyWageRate = 0L,
                                hourlyHours = 0.0,
                                overtimeHours = 0.0,
                                overtimeRate = 0L,
                                workplaceName = folder.name,
                                foremanName = folder.foremanName,
                                employerName = folder.employerName,
                                notes = "غیبت"
                            )
                        )
                    }
                }
            }
        }
    }

    fun toggleAttendanceStatus(worker: WorkerEntity, dateStr: String, isPresent: Boolean) {
        setAttendanceStatus(worker, dateStr, if (isPresent) "FULL" else "ABSENT")
    }

    fun deleteAttendance(attendance: AttendanceEntity) {
        viewModelScope.launch { repository.deleteAttendance(attendance) }
    }

    // Expense Actions
    fun addExpense(expense: ExpenseEntity) {
        val folder = currentFolder.value ?: return
        viewModelScope.launch {
            repository.insertExpense(
                expense.copy(
                    folderId = folder.id,
                    workplaceName = folder.name,
                    foremanName = folder.foremanName,
                    employerName = folder.employerName
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.loadSampleData()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            currentFolder.value = null
            selectedDateFolder.value = null
        }
    }
}
