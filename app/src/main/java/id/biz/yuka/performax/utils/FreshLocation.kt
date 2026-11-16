package id.biz.yuka.performax.utils

import android.annotation.SuppressLint
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CancellationException

/** Never publish an old cached coordinate with a new timestamp. */
@SuppressLint("MissingPermission")
suspend fun FusedLocationProviderClient.fetchFreshLocation(): Location? {
    fun fresh(location: Location?): Location? = location?.takeIf {
        AttendancePolicy.isFreshLocation(SystemClock.elapsedRealtime(), it.elapsedRealtimeNanos / 1_000_000L)
    }
    for (priority in listOf(Priority.PRIORITY_HIGH_ACCURACY, Priority.PRIORITY_BALANCED_POWER_ACCURACY)) {
        val cancellation = CancellationTokenSource()
        val location = try {
            withTimeoutOrNull(12_000L) {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(priority).setDurationMillis(10_000L).setMaxUpdateAgeMillis(5_000L).build()
                getCurrentLocation(request, cancellation.token).await()
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
        finally { cancellation.cancel() }
        fresh(location)?.let { return it }
    }
    return fresh(withTimeoutOrNull(2_000L) { lastLocation.await() })
}
