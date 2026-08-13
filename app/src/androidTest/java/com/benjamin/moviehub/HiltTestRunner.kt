package com.benjamin.moviehub

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)

    override fun callApplicationOnCreate(application: Application) {
        WorkManager.initialize(application, Configuration.Builder().build())
        super.callApplicationOnCreate(application)
    }
}
