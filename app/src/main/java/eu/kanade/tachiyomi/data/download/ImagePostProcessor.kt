package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hippo.unifile.UniFile
import io.github.imagelibs.avir.AvirImageResizer
import io.github.imagelibs.avir.LancirImageResizer
import io.github.imagelibs.vips.Vips
import logcat.LogPriority
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

object ImagePostProcessor {

    private val downloadPreferences: DownloadPreferences by lazy { Injekt.get() }

    fun resizeIfNeeded(context: Context, file: UniFile, tmpDir: UniFile, filename: String): UniFile {
        if (!downloadPreferences.downloadImageResize().get()) return file

        val targetWidth = downloadPreferences.downloadImageResizeWidth().get()
        val resizeMethod = downloadPreferences.downloadImageResizeMethod().get()

        try {
            val bytes = file.openInputStream().use { it.readBytes() }
            if (bytes.isEmpty()) return file

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            val srcWidth = options.outWidth

            if (srcWidth <= targetWidth) {
                return file
            }

            val srcBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return file

            val resizedBitmap = when (resizeMethod) {
                "avir" -> {
                    AvirImageResizer().use { it.resize(srcBitmap, targetWidth) }
                }
                "lancir" -> {
                    LancirImageResizer().use { it.resize(srcBitmap, targetWidth) }
                }
                "libvips" -> {
                    Vips.init()
                    Vips.resizeBitmap(srcBitmap, targetWidth)
                }
                else -> srcBitmap
            }

            if (resizedBitmap != srcBitmap) {
                val extension = ImageUtil.findImageType { file.openInputStream() }?.extension ?: "jpg"
                val tempResizedFile = tmpDir.createFile("$filename.resized.tmp")!!
                tempResizedFile.openOutputStream().use { out ->
                    if (extension == "png") {
                        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } else {
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                }
                srcBitmap.recycle()
                resizedBitmap.recycle()

                val originalName = file.name!!
                file.delete()
                tempResizedFile.renameTo(originalName)
                return tmpDir.findFile(originalName) ?: tempResizedFile
            } else {
                srcBitmap.recycle()
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Failed to resize image" }
        }
        return file
    }

    fun compressAndConvertIfNeeded(context: Context, tmpDir: UniFile, filenamePrefix: String) {
        val convertEnabled = downloadPreferences.downloadImageConvert().get()
        val compressEnabled = downloadPreferences.downloadImageCompress().get()

        if (!convertEnabled && !compressEnabled) return

        val targetFormat = if (convertEnabled) downloadPreferences.downloadImageFormat().get() else null
        val quality = downloadPreferences.downloadImageCompressQuality().get()

        try {
            Vips.init()
            if (!Vips.isInitialized()) {
                logcat(LogPriority.ERROR) { "libvips failed to initialize, skipping compress/convert" }
                return
            }

            val filesToProcess = tmpDir.listFiles()?.filter {
                it.name.orEmpty().startsWith(filenamePrefix) && !it.name.orEmpty().endsWith(".tmp")
            }.orEmpty()

            for (file in filesToProcess) {
                val inputBytes = file.openInputStream().use { it.readBytes() }
                if (inputBytes.isEmpty()) continue

                val originalFormat = ImageUtil.findImageType { file.openInputStream() }?.extension ?: "jpg"
                val format = targetFormat ?: originalFormat

                val outputBytes = when (format.lowercase(Locale.ENGLISH)) {
                    "webp" -> Vips.compressWebp(inputBytes, if (compressEnabled) quality else 90)
                    "jpeg", "jpg" -> Vips.compressJpeg(inputBytes, if (compressEnabled) quality else 90)
                    "png" -> Vips.compressPng(inputBytes, if (compressEnabled) (quality * 9 / 100).coerceIn(0, 9) else 6)
                    else -> inputBytes
                }

                if (outputBytes != null && outputBytes !== inputBytes) {
                    val finalExt = if (format == "jpg") "jpg" else format
                    val baseName = file.name!!.substringBeforeLast('.')
                    val newFileName = "$baseName.$finalExt"

                    val tempOutFile = tmpDir.createFile("$newFileName.tmp")!!
                    tempOutFile.openOutputStream().use { it.write(outputBytes) }

                    file.delete()
                    tempOutFile.renameTo(newFileName)
                }
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Failed to compress/convert image" }
        }
    }
}
