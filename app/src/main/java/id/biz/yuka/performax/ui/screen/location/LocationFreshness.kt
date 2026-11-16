package id.biz.yuka.performax.ui.screen.location

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class LocationFreshness(val label: String, val isFresh: Boolean)

/** Describes observation age, not whether the employee device is online. */
fun locationFreshness(recordedAt: String?, now: Instant): LocationFreshness {
    val recorded = try {
        OffsetDateTime.parse(recordedAt?.trim()).toInstant()
    } catch (_: Exception) {
        return LocationFreshness("Waktu tidak diketahui", false)
    }
    if (recorded.isAfter(now.plusSeconds(60))) {
        return LocationFreshness("Waktu perangkat tidak sesuai", false)
    }
    val age = Duration.between(recorded, now)
    val isFresh = age <= Duration.ofMinutes(2)
    val relativeTime = when {
        age.toMinutes() < 1 -> "Baru saja"
        age.toMinutes() < 60 -> "${age.toMinutes()} menit lalu"
        age.toHours() < 24 -> "${age.toHours()} jam lalu"
        else -> recorded.atZone(ZoneId.of("Asia/Jakarta"))
            .format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale("id", "ID"))) + " WIB"
    }
    val prefix = if (isFresh) "Lokasi terbaru" else "Lokasi terakhir"
    return LocationFreshness("$prefix • $relativeTime", isFresh)
}
