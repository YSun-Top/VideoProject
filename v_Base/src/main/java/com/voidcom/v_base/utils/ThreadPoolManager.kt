package com.voidcom.v_base.utils

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.os.Build
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 *
 * @Description: 任务执行窗口
 * @Author: Void
 * @CreateDate: 2022/11/13 21:22
 * @UpdateDate: 2022/11/13 21:22
 */
class ThreadPoolManager private constructor() {
    private val num = Runtime.getRuntime().availableProcessors()
    private val service = Executors.newFixedThreadPool(num)
    private val serialExecutor = SerialExecutor(service)

    /**
     * 顺序执行一个任务
     *
     * @param runnable              任务
     * @param isSequentialExecution 是否顺序执行
     */
    @JvmOverloads
    fun executeTask(runnable: Runnable, isSequentialExecution: Boolean = false) {
        if (isSequentialExecution) {
            serialExecutor.execute(runnable)
        } else {
            service.execute(runnable)
        }
    }

    fun executeTasks(list: ArrayList<Runnable>, isSequentialExecution: Boolean) {
        for (runnable in list) {
            executeTask(runnable, isSequentialExecution)
        }
    }

    companion object {
        val instance: ThreadPoolManager by lazy { ThreadPoolManager() }
    }
}
