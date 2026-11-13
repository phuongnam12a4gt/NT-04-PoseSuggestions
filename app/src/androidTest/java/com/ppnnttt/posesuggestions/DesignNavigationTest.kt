package com.ppnnttt.posesuggestions

import android.content.Intent
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.content.edit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Click semantic actions instead of screen coordinates, which vary with device scaling. */
@RunWith(AndroidJUnit4::class)
class DesignNavigationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun find(node: AccessibilityNodeInfo?, label: String): AccessibilityNodeInfo? {
        node ?: return null
        if (node.text?.toString() == label || node.contentDescription?.toString() == label) return node
        for (i in 0 until node.childCount) find(node.getChild(i), label)?.let { return it }
        return null
    }

    private fun awaitLabel(label: String): AccessibilityNodeInfo {
        val deadline = android.os.SystemClock.uptimeMillis() + 10000
        do {
            find(instrumentation.uiAutomation.rootInActiveWindow, label)?.let { return it }
            android.os.SystemClock.sleep(150)
        } while (android.os.SystemClock.uptimeMillis() < deadline)
        error("Missing label: $label")
    }

    private fun tap(resource: Int) {
        var node = awaitLabel(context.getString(resource))
        while (!node.isClickable && node.parent != null) node = node.parent
        assertTrue("Click ${context.getString(resource)}", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(400)
    }

    private fun capture(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
        try {
            File(context.externalCacheDir, "$name.png").outputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally { bitmap.recycle() }
    }

    @Test fun libraryFiltersAndCameraToolsRemainAccessible() {
        val settings = context.getSharedPreferences("camera_settings", android.content.Context.MODE_PRIVATE)
        val previousAutoCapture = settings.getBoolean("auto_capture", false)
        settings.edit { putBoolean("auto_capture", false) }
        try {
        instrumentation.startActivitySync(Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        awaitLabel(context.getString(R.string.smart_photo_assistant))
        capture("redesign-home-verified")
        tap(R.string.explore)
        awaitLabel(context.getString(R.string.pose_marketplace))
        awaitLabel(context.getString(R.string.pose_filters))
        capture("redesign-library-verified")
        tap(R.string.pose_filters)
        awaitLabel(context.getString(R.string.pose_difficulty))
        tap(R.string.pose_filters)
        var pose = awaitLabel("Cool Lean")
        while (!pose.isClickable && pose.parent != null) pose = pose.parent
        assertTrue(pose.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        awaitLabel(context.getString(R.string.customize))
        capture("redesign-camera-verified")
        // Exercise the upgraded CameraX pipeline, using only the isolated QA app's storage.
        val pictures = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)!!
        val existing = pictures.listFiles()?.map { it.name }?.toSet().orEmpty()
        var shot: File? = null
        try {
            android.os.SystemClock.sleep(1500)
            tap(R.string.capture_photo)
            val deadline = android.os.SystemClock.uptimeMillis() + 10000
            while (android.os.SystemClock.uptimeMillis() < deadline) {
                val added = pictures.listFiles()?.filter { it.name !in existing }.orEmpty()
                if (added.size == 1 && added.single().length() > 0) {
                    shot = added.single()
                    break
                }
                android.os.SystemClock.sleep(150)
            }
            assertNotNull("Camera must save a photo", shot)
            val image = android.graphics.BitmapFactory.decodeFile(shot!!.absolutePath)
            assertNotNull("Camera photo must decode", image)
            try { assertTrue(image.width > 0 && image.height > 0) }
            finally { image.recycle() }
        } finally { shot?.let { assertTrue("Remove only the test capture", it.delete()) } }
        tap(R.string.customize)
        awaitLabel(context.getString(R.string.auto_capture))
        awaitLabel(context.getString(R.string.voice_guidance))
        awaitLabel(context.getString(R.string.pose_visibility))
        capture("redesign-tools-verified")
        } finally { settings.edit { putBoolean("auto_capture", previousAutoCapture) } }
    }
}
