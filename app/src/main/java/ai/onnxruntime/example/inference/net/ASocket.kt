package ai.onnxruntime.example.inference.net

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Logger
import kotlin.math.min

class SocketClient {
    private val logger = Logger.getLogger(SocketClient::class.java.name)

    fun sendData(url: String, data: ByteArray) {
        thread {
            val parts = url.split(":")
            val ip = parts[0]
            val port = parts[1].toInt()
            try {
                Socket(ip, port).use { socket ->
                    socket.getOutputStream().use { outputStream ->
                        sendBytesInChunks(outputStream, data)
                    }
                }
                logger.info("Data sent to $url")
            } catch (e: Exception) {
                logger.warning("Exception during sending data: ${e.message}")
            }
        }
    }

    private fun sendBytesInChunks(outputStream: OutputStream, data: ByteArray) {
        val chunkSize = 1024
        var offset = 0
        while (offset < data.size) {
            val end = min(data.size, offset + chunkSize)
            try {
                outputStream.write(data, offset, end - offset)
            } catch (e: Exception) {
                logger.warning("Exception during writing data chunk: ${e.message}")
                break
            }
            offset += chunkSize
        }
        // 使用特定的结束标志
        try {
            outputStream.write(byteArrayOf(0xFF.toByte()))
            outputStream.flush()  // Ensure data is sent immediately
        } catch (e: Exception) {
            logger.warning("Exception during writing end signal: ${e.message}")
        }
    }
}


class SocketServer(
    private val port: Int,
    private val messageQueue: BlockingQueue<ByteArray>,
    private val executorService: ExecutorService
) {
    private val cache: MutableMap<String, ByteArrayOutputStream> = mutableMapOf()
    private val logger = Logger.getLogger(SocketServer::class.java.name)

    fun startServer() {
        executorService.execute {
            ServerSocket(port).use { serverSocket ->
                println("Server started on port $port")
                while (true) {
                    val clientSocket = serverSocket.accept()
                    val clientAddress = clientSocket.remoteSocketAddress.toString()
                    handleClient(clientSocket, clientAddress)
                }
            }
        }
    }

    private fun handleClient(socket: Socket, clientAddress: String) {
        executorService.execute {
            try {
                socket.use { clientSocket ->
                    clientSocket.getInputStream().use { inputStream ->
                        receiveBytesInChunks(inputStream, clientAddress)
                    }
                }
            } catch (e: Exception) {
                logger.warning("Exception during handling client: ${e.message}")
            }
        }
    }

    private fun receiveBytesInChunks(inputStream: InputStream, clientAddress: String) {
        val chunkSize = 1024
        val buffer = ByteArray(chunkSize)
        val byteArrayOutputStream = cache.getOrPut(clientAddress) { ByteArrayOutputStream() }

        while (true) {
            try {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1 || (bytesRead == 1 && buffer[0] == 0xFF.toByte())) {
                    // 使用特定的结束标志0xFF
                    synchronized(cache) {
                        val completeData = byteArrayOutputStream.toByteArray()
                        if (completeData.isNotEmpty() && completeData.last() == 0xFF.toByte()) {
                            byteArrayOutputStream.reset()
                            byteArrayOutputStream.write(completeData, 0, completeData.size - 1)
                        }
                        messageQueue.put(byteArrayOutputStream.toByteArray())
                        cache.remove(clientAddress)
                    }
                    logger.info("Data received from $clientAddress, size is ${byteArrayOutputStream.size()}")
                    break
                }
                synchronized(cache) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead)
                }
            } catch (e: Exception) {
                logger.warning("Exception during reading data chunk: ${e.message}")
                break
            }
        }
    }
}


