package id.biz.yuka.libperformx.entity

/**
 * Entity untuk menyimpan data lokasi terakhir karyawan beserta detail profilnya.
 */
data class UserLocationDetailEntity(
    val userId: String,
    val fullName: String,
    val phoneNumber: String,
    val profileUrl: String,
    val latitude: Double,
    val longitude: Double,
    val recordedAt: String
)
