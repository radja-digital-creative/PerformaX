package id.biz.yuka.libperformx.utils

sealed class ResultState<out T> {
    data class Success<out T>(val data: T) : ResultState<T>()
    data class Error(val message: String, val exception: Exception? = null) : ResultState<Nothing>()
}