package com.voidcom.v_base.utils.net

import com.voidcom.v_base.utils.log.LogUtils
import okhttp3.FormBody
import okhttp3.RequestBody

class NetRequestFormModel(
    var url: String,
    var requestCallback: NetWorkManager.RequestCallback
) : NetRequest() {
    private var data = HashMap<String, Any>()

    init {
        setContentType(contentType())
    }

    override fun contentType(): String = "application/x-www-form-urlencoded"

    override fun requestUrl(): String = url

    override fun requestCallback(): NetWorkManager.RequestCallback = requestCallback

    override fun getRequestBody(): RequestBody = FormBody.Builder().apply {
        data.forEach {
            if (isEncode())
                addEncoded(it.key, it.value.toString())
            else
                add(it.key, it.value.toString())
        }
    }.build()

    override fun printDetail() {
        LogUtils.d("网络请求", StringBuilder().apply {
            append("\n")
            append("==============================================").append("\n")
            append("请求网址：").append(requestUrl()).append("\n")
            append("请求参数列表长度 ").append(sizeBody()).append(" 列表内容 ↓").append("\n")
            append(toString() + "\n")
            append("==============================================").append("\n")
        }.toString())
    }

    fun getData() = data

    //region header
    fun addHeader(keyStr: String, valueStr: String) {
        requestHeader[keyStr] = valueStr
    }

    fun setContentType(string: String) {
        removeHeader("Content-Type")
        addHeader("Content-Type", string)
    }

    fun addAllHeader(items: Map<String, String>) {
        requestHeader.putAll(items)
    }

    fun removeHeader(keyStr: String) {
        requestHeader.remove(keyStr)
    }

    fun removeAllHeader() {
        requestHeader.clear()
    }

    fun containsKeyHeader(keyStr: String) = requestHeader.containsKey(keyStr)

    fun sizeHeader() = requestHeader.size
    //endregion

    //region body
    fun addBody(keyStr: String, valueStr: Any): NetRequestFormModel {
        data[keyStr] = valueStr
        return this
    }

    fun addAllBody(items: Map<String, String>) {
        data.putAll(items)
    }

    fun removeBody(keyStr: String) {
        data.remove(keyStr)
    }

    fun removeAllData() {
        data.clear()
    }

    fun containsKeyBody(keyStr: String) = data.containsKey(keyStr)

    fun sizeBody() = data.size
    //endregion

    override fun toString(): String = StringBuilder().apply {
        append("-----header-----\n")
        for (s in requestHeader)
            append(s.key).append(":").append(s.value).append("; \n")
        append("------body------\n")
        for (s in data)
            append(s.key).append(":").append(s.value).append("; \n")
    }.toString()
}