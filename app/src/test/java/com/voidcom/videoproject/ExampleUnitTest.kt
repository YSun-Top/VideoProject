package com.voidcom.videoproject

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import okhttp3.Response
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.lang.Runnable
import java.net.HttpURLConnection
import java.net.URL

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {

//        mHandler.removeCallbacks { runC() }
//        mHandler.postDelayed({ runC() }, 500)
//        Thread.sleep(1000000L)
        val arr = intArrayOf(4, 2, 3, 0, 1)
        sort(arr, 0, arr.size - 1)
        //打印数组
        arr.forEach {
            println(it)
        }
    }
    private fun guiBingPaiXu() {
    }

    // 递归使用归并排序,对arr[l...r]的范围进行排序
    private fun sort(arr: IntArray, l: Int, r: Int) {
        if (l >= r) return
        val mid = (l + r) / 2
        sort(arr, l, mid)
        sort(arr, mid + 1, r)
        // 对于arr[mid] <= arr[mid+1]的情况,不进行merge
        // 对于近乎有序的数组非常有效,但是对于一般情况,有一定的性能损失
        if (arr[mid] > arr[mid + 1]) merge(arr, l, mid, r)
    }

    // 将arr[l...mid]和arr[mid+1...r]两部分进行归并
    private fun merge(arr: IntArray, l: Int, mid: Int, r: Int) {
        val aux = arr.copyOfRange(l, r + 1)
        // 初始化，i指向左半部分的起始索引位置l；j指向右半部分起始索引位置mid+1
        var i = l
        var j = mid + 1
        for (k in l..r) {
            print("merge:")
            arr.forEach {
                print(it)
            }
            if (i > mid) {  // 如果左半部分元素已经全部处理完毕
                arr[k] = aux[j - l]
                j++
            } else if (j > r) {   // 如果右半部分元素已经全部处理完毕
                arr[k] = aux[i - l]
                i++
            } else if (aux[i - l] < aux[j - l]) {  // 左半部分所指元素 < 右半部分所指元素
                arr[k] = aux[i - l]
                i++
            } else {  // 左半部分所指元素 >= 右半部分所指元素
                arr[k] = aux[j - l]
                j++
            }
            println("")
        }
    }

    suspend fun runA():Boolean{
        var isOnline = false

        val responseCode=suspendCancellableCoroutine<Int> {
            try {
                val url = URL("http://www.baidu.com") // or your server address
                val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Connection", "close")
                conn.connectTimeout = 3000
                conn.responseCode
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        isOnline = responseCode == 200
        println("-----------$isOnline")
        return isOnline
    }

    inner class RunTask(val name: String) : Runnable {

        override fun run() {
            var i = 10
            while (i >= 0) {
                println(name + i)
                i--
            }
        }
    }
    fun main() = runBlocking {
        println(1_0_0_0)
        println(1_0_0_0==1000)

//        repeat(1_0_0_0) { i->// 启动大量的协程
//            launch {
//                delay(5000L)
//                print("$i \n")
//            }
//        }
    }
}