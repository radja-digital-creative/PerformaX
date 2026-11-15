package id.biz.yuka.performax.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object IncomeRecap : Screen("income_rekap")
    object Attendance : Screen("attendance")
    object OwnerAttendance : Screen("owner_attendance")
    object Task : Screen("task")
    object CreateTask : Screen("create_task")
    object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: String) = "edit_task/$taskId"
    }
    object HistoryTask : Screen("history_task")
    object Leave : Screen("leave")
    object CreateLeave : Screen("create_leave")
    object ReviewLeave : Screen("review_leave")
    object Salary : Screen("salary/{userId}") {
        fun createRoute(userId: String = "me") = "salary/$userId"
    }
    object AdminSalary : Screen("admin_salary")
    object Schedule : Screen("schedule")
    object ScheduleGenerate : Screen("schedule_generate")
    object ScheduleAdvanced : Screen("schedule_advanced")
    object Employee : Screen("employee")
    object EmployeeCreate : Screen("employee_create")
    object EmployeeDetail : Screen("employee_detail/{employeeId}") {
        fun createRoute(employeeId: String) = "employee_detail/$employeeId"
    }
    object Report : Screen("report")
    object AllFeatures : Screen("all_features")
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
    object Kpi : Screen("kpi")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object ProcessAttendance : Screen("process_attendance")
    object ManualAttendanceApproval : Screen("manual_attendance_approval")
    object RequestExtraWork : Screen("request_extra_work")
    object ExtraWorkApproval : Screen("extra_work_approval")
    object ReviewTask : Screen("review_task")
    object Store : Screen("store")
    object StoreCreate : Screen("store_create")
    object StoreDetail : Screen("store_detail/{storeId}") {
        fun createRoute(storeId: String) = "store_detail/$storeId"
    }
    object ShiftList : Screen("shift_list")
    object ShiftManage : Screen("shift_manage/{storeId}/{shiftId}") {
        fun createRoute(storeId: String, shiftId: String = "new") = "shift_manage/$storeId/$shiftId"
    }
    object Notification : Screen("notification_screen")
    object UserAnnouncement : Screen("user_announcement")
    object PerformaDetail : Screen("performa_detail")
    object Deduction : Screen("deduction")
    object LeaveType : Screen("leave_type")
    object KpiRule : Screen("kpi_rule")
    object EmployeeRule : Screen("employee_rule")
    object Violation : Screen("violation")
    object Bonus : Screen("bonus")
    object BonusCreate : Screen("create_bonus_route")

    object ViolationCreate : Screen("violation_create")
    object Loan : Screen("loan")
    object LoanDetail : Screen("loan_detail/{userId}") {
        fun createRoute(userId: String) = "loan_detail/$userId"
    }
    object ManualDeductions : Screen("manual_deductions_route/{userSalaryId}") {
        fun createRoute(userSalaryId: String = "all") = "manual_deductions_route/$userSalaryId"
    }
    object Resignation : Screen("resignation_route")
    object DailyReport : Screen("daily_report")
    object ReportMenu : Screen("report_menu")
    object DailyReportList : Screen("daily_report_list")
    object LiveLocation : Screen("live_location")
    object Announcement : Screen("announcement")
    object AnnouncementCreate : Screen("announcement_create")
    object AnnouncementDetail : Screen("announcement_detail/{announcementId}") {
        fun createRoute(id: String) = "announcement_detail/$id"
    }
    class DeepLink(val targetRoute: String) : Screen(targetRoute)

    // Task Management Settings
    object TaskSettings : Screen("task_settings")
    object TaskRoutineList : Screen("task_routine_list")
    object TaskRoutineDetail : Screen("task_routine_detail/{routineId}") {
        fun createRoute(routineId: String) = "task_routine_detail/$routineId"
    }
    object TaskUrgentList : Screen("task_urgent_list")
    object TaskUrgentDetail : Screen("task_urgent_detail/{urgentId}") {
        fun createRoute(urgentId: String) = "task_urgent_detail/$urgentId"
    }
    object TaskReport : Screen("task_report")
}

