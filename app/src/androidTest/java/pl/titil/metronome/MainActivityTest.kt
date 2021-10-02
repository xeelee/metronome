package pl.titil.metronome

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ServiceTestRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    var serviceRule = ServiceTestRule()

    private val serviceIntent = Intent(ApplicationProvider.getApplicationContext<Context>(), BeatService::class.java)
    private val getService: () -> BeatService = {
        val binder = serviceRule.bindService(serviceIntent)
        (binder as BeatService.Binder).getService()
    }

    @Test
    fun startStopServiceTest() {
        onView(withId(R.id.btn_start))
            .check(matches(isEnabled()))
            .perform(click()).check(matches(isNotEnabled()))

        with(getService()) {
            assert(isPlaying)
            Assert.assertTrue(isPlaying)

            onView(withId(R.id.btn_stop))
                .check(matches(isEnabled()))
                .perform(click()).check(matches(isNotEnabled()))
            Assert.assertFalse(isPlaying)
        }
    }
}