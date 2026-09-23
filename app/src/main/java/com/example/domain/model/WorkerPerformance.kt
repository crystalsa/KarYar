package com.example.domain.model

import com.example.data.local.entity.WorkerEntity

data class WorkerPerformance(
    val worker: WorkerEntity,
    val totalShifts: Int,
    val regularHours: Double,
    val hourlyHours: Double = 0.0,
    val overtimeHours: Double,
    val earlyDepartureMinutes: Int,
    val baseWageTotal: Long,
    val hourlyPayTotal: Long = 0L,
    val overtimePayTotal: Long,
    val bonusTotal: Long,
    val earlyDepartureDeduction: Long,

    // Allowances (افزایشی - اضافه به دریافتی شخص مانند کمک هزینه)
    val totalAllowances: Long = 0L,
    val transitAllowanceTotal: Long = 0L,
    val foodAllowanceTotal: Long = 0L,
    val accommodationAllowanceTotal: Long = 0L,
    val medicalAllowanceTotal: Long = 0L,

    // Deductions (کاهشی - کسورات از حقوق شخص مانند سهم اسکان/غذا)
    val totalDeductions: Long = 0L,
    val transitDeductionTotal: Long = 0L,
    val foodDeductionTotal: Long = 0L,
    val accommodationDeductionTotal: Long = 0L,
    val medicalDeductionTotal: Long = 0L,

    val groupExpenseShare: Long,
    val netPayout: Long
) {
    val individualExpensesTotal: Long
        get() = totalAllowances + totalDeductions
}
