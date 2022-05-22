//package com.voidcom.v_base.utils.log
//
//import android.util.Log
//import org.json.JSONArray
//import org.json.JSONException
//import org.json.JSONObject
//
///**
// * Created by zhaokaiqiang on 15/11/18.
// */
//object JsonLog {
//    fun printJson(tag: String?, msg: String, headString: String?) {
//        var message = try {
//            when {
//                msg.startsWith("{") -> JSONObject(msg).toString(KLog.JSON_INDENT)
//                msg.startsWith("[") -> JSONArray(msg).toString(KLog.JSON_INDENT)
//                else -> msg
//            }
//        } catch (e: JSONException) {
//            msg
//        }
//        KLogUtil.printLine(tag, true)
//        message = headString + KLog.LINE_SEPARATOR + message
//        message.split(KLog.LINE_SEPARATOR).toTypedArray().forEach { line ->
//            Log.d(tag, line)
//        }
//        KLogUtil.printLine(tag, false)
//    }
//}