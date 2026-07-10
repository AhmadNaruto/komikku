package mihon.core.archive

import android.os.ParcelFileDescriptor
import io.github.bookarchiver.BookReader
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.InputStream

class ArchiveReader(pfd: ParcelFileDescriptor) : Closeable {
    @PublishedApi
    internal val delegate = BookReader(pfd)

    // SY -->
    var encrypted: Boolean = false
        private set
    var wrongPassword: Boolean? = null
        private set
    val archiveHashCode = pfd.hashCode()
    // SY <--

    /**
     * Mengembalikan sequence daftar file di dalam arsip.
     * BookReader mengindeks halaman secara instan dalam O(1) di sisi Rust.
     */
    inline fun <T> useEntries(block: (Sequence<ArchiveEntry>) -> T): T {
        val pages = delegate.getPages()
        val sequence = pages.asSequence().map { name ->
            ArchiveEntry(
                name = name,
                isFile = true,
                isEncrypted = false,
            )
        }
        return block(sequence)
    }

    /**
     * Membaca satu halaman gambar secara cepat (O(1) Memory-mapped offset lookup)
     */
    fun getInputStream(entryName: String): InputStream? {
        return try {
            val bytes = delegate.readPage(entryName)
            ByteArrayInputStream(bytes)
        } catch (e: Exception) {
            null
        }
    }

    override fun close() {
        delegate.close()
    }
}
