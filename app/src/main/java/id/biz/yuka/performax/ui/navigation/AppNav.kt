package id.biz.yuka.performax.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import id.biz.yuka.performax.ui.screen.attendance.AttendanceScreen
import id.biz.yuka.performax.ui.screen.attendance.extrawork.ExtraWorkApprovalScreen
import id.biz.yuka.performax.ui.screen.attendance.extrawork.RequestExtraWorkScreen
import id.biz.yuka.performax.ui.screen.attendance.manual.ManualAttendanceApprovalScreen
import id.biz.yuka.performax.ui.screen.attendance.owner.OwnerAttendanceScreen
import id.biz.yuka.performax.ui.screen.attendance.process.ProcessAttendanceScreen
import id.biz.yuka.performax.ui.screen.bonus.BonusScreen
import id.biz.yuka.performax.ui.screen.bonus.create.CreateBonusScreen
import id.biz.yuka.performax.ui.screen.deduction.DeductionScreen
import id.biz.yuka.performax.ui.screen.employee.EmployeeScreen
import id.biz.yuka.performax.ui.screen.employee.create.EmployeeCreateScreen
import id.biz.yuka.performax.ui.screen.employee.detail.EmployeeDetailScreen
import id.biz.yuka.performax.ui.screen.employee.rule.EmployeeRuleScreen
import id.biz.yuka.performax.ui.screen.announcement.AnnouncementListScreen
import id.biz.yuka.performax.ui.screen.announcement.create.CreateAnnouncementScreen
import id.biz.yuka.performax.ui.screen.announcement.detail.AnnouncementDetailScreen
import id.biz.yuka.performax.ui.screen.features.AllFeaturesScreen
import id.biz.yuka.performax.ui.screen.home.HomeScreen
import id.biz.yuka.performax.ui.screen.kpi.KpiScreen
import id.biz.yuka.performax.ui.screen.kpi.rule.KpiRuleScreen
import id.biz.yuka.performax.ui.screen.leave.LeaveScreen
import id.biz.yuka.performax.ui.screen.leave.create.CreateLeaveScreen
import id.biz.yuka.performax.ui.screen.leave.review.ReviewLeaveScreen
import id.biz.yuka.performax.ui.screen.leave.type.LeaveTypeScreen
import id.biz.yuka.performax.ui.screen.manualdeductions.ManualDeductionsScreen
import id.biz.yuka.performax.ui.screen.resignation.ResignationScreen
import id.biz.yuka.performax.ui.screen.salary.admin.AdminSalaryScreen
import id.biz.yuka.performax.ui.screen.loan.LoanScreen
import id.biz.yuka.performax.ui.screen.loan.detail.LoanDetailScreen
import id.biz.yuka.performax.ui.screen.login.LoginScreen
import id.biz.yuka.performax.ui.screen.notification.NotificationScreen
import id.biz.yuka.performax.ui.screen.performa.PerformaScreen
import id.biz.yuka.performax.ui.screen.profile.ProfileScreen
import id.biz.yuka.performax.ui.screen.profile.edit.EditProfileScreen
import id.biz.yuka.performax.ui.screen.home.salary.IncomeRecapScreen
import id.biz.yuka.performax.ui.screen.home.userannouncement.UserAnnouncementScreen
import id.biz.yuka.performax.ui.screen.salary.SalaryScreen
import id.biz.yuka.performax.ui.screen.schedule.ScheduleScreen
import id.biz.yuka.performax.ui.screen.schedule.advanced.AdvancedScheduleScreen
import id.biz.yuka.performax.ui.screen.schedule.generate.GenerateScheduleScreen
import id.biz.yuka.performax.ui.screen.schedule.shift.ShiftManageScreen
import id.biz.yuka.performax.ui.screen.schedule.shift.ShiftScreen
import id.biz.yuka.performax.ui.screen.splash.SplashScreen
import id.biz.yuka.performax.ui.screen.store.StoreScreen
import id.biz.yuka.performax.ui.screen.store.create.StoreCreateScreen
import id.biz.yuka.performax.ui.screen.store.detail.StoreDetailsScreen
import id.biz.yuka.performax.ui.screen.location.LiveLocationScreen
import id.biz.yuka.performax.ui.screen.report.DailyReportListScreen
import id.biz.yuka.performax.ui.screen.report.ReportListScreen
import id.biz.yuka.performax.ui.screen.summary.DailyReportScreen
import id.biz.yuka.performax.ui.screen.task.TaskScreen
import id.biz.yuka.performax.ui.screen.task.create.CreateTaskScreen
import id.biz.yuka.performax.ui.screen.task.detail.TaskDetailScreen
import id.biz.yuka.performax.ui.screen.task.edit.EditTaskScreen
import id.biz.yuka.performax.ui.screen.task.history.HistoryTaskScreen
import id.biz.yuka.performax.ui.screen.task.review.ReviewTaskScreen
import id.biz.yuka.performax.ui.screen.task.settings.TaskSettingsScreen
import id.biz.yuka.performax.ui.screen.task.settings.routine.TaskRoutineDetailScreen
import id.biz.yuka.performax.ui.screen.task.settings.routine.TaskRoutineListScreen
import id.biz.yuka.performax.ui.screen.task.settings.taskreport.TaskReportScreen
import id.biz.yuka.performax.ui.screen.task.settings.urgent.TaskUrgentDetailScreen
import id.biz.yuka.performax.ui.screen.task.settings.urgent.TaskUrgentListScreen
import id.biz.yuka.performax.ui.screen.violation.ViolationScreen
import id.biz.yuka.performax.ui.screen.violation.create.CreateViolationScreen

