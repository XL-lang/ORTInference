package ai.onnxruntime.example.inference.net

import ai.onnxruntime.example.inference.MainActivity
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.locks.ReentrantLock


internal class Listener(
    messageQueue: BlockingQueue<ByteArray>,
    private val executorService: ExecutorService,
    private val mainActivity: MainActivity,
) :
    Runnable {
    private val messageQueue: BlockingQueue<ByteArray> = messageQueue
    private val lock = ReentrantLock()

    override fun run() {
        while (true) {
            try {
                // 从消息队列中取出消息
                val message: ByteArray = messageQueue.take()



                try {
                    executorService.execute {
                        var mHandler = Handler(message, mainActivity)
                    }
                } finally {
                    // 确保在处理完消息后解锁
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }
}