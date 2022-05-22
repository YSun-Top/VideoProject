package com.voidcom.v_base.utils.net

import com.voidcom.v_base.utils.log.LogUtils
import okhttp3.*
import java.io.IOException
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by Void on 2019/7/11 17:11
 * 网络请求管理
 */
class NetWorkManager : Callback {
    private val NetTAG="NetWorkManager"
    private var threadPoolExecutor = ThreadPoolExecutor(
        3, 10, 120, TimeUnit.SECONDS, LinkedBlockingDeque()
    )
    private var requestList = HashMap<Call, NetRequest>()
    private var okHttpClient = OkHttpClient()

    companion object {
        private var netWorkManager: NetWorkManager? = null
        fun getInstant(): NetWorkManager {
            if (netWorkManager == null)
                netWorkManager = NetWorkManager()
            return netWorkManager!!
        }
    }

    fun executeRequestPost(body: NetRequest) {
        body.printDetail()
        val request = Request.Builder()
        addHeader(request, body)
        request.url(body.requestUrl())
        request.post(body.getRequestBody())
        val call = okHttpClient.newCall(request.build())
        requestList[call] = body
        threadPoolExecutor.execute {
            call.enqueue(this)
        }
    }

    fun executeRequestPut(body: NetRequest) {
        body.printDetail()
        val request = Request.Builder()
        addHeader(request, body)
        request.url(body.requestUrl())
        request.put(body.getRequestBody())
        val call = okHttpClient.newCall(request.build())
        requestList[call] = body
        threadPoolExecutor.execute {
            call.enqueue(this)
        }
    }

    /**
     * 同步执行网络请求
     */
    fun syncExecuteRequest(netRequestObj: NetRequest): Response {
        netRequestObj.printDetail()
        val request = Request.Builder()
        addHeader(request, netRequestObj)
        request.url(netRequestObj.requestUrl())
        request.post(netRequestObj.getRequestBody())
        val call = okHttpClient.newCall(request.build())
        requestList[call] = netRequestObj
        return call.execute()
    }

    /**
     * 添加请求头
     */
    private fun addHeader(request: Request.Builder, netRequestObj: NetRequest) {
        if (netRequestObj.requestHeader.isEmpty()) return
        for (s in netRequestObj.requestHeader)
            request.addHeader(s.key, s.value)
    }

    override fun onFailure(call: Call, e: IOException) {
        try {
            if (requestList.containsKey(call)) {
                requestList[call]?.requestCallback()?.onError(e)
                requestList.remove(call)
            } else {
                LogUtils.e(NetTAG, "一个没有被记录的网络请求！！并且请求失败了")
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResponse(call: Call, response: Response) {
        try {
            if (requestList.containsKey(call)) {
                requestList[call]?.requestCallback()?.onSuccess(response)
                requestList.remove(call)
            } else {
                LogUtils.e(NetTAG, "一个没有被记录的网络请求！！" + response.message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface RequestCallback {
        fun onSuccess(response: Response)
        fun onError(e: IOException)
    }

}