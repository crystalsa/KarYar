package com.example.util

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.AttendanceStatus
import com.example.data.local.entity.WorkerEntity
import com.example.domain.model.WorkerPerformance
import kotlin.math.round

/**
 * WageCalculator is the single source of truth for all wage, overtime,
 * allowance, deduction, shift counting, and net payout calculations in KarYar.
 *
 * Rules:
 * 1. Absent day (روز غیبت):
 *    - Base daily wage, hourly pay, overtime, allowances, and deductions are ALL ZERO.
 *    - Absent days do NOT count towards working shifts (isWorkingDay = false).
 * 2. Half day (نصف روز):
 *    - Base daily wage is half of worker's daily wage: roundToLong(worker.baseDailyWage / 2.0).
 *    - Allowances and deductions apply once per present day.
 * 3. Hourly (ساعتی):
 *    - Hourly pay is roundToLong(hours * rate).
 *    - Overtime is calculated ONLY when hours > 0 and the worker is not absent.
 * 4. Overtime multiplier:
 *    - Configurable parameter [overtimeMultiplier] with default 1.4.
 * 5. Uniform rounding:
 *    - All amounts are rounded using [roundToLong] instead of integer truncation.
 * 6. Minimum net payout:
 *    - Net payout cannot be negative; capped at 0.
 */
object WageCalculator {

    const val DEFAULT_OVERTIME_MULTIPLIER: Double = 1.4

    /**
     * Standard rounding function used across all financial calculations.
     * Prevents truncation errors by rounding mathematically (half-up).
     */
    fun roundToLong(value: Double): Long = kotlin.math.floor(value + 0.5).toLong()

    /**
     * Determines whether a worker is an hourly worker based on their profile or attendance record.
     */
    fun isHourly(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        if (att != null) {
            if (att.status == AttendanceStatus.HOURLY) return true
            if (att.hourlyHours > 0 || (att.hourlyWageRate > 0 && att.dailyWage == 0L)) return true
            if (att.notes == "ساعتی") return true
        }
        return worker.isHourlyEnabled
    }

    /**
     * Determines whether a worker is absent for a given attendance record.
     * Rule: Absent if status is ABSENT or notes contain "غیبت".
     * Daily workers with 0 regular hours and not marked half/full day note are also absent.
     */
    fun isAbsent(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        if (att == null) return false
        if (att.status == AttendanceStatus.ABSENT) return true
        if (att.notes == "غیبت" || att.notes.contains("غیبت")) return true
        val hourly = isHourly(worker, att)
        return if (hourly) {
            // For an hourly worker, absent ONLY if explicitly marked as ABSENT
            false
        } else {
            // For a daily worker, absent if regularHours == 0 and not marked half day or noted full day
            att.regularHours == 0.0 &&
                att.status != AttendanceStatus.HALF_DAY &&
                !att.notes.contains("نصف روز") &&
                !att.notes.contains("تمام روز")
        }
    }

    /**
     * Determines whether a worker worked a half-day shift (نصف روز).
     */
    fun isHalfDay(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        if (isHourly(worker, att)) return false
        if (isAbsent(worker, att)) return false
        return att != null && (att.status == AttendanceStatus.HALF_DAY || att.regularHours == 4.0 || att.notes == "نصف روز" || att.notes.contains("نصف روز"))
    }

    /**
     * Determines whether a worker worked a full-day shift (تمام روز).
     */
    fun isFullDay(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        if (isHourly(worker, att)) return false
        if (isAbsent(worker, att)) return false
        if (isHalfDay(worker, att)) return false
        return att == null || att.status == AttendanceStatus.FULL_DAY || att.regularHours >= 8.0 || att.notes == "تمام روز" || att.notes.contains("تمام روز")
    }

