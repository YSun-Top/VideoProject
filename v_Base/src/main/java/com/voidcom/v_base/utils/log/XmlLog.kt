//package com.voidcom.v_base.utils.log
//
//import android.util.Log
//import java.io.StringReader
//import java.io.StringWriter
//import java.lang.Exception
//import javax.xml.transform.OutputKeys
//import javax.xml.transform.Source
//import javax.xml.transform.TransformerFactory
//import javax.xml.transform.stream.StreamResult
//import javax.xml.transform.stream.StreamSource
//
///**
// * Created by zhaokaiqiang on 15/11/18.
// */
//object XmlLog {
//    fun printXml(tag: String?, str: String?, headString: String?) {
//        var xml: String
//        if (str != null) {
//            xml = formatXML(str)
//            xml = """
//                $headString
//                $xml
//                """.trimIndent()
//        } else {
//            xml = headString + KLog.NULL_TIPS
//        }
//        KLogUtil.printLine(tag, true)
//        val lines = xml.split(KLog.LINE_SEPARATOR).toTypedArray()
//        for (line in lines) {
//            if (!KLogUtil.isEmpty(line)) {
//                Log.d(tag, "║ $line")
//            }
//        }
//        KLogUtil.printLine(tag, false)
//    }
//
//    private fun formatXML(inputXML: String): String {
//        return try {
//            val xmlInput: Source = StreamSource(StringReader(inputXML))
//            val xmlOutput = StreamResult(StringWriter())
//            val transformer = TransformerFactory.newInstance().newTransformer()
//            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
//            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
//            transformer.transform(xmlInput, xmlOutput)
//            xmlOutput.writer.toString().replaceFirst(">".toRegex(), ">\n")
//        } catch (e: Exception) {
//            e.printStackTrace()
//            inputXML
//        }
//    }
//}