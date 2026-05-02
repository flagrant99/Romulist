package com.example.goepnavapp7

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

object EmulatorNavigator {

    enum class EmulatorType {
        DOLPHIN, CITRA, RETROARCH, PPSSPP, AZAHAR_VANILLA
    }

    fun launchGame(context: Context, favoritePath: String?, filePath: String)
    {
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
