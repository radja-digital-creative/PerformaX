package id.biz.yuka.performax.utils

import java.time.LocalTime

object AttendancePolicy {
    fun canAttend(now: LocalTime, start: LocalTime, end: LocalTime, anytime: Boolean): Boolean =
        anytime || if (start <= end) now >= start && now <= end else now >= start || now <= end

    fun isFreshLocation(nowElapsedMs: Long, fixElapsedMs: Long): Boolean =
        nowElapsedMs - fixElapsedMs in 0L..30_000L
}