    /**
     * Detailed single-day calculation result for a worker.
     */
    fun calculateDay(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): DayCalculationResult {
        if (isAbsent(worker, att)) {
            // Rule: Absent day (کارگر غایب):
            // - دستمزد روزانه، دستمزد ساعتی، اضافه کاری و مزایا (کمک‌هزینه‌های افزایشی) شامل نمی‌شود (صفر).
            // - فقط کسورات (کسر ایاب و ذهاب، مسکن، خوراک، درمان) شامل می‌شود.
            val transitDeduction = if (worker.transitImpact == "DEDUCTION") worker.transitAllowance else 0L
            val foodDeduction = if (worker.foodImpact == "DEDUCTION") worker.foodAllowance else 0L
            val accomDeduction = if (worker.accommodationImpact == "DEDUCTION") worker.accommodationAllowance else 0L
            val medDeduction = if (worker.medicalImpact == "DEDUCTION") worker.medicalAllowance else 0L
            val totalDeductions = transitDeduction + foodDeduction + accomDeduction + medDeduction

            return DayCalculationResult(
                isWorkingDay = false,
                regularHours = 0.0,
                hourlyHours = 0.0,
                overtimeHours = 0.0,
                baseWage = 0L,
                hourlyPay = 0L,
                overtimePay = 0L,
                totalAllowances = 0L,
                totalDeductions = totalDeductions,
                netPayout = 0L,
                transitAllowance = 0L,
                foodAllowance = 0L,
                accommodationAllowance = 0L,
                medicalAllowance = 0L,
                transitDeduction = transitDeduction,
                foodDeduction = foodDeduction,
                accommodationDeduction = accomDeduction,
                medicalDeduction = medDeduction
            )
        }

        val hourly = isHourly(worker, att)
        val halfDay = isHalfDay(worker, att)

        // 1. Base Daily Wage
        val baseWage = if (hourly) 0L else when {
            halfDay -> {
                if (att?.dailyWage != null && att.dailyWage > 0L) att.dailyWage
                else roundToLong(worker.baseDailyWage / 2.0)
            }
            att != null && att.dailyWage > 0L -> att.dailyWage
            else -> worker.baseDailyWage
        }

        // 2. Hourly Pay
        val hHours = if (att != null && att.hourlyHours > 0.0) att.hourlyHours
                     else if (worker.hourlyHours > 0.0) worker.hourlyHours else 0.0
        val hRate = if (att != null && att.hourlyWageRate > 0L) att.hourlyWageRate
                    else if (att != null && att.hourlyWage > 0L) att.hourlyWage
                    else if (worker.hourlyWageRate > 0L) worker.hourlyWageRate
                    else worker.baseHourlyWage
        val hourlyPay = if (hourly && hHours > 0.0 && hRate > 0L) roundToLong(hHours * hRate.toDouble()) else 0L

        // 3. Overtime Pay
        // Rule: Overtime only calculated if worker is not absent and (if hourly) hours > 0
        val otHours = if (att != null && att.overtimeHours > 0.0) att.overtimeHours else worker.overtimeHours
        val otRate = if (att != null && att.overtimeRate > 0L) att.overtimeRate
                     else if (worker.overtimeRate > 0L) worker.overtimeRate
                     else {
                         val baseRate = if (hRate > 0L) hRate.toDouble()
                                        else if (worker.baseDailyWage > 0L) (worker.baseDailyWage.toDouble() / 8.0)
                                        else 0.0
                         roundToLong(baseRate * overtimeMultiplier)
                     }

        val canHaveOvertime = if (hourly) hHours > 0.0 else true
        val overtimePay = if (canHaveOvertime && otHours > 0.0 && otRate > 0L) roundToLong(otHours * otRate.toDouble()) else 0L

        // 4. Regular Hours
        val regHours = if (hourly) 0.0
                       else if (att != null && att.regularHours > 0.0) att.regularHours
                       else if (halfDay) 4.0
                       else 8.0

        // 5. Allowances and Deductions (applied once per working day)
        val transitAllowance = if (worker.transitImpact == "ALLOWANCE") worker.transitAllowance else 0L
        val transitDeduction = if (worker.transitImpact == "DEDUCTION") worker.transitAllowance else 0L

        val foodAllowance = if (worker.foodImpact == "ALLOWANCE") worker.foodAllowance else 0L
        val foodDeduction = if (worker.foodImpact == "DEDUCTION") worker.foodAllowance else 0L

        val accomAllowance = if (worker.accommodationImpact == "ALLOWANCE") worker.accommodationAllowance else 0L
        val accomDeduction = if (worker.accommodationImpact == "DEDUCTION") worker.accommodationAllowance else 0L

        val medAllowance = if (worker.medicalImpact == "ALLOWANCE") worker.medicalAllowance else 0L
        val medDeduction = if (worker.medicalImpact == "DEDUCTION") worker.medicalAllowance else 0L

        val totalAllowances = transitAllowance + foodAllowance + accomAllowance + medAllowance
        val totalDeductions = transitDeduction + foodDeduction + accomDeduction + medDeduction

        // 6. Net Payout (Gross - Deductions, capped at 0)
        val gross = baseWage + hourlyPay + overtimePay + totalAllowances
        val net = (gross - totalDeductions).coerceAtLeast(0L)

        return DayCalculationResult(
            isWorkingDay = true,
            regularHours = regHours,
            hourlyHours = if (hourly) hHours else 0.0,
            overtimeHours = otHours,
            baseWage = baseWage,
            hourlyPay = hourlyPay,
            overtimePay = overtimePay,
            totalAllowances = totalAllowances,
            totalDeductions = totalDeductions,
            netPayout = net,
            transitAllowance = transitAllowance,
            foodAllowance = foodAllowance,
            accommodationAllowance = accomAllowance,
            medicalAllowance = medAllowance,
            transitDeduction = transitDeduction,
            foodDeduction = foodDeduction,
            accommodationDeduction = accomDeduction,
            medicalDeduction = medDeduction
        )
    }

