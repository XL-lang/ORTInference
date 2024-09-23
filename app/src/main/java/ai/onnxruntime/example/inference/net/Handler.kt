package ai.onnxruntime.example.inference.net

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.msgpack.core.MessagePack
import org.msgpack.value.Value
import ai.onnxruntime.example.inference.template.MConfig
import ai.onnxruntime.example.inference.utils.Status
import java.util.logging.Logger
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtSession
import ai.onnxruntime.example.inference.MainActivity
import ai.onnxruntime.example.inference.model.Ort
import ai.onnxruntime.example.inference.template.NetworkIn
import ai.onnxruntime.example.inference.template.NetworkOut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.msgpack.core.MessageBufferPacker
import org.msgpack.core.MessageUnpacker
import org.msgpack.value.ArrayValue
import java.lang.Thread.sleep
import java.nio.ByteBuffer

class Handler(var bytes: ByteArray?,val mainActivity: MainActivity) {

    var mStatus  = Status
    private val logger = Logger.getLogger(WSocket::class.java.name)
    private val ort = Ort
init {7


    var deocodedMessage:Map<String,Any> =decodeMessagePack(bytes!!)

    bytes = null
    runDependOnType(deocodedMessage)
}
    fun runDependOnType(deocodedMessage:Map<String,Any>){
        var value:Map<String,Any> = deocodedMessage.get("data") as Map<String, Any>
        when(deocodedMessage.get("type")){
            "config"->{

                mStatus.setMConfig(convertMapToClass(value,MConfig::class.java))


                logger.info("Config set from server")
            }
            "data"->{
                handleTest(value)

            }
            "callback"->{
                mStatus.timeCounter.get_callback_time = System.currentTimeMillis()


            }
            else->{
                throw IllegalArgumentException("Unknown message type: ${deocodedMessage.get("type")}")
            }
        }
    }

    fun handleTest(value: Map<String, Any>) {

        var data_input: Map<String, Any> = value
        mStatus.appendData(data_input)

        var input: Map<String, OnnxTensor> = mStatus.getTensorData()
        for (modelName in mStatus.getMConfig()!!.model!!.model_name!!) {
            mStatus.timeCounter.model_name = modelName
            ort.load_model(modelName)

            for (i in 1..200) {
                sleep(1000)






                    mStatus.timeCounter.inference_start = System.currentTimeMillis()
                    val res: OrtSession.Result? = ort.run_model(input)
                    mStatus.timeCounter.inference_end = System.currentTimeMillis()





                // 启动两个线程



                // 发送测量结果
                sendReport()

                sleep(2000)
            }

            ort.close()
        }
    }


    fun decodeMessagePack(binaryData: ByteArray): Map<String, Any> {
    val unpacker = MessagePack.newDefaultUnpacker(binaryData)
    val value = unpacker.unpackValue()
    return parseValue(value)
}

fun sendReport(){

    var report:Map<String,Any> = mStatus.timeCounter.getReport()
    var data = serializeMap(report)
    var wSClient:WSocket = mainActivity.getWSocket()
    wSClient.sendBytesInChunks(data)
    logger.info("Report sent")

}

fun handleData(value: Map<String, Any>){
    val mDataType = mStatus.getMConfig()!!.decode!!.decode_type;
    var input:Map<String,OnnxTensor> = mapOf()
    when(mDataType){
        "data_input"->{
            var data_input:Map<String, Any> = value
            mStatus.appendData(data_input)
            if(mStatus.getDataSize()==mStatus.getMConfig()!!.network!!.`in`!!.size){

                var data = mStatus.getTensorData()


                input = data
            }
            else
                return

            mStatus.timeCounter.inference_start = System.currentTimeMillis()
            var res:OrtSession.Result? = ort.run_model(input )
            ort.close()
            mStatus.timeCounter.inference_end = System.currentTimeMillis()

            var sendMap = mutableMapOf("type" to "data",
                "data" to changeResult2Map(res!!))
            sendDataToNext(sendMap)


        }
        "msgpack"->{
            throw IllegalArgumentException("Unknown data type: $mDataType")
        }
        else->{
            throw IllegalArgumentException("Unknown data type: $mDataType")
        }
    }
}

fun sendDataToNext(data:MutableMap<String,Any>) {
    var netWorks: MutableList<NetworkOut> = ArrayList()
    for (netOut in mStatus.getMConfig()!!.network!!.out!!) {
        netWorks.add(netOut!!)
    }
    var client = SocketClient()
    if (netWorks[0].to!! == "callback") {
        logger.info("Callback is prepared")
        data["type"] = "callback"
        data["data"] = mapOf("time" to mStatus.timeCounter.get_callback_time)
    }
    var msgBytes =serializeMap(data)

    for (network in netWorks) {
        logger.info("Sending data to ${network.to},size: ${msgBytes.size}")
        client.sendData(network.url!!, msgBytes)
        decodeMessagePack(msgBytes)

    }
}

