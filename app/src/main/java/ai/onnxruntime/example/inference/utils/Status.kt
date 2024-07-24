package ai.onnxruntime.example.inference.utils

import ai.onnxruntime.example.inference.template.MConfig
import java.util.concurrent.locks.ReentrantLock
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.example.inference.model.Ort
import java.nio.FloatBuffer
import java.nio.LongBuffer

object Status {
    private var ort = Ort
    private var mConfig: MConfig? = null
    private val lock = ReentrantLock()
    private val data_list: MutableList<Map<String, Any>> = ArrayList()
    public  var  timeCounter = TimeCounter()

    fun setMConfig(config: MConfig) {
        lock.lock()
        try {
            mConfig = config
        } finally {
            lock.unlock()
        }
    }

    fun getMConfig(): MConfig? {
        lock.lock()
        return try {
            mConfig
        } finally {
            lock.unlock()
        }
    }

    fun appendData(data: Map<String, Any>) {
        lock.lock()
        try {
            data_list.add(data)
        } finally {
            lock.unlock()
        }
    }

    fun getDataSize(): Int {
        lock.lock()
        return try {
            data_list.size
        } finally {
            lock.unlock()
        }
    }

    private fun mergeLists(valuesList: List<List<Any>>): List<Any> {
        if (valuesList.isEmpty()) return emptyList()

        val first = valuesList[0]
        return when (first.first()) {
            is Long, is Int  -> {
                val result = MutableList((first as List<Long>).size) { 0L }
                for (values in valuesList) {
                    val list = values as List<Long>
                    for (i in list.indices) {
                        result[i] += list[i]
                    }
                }
                result.map { it / valuesList.size }
            }
            is Float -> {
                val result = MutableList((first as List<Float>).size) { 0f }
                for (values in valuesList) {
                    val list = values as List<Float>
                    for (i in list.indices) {
                        result[i] += list[i]
                    }
                }
                result.map { it / valuesList.size }
            }
            is List<*> -> {
                val result = MutableList((first as List<*>).size) { mutableListOf<Any>() }
                for (values in valuesList) {
                    val list = values as List<List<Any>>
                    for (i in list.indices) {
                        result[i].add(list[i])
                    }
                }
                result.map { mergeLists(it as List<List<Any>>) }
            }
            else -> throw IllegalArgumentException("Unsupported data type")
        }
    }

    fun mergeData(): Map<String, Any> {
        lock.lock()
        return try {
            val aggregatedData = mutableMapOf<String, MutableList<List<Any>>>()

            for (data in data_list) {
                for ((key, values) in data) {
                    if (aggregatedData.containsKey(key)) {
                        aggregatedData[key]!!.add(values as List<Any>)
                    } else {
                        aggregatedData[key] = mutableListOf(values as List<Any>)
                    }
                }
            }

            val mergedData = mutableMapOf<String, Any>()
            for ((key, valuesList) in aggregatedData) {
                mergedData[key] = mergeLists(valuesList)
            }

            mergedData
        } finally {
            lock.unlock()
        }
    }

    private fun createTensor(value: Any, ortEnv: OrtEnvironment): OnnxTensor {
        return when (value) {
            is List<*> -> {
                val firstValue = value.first()
                when (firstValue) {
                    is Long, is Int -> {
                        val longArray = value.map { it as Long }.toLongArray()
                        val shape = longArrayOf(1, longArray.size.toLong())
                        OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(longArray), shape)
                    }
                    is Float -> {
                        val floatArray = value.map { it as Float }.toFloatArray()
                        val shape = longArrayOf(1, floatArray.size.toLong())
                        OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(floatArray), shape)
                    }
                    is List<*> -> {
                        val flattenedValues = flattenNestedList(value as List<Any>)
                        val shape = getShape(value as List<Any>)
                        when (flattenedValues.first()) {
                            is Long -> {
                                val longArray = flattenedValues.map { it as Long }.toLongArray()
                                OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(longArray), shape)
                            }
                            is Float -> {
                                val floatArray = flattenedValues.map { it as Float }.toFloatArray()
                                OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(floatArray), shape)
                            }
                            else -> throw IllegalArgumentException("Unsupported tensor data type")
                        }
                    }
                    else -> throw IllegalArgumentException("Unsupported data type for nested list")
                }
            }
            else -> throw IllegalArgumentException("Unsupported data type")
        }
    }

    private fun flattenNestedList(nestedList: List<Any>): List<Any> {
        val flattenedList = mutableListOf<Any>()
        for (item in nestedList) {
            if (item is List<*>) {
                flattenedList.addAll(flattenNestedList(item as List<Any>))
            } else {
                flattenedList.add(item)
            }
        }
        return flattenedList
    }

    private fun getShape(nestedList: List<Any>): LongArray {
        val shape = mutableListOf<Long>()
        var current = nestedList
        while (current is List<*>) {
            shape.add(current.size.toLong())
            if (current.isNotEmpty() && current.first() is List<*>) {
                current = current.first() as List<Any>
            } else {
                break
            }
        }
        return shape.toLongArray()
    }

    fun getTensorData(): Map<String, OnnxTensor> {
        lock.lock()
        return try {
            val mergedData = mergeData()
            val tensors = mutableMapOf<String, OnnxTensor>()
            for ((key, value) in mergedData) {
                tensors[key] = createTensor(value, ort.ortEnv)
            }
            tensors
        } finally {
            lock.unlock()
        }
    }
}

