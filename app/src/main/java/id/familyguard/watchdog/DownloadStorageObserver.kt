package id.familyguard.watchdog

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import java.io.File

class DownloadStorageObserver(
    private val context: Context,
    private val targetDirectory: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
) {

    private var fileObserver: FileObserver? = null
    var isWatching = false
        private set

    fun startWatching() {
        if (isWatching || !targetDirectory.exists()) return

        val mask = FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO

        fileObserver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(targetDirectory, mask) {
                override fun onEvent(event: Int, path: String?) {
                    handleFileEvent(event, path)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            object : FileObserver(targetDirectory.absolutePath, mask) {
                override fun onEvent(event: Int, path: String?) {
                    handleFileEvent(event, path)
                }
            }
        }

        fileObserver?.startWatching()
        isWatching = true
    }

    fun stopWatching() {
        fileObserver?.stopWatching()
        fileObserver = null
        isWatching = false
    }

    private fun handleFileEvent(event: Int, path: String?) {
        if (path == null) return
        // Ignore in-progress Chrome/browser downloads
        if (path.endsWith(".crdownload", ignoreCase = true) || path.endsWith(".tmp", ignoreCase = true)) {
            return
        }

        if (path.endsWith(".apk", ignoreCase = true)) {
            val apkFile = File(targetDirectory, path)
            if (apkFile.exists()) {
                ApkQuarantineScanner.scanAndQuarantine(context, apkFile)
            }
        }
    }
}
