package com.ppnnttt.posesuggestions

import android.graphics.Bitmap
import android.graphics.Color
import android.media.*
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ppnnttt.posesuggestions.ui.screens.editor.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class MediaExperienceTest {
    @Test fun importedPhotoIsBoundedSoftwareAndCanBeExported() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val input = File(context.cacheDir, "large-import-regression.png")
        val output = File(context.cacheDir, "large-import-export.png")
        val large = Bitmap.createBitmap(3200, 1600, Bitmap.Config.ARGB_8888)
        try {
            large.eraseColor(Color.BLUE)
            input.outputStream().use { large.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { large.recycle() }
        try {
            val loaded = loadPhoto(context, Uri.fromFile(input))
            try {
                assertTrue(maxOf(loaded.width, loaded.height) <= 2560)
                if (android.os.Build.VERSION.SDK_INT >= 26) assertNotEquals(Bitmap.Config.HARDWARE, loaded.config)
                assertTrue(saveEditedPhotoToUri(context, Uri.fromFile(output), loaded, 10f, 1f, 1f, 90))
                val exported = android.graphics.BitmapFactory.decodeFile(output.absolutePath)
                try {
                    assertEquals(loaded.height, exported.width)
                    assertEquals(loaded.width, exported.height)
                } finally { exported.recycle() }
            } finally { loaded.recycle() }
        } finally { input.delete(); output.delete() }
    }

    @Test fun quarterTurnsPreserveAllCornersAndSource() {
        val source = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(source)
        val paint = android.graphics.Paint()
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
        colors.forEachIndexed { index, color ->
            paint.color = color
            val x = if (index % 2 == 0) 0f else 40f
            val y = if (index < 2) 0f else 20f
            canvas.drawRect(x, y, x + 40f, y + 20f, paint)
        }
        try {
            for (rotation in listOf(0, 90, 180, 270)) {
                val output = renderEditedPhoto(source, PhotoEdits(rotation = rotation))
                try {
                    val corners = listOf(output.getPixel(5, 5), output.getPixel(output.width - 6, 5),
                        output.getPixel(5, output.height - 6), output.getPixel(output.width - 6, output.height - 6))
                    val expected = when (rotation) {
                        90 -> listOf(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN)
                        180 -> colors.reversed()
                        270 -> listOf(Color.GREEN, Color.YELLOW, Color.RED, Color.BLUE)
                        else -> colors
                    }
                    assertEquals(expected, corners)
                    assertEquals(rotatedPhotoSize(source, rotation), output.width to output.height)
                } finally { output.recycle() }
            }
            assertFalse(source.isRecycled)
            assertEquals(Color.RED, source.getPixel(5, 5))
        } finally { source.recycle() }
    }

    @Test fun photoExportRetainsRotationAndTransparency() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, "photo-export-regression.png")
        try {
            source.eraseColor(Color.RED)
            assertTrue(saveEditedPhotoToUri(context, Uri.fromFile(file), source, 0f, 1f, 1f, 90))
            val output = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            try {
                assertEquals(200, output.width)
                assertEquals(400, output.height)
                assertEquals(Color.RED, output.getPixel(100, 200))
                assertFalse(source.isRecycled)
            } finally { output.recycle() }
            source.eraseColor(Color.TRANSPARENT)
            assertTrue(saveEditedPhotoToUri(context, Uri.fromFile(file), source, 20f, 1.2f, 1.5f, 0))
            val cutout = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            try { assertEquals(0, Color.alpha(cutout.getPixel(20, 20))) }
            finally { cutout.recycle() }
        } finally { source.recycle(); file.delete() }
    }

    @Test fun videoExportRetainsRotationKeyframeAndAudioChoice() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val source=File(context.cacheDir,"media-test-source.mp4")
        val withAudio=File(context.cacheDir,"media-test-audio.mp4")
        val silent=File(context.cacheDir,"media-test-silent.mp4")
        try {
            createVideo(source)
            exportTrimmedVideo(context,VideoSelection(Uri.fromFile(source),1500,3000,true),Uri.fromFile(withAudio))
            exportTrimmedVideo(context,VideoSelection(Uri.fromFile(source),1500,3000,false),Uri.fromFile(silent))
            for ((file,audioExpected) in listOf(withAudio to true,silent to false)) {
                val extractor=MediaExtractor()
                try {
                    extractor.setDataSource(file.absolutePath)
                    val tracks=(0 until extractor.trackCount)
                    assertEquals(audioExpected,tracks.any{extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("audio/")})
                    val video=tracks.first{extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("video/")}
                    extractor.selectTrack(video)
                    assertTrue(extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                    assertEquals(0L,extractor.sampleTime)
                    val metadata=MediaMetadataRetriever()
                    try {
                        metadata.setDataSource(file.absolutePath)
                        assertEquals("90",metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION))
                        val duration=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                        assertTrue("Duration: $duration",duration in 1400L..3100L)
                        val frame=metadata.getFrameAtTime(0)
                        assertNotNull("Export must decode",frame); frame?.recycle()
                    } finally { metadata.release() }
                } finally { extractor.release() }
            }
        } finally { source.delete(); withAudio.delete(); silent.delete() }
    }

    private data class Sample(val bytes:ByteArray,val pts:Long,val flags:Int)
    private fun encode(format:MediaFormat,frames:Int,data:ByteArray,stepUs:Long):Pair<MediaFormat,List<Sample>> {
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val software = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull {
            it.isEncoder && (it.name.startsWith("c2.android.") || it.name.startsWith("OMX.google.")) && it.supportedTypes.any { type -> type.equals(mime,true) }
        }
        val codec = if (software != null) MediaCodec.createByCodecName(software.name) else MediaCodec.createEncoderByType(mime)
        val samples=mutableListOf<Sample>(); var outputFormat:MediaFormat?=null
        try {
            codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE); codec.start()
            var next=0; var inputDone=false; var outputDone=false
            val info=MediaCodec.BufferInfo()
            val deadline=System.currentTimeMillis()+30000
            while(!outputDone) {
                check(System.currentTimeMillis()<deadline)
                if(!inputDone) {
                    val index=codec.dequeueInputBuffer(10000)
                    if(index>=0) {
                        val buffer=codec.getInputBuffer(index)!!;buffer.clear()
                        if(next<frames) { buffer.put(data);codec.queueInputBuffer(index,0,data.size,next*stepUs,0);next++ }
                        else { codec.queueInputBuffer(index,0,0,next*stepUs,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true }
                    }
                }
                val index=codec.dequeueOutputBuffer(info,10000)
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) outputFormat=codec.outputFormat
                if(index>=0) {
                    if(info.size>0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG==0 && info.presentationTimeUs>=0) {
                        val buffer=codec.getOutputBuffer(index)!!;buffer.position(info.offset);buffer.limit(info.offset+info.size)
                        val bytes=ByteArray(info.size);buffer.get(bytes);samples+=Sample(bytes,info.presentationTimeUs,info.flags)
                    }
                    outputDone=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0
                    codec.releaseOutputBuffer(index,false)
                }
            }
            return outputFormat!! to samples
        } finally { runCatching{codec.stop()};codec.release() }
    }
    private fun createVideo(file:File) {
        val width=320;val height=240
        val video=MediaFormat.createVideoFormat("video/avc",width,height).apply{
            setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE,150000);setInteger(MediaFormat.KEY_FRAME_RATE,10);setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1)
        }
        val audio=MediaFormat.createAudioFormat("audio/mp4a-latm",44100,1).apply{
            setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC);setInteger(MediaFormat.KEY_BIT_RATE,64000)
        }
        val encodedVideo=encode(video,40,ByteArray(width*height*3/2){if(it<width*height)80 else 128.toByte()},100000)
        val encodedAudio=encode(audio,174,ByteArray(2048),1024L*1000000/44100)
        val muxer=MediaMuxer(file.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            val vt=muxer.addTrack(encodedVideo.first);val at=muxer.addTrack(encodedAudio.first)
            muxer.setOrientationHint(90);muxer.start()
            for((track,samples) in listOf(vt to encodedVideo.second,at to encodedAudio.second)) for(sample in samples) {
                val info=MediaCodec.BufferInfo().apply{set(0,sample.bytes.size,sample.pts,sample.flags)}
                muxer.writeSampleData(track,ByteBuffer.wrap(sample.bytes),info)
            }
            muxer.stop()
        } finally { muxer.release() }
    }
}
