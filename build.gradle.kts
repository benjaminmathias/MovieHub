// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    //alias(libs.plugins.jetbrains.kotlin.serialization) apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1" apply false
}