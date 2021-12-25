package com.udacity.project4.locationreminders.data.dto


/**
 * A sealed class that encapsulates successful outcome with a value of type [T]
 * or encapsulates a failure outcome with message and statusCode
 */
sealed class Result<out T: Any> {
    data class Success<out T: Any>(val data: T): Result<T>()
    data class Error(val message: String?, val statusCode: Int? = null): Result<Nothing>()
}

//https://stackoverflow.com/questions/68774084/how-to-use-sealed-classes-in-android-using-kotlin