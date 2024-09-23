package ai.onnxruntime.example.inference.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

// 定义结构类
@JsonIgnoreProperties(ignoreUnknown = true)
data class MConfig(
    val name: String?,
    val network: Network?,
    val model: Model?,
    val decode: Decode?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Network(
    val `in`: List<NetworkIn>?,
    val out: List<NetworkOut>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NetworkIn(
    val from: String?,
    val structure: List<String>?,
    val url: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NetworkOut(
    val structure: List<String>?,
    val to: String?,
    val url: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Model(
    val model_name: List<String>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Decode(
    val open: Boolean?,
    val decode_type: String?
)

