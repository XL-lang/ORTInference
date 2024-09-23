package ai.onnxruntime.example.inference.utils

class TimeCounter {
    public var inference_start: Long = 0
    public var inference_end: Long = 0
    public var get_callback_time: Long = 0
    public var model_name:String = "not set"
    public var size:Long = 0L




    fun getReport(): Map<String, Any> {
        var report:Map<String,Any> = mapOf(
            "inference" to (inference_end - inference_start),
            "model_name" to model_name,
            "size(B)" to size

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