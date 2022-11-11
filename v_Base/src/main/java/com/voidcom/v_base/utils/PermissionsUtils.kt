package com.voidcom.v_base.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.ArrayMap
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.voidcom.v_base.R

object PermissionsUtils {
    private val TAG = PermissionsUtils::class.java.simpleName
    private var permissionDialog: AlertDialog? = null

    fun getStringFormRequestType(mContext: Context, type: Int): String {
        return when (type) {
            AppCode.requestReadStorage,
            AppCode.requestWriteStorage -> mContext.getString(R.string.requestPermission_readStorageMessage)
            AppCode.requestReadPhoneState -> mContext.getString(R.string.requestPermission_readPhoneStateMessage)
            AppCode.requestRecordAudio -> mContext.getString(R.string.requestPermission_recordAudioMessage)
            AppCode.requestAudioSettings -> mContext.getString(R.string.requestPermission_audioSettingsMessage)
            AppCode.requestCamera -> mContext.getString(R.string.requestPermission_cameraMessage)
            AppCode.requestLocation -> mContext.getString(R.string.requestPermission_locationMessage)
            else -> mContext.getString(R.string.requestPermission_notFoundMessage)
        }
    }

    fun getPermissionsFormRequestType(type: Int): Array<String> {
        return when (type) {
            AppCode.requestReadStorage -> AppCode.readStoragePermissions
            AppCode.requestWriteStorage -> AppCode.writeStoragePermissions
            AppCode.requestReadPhoneState -> AppCode.readPhoneStatePermissions
            AppCode.requestRecordAudio -> AppCode.recordAudioPermissions
            AppCode.requestAudioSettings -> AppCode.audioSettingsPermissions
            AppCode.requestCamera -> AppCode.cameraPermissions
            AppCode.requestLocation -> AppCode.locationPermission
            else -> arrayOf("")
        }
    }

    fun gotoPermissionSetting(context: Context, applicationID: String) {
        val brand: String = Build.BRAND //手机厂商
        when (brand.lowercase()) {
            "redmi", "xiaomi" -> gotoMiuiPermission(context) //小米
            "meizu" -> gotoMeizuPermission(context, applicationID)
            "huawei", "honor" -> gotoHuaweiPermission(context)
            else -> context.startActivity(getAppDetailSettingIntent(context))
        }
    }

    fun checkPermission(context: Context, type: Int): Map<String, Boolean> {
        return checkPermission(context, getPermissionsFormRequestType(type))
    }

    fun checkPermission(context: Context, array: Array<String>): Map<String, Boolean> {
        array.forEach {
            if (ContextCompat.checkSelfPermission(
                    context,
                    it
                ) == PackageManager.PERMISSION_DENIED
            ) {
                return ArrayMap<String, Boolean>().apply {
                    set(it, false)
                }
            }
        }
        return emptyMap()
    }

    /**
     * 跳转到miui的权限管理页面
     */
    private fun gotoMiuiPermission(context: Context) {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            putExtra("extra_pkgname", context.packageName)
        }
        try { // MIUI 8
            context.startActivity(intent.apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
            })
        } catch (e: Exception) {
            try { // MIUI 5/6/7
                context.startActivity(intent.apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
                    )
                })
            } catch (e1: Exception) { // 否则跳转到应用详情
                context.startActivity(getAppDetailSettingIntent(context))
            }
        }
    }

    /**
     * 跳转到魅族的权限管理系统
     */
    private fun gotoMeizuPermission(context: Context, applicationID: String) {
        try {
            val intent = Intent("com.meizu.safe.security.SHOW_APPSEC")
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.putExtra("packageName", applicationID)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            context.startActivity(getAppDetailSettingIntent(context))
        }
    }

    /**
     * 华为的权限管理页面
     */
    private fun gotoHuaweiPermission(context: Context) {
        try {
            context.startActivity(Intent().apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.permissionmanager.ui.MainActivity"
                )//华为权限管理
            })
        } catch (e: Exception) {
            e.printStackTrace()
            context.startActivity(getAppDetailSettingIntent(context))
        }
    }

    /**
     * 获取应用详情页面intent（如果找不到要跳转的界面，也可以先把用户引导到系统设置页面）
     */
    private fun getAppDetailSettingIntent(context: Context): Intent = Intent().apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        action = "android.settings.APPLICATION_DETAILS_SETTINGS"
        data = Uri.fromParts("package", context.packageName, null)
    }
}