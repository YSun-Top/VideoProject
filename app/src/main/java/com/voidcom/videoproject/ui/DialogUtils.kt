package com.voidcom.videoproject.ui

import android.app.Activity
import android.app.AlertDialog
import android.view.Gravity
import android.view.Window
import android.view.WindowManager

/**
 *
 * @Description: java类作用描述
 * @Author: Void
 * @CreateDate: 2022/11/13 21:15
 * @UpdateDate: 2022/11/13 21:15
 */
object DialogUtils {
    fun showLoading(context: Activity?): AlertDialog? {
        return try {
            if (context == null || context.isFinishing) {
                return null
            }
            val dlg: AlertDialog = AlertDialog.Builder(context).show()
            dlg.setCanceledOnTouchOutside(false)
            val window: Window? = dlg.window
            window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            window?.setContentView(android.R.layout.select_dialog_item)
            val lp: WindowManager.LayoutParams? = window?.attributes
            //这里设置居中
            lp?.gravity = Gravity.CENTER
            window?.attributes = lp
            dlg
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
