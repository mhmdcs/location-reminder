package com.udacity.project4.locationreminders.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.runBlocking
import java.util.LinkedHashMap

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    //LinkedHashMap represents the data that comes back from the database and the network
    var remindersServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    //a MutableLiveData that contains your list of observable reminders
    private val observableReminders = MutableLiveData<Result<List<ReminderDTO>>>()

    private var shouldReturnError = false //boolean flag shouldReturnError, set initially to false, which means that by default an error is not returned.

    fun setReturnError(value: Boolean) { //Create a setReturnError method that changes whether or not the FakeDataSource should return errors
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        if(shouldReturnError){ //Wrap getReminders in if statements so that if shouldReturnError is true, the method returns Result.Error:
            return Result.Error("Reminders not found")
        }
        return Result.Success(remindersServiceData.values.toList())

    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {

        if(shouldReturnError){ //Wrap getReminder in if statements so that if shouldReturnError is true, the method returns Result.Error:
            return Result.Error("Reminder not found")
        }
        remindersServiceData[id]?.let {
            return Result.Success(it)
        }
        return Result.Error("Could not find reminder")

    }

    override fun observeReminders(): LiveData<Result<List<ReminderDTO>>> {
        runBlocking { refreshReminders() }
        return observableReminders
    }

    override suspend fun refreshReminders() {
        observableReminders.value = getReminders()
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

}