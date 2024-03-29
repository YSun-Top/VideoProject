package com.voidcom.v_base.utils

import android.text.TextUtils
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/**
 * This is a Log tool，with this you can the following
 *
 *  1. use KLog.d(),you could print whether the method execute,and the default tag is current class's name
 *  1. use KLog.d(msg),you could print log as before,and you could location the method with a click in Android Studio Logcat
 *  1. use KLog.json(),you could print json string with well format automatic
 *
 *
 * @author zhaokaiqiang
 * github https://github.com/ZhaoKaiQiang/KLog
 * 15/11/17 扩展功能，添加对文件的支持
 * 15/11/18 扩展功能，增加对XML的支持，修复BUG
 * 15/12/8  扩展功能，添加对任意参数的支持
 * 15/12/11 扩展功能，增加对无限长字符串支持
 * 16/6/13  扩展功能，添加对自定义全局Tag的支持,修复内部类不能点击跳转的BUG
 * 16/6/15  扩展功能，添加不能关闭的KLog.debug(),用于发布版本的Log打印,优化部分代码
 */
object KLog {
    private val LINE_SEPARATOR = System.getProperty("line.separator") ?: ""
    private const val NULL_TIPS = "Log with null object"
    private const val DEFAULT_MESSAGE = "execute"
    private const val TAG_DEFAULT = "KLog"
    private const val FILE_PREFIX = "KLog_"
    private const val FILE_FORMAT = ".log"

    private const val MAX_LENGTH = 4000
    private const val JSON_INDENT = 4
    private const val STACK_TRACE_INDEX_5 = 5

    const val V = 0x1
    const val D = 0x2
    const val I = 0x3
    const val W = 0x4
    const val E = 0x5
    const val A = 0x6

    private const val JSON = 0x7

    //XML=JSON+E
    private const val XML = 0xC

    private var globalTag: String = TAG_DEFAULT
    private var isShowLog = true

    @JvmStatic
    fun init(isShowLog: Boolean, tag: String?) {
        KLog.isShowLog = isShowLog

        tag?.run {
            globalTag = this
        }
    }

    /*
     * base
     * */
    @JvmStatic
    fun v(tag: String? = null, msg: Any) {
        printLog(V, tag, msg)
    }

    @JvmStatic
    fun d(tag: String? = null, msg: Any) {
        printLog(D, tag, msg)
    }

    @JvmStatic
    fun i(tag: String? = null, msg: Any) {
        printLog(I, tag, msg)
    }

    @JvmStatic
    fun w(tag: String? = null, msg: Any) {
        printLog(W, tag, msg)
    }

    @JvmStatic
    fun e(tag: String? = null, msg: Any) {
        printLog(E, tag, msg)
    }

    @JvmStatic
    fun a(tag: String? = null, msg: Any) {
        printLog(A, tag, msg)
    }

    @JvmStatic
    fun json(level: Int, tag: String? = null, jsonFormat: String) {
        printLog(JSON + level, tag, jsonFormat)
    }

    @JvmStatic
    fun xml(level: Int, tag: String? = null, xml: String) {
        printLog(XML + level, tag, xml)
    }

    @JvmStatic
    fun mapString(level: Int, tag: String? = null, map: Map<String, String>) {
        printLog(JSON + level, tag, map)
    }

