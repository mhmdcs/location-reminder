package com.udacity.project4.locationreminders.data

import androidx.lifecycle.MutableLiveData
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        reminders?.let { return Result.Success(ArrayList(it)) }
        return Result.Error("Reminders not found")

    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return reminders?.firstOrNull { it.id == id }?.let {
            Result.Success(it)
        } ?: Result.Error("Reminder $id not found")    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

}