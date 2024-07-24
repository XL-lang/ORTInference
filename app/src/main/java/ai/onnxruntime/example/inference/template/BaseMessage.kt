package ai.onnxruntime.example.inference.template

class BaseMessage {
    public  lateinit var type : String
    public   var data : Map<String, Any>? = null
}

