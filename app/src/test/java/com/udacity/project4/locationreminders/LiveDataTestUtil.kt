package com.udacity.project4.locationreminders

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


//For full explanation of what this class does, check out this blog post.
//https://medium.com/androiddevelopers/unit-testing-livedata-and-other-common-observability-problems-bb477262eb04


//As you write your own tests that test LiveData, you can similarly copy and use this class in your code.

//this code is similar to the boilerplate code, it creates dummy observer and then removes it, somewhat follows the same pattern
//but in addition, this code observes the LiveData until onChanged is called, or until 2 seconds pass, whichever of those two happens first
@VisibleForTesting(otherwise = VisibleForTesting.NONE)
fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    afterObserve: () -> Unit = {}
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(o: T?) {
            data = o
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }
    this.observeForever(observer)

    try {
        afterObserve.invoke()

        // Don't wait indefinitely if the LiveData is not set.
        if (!latch.await(time, timeUnit)) {
            throw TimeoutException("LiveData value was never set.")
        }

    } finally {
        this.removeObserver(observer)
    }

    @Suppress("UNCHECKED_CAST")
    return data as T
}