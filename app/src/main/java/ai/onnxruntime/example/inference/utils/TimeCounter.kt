package ai.onnxruntime.example.inference.utils

class TimeCounter {
    public var inference_start: Long = 0
    public var inference_end: Long = 0
    public var get_callback_time: Long = 0
    public var decodeTime:Long = 0
    public var encodeTime:Long = 0
    public var decodeTmp:Long = 0
    public var encodeTmp:Long = 0

    @Synchronized
    public fun startDecode() {
        if (decodeTmp.toInt()!=0)
            return
        decodeTmp = System.currentTimeMillis()
    }

    @Synchronized
    public fun endDecode() {
        if (decodeTime.toInt()!=0)
        {
            if ((System.currentTimeMillis() - decodeTmp) > decodeTime)
                decodeTime = System.currentTimeMillis() - decodeTmp
        }
        else
        decodeTime = System.currentTimeMillis() - decodeTmp
        decodeTmp = 0
    }

    @Synchronized
    public fun startEncode() {
        if (encodeTmp.toInt()!=0)
            return
        encodeTmp = System.currentTimeMillis()
    }

    @Synchronized
    public fun endEncode() {
        if (encodeTime.toInt()!=0)
        {
            if ((System.currentTimeMillis() - encodeTmp) > encodeTime)
                encodeTime = System.currentTimeMillis() - encodeTmp
        }
        else
        encodeTime = System.currentTimeMillis() - encodeTmp
        encodeTmp = 0
    }


    fun getReport(): Map<String, Any> {
        var report:Map<String,Long> = mapOf(
            "inference" to (inference_end - inference_start),
            "drop_time" to (decodeTime+encodeTime)

        )

        if (get_callback_time != 0L) {
            report += mapOf(
                "callback" to (get_callback_time - inference_start)
            )
        }
        return mapOf(
            "type" to "report",
            "data" to report
        )
    }
}