package com.ppnnttt.posesuggestions

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // The isolated QA install keeps the production identity with a .qa suffix.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ppnnttt.posesuggestions", appContext.packageName.removeSuffix(".qa"))
    }
}
