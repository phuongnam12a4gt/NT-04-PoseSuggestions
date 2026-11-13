package com.ppnnttt.posesuggestions

import org.junit.Assert.*
import org.junit.Test

class PoseExperienceTest {
    @Test fun liveScoringRejectsUncertainOrOutOfFrameBodies() {
        val body = DetectedPose(listOf(
            PoseLandmarkData(11, 300f, 200f, .95f), PoseLandmarkData(12, 600f, 200f, .95f),
            PoseLandmarkData(23, 350f, 550f, .95f), PoseLandmarkData(24, 550f, 550f, .95f),
            PoseLandmarkData(13, 250f, 400f, .95f), PoseLandmarkData(14, 650f, 400f, .95f),
            PoseLandmarkData(25, 350f, 750f, .95f), PoseLandmarkData(26, 550f, 750f, .95f)
        ), 1000, 1000)
        assertTrue(body.hasTrackableBody())
        assertFalse(body.copy(landmarks = body.landmarks.map { it.copy(inFrameLikelihood = .6f) }).hasTrackableBody())
        assertFalse(body.copy(landmarks = body.landmarks.map { it.copy(x = -50f) }).hasTrackableBody())
        assertFalse(body.copy(landmarks = body.landmarks.map { it.copy(y = it.y * .1f) }).hasTrackableBody())
    }
    @Test fun autoCaptureRequiresLeavingThePoseBeforeAnotherShot() {
        val gate = AutoCaptureGate()
        assertFalse(gate.canStart(84f)); assertTrue(gate.canStart(85f))
        gate.captured()
        repeat(100) { assertFalse(gate.canStart(95f)) }
        assertFalse(gate.canStart(81f)); assertFalse(gate.canStart(70f))
        assertTrue(gate.canStart(90f))
    }
    @Test fun selectingANewPoseRearmsCapture() {
        val gate = AutoCaptureGate(); gate.captured(); gate.reset(); assertTrue(gate.canStart(90f))
    }
    @Test fun lowConfidenceEndpointsCannotProducePerfectScore() {
        val template = PoseTemplate("test", "test", "cool", "", "Easy", listOf(
            LandmarkTemplate(11, .2f, .2f), LandmarkTemplate(13, .2f, .4f), LandmarkTemplate(15, .4f, .4f)))
        val user = template.toDetectedPose(1000, 1000)
        val engine = PoseSimilarityEngine()
        assertEquals(100f, engine.calculateSimilarity(user, template), .01f)
        val hiddenWrist = user.copy(landmarks = user.landmarks.map { if (it.type == 15) it.copy(inFrameLikelihood = .1f) else it })
        assertEquals(0f, engine.calculateSimilarity(hiddenWrist, template), .01f)
    }
    @Test fun referenceAnglesDoNotChangeWithCameraAspectRatio() {
        val template = PoseTemplate("test", "test", "cool", "", "Easy", listOf(
            LandmarkTemplate(11, .2f, .2f), LandmarkTemplate(13, .3f, .4f), LandmarkTemplate(15, .5f, .5f)))
        val square = template.toDetectedPose(1000, 1000)
        val portrait = square.copy(imageWidth = 1080, imageHeight = 1920)
        assertEquals(PoseSimilarityEngine().calculateSimilarity(square, template), PoseSimilarityEngine().calculateSimilarity(portrait, template), .01f)
    }
    @Test fun emptyPhotoCannotBecomeACustomPose() {
        assertFalse(DetectedPose(emptyList(), 100, 100).hasUsableBody())
        val faceOnly = DetectedPose((0..10).map { PoseLandmarkData(it, 10f, 10f, 1f) }, 100, 100)
        assertFalse(faceOnly.hasUsableBody())
    }
    @Test fun hiddenLimbCannotBeIgnoredForAPerfectFullBodyScore() {
        val template = PoseTemplate("test","test","cool","","Easy",listOf(
            LandmarkTemplate(11,.2f,.2f),LandmarkTemplate(13,.2f,.4f),LandmarkTemplate(15,.4f,.4f),
            LandmarkTemplate(12,.6f,.2f),LandmarkTemplate(14,.6f,.4f),LandmarkTemplate(16,.8f,.4f)))
        val full = template.toDetectedPose(1000,1000)
        val partial = full.copy(landmarks=full.landmarks.map{if(it.type==16)it.copy(inFrameLikelihood=.1f) else it})
        assertTrue(PoseSimilarityEngine().calculateSimilarity(partial,template)<85f)
    }
}
