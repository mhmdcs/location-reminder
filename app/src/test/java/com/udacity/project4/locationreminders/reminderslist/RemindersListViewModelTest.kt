package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
class RemindersListViewModelTest {

    //provide testing to the RemindersListViewModel and its live data objects

    @get: Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val fakeDataSource = FakeDataSource()

    private lateinit var remindersListViewModel: RemindersListViewModel

    @Before
    fun createViewModel(){
        stopKoin()
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @Test
    fun loadingReminders_resultsInShowingLoading() = runBlocking {
        fakeDataSource.deleteAllReminders()
        val reminder = ReminderDTO(
            "Home",
            "Visit family",
            "Home",
            26.57275,
            77.32623,
            "1"
        )
        fakeDataSource.saveReminder(reminder)
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(true))
        mainCoroutineRule.resumeDispatcher()
        MatcherAssert.assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(false))
    }

    @Test
    fun noData_resultsInShowingNoData() = runBlockingTest{
        fakeDataSource.deleteAllReminders()
        remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), CoreMatchers.`is`(true))
    }

    @Test
    fun testEmptyLiveDataInViewModelUsingSetReturnError(){
         fakeDataSource.setReturnError(true)
         remindersListViewModel.loadReminders()
        //THEN - verify(assert) that empty LiveData value in the ViewModel is true (which triggers an error message to be shown)
        Assert.assertThat(remindersListViewModel.empty.getOrAwaitValue(), CoreMatchers.`is`(true))
    }

    @Test
    fun testSnackbarInCaseOfExceptionThrown() = runBlockingTest {

        fakeDataSource.deleteAllReminders()
        fakeDataSource.setReturnError(true)
        remindersListViewModel.loadReminders()

        val bool = remindersListViewModel.showSnackBar.getOrAwaitValue() == "Reminders not found"

        MatcherAssert.assertThat(bool, Matchers.`is`(true))

    }

}