package id.biz.yuka.performax.service

import android.content.Context
import androidx.work.*
import id.biz.yuka.libperformx.SupabaseConfig
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class TokenSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ready = withTimeoutOrNull(5_000) { SupabaseConfig.client.auth.sessionStatus.first { it !is SessionStatus.LoadingFromStorage } }
        if (ready == null) return Result.retry()
        if (SupabaseConfig.client.auth.currentUserOrNull() == null) return Result.success()
        return if (withTimeoutOrNull(20_000) { FcmManager().syncTokenToSupabase() } == true) Result.success() else Result.retry()
    }
    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("sync_current_fcm_token", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<TokenSyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
        }
    }
}
