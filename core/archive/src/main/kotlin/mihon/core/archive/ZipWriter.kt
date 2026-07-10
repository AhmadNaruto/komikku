package mihon.core.archive

import android.content.Context
import bookarchiver.BookFormat
import bookarchiver.BookWriter
import com.hippo.unifile.UniFile
import java.io.Closeable

class ZipWriter(
    val context: Context,
    file: UniFile,
    // SY -->
    encrypt: Boolean = false,
    // SY <--
) : Closeable {
    private val pfd = file.openFileDescriptor(context, "wt")

    // Delegasikan penulisan ke BookWriter dengan format CBZ (ZIP)
    private val delegate = BookWriter(pfd, BookFormat.CBZ)

    fun write(file: UniFile) {
        file.openInputStream().use { input ->
            delegate.writePage(file.name ?: "", input)
        }
    }

    // SY -->
    fun write(fileData: ByteArray, fileName: String) {
        delegate.writePage(fileName, fileData)
    }
    // SY <--

    override fun close() {
        delegate.close()
        pfd.close()
    }
}