@Composable
fun AppNav(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigate = { screen ->
                    if (screen is Screen.DeepLink) {
                        navController.navigate(screen.targetRoute) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToMenu = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.IncomeRecap.route) {
            IncomeRecapScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Announcement.route) {
            AnnouncementListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate(Screen.AnnouncementCreate.route) },
                onNavigateToDetail = { id -> navController.navigate(Screen.AnnouncementDetail.createRoute(id)) }
            )
        }

        composable(Screen.AnnouncementCreate.route) {
            CreateAnnouncementScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AnnouncementDetail.route,
            arguments = listOf(navArgument("announcementId") { type = NavType.StringType })
        ) {
            AnnouncementDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Task.route) {
            TaskScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onNavigateToCreate = { navController.navigate(Screen.CreateTask.route) },
                onNavigateToReview = { navController.navigate(Screen.ReviewTask.route) },
                onNavigateToHistory = { navController.navigate(Screen.HistoryTask.route) },
                onNavigateToSettings = { navController.navigate(Screen.TaskSettings.route) }
            )
        }

        composable(Screen.ReviewTask.route) {
            ReviewTaskScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                }
            )
        }

        composable(Screen.CreateTask.route) {
            CreateTaskScreen(
                onBack = { navController.popBackStack() },
                onSuccessReturn = { id, isRoutine ->
                    if (isRoutine) {
                        // Task routine: redirect ke detail template/routine
                        navController.navigate(Screen.TaskRoutineDetail.createRoute(id)) {
                            popUpTo(Screen.CreateTask.route) { inclusive = true }
                        }
                    } else {
                        // Task biasa: redirect ke detail task
                        navController.navigate(Screen.TaskDetail.createRoute(id)) {
                            popUpTo(Screen.CreateTask.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) {
            TaskDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEdit = { taskId ->
                    navController.navigate(Screen.EditTask.createRoute(taskId))
                },
                onNavigateToRoutineDetail = { routineId ->
                    navController.navigate(Screen.TaskRoutineDetail.createRoute(routineId))
                },
                onNavigateToUrgentDetail = { urgentId ->
                    navController.navigate(Screen.TaskUrgentDetail.createRoute(urgentId))
                }
            )
        }

        composable(Screen.HistoryTask.route) {
            HistoryTaskScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                }
            )
        }

        // ✅ TAMBAHKAN BLOK INI:
        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) {
            EditTaskScreen(
                onBack = { navController.popBackStack() },
                onSuccessReturn = { navController.popBackStack() }
            )
        }

        composable(Screen.Schedule.route) {
            ScheduleScreen(
                onBack = { navController.popBackStack() },
                onNavigateToShiftList = { navController.navigate(Screen.ShiftList.route) },
                onNavigateToGenerate = { navController.navigate(Screen.ScheduleGenerate.route) }, // ✅ Sambungkan
                onNavigateToAdvanced = { navController.navigate(Screen.ScheduleAdvanced.route) }  // ✅ Sambungkan
            )
        }

        composable(Screen.ScheduleGenerate.route) {
            GenerateScheduleScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ScheduleAdvanced.route) {
            AdvancedScheduleScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ShiftList.route) {
            ShiftScreen(
                onBack = { navController.popBackStack() },
                onNavigateToManage = { storeId, shiftId ->
                    navController.navigate(Screen.ShiftManage.createRoute(storeId, shiftId))
                }
            )
        }

        composable(
            route = Screen.ShiftManage.route,
            arguments = listOf(
                navArgument("storeId") { type = NavType.StringType },
                navArgument("shiftId") { type = NavType.StringType }
            )
        ) {
            ShiftManageScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Attendance.route) {
            AttendanceScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ProcessAttendance.route) {
            ProcessAttendanceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToRequestExtraWork = { navController.navigate(Screen.RequestExtraWork.route) }
            )
        }

        composable(Screen.RequestExtraWork.route) {
            RequestExtraWorkScreen(
                onBack = { navController.popBackStack() },
                onSuccessNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ExtraWorkApproval.route) {
            ExtraWorkApprovalScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OwnerAttendance.route) {
            OwnerAttendanceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToManualApproval = { navController.navigate(Screen.ManualAttendanceApproval.route) }
            )
        }

        composable(Screen.ManualAttendanceApproval.route) {
            ManualAttendanceApprovalScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Leave.route) {
            LeaveScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate(Screen.CreateLeave.route) },
                onNavigateToReview = { navController.navigate(Screen.ReviewLeave.route) }
            )
        }

        composable(Screen.CreateLeave.route) {
            CreateLeaveScreen(
                onBack = { navController.popBackStack() },
                onSuccessReturn = { navController.popBackStack() }
            )
        }

        composable(Screen.ReviewLeave.route) {
            ReviewLeaveScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Salary.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType; defaultValue = "me" })
        ) {
            SalaryScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AdminSalary.route) {
            AdminSalaryScreen(
                onBack = { navController.popBackStack() },
                onSelectEmployee = { userId ->
                    navController.navigate(Screen.Salary.createRoute(userId))
                }
            )
        }
        composable(Screen.Employee.route) {
            EmployeeScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { employeeId ->
                    navController.navigate(Screen.EmployeeDetail.createRoute(employeeId))
                },
                onNavigateToCreate = { navController.navigate(Screen.EmployeeCreate.route) }
            )
        }

        composable(Screen.EmployeeCreate.route) {
            EmployeeCreateScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EmployeeDetail.route,
            arguments = listOf(navArgument("employeeId") { type = NavType.StringType })
        ) {
            EmployeeDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Kpi.route) {
            KpiScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Screen.EditProfile.route) }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Store.route) {
            StoreScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate(Screen.StoreCreate.route) }, // Panggil StoreCreate
                onNavigateToEdit = { storeId -> navController.navigate(Screen.StoreDetail.createRoute(storeId)) } // Panggil StoreDetail
            )
        }

        composable(Screen.StoreCreate.route) {
            StoreCreateScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.StoreDetail.route,
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) {
            StoreDetailsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Notification.route) {
            NotificationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                }
            )
        }

        composable(Screen.UserAnnouncement.route) {
            UserAnnouncementScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id ->
                    navController.navigate(Screen.AnnouncementDetail.createRoute(id))
                }
            )
        }

        composable(Screen.PerformaDetail.route) {
            PerformaScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Deduction.route) {
            DeductionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LeaveType.route) {
            LeaveTypeScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.KpiRule.route) {
            KpiRuleScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EmployeeRule.route) {
            EmployeeRuleScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Violation.route) {
            ViolationScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCreateManual = { navController.navigate(Screen.ViolationCreate.route) },
                onNavigateToHistory = { navController.navigate(Screen.HistoryTask.route) }
            )
        }

        composable(Screen.ViolationCreate.route) {
            CreateViolationScreen(
                onBack = { navController.popBackStack() },
                onSuccessReturn = { navController.navigate(Screen.Violation.route) }
            )
        }

        composable(Screen.Bonus.route) {
            BonusScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCreateManual = { navController.navigate(Screen.BonusCreate.route) },
            )
        }

        composable(Screen.BonusCreate.route) {
            CreateBonusScreen(
                onBack = { navController.popBackStack() },
                onSuccessReturn = { navController.popBackStack() } // Kembali ke list ketika sukses
            )
        }

        composable(Screen.Loan.route) {
            LoanScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { userId ->
                    navController.navigate(Screen.LoanDetail.createRoute(userId))
                }
            )
        }

        composable(
            route = Screen.LoanDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            LoanDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ManualDeductions.route,
            arguments = listOf(navArgument("userSalaryId") { type = NavType.StringType })
        ) {
            ManualDeductionsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Resignation.route) {
            ResignationScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // --- TASK SETTINGS WIZARD & MANAGEMENT ---
        composable(Screen.TaskSettings.route) {
            TaskSettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToRoutine = { navController.navigate(Screen.TaskRoutineList.route) },
                onNavigateToUrgent = { navController.navigate(Screen.TaskUrgentList.route) },
                onNavigateToReport = { navController.navigate(Screen.TaskReport.route) }
            )
        }
        composable(Screen.TaskRoutineList.route) {
            TaskRoutineListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { routineId ->
                    navController.navigate(Screen.TaskRoutineDetail.createRoute(routineId))
                }
            )
        }
        composable(
            route = Screen.TaskRoutineDetail.route,
            arguments = listOf(navArgument("routineId") { type = NavType.StringType })
        ) {
            TaskRoutineDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.TaskUrgentList.route) {
            TaskUrgentListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { urgentId ->
                    navController.navigate(Screen.TaskUrgentDetail.createRoute(urgentId))
                }
            )
        }
        composable(
            route = Screen.TaskUrgentDetail.route,
            arguments = listOf(navArgument("urgentId") { type = NavType.StringType })
        ) {
            TaskUrgentDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.TaskReport.route) {
            TaskReportScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Report.route) {
            ReportListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDailyReportList = { navController.navigate(Screen.DailyReportList.route) }
            )
        }

        composable(Screen.ReportMenu.route) {
            ReportListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDailyReportList = { navController.navigate(Screen.DailyReportList.route) }
            )
        }

        composable(Screen.DailyReportList.route) {
            DailyReportListScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DailyReport.route) {
            DailyReportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LiveLocation.route) {
            LiveLocationScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AllFeatures.route) {
            AllFeaturesScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }
    }
}

@Composable
fun PlaceholderFeatureScreen(name: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Halaman $name segera hadir")
    }
}
