package com.voidcom.v_base.utils

import java.util.*
import java.util.concurrent.Executor

/**
 *
 * @Description: java类作用描述
 * @Author: Void
 * @CreateDate: 2022/11/13 21:23
 * @UpdateDate: 2022/11/13 21:23
 */
open class SerialExecutor internal constructor(val executor: Executor) : Executor {
    val tasks = ArrayDeque<Runnable>()
    var active: Runnable? = null

    @Synchronized
    override fun execute(r: Runnable) {
        tasks.offer(Runnable {
            try {
                r.run()
            } finally {
                scheduleNext()
            }
        })
        if (active == null) {
            scheduleNext()
        }
    }

    @Synchronized
    protected fun scheduleNext() {
        if (tasks.poll().also { active = it } != null) {
            executor.execute(active)
        }
    }
}