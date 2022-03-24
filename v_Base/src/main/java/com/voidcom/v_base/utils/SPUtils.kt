package com.voidcom.v_base.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

object SPUtils {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences(SPUtils::class.java.name, Context.MODE_PRIVATE)
    }

    fun getString(key: String, defValue: String): String = sp.getString(key, defValue) ?: defValue

    /**
     * 保存数据
     */
    fun saveString(key: String, value: String) {
        sp.edit().putString(key, value).apply()
    }

    /**
     * 在 SP 保存的 json 字符串中获取传入的数据类型的数据
     */
    fun <T : Any> getData(key: String, defValue: String, clazz: Class<T>): T? {
        return try {
            Gson().fromJson(sp.getString(key, defValue), clazz)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将传入的数据类型的对象转为 json 字符串并传入 SP
     */
    fun <T : Any> saveData(key: String, clazz: T) {
        saveString(key, Gson().toJson(clazz).toString())
    }


    /**
     * 获取数据
     */
    fun getMsgInt(key: String): Int? {
        return sp.getInt(key, 0)
    }

    /**
     * 保存数据
     */
    fun saveMsgInt(key: String, num: Int) {
        sp.edit().putInt(key, num).apply()
    }

    /**
     * 移除数据
     */
    fun removeMsgInt(key: String) {
        sp.edit().remove(key).apply()
    }

    /**
     * 保存数据 -- boolean
     */
    fun saveMsgBoolean(key: String, boolean: Boolean) {
        sp.edit().putBoolean(key, boolean).apply()
    }

    /**
     * 获取数据
     */
    fun getMsgBoolean(key: String, defValue: Boolean): Boolean {
        return sp.getBoolean(key, defValue)
    }

    /**
     * 清理数据
     */
    fun clear() {
        sp.edit().clear().apply()
    }
}