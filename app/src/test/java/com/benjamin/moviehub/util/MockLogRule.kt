package com.benjamin.moviehub.util

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Stubs [Log.e] so JVM tests can exercise ViewModel error paths that log handled exceptions. */
class MockLogRule : TestWatcher() {
    override fun starting(description: Description) {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any<Throwable>()) } returns 0
    }

    override fun finished(description: Description) {
        unmockkStatic(Log::class)
    }
}
