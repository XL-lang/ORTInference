package ai.onnxruntime.example.inference.model

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtLoggingLevel
import ai.onnxruntime.OrtSession
import ai.onnxruntime.example.inference.MainActivity
import ai.onnxruntime.extensions.OrtxPackage
import android.content.Context
import java.lang.Thread.sleep
import java.util.logging.Logger

object Ort {
    public var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null
    public var mainActivity : MainActivity? = null
    private var logger = Logger.getLogger(Ort::class.java.name)



    fun load_model(modelName: String){
        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
//        sessionOptions.setSessionLogLevel(OrtLoggingLevel.ORT_LOGGING_LEVEL_VERBOSE)
//        sessionOptions.setSessionLogVerbosityLevel(0)
//        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        var context: Context = mainActivity!!.getContext()
        ortSession = ortEnv.createSession(readModel(modelName,context), sessionOptions)
        logger.info(modelName+" Model loaded")

    }

    fun run_model(data: Map<String, OnnxTensor>): OrtSession.Result? {
        var res = ortSession!!.run(data)
        return res

    }

    private fun readModel(modelName: String,context: Context): ByteArray {
        val modelResId = getRawResourceId(context, modelName)
        return context.resources.openRawResource(modelResId).readBytes()

    }

    private fun getRawResourceId(context: Context, modelName: String): Int {
        return context.resources.getIdentifier(modelName, "raw", context.packageName)
    }

    fun close(){
        ortSession!!.close()

    }
}