package com.udacity.project4.locationreminders

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

//to reduce boilerplate code, create this reusable custom JUnit rule and use it in TasksViewModelTest and DefaultTasksRepositoryTest instead of creating @Before and @After methods for the TestCoroutineDispatcher in every class
//MainCoroutineRule does the following: it creates a TestCoroutineDispatcher, swaps the Dispatchers.Main to the TestCoroutineDispatcher, and cleans the TestCoroutineDispatcher after every test
//when you're writing local tests you should make sure to use a single TestCoroutineDispatcher instead of making multiple ones, and you should run all of your test code using that single TestCoroutineDispatcher, so now that you've created this custom JUnit rule, make sure to always use it, such as when using runBlockingTest since it creates a new TestCoroutineDispatcher by default if you don't provide one, so now use MainCoroutineRule's TestCoroutineDispatcher to run runBlockingTest
@ExperimentalCoroutinesApi
class MainCoroutineRule(private val dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()):
    TestWatcher(), //extending TestWatcher() is what makes MainCoroutineRule a JUnit rule
    TestCoroutineScope by TestCoroutineScope(dispatcher) { //implementing TestCoroutineScope and passing to it a TestCoroutineDispatcher gives MainCoroutineRule the ability to to control Coroutine timing using the TestCoroutineDispatcher
    override fun starting(description: Description?) { //this starting method you're overriding from TestWatcher() look basically like the @Before method from before
        super.starting(description)
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description?) { //this finished method you're overriding from TestWatcher() look basically like the @After method from before
        super.finished(description)
        cleanupTestCoroutines()
        Dispatchers.resetMain()
    }
}