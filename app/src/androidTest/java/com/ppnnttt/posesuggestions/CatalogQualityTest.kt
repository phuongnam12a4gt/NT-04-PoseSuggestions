package com.ppnnttt.posesuggestions

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CatalogQualityTest {
    @Test fun referenceArtworkHasScorableLandmarks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val templates = PoseTemplateRepository(context).loadTemplates().filter { !it.isCustom }
        assertEquals(4, templates.size)
        templates.forEach { template ->
            val resource = template.artworkResource()
            assertTrue("Missing artwork: ${template.id}", resource != 0)
            val types = template.landmarks.map { it.type }.toSet()
            assertTrue("Missing scoring joints: ${template.id}", listOf(11,12,13,14,15,16,23,24,25,26,27,28).all { it in types })
            assertEquals(types.size, template.landmarks.size)
            assertTrue(template.landmarks.all { it.x in 0f..1f && it.y in 0f..1f })
            assertTrue("Unscorable reference: ${template.id}", PoseSimilarityEngine().calculateSimilarity(template.toDetectedPose(1000,1000), template) > 99f)
        }
    }
    @Test fun guidanceSaysStraightenWhenTheTargetAngleIsLarger() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val template = PoseTemplate("test", "test", "cool", "", "Easy", listOf(
            LandmarkTemplate(11, .2f, .2f), LandmarkTemplate(13, .2f, .4f), LandmarkTemplate(15, .2f, .6f)))
        val user = DetectedPose(listOf(PoseLandmarkData(11,200f,200f,1f), PoseLandmarkData(13,200f,400f,1f), PoseLandmarkData(15,400f,400f,1f)),1000,1000)
        val guidance = PoseGuidanceEngine(context).getGuidance(user, template)
        assertEquals(context.getString(R.string.straighten_joint, context.getString(R.string.left_arm)), guidance?.message)
    }
}
