package com.android.applicationoperation.ext

import java.io.*
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*

const val userAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"

/**
 * 向指定URL发送GET方法的请求
 *
 * @param url    发送请求的URL
 * @param params 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
 * @return URL 所代表远程资源的响应结果
 */
fun sendGet(url: String, params: Map<String, String>?, header: Map<String, String>?): String {
    val result = StringBuilder()
    val bufferedReader: BufferedReader
    var param = StringBuilder()
    if (!params.isNullOrEmpty()) {
        param.append("?")
        for (key in params.keys) param.append(key).append("=").append(params[key]).append("&")
        param = StringBuilder(param.substring(0, param.length - 1))
    }
    val realUrl = URL(url + param)
    val connection = realUrl.openConnection()
    if (url.startsWith("https")) {
        val ctx = sslContext()
        (connection as HttpsURLConnection).sslSocketFactory = ctx!!.socketFactory
        connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
    }
    connection.setRequestProperty("accept", "*/*")
    connection.setRequestProperty("connection", "Keep-Alive")
    connection.setRequestProperty("user-agent", userAgent)
    if (!header.isNullOrEmpty()) {
        for (key in header.keys) {
            connection.setRequestProperty(key, header[key])
        }
    }
    val map = connection.headerFields
    println(url)
    for (key in map.keys) println(key + "--->" + map[key])
    connection.connect()
    bufferedReader = BufferedReader(InputStreamReader(connection.getInputStream()))
    var line: String?
    while (bufferedReader.readLine().also { line = it } != null) result.append(line)
    bufferedReader.close()
    return result.toString()
}

/**
 * 向指定 URL 发送POST方法的请求
 *
 * @param url   发送请求的 URL
 * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
 * @return 所代表远程资源的响应结果
 */
fun sendPost(url: String, param: Map<String, String>?, header: Map<String, String>?): String {
    val result = StringBuilder()
    val out: PrintWriter
    val bufferedReader: BufferedReader
    val paramStr = StringBuilder()
    param?.also {
        for (key in param.keys) paramStr.append(key).append("=").append(it[key]).append("&")
    }
    val realUrl = URL(url)
    val conn = realUrl.openConnection()
    conn.setRequestProperty("accept", "*/*")
    conn.setRequestProperty("connection", "Keep-Alive")
    conn.setRequestProperty("user-agent", userAgent)
    if (!header.isNullOrEmpty()) for (key in header.keys) conn.setRequestProperty(key, header[key])
    conn.doOutput = true
    conn.doInput = true
    out = PrintWriter(conn.getOutputStream())
    out.print(paramStr)
    out.flush()
    println(url)
    println(paramStr)
    val map = conn.headerFields
    for (key in map.keys) {
        println(key + "--->" + map[key])
    }
    bufferedReader = BufferedReader(InputStreamReader(conn.getInputStream()))
    var line: String?
    while (bufferedReader.readLine().also { line = it } != null) {
        result.append(line)
    }
    out.close()
    bufferedReader.close()
    return result.toString()
}

fun postJson(url: String, json: String, header: Map<String, String>?): String {
    var result = ""
    val realUrl = URL(url)
    val conn = realUrl.openConnection() as HttpURLConnection
    conn.setRequestProperty("connection", "Keep-Alive")
    conn.setRequestProperty("Charset", "UTF-8")
    conn.setRequestProperty("user-agent", userAgent)
    conn.setRequestProperty("Content-Length", json.toByteArray().size.toString())
    conn.setRequestProperty("Content-type", "application/json")
    if (!header.isNullOrEmpty()) for (key in header.keys) conn.setRequestProperty(key, header[key])
    conn.doOutput = true
    conn.doInput = true
    val out = conn.outputStream
    out.write(json.toByteArray())
    out.flush()
    out.close()
    val map = conn.headerFields
    println("网址:$url")
    println("json:$json")
    for (key in map.keys) {
        println(key + "--->" + map[key])
    }
    if (conn.responseCode == 200) {
        val strBuf = StringBuilder()
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) strBuf.append(line).append("\n")
        reader.close()
        result = strBuf.toString()
    }
    println("请求结果:$result")
    return result
}

