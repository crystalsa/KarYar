package com.example.data.repository

import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.DateFolderDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.WorkerDao
import com.example.data.local.dao.WorkplaceFolderDao
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.DateFolderEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.WorkerEntity
import com.example.data.local.entity.WorkplaceFolderEntity
import com.example.util.JalaliCalendar
import kotlinx.coroutines.flow.Flow

class WorkerRepository(
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
    suspend fun deleteDateFolder(dateFolder: DateFolderEntity) {
        dateFolderDao.deleteDateFolder(dateFolder)
        workerDao.deleteWorkersByDateFolder(dateFolder.id)
    }

    suspend fun deleteFolder(folder: WorkplaceFolderEntity) {
        folderDao.deleteFolder(folder)
        dateFolderDao.deleteDateFoldersByFolder(folder.id)
        workerDao.deleteWorkersByFolder(folder.id)
        attendanceDao.deleteAttendanceByFolder(folder.id)
        expenseDao.deleteExpensesByFolder(folder.id)
    }

    suspend fun duplicateFolder(folder: WorkplaceFolderEntity): Long {
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

        val attendances = attendanceDao.getAttendanceByFolderSync(folder.id)
        for (att in attendances) {
            val mappedWorkerId = workerIdMap[att.workerId] ?: continue
            attendanceDao.insertAttendance(
                att.copy(
                    id = 0,
                    folderId = newFolderId,
                    workerId = mappedWorkerId,
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

        return newFolderId
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
     */
    suspend fun loadSampleData() {
        clearAllData()

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
                dateFolderId = df1Saturday,
                workDate = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                name = "علی رضایی",
                role = "استادکار بنا",
                phone = "09121112233",
                nationalId = "0012345678",
                baseDailyWage = 1200000L,
                baseHourlyWage = 150000L,
                isHourlyEnabled = true,
                hourlyWageRate = 150000L,
                hourlyHours = 2.0,
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
                dateFolderId = df1Saturday,
                workDate = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                name = "حسین مرادی",
                role = "آرماتوربند",
                phone = "09359876543",
                nationalId = "0087654321",
                baseDailyWage = 1100000L,
                baseHourlyWage = 140000L,
                isHourlyEnabled = true,
                hourlyWageRate = 140000L,
                hourlyHours = 4.0,
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
                dateFolderId = df1Saturday,
                workDate = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                name = "رضا کریمی",
                role = "کارگر ساده",
                phone = "09194445566",
                nationalId = "0441122334",
                baseDailyWage = 800000L,
                baseHourlyWage = 100000L,
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
                dateFolderId = df1Saturday,
                workDate = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                name = "سعید احمدی",
                role = "جوشکار اسکلت فلزی",
                phone = "09123334455",
                nationalId = "0055667788",
                baseDailyWage = 1350000L,
                baseHourlyWage = 170000L,
                isHourlyEnabled = true,
                hourlyWageRate = 170000L,
                hourlyHours = 3.0,
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
                dateFolderId = df1Saturday,
                workDate = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                name = "مجتبی بیات",
                role = "تأسیسات و لوله‌کش",
                phone = "09187778899",
                nationalId = "0489988776",
                baseDailyWage = 1250000L,
                baseHourlyWage = 160000L,
                isHourlyEnabled = true,
                hourlyWageRate = 160000L,
                hourlyHours = 2.5,
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
                hourlyHours = 2.0,
                hourlyWageRate = 150000L,
                overtimeHours = 2.0,
                overtimeRate = 180000L,
                dailyWage = 1200000L,
                hourlyWage = 150000L,
                bonus = 100000L,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "حاضر - اجرای عالی دیوارچینی"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w2Id,
                date = todayJalali,
                entryTime = "08:00",
                exitTime = "17:30",
                regularHours = 8.0,
                hourlyHours = 4.0,
                hourlyWageRate = 140000L,
                overtimeHours = 1.5,
                overtimeRate = 160000L,
                dailyWage = 1100000L,
                hourlyWage = 140000L,
                bonus = 0L,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "حاضر - تکمیل خاموت‌گذاری"
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
                hourlyWage = 100000L,
                bonus = 50000L,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "حاضر - همکاری در بارگیری و بتن‌ریزی"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder1Id,
                workerId = w4Id,
                date = todayJalali,
                entryTime = "08:00",
                exitTime = "18:00",
                regularHours = 8.0,
                hourlyHours = 3.0,
                hourlyWageRate = 170000L,
                overtimeHours = 2.0,
                overtimeRate = 200000L,
                dailyWage = 1350000L,
                hourlyWage = 170000L,
                bonus = 80000L,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "حاضر - جوشکاری اتصالات بادبندها"
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
                hourlyHours = 2.5,
                hourlyWageRate = 160000L,
                overtimeHours = 1.0,
                overtimeRate = 190000L,
                dailyWage = 1250000L,
                hourlyWage = 160000L,
                bonus = 0L,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "حاضر - نصب کلکتورهای آب سرد و گرم"
            )
        )

        // Expenses for Workplace 1
        expenseDao.insertExpense(
            ExpenseEntity(
                folderId = folder1Id,
                title = "کرایه سرویس مینی‌بوس کارگران",
                category = "TRANSIT",
                scope = "GROUP",
                impactType = "DEDUCTION",
                amount = 450000L,
                date = todayJalali,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "سرویس رفت و برگشت روزانه"
            )
        )
        expenseDao.insertExpense(
            ExpenseEntity(
                folderId = folder1Id,
                title = "تهیه ناهار گرم پرسنل",
                category = "FOOD",
                scope = "GROUP",
                impactType = "DEDUCTION",
                amount = 680000L,
                date = todayJalali,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "ناهار روزانه با کیفیت"
            )
        )
        expenseDao.insertExpense(
            ExpenseEntity(
                folderId = folder1Id,
                title = "کمک‌هزینه مسکن کارگر (افزایشی)",
                category = "ACCOMMODATION",
                scope = "INDIVIDUAL",
                impactType = "ALLOWANCE",
                workerId = w1Id,
                workerName = "علی رضایی",
                amount = 200000L,
                date = todayJalali,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "کمک‌هزینه اسکان"
            )
        )
        expenseDao.insertExpense(
            ExpenseEntity(
                folderId = folder1Id,
                title = "کسر هزینه دارو و معاینه (کاهشی)",
                category = "MEDICAL",
                scope = "INDIVIDUAL",
                impactType = "DEDUCTION",
                workerId = w2Id,
                workerName = "حسین مرادی",
                amount = 150000L,
                date = todayJalali,
                workplaceName = "پروژه برج سپهر",
                employerName = "مهندس سعیدی",
                foremanName = "حاج اصغر کریمی",
                notes = "ویزیت پزشک درمانگاه"
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
                title = "شیفت روزانه",
                notes = "کابل‌کشی تابلوهای برق اضطراری"
            )
        )

        // 5 Workers for Workplace 2
        val w6Id = workerDao.insertWorker(
            WorkerEntity(
                folderId = folder2Id,
                dateFolderId = df2Saturday,
                workDate = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                name = "مهدی کاظمی",
                role = "تکنسین برق صنعتی",
                phone = "09129998877",
                nationalId = "0077889900",
                baseDailyWage = 1400000L,
                baseHourlyWage = 180000L,
                isHourlyEnabled = true,
                hourlyWageRate = 180000L,
                hourlyHours = 3.0,
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
                dateFolderId = df2Saturday,
                workDate = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                name = "بهزاد رستمی",
                role = "گچ‌کار و ابزارزن",
                phone = "09361114477",
                nationalId = "0066554433",
                baseDailyWage = 1150000L,
                baseHourlyWage = 145000L,
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
                dateFolderId = df2Saturday,
                workDate = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                name = "فرزاد اکبری",
                role = "نقاش ساختمان",
                phone = "09192226688",
                nationalId = "0044332211",
                baseDailyWage = 1050000L,
                baseHourlyWage = 130000L,
                isHourlyEnabled = true,
                hourlyWageRate = 130000L,
                hourlyHours = 4.0,
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
                dateFolderId = df2Saturday,
                workDate = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                name = "میلاد عباسی",
                role = "کاشی‌کار و سرامیک‌کار",
                phone = "09128883344",
                nationalId = "0033221199",
                baseDailyWage = 1300000L,
                baseHourlyWage = 160000L,
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
                dateFolderId = df2Saturday,
                workDate = todayJalali,
                dayOfWeek = JalaliCalendar.getDayOfWeek(todayJalali),
                name = "امید حسینی",
                role = "نصاب درب ضدحریق و پنجره",
                phone = "09375551122",
                nationalId = "0022118877",
                baseDailyWage = 950000L,
                baseHourlyWage = 120000L,
                isHourlyEnabled = true,
                hourlyWageRate = 120000L,
                hourlyHours = 2.0,
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
                regularHours = 8.0,
                hourlyHours = 3.0,
                hourlyWageRate = 180000L,
                overtimeHours = 2.0,
                overtimeRate = 210000L,
                dailyWage = 1400000L,
                hourlyWage = 180000L,
                bonus = 100000L,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "حاضر - سیم‌کشی تابلوی اضطراری اتاق عمل"
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
                hourlyWage = 145000L,
                bonus = 0L,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "حاضر - گچ‌کاری دور ستون‌ها"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder2Id,
                workerId = w8Id,
                date = todayJalali,
                entryTime = "08:15",
                exitTime = "17:00",
                regularHours = 8.0,
                hourlyHours = 4.0,
                hourlyWageRate = 130000L,
                overtimeHours = 0.0,
                overtimeRate = 0L,
                dailyWage = 1050000L,
                hourlyWage = 130000L,
                bonus = 0L,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "حاضر - بتونه‌کاری و نقاشی"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder2Id,
                workerId = w9Id,
                date = todayJalali,
                entryTime = "08:00",
                exitTime = "18:00",
                regularHours = 8.0,
                hourlyHours = 0.0,
                hourlyWageRate = 0L,
                overtimeHours = 2.0,
                overtimeRate = 175000L,
                dailyWage = 1300000L,
                hourlyWage = 160000L,
                bonus = 50000L,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "حاضر - بندکشی و آب‌بندی سرامیک"
            )
        )

        attendanceDao.insertAttendance(
            AttendanceEntity(
                folderId = folder2Id,
                workerId = w10Id,
                date = todayJalali,
                entryTime = "08:30",
                exitTime = "16:30",
                regularHours = 8.0,
                hourlyHours = 2.0,
                hourlyWageRate = 120000L,
                overtimeHours = 0.0,
                overtimeRate = 0L,
                dailyWage = 950000L,
                hourlyWage = 120000L,
                bonus = 0L,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "حاضر - تست درب‌های ضد حریق"
            )
        )

        // Expenses for Workplace 2
        expenseDao.insertExpense(
            ExpenseEntity(
                folderId = folder2Id,
                title = "هزینه سرویس و ایاب و ذهاب پرسنل",
                category = "TRANSIT",
                scope = "GROUP",
                impactType = "DEDUCTION",
                amount = 400000L,
                date = todayJalali,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "کرایه مینی‌بوس"
            )
        )
        expenseDao.insertExpense(
            ExpenseEntity(
                folderId = folder2Id,
                title = "تهیه غذای گرم و پذیرایی",
                category = "FOOD",
                scope = "GROUP",
                impactType = "DEDUCTION",
                amount = 550000L,
                date = todayJalali,
                workplaceName = "کارگاه بیمارستان میلاد",
                employerName = "شرکت توسعه درمان",
                foremanName = "مهندس صادقی",
                notes = "ناهار روزانه کارگاه"
            )
        )
    }
}
