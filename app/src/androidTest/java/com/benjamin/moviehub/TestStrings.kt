package com.benjamin.moviehub

import android.content.Context
import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry

/** Resolves app strings so UI tests assert behavior instead of hardcoded copy. */
object TestStrings {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    fun get(
        @StringRes id: Int,
        vararg formatArgs: Any,
    ): String = context.getString(id, *formatArgs)
}
