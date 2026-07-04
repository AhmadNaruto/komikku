package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.github.imagelibs.avir.AvirImageResizer
import io.github.imagelibs.avir.LancirImageResizer
import logcat.LogPriority
import me.robb.ai_upscale.AiUpscaler
import okio.Buffer
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.nio.ByteBuffer

object ImageEnhancer {

    private val readerPreferences: ReaderPreferences by lazy { Injekt.get() }

    private var aiUpscaler: AiUpscaler? = null

    @Synchronized
    private fun getOrInitAiUpscaler(context: Context): AiUpscaler {
        var upscaler = aiUpscaler
        if (upscaler == null) {
            upscaler = AiUpscaler().apply {
                init(
                    context.assets,
                    "realsr/models-nose/up2x-no-denoise.param",
                    "realsr/models-nose/up2x-no-denoise.bin",
                )
            }
            aiUpscaler = upscaler
        }
        return upscaler
    }

    @Synchronized
    fun cleanup() {
        try {
            aiUpscaler?.cleanup()
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Failed to cleanup AiUpscaler" }
        }
        aiUpscaler = null
    }

    fun enhanceIfNeeded(context: Context, source: BufferedSource): BufferedSource {
        if (!readerPreferences.readerImageEnhance().get()) return source

        val method = readerPreferences.readerImageEnhanceMethod().get()
        try {
            val inputBytes = source.use { it.readByteArray() }
            if (inputBytes.isEmpty()) return Buffer().write(inputBytes)

            if (ImageUtil.isAnimatedAndSupported(Buffer().write(inputBytes))) {
                return Buffer().write(inputBytes)
            }

            val enhancedBytes = when (method) {
                "ai_upscale" -> {
                    upscaleAi(context, inputBytes)
                }
                "avir" -> {
                    upscaleAvir(inputBytes)
                }
                "lancir" -> {
                    upscaleLancir(inputBytes)
                }
                else -> inputBytes
            }
            return Buffer().write(enhancedBytes ?: inputBytes)
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Failed to enhance image using $method" }
            return source
        }
    }

    private fun upscaleAi(context: Context, inputBytes: ByteArray): ByteArray? {
        val cacheDir = File(context.cacheDir, "upscale").apply { mkdirs() }
        val outputFile = File(cacheDir, "upscale_${System.nanoTime()}_${(0..1000).random()}.png")
        try {
            val upscaler = getOrInitAiUpscaler(context)

            val progress = ByteBuffer.allocateDirect(1)
            val killSwitch = ByteBuffer.allocateDirect(1)
            val dataIn = ByteBuffer.allocateDirect(inputBytes.size).apply {
                put(inputBytes)
                position(0)
            }

            killSwitch.put(0, 0)
            val res = upscaler.upscale(dataIn, outputFile.absolutePath, progress, killSwitch)
            if (res == 0 && outputFile.exists()) {
                val outputBytes = outputFile.readBytes()
                outputFile.delete()
                return outputBytes
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "AI Upscale failed" }
        } finally {
            if (outputFile.exists()) {
                outputFile.delete()
            }
        }
        return null
    }

    private fun upscaleAvir(inputBytes: ByteArray): ByteArray? {
        val srcBitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size) ?: return null
        val targetWidth = srcBitmap.width * 2
        try {
            val destBitmap = AvirImageResizer().use { it.resize(srcBitmap, targetWidth) }
            val bos = java.io.ByteArrayOutputStream()
            destBitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
            srcBitmap.recycle()
            destBitmap.recycle()
            return bos.toByteArray()
        } catch (e: Throwable) {
            srcBitmap.recycle()
            throw e
        }
    }

    private fun upscaleLancir(inputBytes: ByteArray): ByteArray? {
        val srcBitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size) ?: return null
        val targetWidth = srcBitmap.width * 2
        try {
            val destBitmap = LancirImageResizer().use { it.resize(srcBitmap, targetWidth) }
            val bos = java.io.ByteArrayOutputStream()
            destBitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
            srcBitmap.recycle()
            destBitmap.recycle()
            return bos.toByteArray()
        } catch (e: Throwable) {
            srcBitmap.recycle()
            throw e
        }
    }
}
