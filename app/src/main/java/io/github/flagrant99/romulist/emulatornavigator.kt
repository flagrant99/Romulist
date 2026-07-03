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
        val nameExclusions: List<String> = emptyList(),
        val configSource: String? = null
    )
    
    data class MediaItem(
        val path: String?,
        val type: String = "relative",
        val useSubfolder: Boolean = false
    ) {
        fun resolvePath(configSource: String?): String? {
            val p = path ?: return null
            val configParent = configSource?.let { File(it).parentFile }
            val parentFolderName = configParent?.name ?: ""
            val substitutedPath = p.replace("\$PARENT_FOLDER_NAME", parentFolderName)

            if (type == "fixed") return substitutedPath
            if (configParent == null) return null
            return File(configParent, substitutedPath).absolutePath
        }

        fun resolveMediaFile(configSource: String?, selectedFile: File?, extensions: List<String>): File? {
            val basePath = resolvePath(configSource) ?: return null
            if (selectedFile == null) return null
            val baseName = selectedFile.nameWithoutExtension
            val baseDir = File(basePath)

            val configParent = configSource?.let { File(it).parentFile }
            val relativeDir = if (configParent != null && selectedFile.absolutePath.startsWith(configParent.absolutePath)) {
                val rel = selectedFile.parentFile.absolutePath.substring(configParent.absolutePath.length)
                    .trim(File.separatorChar)
                if (rel.isEmpty()) "" else rel
            } else {
                ""
            }

            val dir = if (useSubfolder) {
                File(baseDir, relativeDir)
            } else {
                baseDir
            }

            val found = extensions
                .map { File(dir, "$baseName$it") }
                .firstOrNull { it.exists() }

            android.util.Log.d("Romulist", "Resolving media: base=$basePath, useSubfolder=$useSubfolder, game=$baseName, relDir=$relativeDir, searched_dir=${dir.absolutePath}, found=${found != null}")

            return found
        }
    }

    data class MediaConfig(
        val cover: MediaItem? = null,
        val marquee: MediaItem? = null,
        val mixart: MediaItem? = null,
        val screen: MediaItem? = null,
        val video: MediaItem? = null
    )
    
    data class FolderConfig(
        val name: String, 
        val extensions: List<String>, 
        val mainIntent: IntentConfig?,
        val altIntents: List<IntentConfig> = emptyList(),
        val media: MediaConfig? = null,
        val useGamelistXmlNames: Boolean = false
    )
    
    data class IntentConfig(
        val name: String,
        val packageName: String,
        val packageName32: String?,
        val className: String?,
        val action: String,
        val categories: List<String> = emptyList(),
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
            val currentCategories = mutableListOf<String>()
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
                            "folder", "app" -> {
                                val nameAttr = parser.getAttributeValue(null, "name")
                                android.util.Log.d("Romulist", "  $tagName nameAttr: $nameAttr, inExclusions: $inExclusions")
                                if (inExclusions) {
                                    nameAttr?.let { exclusions.add(it) }
                                } else {
                                    val extensionsAttr = parser.getAttributeValue(null, "extensions")
                                    val extensions = extensionsAttr?.split(",")?.map { it.trim() } ?: emptyList()
                                    val useGamelistNamesAttr = parser.getAttributeValue(null, "use_gamelistxml_names")
                                    val useGamelistNames = useGamelistNamesAttr?.equals("Y", ignoreCase = true) ?: false
                                    currentFolder = FolderConfig(nameAttr ?: systemName, extensions, null, useGamelistXmlNames = useGamelistNames)
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
                                
                                currentIntent = IntentConfig(effectiveName, pkg, pkg32, cls, "", emptyList(), dataAttr, emptyMap())
                                currentExtras.clear()
                                currentCategories.clear()
                            }
                            "action" -> {
                                val actionName = parser.getAttributeValue(null, "name") ?: ""
                                android.util.Log.d("Romulist", "  action: $actionName")
                                currentIntent = currentIntent?.copy(action = actionName)
                            }
                            "category" -> {
                                val categoryName = parser.getAttributeValue(null, "name")
                                android.util.Log.d("Romulist", "  category: $categoryName")
                                if (categoryName != null) {
                                    currentCategories.add(categoryName)
                                }
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
                                    val typeAttr = parser.getAttributeValue(null, "type") ?: "relative"
                                    val useSubfolderAttr = parser.getAttributeValue(null, "useSubfolder")
                                    val useSubfolder = useSubfolderAttr?.toBoolean() ?: false
                                    val text = parser.nextText()
                                    android.util.Log.d("Romulist", "  media item: $tagName, type=$typeAttr, subfolder=$useSubfolder, path=$text")
                                    val mediaItem = MediaItem(text, typeAttr, useSubfolder)
                                    currentMedia = when (tagName) {
                                        "cover" -> (currentMedia ?: MediaConfig()).copy(cover = mediaItem)
                                        "marquee" -> (currentMedia ?: MediaConfig()).copy(marquee = mediaItem)
                                        "mixart" -> (currentMedia ?: MediaConfig()).copy(mixart = mediaItem)
                                        "screen" -> (currentMedia ?: MediaConfig()).copy(screen = mediaItem)
                                        "video" -> (currentMedia ?: MediaConfig()).copy(video = mediaItem)
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
                                val finalizedIntent = currentIntent?.copy(
                                    extras = currentExtras.toMap(),
                                    categories = currentCategories.toList()
                                )
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
                                currentCategories.clear()
                            }
                            "media" -> inMedia = false
                            "folder", "app" -> {
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
            return RomulistConfig(systemConfig, exclusions, file.absolutePath)
        } catch (e: Exception) {
            android.util.Log.e("Romulist", "Failed to open/parse config: ${e.message}")
            return null
        }
    }

    fun launchGame(context: Context, filePath: String, config: RomulistConfig? = null, preferredIntent: IntentConfig? = null) {
        var intentCfg = preferredIntent ?: config?.systemConfig?.mainIntent

        if (filePath.lowercase().endsWith(".rax")) {
            val raxFile = File(filePath)
            if (raxFile.exists()) {
                val raxConfig = parseConfig(raxFile)
                raxConfig?.systemConfig?.mainIntent?.let {
                    intentCfg = it
                }
            }
        }
        
        val currentIntentCfg = intentCfg
        if (currentIntentCfg == null) {
            Toast.makeText(context, "No emulator configuration found", Toast.LENGTH_SHORT).show()
            return
        }
        val is64Bit = android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        val packageName = if (is64Bit) currentIntentCfg.packageName else (currentIntentCfg.packageName32 ?: currentIntentCfg.packageName)

        if (!isAppInstalled(context, packageName)) {
            Toast.makeText(context, "Emulator not installed: $packageName", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            if (!currentIntentCfg.className.isNullOrBlank()) {
                setClassName(packageName, currentIntentCfg.className)
            } else {
                setPackage(packageName)
            }
            
            action = currentIntentCfg.action.ifEmpty { Intent.ACTION_VIEW }

            currentIntentCfg.categories.forEach { addCategory(it) }

            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val dataDir = appInfo.dataDir
            val externalFilesDir = "/storage/emulated/0/Android/data/$packageName/files"

            currentIntentCfg.data?.let {
                data = Uri.parse(it)
            }

            currentIntentCfg.extras.forEach { (k, v) ->
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

    fun parseGamelist(file: File): Map<String, String> {
        android.util.Log.d("Romulist", "--- Starting parse of gamelist: ${file.absolutePath} ---")
        val fileNamesMap = mutableMapOf<String, String>()
        try {
            val parser = Xml.newPullParser()
            val inputStream = FileInputStream(file)
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentPath: String? = null
            var currentName: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "game" -> {
                                currentPath = null
                                currentName = null
                            }
                            "path" -> currentPath = parser.nextText()
                            "name" -> currentName = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "game") {
                            if (currentPath != null && currentName != null) {
                                val fileName = File(currentPath).name
                                fileNamesMap[fileName] = currentName
                            }
                        }
                    }
                }
                eventType = try { parser.next() } catch (e: Exception) { XmlPullParser.END_DOCUMENT }
            }
            inputStream.close()
        } catch (e: Exception) {
            android.util.Log.e("Romulist", "Failed to parse gamelist.xml: ${e.message}")
        }
        android.util.Log.d("Romulist", "--- Finished parse of gamelist. Found ${fileNamesMap.size} entries ---")
        return fileNamesMap
    }
}
