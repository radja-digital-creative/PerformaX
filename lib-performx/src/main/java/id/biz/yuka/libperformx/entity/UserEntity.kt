package id.biz.yuka.libperformx.entity

data class UserEntity(
    val userId: String,
    val email: String,
    val accessToken: String,
    val role: String
)