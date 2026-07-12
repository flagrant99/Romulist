package io.github.flagrant99.romulist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

fun openWithChrome(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val extension = file.extension.lowercase()
        var mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        
        // Manual overrides for better compatibility
        if (extension == "xml") {
            // Chrome on Android can be extremely stubborn with text/xml for local content URIs.
            // Often, treating it as text/plain is the only way it will actually open the file.
            mimeType = "text/plain"
        } else if (mimeType == null) {
            mimeType = "*/*"
        }
        
        android.util.Log.d("Romulist", "Opening file: ${file.absolutePath} as $mimeType")
        
        fun createIntent(targetMime: String, targetPackage: String?): Intent {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, targetMime)
            targetPackage?.let { intent.setPackage(it) }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }

        try {
            // 1. Try Chrome with the resolved MIME (or text/plain for XML)
            context.startActivity(createIntent(mimeType!!, "com.android.chrome"))
        } catch (e: Exception) {
            try {
                // 2. Try Chrome with text/plain as a second attempt for XML
                if (extension == "xml") {
                    context.startActivity(createIntent("text/plain", "com.android.chrome"))
                } else {
                    throw e
                }
            } catch (e2: Exception) {
                try {
                    // 3. Try Fallback Chooser
                    val chooser = Intent.createChooser(createIntent(mimeType!!, null), "Open with")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } catch (e3: Exception) {
                    // 4. Last resort: Try text/plain for anyone
                    val lastResort = Intent.createChooser(createIntent("text/plain", null), "Open as text")
                    lastResort.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(lastResort)
                }
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
        android.util.Log.e("Romulist", "openWithChrome failed", e)
    }
}
