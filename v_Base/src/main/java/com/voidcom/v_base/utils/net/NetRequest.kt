package com.voidcom.v_base.utils.net

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody

abstract class NetRequest() {
    private val TAG = "NetRequestBody"
    var requestHeader = HashMap<String, String>()

    abstract fun contentType(): String

    abstract fun requestUrl(): String

    abstract fun requestCallback(): NetWorkManager.RequestCallback

    abstract fun getRequestBody(): RequestBody

    abstract fun printDetail()

    /**
     * 请求参数是否进行HTML编码
     */
    fun isEncode(): Boolean = false

    fun getMediaType(): MediaType = contentType().toMediaType()
}