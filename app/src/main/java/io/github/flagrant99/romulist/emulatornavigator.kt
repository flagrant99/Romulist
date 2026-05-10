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
    
    data class MediaConfig(
        val cover: String? = null,
        val marquee: String? = null,
        val mixart: String? = null,
        val screen: String? = null,
        val video: String? = null
    )
    
    data class FolderConfig(
        val name: String, 
        val extensions: List<String>, 
        val mainIntent: IntentConfig?,
        val altIntents: List<IntentConfig> = emptyList(),
        val media: MediaConfig? = null
    )
    
    data class IntentConfig(
        val name: String,
        val packageName: String,
        val packageName32: String?,
        val className: String?,
        val action: String,
        val data: String?,
        val extras: Map<String, String>
    )

    fun parseConfig(file: File): RomulistConfig? {
        android.util.Log.d("Romulist", "--- Starting parse of: ${file.absolutePath} ---")
        try {
            var systemConfig: FolderConfig? = null
            val exclusions = mutableListOf<String>()
            val parser = Xml.newPullParser()
            val inputStream = FileInputStream(file)
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentFolder: FolderConfig? = null
            var currentIntent: IntentConfig? = null
            val currentExtras = mutableMapOf<String, String>()
            var inExclusions = false
            var inAltIntents = false
            var inMedia = false
            val altIntents = mutableListOf<IntentConfig>()
            var currentMedia: MediaConfig? = null

            // Determine the system name from the parent directory (e.g., "nes" from Roms/nes/romulist.xml)
            val systemName = file.parentFile?.name ?: "Unknown"

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        android.util.Log.d("Romulist", "START_TAG: <$tagName>")
                        when (tagName) {
                            "exclusions" -> inExclusions = true
                            "alt_intents" -> inAltIntents = true
                            "folder" -> {
                                val nameAttr = parser.getAttributeValue(null, "name")
                                android.util.Log.d("Romulist", "  folder nameAttr: $nameAttr, inExclusions: $inExclusions")
                                if (inExclusions) {
                                    nameAttr?.let { exclusions.add(it) }
                                } else {
                                    val extensionsAttr = parser.getAttributeValue(null, "extensions")
                                    val extensions = extensionsAttr?.split(",")?.map { it.trim() } ?: emptyList()
                                    currentFolder = FolderConfig(nameAttr ?: systemName, extensions, null)
                                }
                            }
                            "intent" -> {
                                val nameAttr = parser.getAttributeValue(null, "name") 
                                    ?: parser.getAttributeValue(null, "Name")
                                
                                val pkg = parser.getAttributeValue(null, "packageName") ?: ""
                                val pkg32 = parser.getAttributeValue(null, "packageName32")
                                val cls = parser.getAttributeValue(null, "className")
                                val dataAttr = parser.getAttributeValue(null, "data")
                                
                                val effectiveName = when {
                                    !nameAttr.isNullOrBlank() -> nameAttr
                                    currentFolder != null && !currentFolder.name.isBlank() -> currentFolder.name
                                    !systemName.isBlank() -> systemName
                                    else -> "Launch"
                                }

                                android.util.Log.d("Romulist", "Found intent start: nameAttr='$nameAttr', effective='$effectiveName', pkg='$pkg'")
                                
                                currentIntent = IntentConfig(effectiveName, pkg, pkg32, cls, "", dataAttr, emptyMap())
                                currentExtras.clear()
                            }
                            "action" -> {
                                val actionName = parser.getAttributeValue(null, "name") ?: ""
                                android.util.Log.d("Romulist", "  action: $actionName")
                                currentIntent = currentIntent?.copy(action = actionName)
                            }
                            "extra" -> {
                                val extraName = parser.getAttributeValue(null, "name")
                                val extraValue = parser.getAttributeValue(null, "value")
                                android.util.Log.d("Romulist", "  extra: $extraName = $extraValue")
                                if (extraName != null && extraValue != null) {
                                    currentExtras[extraName] = extraValue
                                }
                            }
                            "media" -> inMedia = true
                            "cover", "marquee", "mixart", "screen", "video" -> {
                                if (inMedia) {
                                    val text = parser.nextText()
                                    currentMedia = when (tagName) {
                                        "cover" -> (currentMedia ?: MediaConfig()).copy(cover = text)
                                        "marquee" -> (currentMedia ?: MediaConfig()).copy(marquee = text)
                                        "mixart" -> (currentMedia ?: MediaConfig()).copy(mixart = text)
                                        "screen" -> (currentMedia ?: MediaConfig()).copy(screen = text)
                                        "video" -> (currentMedia ?: MediaConfig()).copy(video = text)
                                        else -> currentMedia
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        android.util.Log.d("Romulist", "END_TAG: </$tagName>")
                        when (tagName) {
                            "exclusions" -> inExclusions = false
                            "alt_intents" -> inAltIntents = false
                            "intent" -> {
                                val finalizedIntent = currentIntent?.copy(extras = currentExtras.toMap())
                                android.util.Log.d("Romulist", "  Finalizing intent: ${finalizedIntent?.name}")
                                if (finalizedIntent != null) {
                                    if (inAltIntents) {
                                        altIntents.add(finalizedIntent)
                                    } else {
                                        currentFolder = currentFolder?.copy(mainIntent = finalizedIntent)
                                    }
                                }
                                currentIntent = null
                                currentExtras.clear()
                            }
                            "media" -> inMedia = false
                            "folder" -> {
                                if (!inExclusions) {
                                    systemConfig = currentFolder?.copy(
                                        altIntents = altIntents.toList(),
                                        media = currentMedia
                                    )
                                    android.util.Log.d("Romulist", "  Finalizing systemConfig for: ${systemConfig?.name}")
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
            android.util.Log.d("Romulist", "--- Finished parse. systemConfig found: ${systemConfig != null} ---")
            return RomulistConfig(systemConfig, exclusions)
        } catch (e: Exception) {
            android.util.Log.e("Romulist", "Failed to open/parse config: ${e.message}")
            return null
        }
    }

    fun launchGame(context: Context, filePath: String, config: RomulistConfig? = null, preferredIntent: IntentConfig? = null) {
        val systemCfg = config?.systemConfig
        val intentCfg = preferredIntent ?: systemCfg?.mainIntent
        
        if (intentCfg == null) {
            Toast.makeText(context, "No emulator configuration found", Toast.LENGTH_SHORT).show()
            return
        }
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
            val externalFilesDir = "/storage/emulated/0/Android/data/$packageName/files"

            intentCfg.data?.let {
                data = Uri.parse(it)
            }

            intentCfg.extras.forEach { (k, v) ->
                val resolvedValue = v.replace("\$DATA_DIR", dataDir)
                    .replace("\$FILE_PATH", filePath)
                    .replace("\$EXTERNAL_FILES_DIR", externalFilesDir)
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
