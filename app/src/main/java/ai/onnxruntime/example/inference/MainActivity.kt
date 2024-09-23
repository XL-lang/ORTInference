package ai.onnxruntime.example.inference

import ai.onnxruntime.example.inference.model.Ort
import ai.onnxruntime.example.inference.net.Listener
import ai.onnxruntime.example.inference.net.WSocket
import ai.onnxruntime.example.inference.net.SocketServer
import ai.onnxruntime.example.inference.utils.Status
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import android.os.Debug
import android.os.SystemClock
import android.util.Log


class MainActivity : AppCompatActivity() {



    private var status = Status
    private var Cur_EP: String = ""
    private var wsClient: WSocket? = null
    private var ort = Ort




    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val executorService = Executors.newCachedThreadPool()
        ort.mainActivity = this


        var messageQueue = LinkedBlockingQueue<ByteArray>()

        var listener = Listener(messageQueue, executorService,this)
        Executors.newSingleThreadExecutor().execute(listener)
        var server = SocketServer(4563,messageQueue,executorService)
        server.startServer()
        Executors.newSingleThreadExecutor().execute {
            wsClient = WSocket(messageQueue)
            wsClient?.start("ws://192.168.1.6:4112")}

    }

    fun getContext(): Context {
        return this
    }

    fun getWSocket(): WSocket {
        return wsClient!!
    }





    override fun onDestroy() {
        super.onDestroy()


    }

    fun logMemoryAndCpuUsage() :ArrayList<Float>{
        val context = this
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pid = android.os.Process.myPid()

        // 获取内存信息
        val memoryInfo = activityManager.getProcessMemoryInfo(intArrayOf(pid))[0]
        val totalPss = memoryInfo.totalPss / 1024.0f // 总内存 (MB)
        val dalvikPss = memoryInfo.dalvikPss / 1024.0f // Dalvik 内存 (MB)
        val nativePss = memoryInfo.nativePss / 1024.0f // Native 内存 (MB)
        val otherPss = memoryInfo.otherPss / 1024.0f // 其他内存 (MB)

        // 获取 CPU 使用情况
        val cpuUsage = getCpuUsageForProcess(pid)

        // 日志输出
        Log.i("Test_Log", """
        总内存: ${String.format("%.2f", totalPss)} MB
        Dalvik Pss: ${String.format("%.2f", dalvikPss)} MB
        Native Pss: ${String.format("%.2f", nativePss)} MB
        Other Pss: ${String.format("%.2f", otherPss)} MB
        CPU 使用率: $cpuUsage%
    """.trimIndent())
        val res = arrayListOf(totalPss,cpuUsage)
        return res
    }

    private fun getCpuUsageForProcess(pid: Int): Float {
        // 读取 /proc/[pid]/stat 文件来获取 CPU 使用情况
        val statFile = "/proc/$pid/stat"
        val statContent = try {
            statFile.toFile().readText()
        } catch (e: Exception) {
            Log.e("Test_Log", "无法读取 $statFile 文件", e)
            return -1f
        }

        val stats = statContent.split(" ")
        val utime = stats[13].toLong()
        val stime = stats[14].toLong()
        val totalTime = utime + stime

        val uptime = SystemClock.uptimeMillis() / 1000
        return (totalTime.toFloat() / uptime) * 100
    }

    private fun String.toFile() = java.io.File(this)




}

