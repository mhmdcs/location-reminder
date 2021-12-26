package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    private val databindingIdlingResource = DataBindingIdlingResource()

    // get activity context
    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }


    //add End to End testing to the app

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(databindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(databindingIdlingResource)
    }

    @Test
    fun toastMessageTest()  {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        databindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderTitle)).perform(replaceText("New title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("New description"))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.select_location_fragment_save_location)).perform(click())

        val toastMessage = getActivity(activityScenario)?.getString(R.string.err_select_location)
        onView(withText(toastMessage)).inRoot(withDecorView(not(`is`(getActivity(activityScenario)?.window?.decorView))))

        activityScenario.close()
    }


    @Test
    fun successfullyCreateReminder() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        databindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.noDataTextView))
            .check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())

        onView(withId(R.id.reminderTitle))
            .perform(ViewActions.typeText("Reminder"), ViewActions.closeSoftKeyboard())
        onView(withId(R.id.reminderDescription))
            .perform(ViewActions.typeText("Description"), ViewActions.closeSoftKeyboard())

        onView(withId(R.id.selectLocation)).perform(click())

        onView(withId(R.id.select_location_fragment_map_view)).perform(click())
        onView(withId(R.id.select_location_fragment_save_location)).perform(click())

        onView(withId(R.id.saveReminder)).perform(click())

        onView(withText("Reminder"))
            .check(matches(isDisplayed()))
        onView(withText("Description"))
            .check(matches(isDisplayed()))

        //toast message "Reminder Saved !"  after saving a reminder test
        val toastMessage = getActivity(activityScenario)?.getString(R.string.reminder_saved)
        onView(withText(toastMessage)).inRoot(withDecorView(not(`is`(getActivity(activityScenario)?.window?.decorView))))

        activityScenario.close()
    }

}
