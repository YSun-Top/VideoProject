package com.voidcom.v_base.utils.net

import com.voidcom.v_base.utils.LogUtils
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class NetRequestJSONModel(
    val url: String,
    val jsonData: String,
    val callback: NetWorkManager.RequestCallback
) : NetRequest() {

    override fun contentType(): String = "application/json"

    override fun requestUrl(): String = url

    override fun requestCallback(): NetWorkManager.RequestCallback = callback

    override fun getRequestBody(): RequestBody = getRequestJsonBody().toRequestBody(getMediaType())

    override fun printDetail() {
        LogUtils.d("网络请求", StringBuilder().apply {
            append("\n")
            append("==============NetRequestJSONModel=================").append("\n")
            append("请求网址：").append(requestUrl()).append("\n")
            append(" 列表内容 ↓").append("\n")
            append(getRequestJsonBody() + "\n")
            append("==============================================").append("\n")
        }.toString())
    }

    fun getRequestJsonBody(): String = jsonData
}