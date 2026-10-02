package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.DateFolderDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.WorkerDao
import com.example.data.local.dao.WorkplaceFolderDao
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.AttendanceStatus
import com.example.data.local.entity.DateFolderEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.WorkerEntity
import com.example.data.local.entity.WorkplaceFolderEntity
import com.example.util.JalaliCalendar
import kotlinx.coroutines.flow.Flow

class WorkerRepository(
    private val database: AppDatabase,
    private val folderDao: WorkplaceFolderDao,
    private val dateFolderDao: DateFolderDao,
    private val workerDao: WorkerDao,
    private val attendanceDao: AttendanceDao,
    private val expenseDao: ExpenseDao
) {
    val allFolders: Flow<List<WorkplaceFolderEntity>> = folderDao.getAllFolders()

    fun getDateFoldersByFolder(folderId: Long): Flow<List<DateFolderEntity>> = dateFolderDao.getDateFoldersByFolder(folderId)
    fun getWorkersByFolder(folderId: Long): Flow<List<WorkerEntity>> = workerDao.getWorkersByFolder(folderId)
    fun getWorkersByDateFolder(dateFolderId: Long): Flow<List<WorkerEntity>> = workerDao.getWorkersByDateFolder(dateFolderId)
    fun getAttendanceByFolder(folderId: Long): Flow<List<AttendanceEntity>> = attendanceDao.getAttendanceByFolder(folderId)
    fun getExpensesByFolder(folderId: Long): Flow<List<ExpenseEntity>> = expenseDao.getExpensesByFolder(folderId)

    suspend fun insertFolder(folder: WorkplaceFolderEntity): Long = folderDao.insertFolder(folder)
    suspend fun updateFolder(folder: WorkplaceFolderEntity) = folderDao.updateFolder(folder)

    suspend fun insertDateFolder(dateFolder: DateFolderEntity): Long = dateFolderDao.insertDateFolder(dateFolder)
    suspend fun updateDateFolder(dateFolder: DateFolderEntity) = dateFolderDao.updateDateFolder(dateFolder)
    suspend fun getDateFolderByDate(folderId: Long, date: String): DateFolderEntity? = dateFolderDao.getDateFolderByDate(folderId, date)
    suspend fun deleteDateFolder(dateFolder: DateFolderEntity) = database.withTransaction {
        attendanceDao.deleteAttendanceByDateFolder(dateFolder.id)
        dateFolderDao.deleteDateFolder(dateFolder)
    }

    suspend fun deleteFolder(folder: WorkplaceFolderEntity) = database.withTransaction {
        folderDao.deleteFolder(folder)
    }

    suspend fun duplicateFolder(folder: WorkplaceFolderEntity): Long = database.withTransaction {
        val newFolderId = folderDao.insertFolder(
            folder.copy(
                id = 0,
                name = "${folder.name} (کپی)",
                createdAt = JalaliCalendar.todayString()
            )
        )

        val workers = workerDao.getWorkersByFolderSync(folder.id)
        val workerIdMap = mutableMapOf<Long, Long>()

        for (w in workers) {
            val newWId = workerDao.insertWorker(w.copy(id = 0, folderId = newFolderId))
            workerIdMap[w.id] = newWId
        }

        val dateFolders = dateFolderDao.getDateFoldersByFolderSync(folder.id)
        val dateFolderIdMap = mutableMapOf<Long, Long>()
        for (df in dateFolders) {
            val newDfId = dateFolderDao.insertDateFolder(df.copy(id = 0, folderId = newFolderId))
            dateFolderIdMap[df.id] = newDfId
        }

        val attendances = attendanceDao.getAttendanceByFolderSync(folder.id)
        for (att in attendances) {
            val mappedWorkerId = workerIdMap[att.workerId] ?: continue
            val mappedDateFolderId = dateFolderIdMap[att.dateFolderId] ?: 0L
            attendanceDao.insertAttendance(
                att.copy(
                    id = 0,
                    folderId = newFolderId,
                    workerId = mappedWorkerId,
                    dateFolderId = mappedDateFolderId,
                    workplaceName = "${folder.name} (کپی)"
                )
            )
        }

        val expenses = expenseDao.getExpensesByFolderSync(folder.id)
        for (exp in expenses) {
            val mappedWorkerId = if (exp.workerId != null) workerIdMap[exp.workerId] else null
            expenseDao.insertExpense(
                exp.copy(
                    id = 0,
                    folderId = newFolderId,
                    workerId = mappedWorkerId,
                    workplaceName = "${folder.name} (کپی)"
                )
            )
        }

        newFolderId
    }

    suspend fun createNextDay(
        folderId: Long,
        date: String,
        dayOfWeek: String,
        title: String = "روز کاری",
        notes: String = ""
    ): Long = database.withTransaction {
        val newDfId = dateFolderDao.insertDateFolder(
            DateFolderEntity(
                folderId = folderId,
                date = date,
                dayOfWeek = dayOfWeek,
                title = title,
                notes = notes,
                epochDay = JalaliCalendar.toEpochDay(date)
            )
        )
        val activeWorkers = workerDao.getWorkersByFolderSync(folderId).filter { it.isActive }
        for (w in activeWorkers) {
            val isHourly = w.isHourlyEnabled
            val attStatus = if (isHourly) AttendanceStatus.HOURLY else AttendanceStatus.FULL_DAY
            attendanceDao.insertAttendance(
                AttendanceEntity(
                    folderId = folderId,
                    workerId = w.id,
                    dateFolderId = newDfId,
                    date = date,
                    epochDay = JalaliCalendar.toEpochDay(date),
                    status = attStatus,
                    dailyWage = if (isHourly) 0L else w.baseDailyWage,
                    hourlyWage = if (w.hourlyWageRate > 0) w.hourlyWageRate else w.baseHourlyWage,
                    hourlyWageRate = w.hourlyWageRate,
                    hourlyHours = w.hourlyHours,
                    overtimeHours = w.overtimeHours,
                    overtimeRate = w.overtimeRate,
                    regularHours = if (isHourly) 0.0 else 8.0,
                    notes = ""
                )
            )
        }
        newDfId
    }

    suspend fun duplicateWorker(
        worker: WorkerEntity,
        targetDate: String,
        targetDateFolderId: Long
    ): Long = database.withTransaction {
        val newWorkerId = workerDao.insertWorker(
            worker.copy(
                id = 0,
                name = "${worker.name} (کپی)"
            )
        )
        val isHourly = worker.isHourlyEnabled
        val attStatus = if (isHourly) AttendanceStatus.HOURLY else AttendanceStatus.FULL_DAY
        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = worker.folderId,
                workerId = newWorkerId,
                dateFolderId = targetDateFolderId,
                date = targetDate,
                epochDay = JalaliCalendar.toEpochDay(targetDate),
                status = attStatus,
                dailyWage = if (isHourly) 0L else worker.baseDailyWage,
                hourlyWage = if (worker.hourlyWageRate > 0) worker.hourlyWageRate else worker.baseHourlyWage,
                hourlyWageRate = worker.hourlyWageRate,
                hourlyHours = worker.hourlyHours,
                overtimeHours = worker.overtimeHours,
                overtimeRate = worker.overtimeRate,
                regularHours = if (isHourly) 0.0 else 8.0,
                notes = ""
            )
        )
        newWorkerId
    }

    suspend fun insertWorker(worker: WorkerEntity): Long = workerDao.insertWorker(worker)
    suspend fun updateWorker(worker: WorkerEntity) = workerDao.updateWorker(worker)
    suspend fun deleteWorker(worker: WorkerEntity) = workerDao.deleteWorker(worker)

    suspend fun insertAttendance(attendance: AttendanceEntity): Long = attendanceDao.insertAttendance(attendance)
    suspend fun updateAttendance(attendance: AttendanceEntity) = attendanceDao.updateAttendance(attendance)
    suspend fun deleteAttendance(attendance: AttendanceEntity) = attendanceDao.deleteAttendance(attendance)

    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteExpense(expense)

    suspend fun clearAllData() {
        folderDao.clearAll()
        dateFolderDao.clearAll()
        workerDao.clearAll()
        attendanceDao.clearAll()
        expenseDao.clearAll()
    }

    /**
     * Loads rich, accurate sample data with at least 5 workers per workplace folder,
     * including realistic 11-digit phone numbers, 10-digit national IDs, hourly rates,
     * overtime rates and hours, attendance records, and expenses.
     * Does NOT clear existing data automatically.
     */
    suspend fun loadSampleData() {
        val todayJalali = JalaliCalendar.todayString()
        val yesterdayJalali = JalaliCalendar.fromTimestamp(System.currentTimeMillis() - 86400000L).format()
        val twoDaysAgoJalali = JalaliCalendar.fromTimestamp(System.currentTimeMillis() - 172800000L).format()

        // ==========================================
        // WORKPLACE 1: پروژه برج سپهر
        // ==========================================
        val folder1Id = folderDao.insertFolder(
            WorkplaceFolderEntity(
                name = "پروژه برج سپهر",
                foremanName = "حاج اصغر کریمی",
                employerName = "مهندس سعیدی",
                colorTag = 0xFFD97706L,
                createdAt = twoDaysAgoJalali,
                notes = "پروژه احداث مجتمع تجاری مسکونی ۲۴ طبقه"
            )
        )

        // Date Folders for Workplace 1
        val df1Saturday = dateFolderDao.insertDateFolder(
            DateFolderEntity(
                folderId = folder1Id,
                date = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                title = "روز کاری جاری",
                notes = "بتن‌ریزی سقف طبقه پنجم"
            )
        )
        val df1Sunday = dateFolderDao.insertDateFolder(
            DateFolderEntity(
                folderId = folder1Id,
                date = yesterdayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(yesterdayJalali),
                title = "روز کاری قبل",
                notes = "آرماتوربندی و قالب‌بندی ستون‌ها"
            )
        )

        // 5 Workers for Workplace 1
        val w1Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder1Id,
                name = "علی رضایی",
                role = "استادکار بنا",
                phone = "09121112233",
                nationalId = "0012345678",
                baseDailyWage = 1200000L,
                baseHourlyWage = 0L,
                isHourlyEnabled = false,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                isOvertimeEnabled = true,
                overtimeRate = 180000L,
                overtimeHours = 2.0,
                isActive = true,
                notes = "با سابقه و مسلط به دیوارچینی و نماکاری",
                colorTag = 0xFFD97706L,
                transitAllowance = 50000L,
                transitImpact = "ALLOWANCE"
            )
        )

        val w2Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder1Id,
                name = "حسین مرادی",
                role = "آرماتوربند",
                phone = "09359876543",
                nationalId = "0087654321",
                baseDailyWage = 0L,
                baseHourlyWage = 140000L,
                isHourlyEnabled = true,
                hourlyWageRate = 140000L,
                hourlyHours = 5.0,
                isOvertimeEnabled = true,
                overtimeRate = 160000L,
                overtimeHours = 1.5,
                isActive = true,
                notes = "دقیق در نقشه‌خوانی فونداسیون",
                colorTag = 0xFF0284C7L,
                accommodationAllowance = 100000L,
                accommodationImpact = "DEDUCTION"
            )
        )

        val w3Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder1Id,
                name = "رضا کریمی",
                role = "کارگر ساده",
                phone = "09194445566",
                nationalId = "0441122334",
                baseDailyWage = 800000L,
                baseHourlyWage = 0L,
                isHourlyEnabled = false,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                isOvertimeEnabled = true,
                overtimeRate = 120000L,
                overtimeHours = 3.0,
                isActive = true,
                notes = "منظم در جابجایی مصالح و نظافت کارگاه",
                colorTag = 0xFF059669L
            )
        )

        val w4Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder1Id,
                name = "سعید احمدی",
                role = "جوشکار اسکلت فلزی",
                phone = "09123334455",
                nationalId = "0055667788",
                baseDailyWage = 0L,
                baseHourlyWage = 170000L,
                isHourlyEnabled = true,
                hourlyWageRate = 170000L,
                hourlyHours = 5.0,
                isOvertimeEnabled = true,
                overtimeRate = 200000L,
                overtimeHours = 2.0,
                isActive = true,
                notes = "دارای گواهینامه جوشکاری نفوذی و استاندارد",
                colorTag = 0xFFE11D48L,
                foodAllowance = 60000L,
                foodImpact = "ALLOWANCE"
            )
        )

        val w5Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder1Id,
                name = "مجتبی بیات",
                role = "تأسیسات و لوله‌کش",
                phone = "09187778899",
                nationalId = "0489988776",
                baseDailyWage = 1250000L,
                baseHourlyWage = 0L,
                isHourlyEnabled = false,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                isOvertimeEnabled = true,
                overtimeRate = 190000L,
                overtimeHours = 1.0,
                isActive = true,
                notes = "مسلط به لوله‌کشی پنج‌لایه و فاضلاب پوش‌فیت",
                colorTag = 0xFF7C3AEDL
            )
        )

        // Attendance records for Workplace 1 (Today)
        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w1Id,
                date = todayJalali,
                entryTime = "07:30",
                exitTime = "18:00",
                regularHours = 8.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 2.0,
                overtimeRate = 180000L,
                dailyWage = 1200000L,
                hourlyWage = 0L,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "تمام روز"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w2Id,
                date = todayJalali,
                entryTime = "08:00",
                exitTime = "17:30",
                regularHours = 0.0,
                hourlyHours = 5.0,
                hourlyWageRate = 140000L,
                overtimeHours = 1.5,
                overtimeRate = 160000L,
                dailyWage = 0L,
                hourlyWage = 140000L,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "ساعتی"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w3Id,
                date = todayJalali,
                entryTime = "07:45",
                exitTime = "19:00",
                regularHours = 8.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 3.0,
                overtimeRate = 120000L,
                dailyWage = 800000L,
                hourlyWage = 0L,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "تمام روز"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w4Id,
                date = todayJalali,
                entryTime = "08:00",
                exitTime = "18:00",
                regularHours = 0.0,
                hourlyHours = 5.0,
                hourlyWageRate = 170000L,
                overtimeHours = 2.0,
                overtimeRate = 200000L,
                dailyWage = 0L,
                hourlyWage = 170000L,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "ساعتی"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w5Id,
                date = todayJalali,
                entryTime = "08:30",
                exitTime = "17:30",
                regularHours = 8.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 1.0,
                overtimeRate = 190000L,
                dailyWage = 1250000L,
                hourlyWage = 0L,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "تمام روز"
            )
        )

        // ==========================================
        // WORKPLACE 2: کارگاه بیمارستان میلاد
        // ==========================================
        val folder2Id = folderDao.insertFolder(
            WorkplaceFolderEntity(
                name = "کارگاه بیمارستان میلاد",
                foremanName = "مهندس صادقی",
                employerName = "شرکت توسعه درمان",
                colorTag = 0xFF0284C7L,
                createdAt = yesterdayJalali,
                notes = "بازسازی بخش جراحی، تأسیسات الکتریکی و هوارسان‌ها"
            )
        )

        // Date Folders for Workplace 2
        val df2Saturday = dateFolderDao.insertDateFolder(
            DateFolderEntity(
                folderId = folder2Id,
                date = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                title = "روز کاری",
                notes = "کابل‌کشی تابلوهای برق اضطراری"
            )
        )

        // 5 Workers for Workplace 2
        val w6Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder2Id,
                name = "مهدی کاظمی",
                role = "تکنسین برق صنعتی",
                phone = "09129998877",
                nationalId = "0077889900",
                baseDailyWage = 0L,
                baseHourlyWage = 180000L,
                isHourlyEnabled = true,
                hourlyWageRate = 180000L,
                hourlyHours = 6.0,
                isOvertimeEnabled = true,
                overtimeRate = 210000L,
                overtimeHours = 2.0,
                isActive = true,
                notes = "مسلط به تابلو برق و ژنراتور اضطراری",
                colorTag = 0xFF0284C7L
            )
        )

        val w7Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder2Id,
                name = "بهزاد رستمی",
                role = "گچ‌کار و ابزارزن",
                phone = "09361114477",
                nationalId = "0066554433",
                baseDailyWage = 1150000L,
                baseHourlyWage = 0L,
                isHourlyEnabled = false,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                isOvertimeEnabled = true,
                overtimeRate = 150000L,
                overtimeHours = 2.5,
                isActive = true,
                notes = "سفیدکاری و لکه‌گیری سقف کاذب",
                colorTag = 0xFFD97706L
            )
        )

        val w8Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder2Id,
                name = "فرزاد اکبری",
                role = "نقاش ساختمان",
                phone = "09192226688",
                nationalId = "0044332211",
                baseDailyWage = 0L,
                baseHourlyWage = 130000L,
                isHourlyEnabled = true,
                hourlyWageRate = 130000L,
                hourlyHours = 5.0,
                isOvertimeEnabled = false,
                overtimeRate = 0L,
                overtimeHours = 0.0,
                isActive = true,
                notes = "رنگ‌آمیزی اپوکسی بهداشتی دیوارها",
                colorTag = 0xFF059669L
            )
        )

        val w9Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder2Id,
                name = "میلاد عباسی",
                role = "کاشی‌کار و سرامیک‌کار",
                phone = "09128883344",
                nationalId = "0033221199",
                baseDailyWage = 1300000L,
                baseHourlyWage = 0L,
                isHourlyEnabled = false,
                hourlyWageRate = 0L,
                hourlyHours = 0.0,
                isOvertimeEnabled = true,
                overtimeRate = 175000L,
                overtimeHours = 2.0,
                isActive = true,
                notes = "نصب سرامیک اسلب بخش مراقبت‌های ویژه",
                colorTag = 0xFFE11D48L
            )
        )

        val w10Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder2Id,
                name = "امید حسینی",
                role = "نصاب درب ضدحریق و پنجره",
                phone = "09375551122",
                nationalId = "0022118877",
                baseDailyWage = 0L,
                baseHourlyWage = 120000L,
                isHourlyEnabled = true,
                hourlyWageRate = 120000L,
                hourlyHours = 4.0,
                isOvertimeEnabled = false,
                overtimeRate = 0L,
                overtimeHours = 0.0,
                isActive = true,
                notes = "رگلاژ درب‌های بیمارستانی و قفل‌ها",
                colorTag = 0xFF7C3AEDL
            )
        )

        // Attendance records for Workplace 2 (Today)
        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder2Id,
                workerId = w6Id,
                date = todayJalali,
                entryTime = "08:00",
                exitTime = "18:00",
                regularHours = 0.0,
                hourlyHours = 6.0,
                hourlyWageRate = 180000L,
                overtimeHours = 2.0,
                overtimeRate = 210000L,
                dailyWage = 0L,
                hourlyWage = 180000L,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "ساعتی"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder2Id,
                workerId = w7Id,
                date = todayJalali,
                entryTime = "08:00",
                exitTime = "17:30",
                regularHours = 8.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 2.5,
                overtimeRate = 150000L,
                dailyWage = 1150000L,
                hourlyWage = 0L,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "تمام روز"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder2Id,
                workerId = w8Id,
                date = todayJalali,
                entryTime = "08:15",
                exitTime = "17:00",
                regularHours = 0.0,
                hourlyHours = 5.0,
                hourlyWageRate = 130000L,
                overtimeHours = 0.0,
                overtimeRate = 0L,
                dailyWage = 0L,
                hourlyWage = 130000L,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "ساعتی"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder2Id,
                workerId = w9Id,
                date = todayJalali,
                entryTime = "08:00",
                exitTime = "13:00",
                regularHours = 4.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 0.0,
                overtimeRate = 0L,
                dailyWage = 650000L,
                hourlyWage = 0L,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "نصف روز"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder2Id,
                workerId = w10Id,
                date = todayJalali,
                entryTime = "08:30",
                exitTime = "16:30",
                regularHours = 0.0,
                hourlyHours = 4.0,
                hourlyWageRate = 120000L,
                overtimeHours = 0.0,
                overtimeRate = 0L,
                dailyWage = 0L,
                hourlyWage = 120000L,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "ساعتی"
            )
        )
    }
}