    @JvmStatic
    fun map(level: Int, tag: String, map: Map<String, Any>?) {
        if (map.isNullOrEmpty()) {
            printLog(level, tag, null)
            return
        }
        val jsonObject = JSONObject()
        try {
            map.forEach { (key, value) ->
                jsonObject.put(key, value.toString())
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        printLog(JSON + level, tag, jsonObject.toString())
    }

    @JvmStatic
    fun file(tag: String, targetDirectory: File, fileName: String? = null, msg: Any) {
        printFile(tag, targetDirectory, fileName, msg)
    }

    @JvmStatic
    fun debug(tag: String? = null, msg: Any? = DEFAULT_MESSAGE) {
        printDebug(tag, msg)
    }

    /*
     * log内容处理
     * */
    private fun wrapperContent(
        stackTraceIndex: Int,
        tagStr: String?,
        msg: Any?
    ): Array<String> {
        val targetElement = Thread.currentThread().stackTrace[stackTraceIndex]
        val className = targetElement.fileName
        val methodName = targetElement.methodName
        var lineNumber = targetElement.lineNumber

        if (lineNumber < 0) lineNumber = 0

        return arrayOf(
            if (TextUtils.isEmpty(tagStr)) globalTag else tagStr ?: "",
            msg?.toString() ?: NULL_TIPS,
            "[($className:$lineNumber)#$methodName] "
        )
    }

    /*
     * log输出
     * */
    private fun printLog(type: Int, tagStr: String?, msg: Any? = DEFAULT_MESSAGE) {
        if (!isShowLog || msg == null) return

        val contents = wrapperContent(STACK_TRACE_INDEX_5, tagStr, msg)
        val tag = contents[0]
        val message = contents[1]
        val headString = contents[2]
        when (type) {
            V, D, I, W, E, A -> checkLenPrint(type, tag, headString + message)
            JSON + V, JSON + D, JSON + I, JSON + W, JSON + E -> printJson(
                type,
                tag,
                message,
                headString
            )
            XML + V, XML + D, XML + I, XML + W, XML + E -> printXml(type, tag, message, headString)
        }
    }

    private fun printDebug(tagStr: String?, msg: Any?) {
        if (msg == null) return
        val contents = wrapperContent(STACK_TRACE_INDEX_5, tagStr, msg)
        checkLenPrint(D, contents[0], contents[2] + contents[1])
    }

    /**
     * 检查长度，如果超出MAX_LENGTH则分段输出
     *
     * @param type
     * @param tag
     * @param msg
     */
    private fun checkLenPrint(type: Int, tag: String, msg: String) {
        val length = msg.length
        val countOfSub = length / MAX_LENGTH
        if (countOfSub == 0) {
            //length<MAX_LENGTH
            print(type, tag, msg)
            return
        }
        //length>MAX_LENGTH
        var sub: String
        var index = 0
        for (i in 0 until countOfSub) {
            sub = msg.substring(index, index + MAX_LENGTH)
            print(type, tag, sub)
            index += MAX_LENGTH
        }
        print(type, tag, msg.substring(index, length))
    }

    private fun print(type: Int, tag: String, sub: String) {
        when (type) {
            V -> Log.v(tag, sub)
            D -> Log.d(tag, sub)
            I -> Log.i(tag, sub)
            W -> Log.w(tag, sub)
            E -> Log.e(tag, sub)
            A -> Log.wtf(tag, sub)
        }
    }

    private fun printJsonXml(level: Int, tag: String, msg: String, headString: String) {
        print(
            level,
            tag,
            "╔═══════════════════════════════════════════════════════════════════════════════════════"
        )

        val message = headString + LINE_SEPARATOR + msg
        if (message.length > MAX_LENGTH) {
            checkLenPrint(level, tag, message)
        } else {
            val lines = message.split(LINE_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            for (line in lines) {
                print(level, tag, "║ $line")
            }
        }

        print(
            level,
            tag,
            "╚═══════════════════════════════════════════════════════════════════════════════════════"
        )
    }

    /*
     * file
     * */

    /**
     * @param tagStr
     * @param targetDirectory
     * @param fileName
     * @param objectMsg
     */
    private fun printFile(
        tagStr: String?,
        targetDirectory: File,
        fileName: String?,
        objectMsg: Any
    ) {
        if (!isShowLog) return
        val contents = wrapperContent(STACK_TRACE_INDEX_5, tagStr, objectMsg)
        printFile(contents[0], targetDirectory, fileName, contents[2], contents[1])
    }

    /**
     * @param tag
     * @param targetDirectory
     * @param fileName
     * @param headString
     * @param msg
     */
    private fun printFile(
        tag: String,
        targetDirectory: File,
        fileName: String?,
        headString: String,
        msg: String
    ) {
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA)
        val nFileName = if (TextUtils.isEmpty(fileName))
            FILE_PREFIX + formatter.format(System.currentTimeMillis()) + FILE_FORMAT
        else
            fileName
        nFileName?.let {
            if (save(targetDirectory, nFileName, msg)) {
                Log.d(
                    tag,
                    headString + " save log success ! location is >>>" + targetDirectory.absolutePath + "/" + nFileName
                )
            } else {
                Log.e(tag, headString + "save log fails !")
            }
        }
    }

    /**
     * @param dic
     * @param fileName
     * @param msg
     * @return
     */
    private fun save(dic: File, fileName: String, msg: String): Boolean {
        return try {
            val outputStream = FileOutputStream(File(dic, fileName))
            val outputStreamWriter = OutputStreamWriter(outputStream, "UTF-8")
            outputStreamWriter.write(msg)
            outputStreamWriter.flush()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 输出json格式
     * @param type Int
     * @param tag String
     * @param msg String
     * @param headString String
     */
    private fun printJson(type: Int, tag: String, msg: String, headString: String) {
        val message = try {
            when {
                msg.startsWith("{") -> JSONObject(msg).toString(JSON_INDENT)
                msg.startsWith("[") -> JSONArray(msg).toString(JSON_INDENT)
                else -> msg
            }
        } catch (e: JSONException) {
            msg
        }
        printJsonXml(type - JSON, tag, message, headString)
    }

    /*
     * xml
     * */
    private fun printXml(type: Int, tag: String, xml: String, headString: String) {
        val nXml = if (TextUtils.isEmpty(xml)) NULL_TIPS else formatXML(xml)
        printJsonXml(type - XML, tag, nXml, headString)
    }

    private fun formatXML(inputXML: String): String {
        return try {
            val xmlInput = StreamSource(StringReader(inputXML))
            val xmlOutput = StreamResult(StringWriter())
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            transformer.transform(xmlInput, xmlOutput)
            xmlOutput.writer.toString().replaceFirst(">".toRegex(), ">\n")
        } catch (e: Exception) {
            e.printStackTrace()
            inputXML
        }
    }
}