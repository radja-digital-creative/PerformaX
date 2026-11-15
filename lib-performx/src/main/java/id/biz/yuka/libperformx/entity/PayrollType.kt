package id.biz.yuka.libperformx.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PayrollType {
    @SerialName("monthly")
    MONTHLY,

    @SerialName("weekly")
    WEEKLY;

    companion object {
        fun fromString(type: String?): PayrollType {
            return when (type?.lowercase()) {
                "weekly" -> WEEKLY
                else -> MONTHLY
            }
        }
    }
}