    fun serializeMap(map: Map<String, Any>): ByteArray {
        val packer: MessageBufferPacker = MessagePack.newDefaultBufferPacker()
        packer.packValue(convertMapToValue(map))
        packer.close()
        return packer.toByteArray()
    }

    fun convertMapToValue(map: Map<String, Any>): Value {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(map.size)
        for ((key, value) in map) {
            packer.packString(key)
            packValueContent(packer, value)
        }
        packer.close()
        return MessagePack.newDefaultUnpacker(packer.toByteArray()).unpackValue()
    }

    fun packValueContent(packer: MessageBufferPacker, value: Any) {
        when (value) {
            is Boolean -> packer.packBoolean(value)
            is Int -> packer.packInt(value)
            is Float -> packer.packFloat(value)
            is Double -> packer.packDouble(value)
            is String -> packer.packString(value)
            is Long -> packer.packLong(value)
            is ByteArray -> packer.packBinaryHeader(value.size).writePayload(value)
            is List<*>-> {
                packer.packArrayHeader(value.size)
                for (item in value) {
                    packValueContent(packer, item!!)
                }
            }
            is Array<*> -> {
                packer.packArrayHeader(value.size)
                for (item in value) {
                    packValueContent(packer, item!!)
                }
            }

            is Map<*, *> -> {
                packer.packMapHeader(value.size)
                for ((k, v) in value) {
                    packValueContent(packer, k!!)
                    packValueContent(packer, v!!)
                }
            }
            is FloatArray -> {
                packer.packArrayHeader(value.size)
                for (item in value) {
                    packer.packFloat(item)
                }
            }
            is LongArray -> {
                packer.packArrayHeader(value.size)
                for (item in value) {
                    packer.packLong(item)
                }
            }
            is IntArray -> {
                packer.packArrayHeader(value.size)
                for (item in value) {
                    packer.packInt(item)
                }
            }
            else ->

                    throw IllegalArgumentException("Unknown value type: $value")


        }
    }

    fun changeResult2OnnxMap(result: OrtSession.Result): Map<String, OnnxTensor> {
        val resultMap = mutableMapOf<String, OnnxTensor>()

        for ((key, value) in result) {
            if (value is OnnxTensor) {
                resultMap[key] = value
            } else {
                throw IllegalStateException("Expected OnnxTensor, but found ${value.javaClass.simpleName} for key $key")
            }
        }

        return resultMap
    }

    fun changeResult2Map(result:OrtSession.Result):Map<String,Any>{
    var res:Map<String,Any> = result.map { (key, value) ->
        key to value.value
    }.toMap()
    return res
}

fun parseValue(value: Value): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    value.asMapValue().map().forEach { (key, value) ->
        val keyStr = key.asStringValue().asString()
        map[keyStr] = parseValueContent(value)
    }
    return map
}

    fun parseValueContent(value: Value): Any {
        return when {
            value.isBooleanValue -> value.asBooleanValue().boolean
            value.isIntegerValue -> value.asIntegerValue().toInt()
            value.isFloatValue -> value.asFloatValue().toFloat()
            value.isStringValue -> value.asStringValue().asString()
            value.isBinaryValue -> value.asBinaryValue().asByteArray()
            value.isArrayValue -> parseArrayContent(value.asArrayValue())
            value.isMapValue -> parseValue(value)
            else -> throw IllegalArgumentException("Unknown value type: $value")
        }
    }

    fun parseArrayContent(arrayValue: ArrayValue): List<Any> {
        val firstItem = arrayValue.list().firstOrNull() ?: return emptyList()
        return when {
            firstItem.isArrayValue -> arrayValue.list().map { parseArrayContent(it.asArrayValue()) }
            firstItem.isBooleanValue -> arrayValue.list().map { it.asBooleanValue().boolean }
            firstItem.isIntegerValue -> arrayValue.list().map { it.asIntegerValue().toLong() }
            firstItem.isFloatValue -> arrayValue.list().map { it.asFloatValue().toFloat() }
            firstItem.isStringValue -> arrayValue.list().map { it.asStringValue().asString() }
            firstItem.isBinaryValue -> arrayValue.list().map { it.asBinaryValue().asByteArray() }
            else -> arrayValue.list().map { parseValueContent(it) }
        }
    }

    fun <T> convertMapToClass(map: Map<String, Any>, clazz: Class<T>): T {
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        return objectMapper.convertValue(map, clazz)
    }





}