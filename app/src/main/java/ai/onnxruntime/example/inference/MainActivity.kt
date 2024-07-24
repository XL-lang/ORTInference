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
            wsClient?.start("ws://172.27.140.88:4112")}

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




}

