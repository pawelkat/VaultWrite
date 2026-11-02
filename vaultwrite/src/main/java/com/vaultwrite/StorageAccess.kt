package com.vaultwrite

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/**
 * All-files access check + settings deep link (ToDaily UX). Android 11+: MANAGE_EXTERNAL_STORAGE;
 * older: legacy WRITE_EXTERNAL_STORAGE. The host app must declare those permissions.
 */
class StorageAccess(context: Context) {
    private val appContext = context.applicationContext

    fun hasWriteAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            appContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    fun manageAllFilesIntent(): Intent {
        val pkg = Uri.parse("package:${appContext.packageName}")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkg)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Fallback when the per-app screen is missing on some OEM builds. */
    fun manageAllFilesFallbackIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${appContext.packageName}"))
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
