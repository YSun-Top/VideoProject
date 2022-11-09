package com.voidcom.videoproject.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewbinding.ViewBinding
import com.voidcom.v_base.ui.BaseActivity
import com.voidcom.v_base.ui.BaseViewModel
import com.voidcom.v_base.utils.FileTools
import com.voidcom.v_base.utils.ToastUtils

/**
 *
 * @Description: 将请求读写权限单独抽出来
 * @Author: Void
 * @CreateDate: 2022/11/10 15:21
 * @UpdateDate: 2022/11/10 15:21
 */
abstract class ReadStorageActivity<VB : ViewBinding, VM : BaseViewModel> : BaseActivity<VB, VM>() {

    override fun onInitUI() {
    }

    abstract fun onFilePathCallback(path: String)

    fun getFilePathCallbackRegister(): ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        getFileUriCallback
    )

    private val getFileUriCallback = ActivityResultCallback<ActivityResult> {
        when {
            it.resultCode != RESULT_OK -> ToastUtils.showShort(applicationContext, "文件获取失败")
            it.data == null -> ToastUtils.showShort(applicationContext, "文件路径为空")
            else -> {
                try {
                    val uri = it.data?.data as Uri
                    onFilePathCallback(FileTools.getFilePathByUri(applicationContext, uri) ?: "")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}