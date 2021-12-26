package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var database: RemindersDatabase
    private lateinit var remindersLocalRepository: RemindersLocalRepository

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        remindersLocalRepository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.Main
        )
    }

    @After
    fun closeDB() {
        database.close()
    }

    @Test
    fun callingGetReminder_resultInCorrectIdReturned() = runBlocking {
        val reminder = ReminderDTO(
            "Home",
            "Visit family",
            "Home",
            26.57275,
            77.32623,
            "1"
        )
        remindersLocalRepository.saveReminder(reminder)
        val retrievedReminder = remindersLocalRepository.getReminder(reminder.id) as Result.Success
        assertThat(retrievedReminder.data.id, `is`(reminder.id))
    }

    //two predictable errors "data not found" tests fore the repo:

    @Test
    fun callingGetReminderWithWrongId_resultInErrorMessage() = runBlocking {
        val reminder1 = ReminderDTO(
            "Home",
            "Visit family",
            "Home",
            26.57275,
            77.32623,
            "1"
        )

        val reminder2 = ReminderDTO(
            "School",
            "Study",
            "School",
            56.27275,
            47.72665,
            "2"
        )
        remindersLocalRepository.saveReminder(reminder1)
        remindersLocalRepository.saveReminder(reminder2)

        val retrievedReminder = remindersLocalRepository.getReminder("2") as Result.Success
        assertThat(retrievedReminder.data.id, not(equalTo(reminder1.id)))
    }

    @Test
    fun callingReminderIdThatDoesntExist_resultInErrorMessage() = runBlocking {

        val reminder = ReminderDTO(
            "title",
            "dec",
            "loc",
            0.0,
            0.0,
            "0"
        )

        val result = (remindersLocalRepository.getReminder(reminder.id) as Result.Error).message

        assertThat(result, `is`("Reminder not found!"))

    }

}