fun uploadFile(
    urlStr: String?, fileMap: Map<String, File>?, params: Map<String, String>, header: Map<String, String>
): String {
    val result: String
    val conn: HttpURLConnection
    val boundary = "---------------------------123821742118716" //boundary就是request头和上传文件内容的分隔符
    for (key in params.keys) println("上传参数：" + key + "->" + params[key])
    for (key in header.keys) println("上传头：" + key + "->" + header[key])
    val url = URL(urlStr)
    conn = url.openConnection() as HttpURLConnection
    conn.connectTimeout = 5000
    conn.readTimeout = 30000
    conn.doOutput = true
    conn.doInput = true
    conn.useCaches = false
    conn.requestMethod = "POST"
    conn.setRequestProperty("Connection", "Keep-Alive")
    conn.setRequestProperty("User-Agent", userAgent)
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
    if (header.isNotEmpty()) for (key in header.keys) conn.setRequestProperty(key, header[key])
    val out: OutputStream = DataOutputStream(conn.outputStream)
    val strBuf2 = StringBuilder()
    for ((inputName, inputValue) in params) {
        strBuf2.append("\r\n").append("--").append(boundary).append("\r\n")
        strBuf2.append("Content-Disposition: form-data; name=\"").append(inputName).append("\"\r\n\r\n")
        strBuf2.append(inputValue)
    }
    out.write(strBuf2.toString().toByteArray())
    if (fileMap != null) {
        for ((inputName, file) in fileMap) {
            val filename = file.name
            val strBuf = """
                
                --$boundary
                Content-Disposition: form-data; name="$inputName"; filename="$filename"
                Content-Type:multipart/form-data
                
                
                """.trimIndent()
            out.write(strBuf.toByteArray())
            val `in` = DataInputStream(FileInputStream(file))
            var bytes: Int
            val bufferOut = ByteArray(1024)
            while (`in`.read(bufferOut).also { bytes = it } != -1) out.write(bufferOut, 0, bytes)
            `in`.close()
        }
    }
    val endData = "\r\n--$boundary--\r\n".toByteArray()
    out.write(endData)
    out.flush()
    out.close()
    println("网址:$url")
    val map = conn.headerFields
    for (key in map.keys) println("head:" + key + "--->" + map[key])
    val strBuf = StringBuilder()
    val reader = BufferedReader(InputStreamReader(conn.inputStream))
    var line: String?
    while (reader.readLine().also { line = it } != null) strBuf.append(line).append("\n")
    result = strBuf.toString()
    reader.close()
    conn.disconnect()
    return result
}

internal class MyX509TrustManager : X509ExtendedTrustManager() {
    override fun checkClientTrusted(arg0: Array<X509Certificate>, arg1: String) {}
    override fun checkServerTrusted(arg0: Array<X509Certificate>, arg1: String) {}
    override fun getAcceptedIssuers(): Array<X509Certificate>? {
        return null
    }

    override fun checkClientTrusted(arg0: Array<X509Certificate>, arg1: String, arg2: Socket) {}

    @Throws(CertificateException::class)
    override fun checkClientTrusted(arg0: Array<X509Certificate>, arg1: String, arg2: SSLEngine) {
    }

    override fun checkServerTrusted(arg0: Array<X509Certificate>, arg1: String, arg2: Socket) {}

    @Throws(CertificateException::class)
    override fun checkServerTrusted(arg0: Array<X509Certificate>, arg1: String, arg2: SSLEngine) {
    }
}


fun sslContext(): SSLContext? {
    val tm = arrayOf<TrustManager>(MyX509TrustManager())
    var ctx: SSLContext? = null
    try {
        ctx = SSLContext.getInstance("TLS")
        ctx.init(null, tm, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ctx
}


interface FileDownloadComplete {
    fun error(throwable: Throwable?)
    fun progress(progress: Long)
    fun complete(file: File?)
}


fun downloadFile(downloadUrl: String, fileSavePath: String, complete: FileDownloadComplete) {
    var downloadFile: File? = null
    var connection: HttpURLConnection? = null
    try {
        Thread.currentThread().priority = Thread.MIN_PRIORITY
        val url = URL(downloadUrl)
        connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 60000
        connection.doInput = true
        val inputStream = connection.inputStream
        val temp = File(fileSavePath)
        if (temp.exists()) temp.delete()
        temp.createNewFile()
        temp.setReadable(true, false)
        temp.setWritable(true, false)
        downloadFile = temp
        val contentLength = connection.contentLength
        val os = FileOutputStream(temp)
        val buf = ByteArray(8 * 1024)
        var len: Int
        var totalRead: Long = 0
        try {
            while (inputStream.read(buf).also { len = it } != -1) {
                os.write(buf, 0, len)
                totalRead += len.toLong()
                complete.progress(totalRead * 100 / contentLength)
            }
            os.flush()
            os.fd.sync()
        } finally {
            closeSilently(os)
            closeSilently(inputStream)
        }
        complete.complete(temp)
    } catch (e: Exception) {
        complete.error(e)
        downloadFile?.delete()
    } finally {
        connection?.disconnect()
    }
}

fun closeSilently(closeable: Any?) {
    try {
        if (closeable != null) if (closeable is Closeable) closeable.close()
    } catch (ignored: IOException) {
    }
}
