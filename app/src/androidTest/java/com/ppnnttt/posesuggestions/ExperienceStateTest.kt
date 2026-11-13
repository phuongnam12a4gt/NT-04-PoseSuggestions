package com.ppnnttt.posesuggestions

import android.app.LocaleManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExperienceStateTest {
    @Test fun savedLanguageIsAppliedToTheAndroidFramework() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (Build.VERSION.SDK_INT >= 33) {
            assertEquals(AppLanguageManager.getLanguage(context),context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags())
        }
        val vi = context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag("vi")) })
        assertEquals("Chụp ảnh theo dáng",vi.getString(R.string.smart_photo_assistant))
    }
    @Test fun photosAreSharedThroughAReadablePrivateContentUri() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"pose-review-fixture.jpg")
        val marker = File(context.filesDir, "pose-review-before.txt")
        val arguments = InstrumentationRegistry.getArguments()
        if (arguments.getString("cleanupGallery") == "true") {
            assertTrue(!fixture.exists() || fixture.delete())
            if (arguments.getString("expectCapture") == "true") {
                val before = marker.readLines().toSet()
                val added = fixture.parentFile!!.listFiles()!!.filter { it.name !in before && it.name != fixture.name }
                // Delete only the one photo the smoke test explicitly requested; never bulk-delete a difference.
                val valid = added.size == 1 && added.single().length() > 0 &&
                    android.graphics.BitmapFactory.decodeFile(added.single().absolutePath)?.let { image ->
                        val ok = image.width > 0 && image.height > 0; image.recycle(); ok
                    } == true
                if (added.size == 1) assertTrue(added.single().delete())
                assertTrue("Expected one decodable camera capture", valid)
            }
            marker.delete()
            return
        }
        if (arguments.getString("prepareGallery") == "true") {
            marker.writeText(fixture.parentFile!!.listFiles()?.joinToString("\n") { it.name }.orEmpty())
        }
        val bitmap=Bitmap.createBitmap(320,480,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.rgb(30,100,150))}
        try {
            fixture.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)) }
            val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",fixture)
            assertEquals("content",uri.scheme)
            assertEquals("image/jpeg",context.contentResolver.getType(uri))
            context.contentResolver.openInputStream(uri)!!.use { assertTrue(it.read()>=0) }
        } finally {
            bitmap.recycle()
            if (InstrumentationRegistry.getArguments().getString("prepareGallery") != "true") fixture.delete()
        }
    }
}
