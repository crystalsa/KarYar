package com.example.util

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.WorkerEntity

object WageCalculator {

    /**
     * Determines whether a worker is an hourly worker based on their profile or attendance record.
     */
    fun isHourly(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        return worker.isHourlyEnabled ||
               (att != null && (att.notes == "ساعتی" || ((att.hourlyWageRate > 0L || att.hourlyWage > 0L) && att.dailyWage == 0L)))
    }

    /**
     * Determines whether a worker is absent.
     * Hourly workers with 0.0 regular hours are NOT absent unless explicitly marked as "غیبت".
     */
    fun isAbsent(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        if (att == null) return false
        if (att.notes == "غیبت") return true
        val hourly = isHourly(worker, att)
        return if (hourly) {
            // For an hourly worker, absent ONLY if explicitly marked as "غیبت"
            false
        } else {
            // For a daily worker, absent if regularHours == 0 and not marked half day or full day
            att.regularHours == 0.0 && att.notes != "نصف روز" && att.notes != "تمام روز"
        }
    }

    /**
     * Determines whether a daily worker has a half-day shift (نصف روز).
     */
    fun isHalfDay(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        if (isHourly(worker, att)) return false
        return att != null && (att.regularHours == 4.0 || att.notes == "نصف روز")
    }

    /**
     * Determines whether a daily worker has a full-day shift (تمام روز).
     */
    fun isFullDay(worker: WorkerEntity, att: AttendanceEntity?): Boolean {
        if (isHourly(worker, att)) return false
        if (isAbsent(worker, att)) return false
        if (isHalfDay(worker, att)) return false
        return att == null || att.regularHours >= 8.0 || att.notes == "تمام روز"
    }

    /**
     * Calculates the exact net daily payout for a worker on a specific date,
     * taking into account attendance status (Full day, Half day, Hourly, Absent),
     * hourly wages, overtime, and allowances/deductions.
     */
    fun calculateDayPayout(worker: WorkerEntity, att: AttendanceEntity?): Long {
        if (isAbsent(worker, att)) return 0L

        val hourly = isHourly(worker, att)
        val halfDay = isHalfDay(worker, att)
        val fullDay = isFullDay(worker, att)

        val dailyWage = if (hourly) 0L else when {
            halfDay -> if (att?.dailyWage != null && att.dailyWage > 0) att.dailyWage else worker.baseDailyWage / 2
            fullDay -> if (att != null && att.dailyWage > 0) att.dailyWage else worker.baseDailyWage
            att != null && att.dailyWage > 0 -> att.dailyWage
            else -> worker.baseDailyWage
        }

        val hHours = if (att != null && att.hourlyHours > 0) att.hourlyHours
                     else (if (worker.hourlyHours > 0) worker.hourlyHours else 0.0)
        val hRate = if (att != null && att.hourlyWageRate > 0) att.hourlyWageRate
                    else (if (att != null && att.hourlyWage > 0) att.hourlyWage
                    else (if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage))
        val hourlyPay = if (hourly && hHours > 0) (hHours * hRate).toLong() else 0L

        val otHours = if (att != null && att.overtimeHours > 0) att.overtimeHours else worker.overtimeHours
        val otRate = if (att != null && att.overtimeRate > 0) att.overtimeRate else worker.overtimeRate
        val overtimePay = if (otHours > 0) (otHours * otRate).toLong() else 0L

        val transitVal = if (worker.transitImpact == "ALLOWANCE") worker.transitAllowance else -worker.transitAllowance
        val accomVal = if (worker.accommodationImpact == "ALLOWANCE") worker.accommodationAllowance else -worker.accommodationAllowance
        val foodVal = if (worker.foodImpact == "ALLOWANCE") worker.foodAllowance else -worker.foodAllowance
        val medVal = if (worker.medicalImpact == "ALLOWANCE") worker.medicalAllowance else -worker.medicalAllowance

        return (dailyWage + hourlyPay + overtimePay + transitVal + accomVal + foodVal + medVal).coerceAtLeast(0L)
    }
}