    /**
     * Calculates the exact net daily payout for a worker on a specific date.
     */
    fun calculateDayPayout(
        worker: WorkerEntity,
        att: AttendanceEntity?,
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): Long = calculateDay(worker, att, overtimeMultiplier).netPayout

    /**
     * Aggregates all attendance records for a single unique worker person into a [WorkerPerformance].
     * Rule: Absent days are ignored for shift count, pay, and allowance accumulation.
     */
    fun calculateWorkerPerformance(
        worker: WorkerEntity,
        attendances: List<AttendanceEntity>,
        groupExpenseShare: Long = 0L,
        overtimeMultiplier: Double = DEFAULT_OVERTIME_MULTIPLIER
    ): WorkerPerformance {
        var shifts = 0
        var regHours = 0.0
        var hHours = 0.0
        var otHours = 0.0
        var baseWageTotal = 0L
        var hourlyPayTotal = 0L
        var otPayTotal = 0L

        var transitAllowanceTotal = 0L
        var foodAllowanceTotal = 0L
        var accomAllowanceTotal = 0L
        var medAllowanceTotal = 0L

        var transitDeductionTotal = 0L
        var foodDeductionTotal = 0L
        var accomDeductionTotal = 0L
        var medDeductionTotal = 0L

        for (att in attendances) {
            val res = calculateDay(worker, att, overtimeMultiplier)
            if (res.isWorkingDay) {
                shifts++
                regHours += res.regularHours
                hHours += res.hourlyHours
                otHours += res.overtimeHours
                baseWageTotal += res.baseWage
                hourlyPayTotal += res.hourlyPay
                otPayTotal += res.overtimePay

                transitAllowanceTotal += res.transitAllowance
                foodAllowanceTotal += res.foodAllowance
                accomAllowanceTotal += res.accommodationAllowance
                medAllowanceTotal += res.medicalAllowance
            }

            // کسورات (ایاب و ذهاب، مسکن، خوراک، درمان) حتی در صورت غیبت نیز کسر و محاسبه می‌شوند
            transitDeductionTotal += res.transitDeduction
            foodDeductionTotal += res.foodDeduction
            accomDeductionTotal += res.accommodationDeduction
            medDeductionTotal += res.medicalDeduction
        }

        val totalAllowances = transitAllowanceTotal + foodAllowanceTotal + accomAllowanceTotal + medAllowanceTotal
        val totalDeductions = transitDeductionTotal + foodDeductionTotal + accomDeductionTotal + medDeductionTotal

        val gross = baseWageTotal + hourlyPayTotal + otPayTotal + totalAllowances
        val net = (gross - totalDeductions - groupExpenseShare).coerceAtLeast(0L)

        return WorkerPerformance(
            worker = worker,
            totalShifts = shifts,
            regularHours = regHours,
            hourlyHours = hHours,
            overtimeHours = otHours,
            baseWageTotal = baseWageTotal,
            hourlyPayTotal = hourlyPayTotal,
            overtimePayTotal = otPayTotal,
            totalAllowances = totalAllowances,
            transitAllowanceTotal = transitAllowanceTotal,
            foodAllowanceTotal = foodAllowanceTotal,
            accommodationAllowanceTotal = accomAllowanceTotal,
            medicalAllowanceTotal = medAllowanceTotal,
            totalDeductions = totalDeductions,
            transitDeductionTotal = transitDeductionTotal,
            foodDeductionTotal = foodDeductionTotal,
            accommodationDeductionTotal = accomDeductionTotal,
            medicalDeductionTotal = medDeductionTotal,
            groupExpenseShare = groupExpenseShare,
            netPayout = net
        )
    }
}

/**
 * Breakdown of a single worker's calculation for one date/attendance entry.
 */
data class DayCalculationResult(
    val isWorkingDay: Boolean,
    val regularHours: Double,
    val hourlyHours: Double,
    val overtimeHours: Double,
    val baseWage: Long,
    val hourlyPay: Long,
    val overtimePay: Long,
    val totalAllowances: Long,
    val totalDeductions: Long,
    val netPayout: Long,
    val transitAllowance: Long = 0L,
    val foodAllowance: Long = 0L,
    val accommodationAllowance: Long = 0L,
    val medicalAllowance: Long = 0L,
    val transitDeduction: Long = 0L,
    val foodDeduction: Long = 0L,
    val accommodationDeduction: Long = 0L,
    val medicalDeduction: Long = 0L
)
