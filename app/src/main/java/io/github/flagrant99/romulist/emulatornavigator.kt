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

    data class RomulistConfig(val folders: List<FolderConfig>)
    data class FolderConfig(val name: String, val extensions: List<String>, val intent: IntentConfig?)
    data class IntentConfig(val packageName: String, val packageName32: String?, val className: String, val action: String, val extras: Map<String, String>)

    fun parseConfig(file: File): RomulistConfig? {
        try {
            val folders = mutableListOf<FolderConfig>()
            val parser = Xml.newPullParser()
            parser.setInput(FileInputStream(file), "UTF-8")

            var eventType = parser.eventType
            var currentFolder: FolderConfig? = null
            var currentIntent: IntentConfig? = null
            var currentExtras = mutableMapOf<String, String>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "folder" -> {
                                val folderName = parser.getAttributeValue(null, "name") ?: ""
                                val extensions = parser.getAttributeValue(null, "extensions")?.split(",")?.map { it.trim() } ?: emptyList()
                                currentFolder = FolderConfig(folderName, extensions, null)
                            }
                            "intent" -> {
                                val pkg = parser.getAttributeValue(null, "packageName") ?: ""
                                val pkg32 = parser.getAttributeValue(null, "packageName32")
                                val cls = parser.getAttributeValue(null, "className") ?: ""
                                currentIntent = IntentConfig(pkg, pkg32, cls, "", emptyMap())
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
                            "intent" -> {
                                currentIntent = currentIntent?.copy(extras = currentExtras.toMap())
                                currentFolder = currentFolder?.copy(intent = currentIntent)
                            }
                            "folder" -> {
                                currentFolder?.let { folders.add(it) }
                                currentFolder = null
                                currentIntent = null
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            return RomulistConfig(folders)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    enum class EmulatorType {
        DOLPHIN, CITRA, RETROARCH, PPSSPP, AZAHAR_VANILLA
    }

    fun launchGame(context: Context, favoritePath: String?, filePath: String, config: RomulistConfig? = null)
    {
        if (config != null) {
            val folderCfg = config.folders.find { f ->
                f.extensions.any { ext -> filePath.lowercase().endsWith(ext.lowercase()) }
            }
            if (folderCfg?.intent != null) {
                val intentCfg = folderCfg.intent
                val is64Bit = android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
                val packageName = if (is64Bit) intentCfg.packageName else (intentCfg.packageName32 ?: intentCfg.packageName)

                if (!isAppInstalled(context, packageName)) {
                    Toast.makeText(context, "Emulator not installed: $packageName", Toast.LENGTH_SHORT).show()
                    return
                }

                val intent = Intent().apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    setClassName(packageName, intentCfg.className)
                    action = intentCfg.action

                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    val dataDir = appInfo.dataDir

                    intentCfg.extras.forEach { (k, v) ->
                        val resolvedValue = v.replace("\$dataDir", dataDir)
                            .replace("/path/to/game.nes", filePath) // For user's specific example
                            .replace("\$filePath", filePath)
                        putExtra(k, resolvedValue)
                    }
                }
                try {
                    context.startActivity(intent)
                    return
                } catch (e: Exception) {
                    Toast.makeText(context, "Error launching custom emulator from XML", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val gamePath = favoritePath?.let { filePath.removePrefix(it).removePrefix("/") } ?: filePath
        val lGamePath = gamePath.lowercase()
        var type = EmulatorType.RETROARCH

        if (lGamePath.startsWith("3ds")) {
            type = EmulatorType.CITRA
        }

        if (lGamePath.startsWith("gamecube")) {
            type = EmulatorType.DOLPHIN
        }

        if (lGamePath.startsWith("wii")) {
            type = EmulatorType.DOLPHIN
        }


        val packageName = when (type) {
            EmulatorType.DOLPHIN -> "org.dolphinemu.dolphinemu"
            EmulatorType.CITRA -> "org.citra.citra_emu"
            EmulatorType.PPSSPP -> "org.ppsspp.ppsspp"
            EmulatorType.RETROARCH -> "com.retroarch.ra32"
            EmulatorType.AZAHAR_VANILLA -> "org.azahar_emu.Azahar"

        }

        if (!isAppInstalled(context, packageName)) {
            Toast.makeText(context, "Emulator not installed: $packageName", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            when (type)
            {
                EmulatorType.DOLPHIN -> {
                    setClassName(packageName, "org.dolphinemu.dolphinemu.ui.main.MainActivity")
                    action = Intent.ACTION_VIEW
                    putExtra("AutoStartFile", filePath)
                }
                EmulatorType.CITRA -> {
                    setClassName(packageName, "org.citra.citra_emu.ui.main.MainActivity")
                    action = Intent.ACTION_VIEW
                    putExtra("AutoStartFile", filePath)
                }
                EmulatorType.PPSSPP -> {
                    setClassName(packageName, "org.ppsspp.ppsspp.PpssppActivity")
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(filePath)
                }
                EmulatorType.RETROARCH -> {
                    setClassName(packageName, "com.retroarch.browser.retroactivity.RetroActivityFuture")
                    putExtra("ROM", filePath)
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    val dataDir = appInfo.dataDir
                    putExtra("LIBRETRO", "$dataDir/cores/fceumm_libretro_android.so")
                    putExtra("CONFIGFILE", "/storage/emulated/0/Android/data/$packageName/files/retroarch.cfg")
                    putExtra("EXTERNAL", "/storage/emulated/0/Android/data/$packageName/files")
                    putExtra("DATADIR", dataDir)
                    putExtra("SDCARD", "/storage/emulated/0")
                }
                EmulatorType.AZAHAR_VANILLA -> {
                    // Logic for AZAHAR_VANILLA can be added here
                    setPackage(packageName)
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(filePath)
                }
            }
        }//end of val intent = Intent().apply {


        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error launching emulator", Toast.LENGTH_SHORT).show()
        }
        return;
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
