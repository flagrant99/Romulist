package io.github.flagrant99.romulist

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Xml
import android.widget.Toast
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream

object EmulatorNavigator {

    data class RomulistConfig(
        val systemConfig: FolderConfig?, 
        val nameExclusions: List<String> = emptyList()
    )
    
    data class FolderConfig(
        val name: String, 
        val extensions: List<String>, 
        val intent: IntentConfig?
    )
    
    data class IntentConfig(
        val packageName: String,
        val packageName32: String?,
        val className: String?,
        val action: String,
        val data: String?,
        val extras: Map<String, String>
    )

    fun parseConfig(file: File): RomulistConfig? {
        try {
            var systemConfig: FolderConfig? = null
            val exclusions = mutableListOf<String>()
            val parser = Xml.newPullParser()
            val inputStream = FileInputStream(file)
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentFolder: FolderConfig? = null
            var currentIntent: IntentConfig? = null
            var currentExtras = mutableMapOf<String, String>()
            var inExclusions = false

            // Determine the system name from the parent directory (e.g., "nes" from Roms/nes/romulist.xml)
            val systemName = file.parentFile?.name ?: "Unknown"

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "exclusions" -> inExclusions = true
                            "folder" -> {
                                val nameAttr = parser.getAttributeValue(null, "name")
                                if (inExclusions) {
                                    nameAttr?.let { exclusions.add(it) }
                                } else {
                                    val extensions = parser.getAttributeValue(null, "extensions")?.split(",")?.map { it.trim() } ?: emptyList()
                                    // Use directory name if 'name' attribute is missing
                                    currentFolder = FolderConfig(nameAttr ?: systemName, extensions, null)
                                }
                            }
                            "intent" -> {
                                val pkg = parser.getAttributeValue(null, "packageName") ?: ""
                                val pkg32 = parser.getAttributeValue(null, "packageName32")
                                val cls = parser.getAttributeValue(null, "className")
                                val dataAttr = parser.getAttributeValue(null, "data")
                                currentIntent = IntentConfig(pkg, pkg32, cls, "", dataAttr, emptyMap())
                                currentExtras = mutableMapOf()
                            }
                            "action" -> {
                                val actionName = parser.getAttributeValue(null, "name") ?: ""
                                currentIntent = currentIntent?.copy(action = actionName)
                            }
                            "extra" -> {
                                val extraName = parser.getAttributeValue(null, "name")
                                val extraValue = parser.getAttributeValue(null, "value")
                                if (extraName != null && extraValue != null) {
                                    currentExtras[extraName] = extraValue
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tagName) {
                            "exclusions" -> inExclusions = false
                            "intent" -> {
                                currentIntent = currentIntent?.copy(extras = currentExtras.toMap())
                                currentFolder = currentFolder?.copy(intent = currentIntent)
                            }
                            "folder" -> {
                                if (!inExclusions) {
                                    systemConfig = currentFolder
                                    currentFolder = null
                                    currentIntent = null
                                }
                            }
                        }
                    }
                }
                eventType = try { parser.next() } catch (e: Exception) { 
                    android.util.Log.e("Romulist", "XML Parse Error in ${file.name}: ${e.message}")
                    XmlPullParser.END_DOCUMENT 
                }
            }
            inputStream.close()
            return RomulistConfig(systemConfig, exclusions)
        } catch (e: Exception) {
            android.util.Log.e("Romulist", "Failed to open/parse config: ${e.message}")
            return null
        }
    }

    fun launchGame(context: Context, filePath: String, config: RomulistConfig? = null) {
        val systemCfg = config?.systemConfig
        if (systemCfg?.intent == null) {
            Toast.makeText(context, "No emulator configuration found", Toast.LENGTH_SHORT).show()
            return
        }

        // Optional: Check if extension matches before launching
        val ext = File(filePath).extension.lowercase()
        if (systemCfg.extensions.isNotEmpty() && !systemCfg.extensions.any { it.lowercase() == ext }) {
            Toast.makeText(context, "Unsupported file extension: $ext", Toast.LENGTH_SHORT).show()
            return
        }

        val intentCfg = systemCfg.intent
        val is64Bit = android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        val packageName = if (is64Bit) intentCfg.packageName else (intentCfg.packageName32 ?: intentCfg.packageName)

        if (!isAppInstalled(context, packageName)) {
            Toast.makeText(context, "Emulator not installed: $packageName", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            if (!intentCfg.className.isNullOrBlank()) {
                setClassName(packageName, intentCfg.className)
            } else {
                setPackage(packageName)
            }
            
            action = if (intentCfg.action.isNotEmpty()) intentCfg.action else Intent.ACTION_VIEW

            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val dataDir = appInfo.dataDir

            intentCfg.data?.let {
                val resolvedData = it.replace("\$DATA_DIR", dataDir)
                    .replace("\$FILE_PATH", filePath)
                data = Uri.parse(resolvedData)
            }

            intentCfg.extras.forEach { (k, v) ->
                val resolvedValue = v.replace("\$DATA_DIR", dataDir)
                    .replace("\$FILE_PATH", filePath)
                putExtra(k, resolvedValue)
            }
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error launching emulator: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
