package id.biz.yuka.performax.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.biz.yuka.libperformx.repository.*
import id.biz.yuka.libperformx.summary.DailyReportRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = AuthRepository()

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository = UserRepository()

    @Provides
    @Singleton
    fun provideEmployeeRepository(): EmployeeRepository = EmployeeRepository()

    @Provides
    @Singleton
    fun provideAttendanceRepository(): AttendanceRepository = AttendanceRepository()

    @Provides
    @Singleton
    fun provideStoreRepository(): StoreRepository = StoreRepository()

    @Provides
    @Singleton
    fun provideRoleRepository(): RoleRepository = RoleRepository()

    @Provides
    @Singleton
    fun provideTaskRepository(): TaskRepository = TaskRepository()

    @Provides
    @Singleton
    fun provideLeaveRepository(): LeaveRepository = LeaveRepository()

    @Provides
    @Singleton
    fun provideSalaryRepository(): SalaryRepository = SalaryRepository()

    @Provides
    @Singleton
    fun provideScheduleRepository(): ScheduleRepository = ScheduleRepository()

    @Provides
    @Singleton
    fun provideKpiRepository(): KpiRepository = KpiRepository()

    @Provides
    @Singleton
    fun provideAppUpdateRepository(): AppUpdateRepository = AppUpdateRepository()

    @Provides
    @Singleton
    fun provideNotificationRepository(): NotificationRepository = NotificationRepository()

    @Provides
    @Singleton
    fun provideSummaryPerformaRepository(): SummaryPerformaRepository = SummaryPerformaRepository()

    @Provides
    @Singleton
    fun provideDeductionRepository(): DeductionRepository = DeductionRepository()

    @Provides
    @Singleton
    fun provideKpiPointRuleRepository(): KpiPointRuleRepository = KpiPointRuleRepository()

    @Provides
    @Singleton
    fun provideEmployeeRuleRepository(): EmployeeRuleRepository = EmployeeRuleRepository()
    @Provides
    @Singleton
    fun provideAppealRepository(): AppealRepository = AppealRepository()
    @Provides
    @Singleton
    fun provideBonusRepository(): BonusRepository = BonusRepository()

    @Provides
    @Singleton
    fun provideLocationRepository(): LocationRepository = LocationRepository()

    @Provides
    @Singleton
    fun provideLoanRepository(): LoanRepository = LoanRepository()

    @Provides
    @Singleton
    fun provideManualDeductionRepository(): ManualDeductionRepository = ManualDeductionRepository()

    @Provides
    @Singleton
    fun provideResignationRepository(): ResignationRepository = ResignationRepository()

    @Provides
    @Singleton
    fun provideDailyReportRepository(): DailyReportRepository = DailyReportRepository()

    @Provides
    @Singleton
    fun provideAnnouncementRepository(): AnnouncementRepository = AnnouncementRepository()
}
