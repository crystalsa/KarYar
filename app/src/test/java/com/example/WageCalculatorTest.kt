package com.example

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.AttendanceStatus
import com.example.data.local.entity.WorkerEntity
import com.example.util.WageCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WageCalculatorTest {

    private val baseDailyWorker = WorkerEntity(
        id = 1L,
        folderId = 10L,
        name = "علی رضایی",
        role = "بنا",
        baseDailyWage = 1_000_000L,
        baseHourlyWage = 0L,
        isHourlyEnabled = false
    )

    private val baseHourlyWorker = WorkerEntity(
        id = 2L,
        folderId = 10L,
        name = "حسین مرادی",
        role = "آرماتوربند",
        baseDailyWage = 0L,
        baseHourlyWage = 150_000L,
        isHourlyEnabled = true,
        hourlyWageRate = 150_000L,
        hourlyHours = 6.0
    )

    // 1. Full day standard daily wage
    @Test
    fun testFullDay_standardDailyWage() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertTrue(result.isWorkingDay)
        assertEquals(1_000_000L, result.baseWage)
        assertEquals(0L, result.hourlyPay)
        assertEquals(0L, result.overtimePay)
        assertEquals(1_000_000L, result.netPayout)
        assertEquals(8.0, result.regularHours, 0.001)
    }

    // 2. Full day with explicit dailyWage override on attendance
    @Test
    fun testFullDay_overrideDailyWage() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            dailyWage = 1_200_000L
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertEquals(1_200_000L, result.baseWage)
        assertEquals(1_200_000L, result.netPayout)
    }

    // 3. Half day standard daily wage (half of baseDailyWage)
    @Test
    fun testHalfDay_standardHalfDailyWage() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.HALF_DAY,
            regularHours = 4.0
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertTrue(result.isWorkingDay)
        assertEquals(500_000L, result.baseWage)
        assertEquals(4.0, result.regularHours, 0.001)
        assertEquals(500_000L, result.netPayout)
    }

    // 4. Half day with explicit dailyWage on attendance
    @Test
    fun testHalfDay_explicitDailyWage() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.HALF_DAY,
            regularHours = 4.0,
            dailyWage = 600_000L
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertEquals(600_000L, result.baseWage)
        assertEquals(600_000L, result.netPayout)
    }

    // 5. Half day detected by notes = "نصف روز"
    @Test
    fun testHalfDay_detectedByNotes() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            regularHours = 4.0,
            notes = "نصف روز"
        )
        assertTrue(WageCalculator.isHalfDay(baseDailyWorker, att))
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertEquals(500_000L, result.baseWage)
    }

    // 6. Hourly worker calculation (hours * rate)
    @Test
    fun testHourly_standardCalculation() {
        val att = AttendanceEntity(
            workerId = 2L,
            date = "1405/01/01",
            status = AttendanceStatus.HOURLY,
            hourlyHours = 7.0,
            hourlyWageRate = 150_000L
        )
        val result = WageCalculator.calculateDay(baseHourlyWorker, att)
        assertTrue(result.isWorkingDay)
        assertEquals(0L, result.baseWage)
        assertEquals(1_050_000L, result.hourlyPay)
        assertEquals(1_050_000L, result.netPayout)
    }

    // 7. Hourly worker with 0 hours produces 0 hourly pay
    @Test
    fun testHourly_zeroHoursProducesZero() {
        val att = AttendanceEntity(
            workerId = 2L,
            date = "1405/01/01",
            status = AttendanceStatus.HOURLY,
            hourlyHours = 0.0
        )
        val workerNoDefault = baseHourlyWorker.copy(hourlyHours = 0.0)
        val result = WageCalculator.calculateDay(workerNoDefault, att)
        assertEquals(0L, result.hourlyPay)
        assertEquals(0L, result.netPayout)
    }

    // 8. Absent day (status = ABSENT): wages, overtime, allowances are 0, but deductions STILL APPLY
    @Test
    fun testAbsent_statusAbsent_zeroWagesAndNotWorkingDay() {
        val workerWithAllowances = baseDailyWorker.copy(
            transitAllowance = 50_000L,
            transitImpact = "ALLOWANCE",
            foodAllowance = 30_000L,
            foodImpact = "DEDUCTION"
        )
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.ABSENT,
            regularHours = 0.0,
            overtimeHours = 2.0
        )
        val result = WageCalculator.calculateDay(workerWithAllowances, att)
        assertFalse(result.isWorkingDay)
        assertEquals(0L, result.baseWage)
        assertEquals(0L, result.hourlyPay)
        assertEquals(0L, result.overtimePay)
        assertEquals(0L, result.totalAllowances)
        assertEquals(30_000L, result.totalDeductions) // Deductions apply to absent workers
        assertEquals(0L, result.netPayout)
    }

    // 9. Absent day marked with notes = "غیبت"
    @Test
    fun testAbsent_notesGhiebat() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            regularHours = 8.0, // even if hours were mistakenly left > 0
            notes = "غیبت"
        )
        assertTrue(WageCalculator.isAbsent(baseDailyWorker, att))
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertFalse(result.isWorkingDay)
        assertEquals(0L, result.netPayout)
    }

    // 10. Daily worker with regularHours = 0 and no half/full marker treated as absent
    @Test
    fun testAbsent_zeroRegularHoursDailyWorker() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            regularHours = 0.0
        )
        assertTrue(WageCalculator.isAbsent(baseDailyWorker, att))
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertFalse(result.isWorkingDay)
        assertEquals(0L, result.netPayout)
    }

    // 11. Overtime with explicit overtime rate
    @Test
    fun testOvertime_explicitRate() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 3.0,
            overtimeRate = 200_000L
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertEquals(600_000L, result.overtimePay)
        assertEquals(1_600_000L, result.netPayout)
    }

    // 12. Overtime without explicit rate (uses baseDailyWage / 8 * 1.4 default)
    @Test
    fun testOvertime_defaultMultiplierFormula() {
        // baseDailyWage = 1,000,000 -> hourly = 125,000 -> 125,000 * 1.4 = 175,000 per hour
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 2.0,
            overtimeRate = 0L
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertEquals(350_000L, result.overtimePay)
        assertEquals(1_350_000L, result.netPayout)
    }

    // 13. Overtime with configurable multiplier
    @Test
    fun testOvertime_configurableMultiplier() {
        // baseDailyWage = 800,000 -> hourly = 100,000 -> multiplier 1.5 -> 150,000 per hour * 2 = 300,000
        val worker = baseDailyWorker.copy(baseDailyWage = 800_000L)
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 2.0
        )
        val result = WageCalculator.calculateDay(worker, att, overtimeMultiplier = 1.5)
        assertEquals(300_000L, result.overtimePay)
    }

    // 14. Overtime on absent day is ZERO
    @Test
    fun testOvertime_onAbsentDayIsZero() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.ABSENT,
            overtimeHours = 4.0,
            overtimeRate = 200_000L
        )
        val result = WageCalculator.calculateDay(baseDailyWorker, att)
        assertEquals(0L, result.overtimePay)
    }

    // 15. Overtime on hourly worker with 0 hours is ZERO
    @Test
    fun testOvertime_hourlyWorkerZeroHoursIsZero() {
        val att = AttendanceEntity(
            workerId = 2L,
            date = "1405/01/01",
            status = AttendanceStatus.HOURLY,
            hourlyHours = 0.0,
            overtimeHours = 3.0,
            overtimeRate = 200_000L
        )
        val workerNoDefault = baseHourlyWorker.copy(hourlyHours = 0.0)
        val result = WageCalculator.calculateDay(workerNoDefault, att)
        assertEquals(0L, result.overtimePay)
    }

    // 16. Allowances added to gross pay
    @Test
    fun testAllowances_addedToNetPayout() {
        val worker = baseDailyWorker.copy(
            transitAllowance = 60_000L,
            transitImpact = "ALLOWANCE",
            foodAllowance = 40_000L,
            foodImpact = "ALLOWANCE"
        )
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val result = WageCalculator.calculateDay(worker, att)
        assertEquals(100_000L, result.totalAllowances)
        assertEquals(0L, result.totalDeductions)
        assertEquals(1_100_000L, result.netPayout)
    }

    // 17. Deductions subtracted from gross pay
    @Test
    fun testDeductions_subtractedFromNetPayout() {
        val worker = baseDailyWorker.copy(
            accommodationAllowance = 150_000L,
            accommodationImpact = "DEDUCTION"
        )
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val result = WageCalculator.calculateDay(worker, att)
        assertEquals(0L, result.totalAllowances)
        assertEquals(150_000L, result.totalDeductions)
        assertEquals(850_000L, result.netPayout)
    }

    // 18. Mixed allowances and deductions on the same working day
    @Test
    fun testMixedAllowancesAndDeductions() {
        val worker = baseDailyWorker.copy(
            transitAllowance = 70_000L,
            transitImpact = "ALLOWANCE",
            accommodationAllowance = 100_000L,
            accommodationImpact = "DEDUCTION"
        )
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val result = WageCalculator.calculateDay(worker, att)
        assertEquals(70_000L, result.totalAllowances)
        assertEquals(100_000L, result.totalDeductions)
        // 1,000,000 + 70,000 - 100,000 = 970,000
        assertEquals(970_000L, result.netPayout)
    }

    // 19. Zero-floor cap: net payout never negative when deductions exceed earnings
    @Test
    fun testZeroFloor_negativeNetPayoutCappedAtZero() {
        val worker = baseDailyWorker.copy(
            baseDailyWage = 200_000L,
            accommodationAllowance = 500_000L,
            accommodationImpact = "DEDUCTION"
        )
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val result = WageCalculator.calculateDay(worker, att)
        assertEquals(0L, result.netPayout)
    }

    // 20. Rounding verification with roundToLong (no truncate error)
    @Test
    fun testRounding_roundToLongFunction() {
        // Half day odd base wage 750,001 -> 750,001 / 2.0 = 375,000.5 -> rounds to 375,001
        val worker = baseDailyWorker.copy(baseDailyWage = 750_001L)
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.HALF_DAY,
            regularHours = 4.0
        )
        val result = WageCalculator.calculateDay(worker, att)
        assertEquals(375_001L, result.baseWage)

        // Mathematical rounding checks
        assertEquals(1235L, WageCalculator.roundToLong(1234.5))
        assertEquals(1234L, WageCalculator.roundToLong(1234.4))
        assertEquals(1235L, WageCalculator.roundToLong(1234.6))
    }

    // 21. Monthly aggregation comparison: sum of daily net payouts equals calculateWorkerPerformance
    @Test
    fun testMonthlyAggregation_matchesSumOfDaily() {
        val worker = baseDailyWorker.copy(
            transitAllowance = 20_000L,
            transitImpact = "ALLOWANCE"
        )
        val day1 = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val day2 = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/02",
            status = AttendanceStatus.HALF_DAY,
            regularHours = 4.0
        )
        val day3 = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/03",
            status = AttendanceStatus.ABSENT,
            regularHours = 0.0
        )

        val res1 = WageCalculator.calculateDay(worker, day1)
        val res2 = WageCalculator.calculateDay(worker, day2)
        val res3 = WageCalculator.calculateDay(worker, day3)

        val totalDailyNet = res1.netPayout + res2.netPayout + res3.netPayout

        val perf = WageCalculator.calculateWorkerPerformance(worker, listOf(day1, day2, day3))
        assertEquals(2, perf.totalShifts) // absent day is NOT counted
        assertEquals(totalDailyNet, perf.netPayout)
        assertEquals(1_500_000L, perf.baseWageTotal)
        assertEquals(40_000L, perf.totalAllowances) // 20,000 * 2 present days
        assertEquals(12.0, perf.regularHours, 0.001)
    }

    // 22. Multiple attendance records aggregation with mixed present and absent days
    @Test
    fun testMultipleDays_withMixedAttendance() {
        val worker = baseDailyWorker.copy(
            overtimeRate = 100_000L,
            foodAllowance = 50_000L,
            foodImpact = "ALLOWANCE"
        )
        val attList = listOf(
            AttendanceEntity(workerId = 1L, date = "1405/01/01", status = AttendanceStatus.FULL_DAY, regularHours = 8.0, overtimeHours = 2.0),
            AttendanceEntity(workerId = 1L, date = "1405/01/02", status = AttendanceStatus.ABSENT),
            AttendanceEntity(workerId = 1L, date = "1405/01/03", status = AttendanceStatus.FULL_DAY, regularHours = 8.0, overtimeHours = 1.0)
        )
        val perf = WageCalculator.calculateWorkerPerformance(worker, attList)
        assertEquals(2, perf.totalShifts)
        assertEquals(2_000_000L, perf.baseWageTotal)
        assertEquals(300_000L, perf.overtimePayTotal) // (2 + 1) * 100,000
        assertEquals(100_000L, perf.totalAllowances) // 50,000 * 2 days
        assertEquals(2_400_000L, perf.netPayout)
    }

    // 23. Group expense share in calculateWorkerPerformance
    @Test
    fun testGroupExpenseShare_deductedFromNet() {
        val att = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0
        )
        val perf = WageCalculator.calculateWorkerPerformance(
            worker = baseDailyWorker,
            attendances = listOf(att),
            groupExpenseShare = 100_000L
        )
        assertEquals(1_000_000L, perf.baseWageTotal)
        assertEquals(100_000L, perf.groupExpenseShare)
        assertEquals(900_000L, perf.netPayout)
    }

    // 24. Absent worker: wages, overtime, allowances are zero, deductions STILL apply in performance
    @Test
    fun testAbsentDay_deductionsApply_allowancesWagesOvertimeDoNotApply() {
        val worker = baseDailyWorker.copy(
            transitAllowance = 40_000L,
            transitImpact = "ALLOWANCE", // Should NOT be paid when absent
            foodAllowance = 35_000L,
            foodImpact = "DEDUCTION",    // MUST be deducted even when absent
            overtimeRate = 120_000L
        )
        val dayPresent = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/01",
            status = AttendanceStatus.FULL_DAY,
            regularHours = 8.0,
            overtimeHours = 2.0
        )
        val dayAbsent = AttendanceEntity(
            workerId = 1L,
            date = "1405/01/02",
            status = AttendanceStatus.ABSENT,
            regularHours = 8.0, // Should be ignored
            overtimeHours = 3.0  // Overtime should NOT apply to absent day
        )

        val perf = WageCalculator.calculateWorkerPerformance(worker, listOf(dayPresent, dayAbsent))
        assertEquals(1, perf.totalShifts) // Only 1 shift worked
        assertEquals(8.0, perf.regularHours, 0.001)
        assertEquals(2.0, perf.overtimeHours, 0.001) // Only present day's overtime (2h)
        assertEquals(1_000_000L, perf.baseWageTotal)
        assertEquals(240_000L, perf.overtimePayTotal) // 2h * 120,000
        assertEquals(40_000L, perf.totalAllowances)   // Only present day's allowance (40k * 1)
        // Deductions apply to BOTH days: 35k * 2 = 70k!
        assertEquals(70_000L, perf.totalDeductions)
        // Net: 1,000,000 + 240,000 + 40,000 - 70,000 = 1,210,000
        assertEquals(1_210_000L, perf.netPayout)
    }
}
