package ai.onnxruntime.example.inference.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.util.concurrent.BlockingQueue
import java.util.logging.Logger
import kotlin.math.min

class WSocket(private val messageQueue: BlockingQueue<ByteArray>) : WebSocketListener() {
    private lateinit var webSocket: WebSocket
    private var cache: ByteArrayOutputStream? = null
    private val logger = Logger.getLogger(WSocket::class.java.name)

    fun start(url: String) {
        val client = OkHttpClient()
        val request: Request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, this)
        logger.info("WebSocket client started")
    }

    fun sendBytesInChunks(data: ByteArray) {
        val chunkSize = 1024 // 可以根据需要调整块大小
        var i = 0
        while (i < data.size) {
            val end = min(data.size, i + chunkSize)
            val chunk = ByteArray(end - i)
            System.arraycopy(data, i, chunk, 0, end - i)
            webSocket.send(ByteString.of(*chunk))
            i += chunkSize
        }
        // 发送空字节作为结束信息
        webSocket.send(EMPTY_BYTE)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        // 接收到分段信息时的处理逻辑
        if (bytes == EMPTY_BYTE) {
            // 接收到空字节时，表示数据接收完毕
            logger.info("接收到数据: ${cache?.size()} 字节")
            cache?.let {
                messageQueue.put(it.toByteArray())
            }
            cache = null
        } else {
            logger.info("接收到数据: ${bytes.size} 字节")
            if (cache == null) {
                cache = ByteArrayOutputStream()
            }
            cache?.write(bytes.toByteArray())
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        println("连接已打开")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        println("连接正在关闭: $reason")
        webSocket.close(code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        System.err.println("连接失败: ${t.message}")
    }

    companion object {
        private val EMPTY_BYTE = ByteString.EMPTY
    }
}


