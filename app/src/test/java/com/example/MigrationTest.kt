package com.example

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate5To6_preservesDistinctWorkersWithSameName() {
        // 1. Create database with schema version 5
        var db = helper.createDatabase(TEST_DB, 5)

        // Insert a workplace folder
        db.execSQL("""
            INSERT INTO `workplace_folders` (`id`, `name`, `foremanName`, `employerName`, `colorTag`, `createdAt`, `notes`)
            VALUES (1, 'پروژه تست', 'سرکارگر ۱', 'کارفرما ۱', 16086790, '1403/01/01', '')
        """)

        // Insert a date folder
        db.execSQL("""
            INSERT INTO `date_folders` (`id`, `folderId`, `date`, `dayOfWeek`, `title`, `notes`, `createdAt`)
            VALUES (10, 1, '1403/01/01', 'شنبه', 'روز اول', '', 1000)
        """)

        // Mandatory scenario: Two distinct workers with the exact SAME name in the same workplace on the same day
        // Worker 1 (id = 101, name = "علی رضایی", role = "بنا")
        db.execSQL("""
            INSERT INTO `workers` (
                `id`, `folderId`, `dateFolderId`, `workDate`, `dayOfWeek`, `name`, `role`, `phone`, `nationalId`,
                `baseDailyWage`, `baseHourlyWage`, `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`,
                `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`, `isActive`, `notes`, `colorTag`,
                `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
            ) VALUES (
                101, 1, 10, '1403/01/01', 'شنبه', 'علی رضایی', 'بنا', '09120000001', '0011111111',
                1000000, 0, 0, 0, 0.0, 0, 0, 0.0, 1, '', 16086790,
                0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1000
            )
        """)

        // Worker 2 (id = 102, name = "علی رضایی" - same name!, role = "نقاش", different nationalId)
        db.execSQL("""
            INSERT INTO `workers` (
                `id`, `folderId`, `dateFolderId`, `workDate`, `dayOfWeek`, `name`, `role`, `phone`, `nationalId`,
                `baseDailyWage`, `baseHourlyWage`, `isHourlyEnabled`, `hourlyWageRate`, `hourlyHours`,
                `isOvertimeEnabled`, `overtimeRate`, `overtimeHours`, `isActive`, `notes`, `colorTag`,
                `transitAllowance`, `transitImpact`, `foodAllowance`, `foodImpact`,
                `accommodationAllowance`, `accommodationImpact`, `medicalAllowance`, `medicalImpact`, `createdAt`
            ) VALUES (
                102, 1, 10, '1403/01/01', 'شنبه', 'علی رضایی', 'نقاش', '09120000002', '0022222222',
                1200000, 0, 0, 0, 0.0, 0, 0, 0.0, 1, '', 16086790,
                0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 0, 'ALLOWANCE', 1000
            )
        """)

        // Attendance for Worker 1
        db.execSQL("""
            INSERT INTO `attendance` (
                `id`, `folderId`, `workerId`, `date`, `timestamp`, `entryTime`, `exitTime`,
                `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`, `hourlyHours`,
                `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES (
                1, 1, 101, '1403/01/01', 2000, '08:00', '17:00',
                8.0, 0.0, 0, 0, 0.0, 1000000, 0, 'پروژه تست', 'کارفرما ۱', 'سرکارگر ۱', 'تمام روز'
            )
        """)

        // Attendance for Worker 2
        db.execSQL("""
            INSERT INTO `attendance` (
                `id`, `folderId`, `workerId`, `date`, `timestamp`, `entryTime`, `exitTime`,
                `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`, `hourlyHours`,
                `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES (
                2, 1, 102, '1403/01/01', 2000, '08:00', '17:00',
                8.0, 0.0, 0, 0, 0.0, 1200000, 0, 'پروژه تست', 'کارفرما ۱', 'سرکارگر ۱', 'تمام روز'
            )
        """)

        // Duplicate attendance row with older timestamp for worker 101 to verify safe deduplication
        db.execSQL("""
            INSERT INTO `attendance` (
                `id`, `folderId`, `workerId`, `date`, `timestamp`, `entryTime`, `exitTime`,
                `regularHours`, `overtimeHours`, `overtimeRate`, `hourlyWageRate`, `hourlyHours`,
                `dailyWage`, `hourlyWage`, `workplaceName`, `employerName`, `foremanName`, `notes`
            ) VALUES (
                999, 1, 101, '1403/01/01', 1000, '08:00', '17:00',
                8.0, 0.0, 0, 0, 0.0, 1000000, 0, 'پروژه تست', 'کارفرما ۱', 'سرکارگر ۱', 'قدیمی'
            )
        """)

        // Close db v5
        db.close()

        // 2. Run migration to version 6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, AppDatabase.MIGRATION_5_6)

        // 3. Verify that BOTH workers still exist with their original IDs (NO MERGING)
        val workersCursor = db.query("SELECT id, name, role FROM workers ORDER BY id ASC")
        val workersList = mutableListOf<Triple<Long, String, String>>()
        while (workersCursor.moveToNext()) {
            workersList.add(Triple(workersCursor.getLong(0), workersCursor.getString(1), workersCursor.getString(2)))
        }
        workersCursor.close()

        assertEquals("Both workers must be preserved without merging", 2, workersList.size)
        assertEquals(101L, workersList[0].first)
        assertEquals("علی رضایی", workersList[0].second)
        assertEquals("بنا", workersList[0].third)

        assertEquals(102L, workersList[1].first)
        assertEquals("علی رضایی", workersList[1].second)
        assertEquals("نقاش", workersList[1].third)

        // 4. Verify that attendances still point to their respective original workerIds and duplicate was removed
        val attCursor = db.query("SELECT id, workerId, status, timestamp FROM attendance ORDER BY workerId ASC")
        val attList = mutableListOf<Triple<Long, Long, String>>()
        while (attCursor.moveToNext()) {
            attList.add(Triple(attCursor.getLong(0), attCursor.getLong(1), attCursor.getString(2)))
        }
        attCursor.close()

        assertEquals("Exactly two attendance records must remain (duplicate deduplicated)", 2, attList.size)
        assertEquals(101L, attList[0].second)
        assertEquals(1L, attList[0].first) // kept newer timestamp row 1 (ts=2000), removed row 999 (ts=1000)
        assertEquals("FULL_DAY", attList[0].third)

        assertEquals(102L, attList[1].second)
        assertEquals(2L, attList[1].first)
        assertEquals("FULL_DAY", attList[1].third)

        db.close()
    }
}
