package com.example.hrapredict.utils.dataframe

import kotlin.math.pow
import kotlin.math.sqrt

class D2Frame(private val numColumns: Int) {
    private var _data : List<MutableList<Float>> =
        List(size=numColumns) {
            mutableListOf()
        }

    private val data
        get() = _data.map {
            it.toList()
        }

    val dataframe
        get() = D2Frame(3).also {
            it.putValues(*(_data.toTypedArray()))
        }

    val shape : Pair<Int, Int>
        get() = Pair(_data[0].size, _data.size)

    fun putValue(vararg values : Float){
        if(values.size != numColumns)
            return //TODO: Should throw exception here
        values.forEachIndexed { index, value ->
            _data[index].add(value)
        }
    }

    private fun putValues(vararg valuesList: List<Float>){
        if(valuesList.size != numColumns)
            return //TODO: Should throw exception here
        valuesList.forEachIndexed { index, list ->
            _data[index].addAll(list)
        }
    }

    /**
     * Splits the frame's data into equal length chunks. If the last chunk's length is less than chunkSize,
     * drop that last chunk. Chunks are returned as Dataframes.
     * @param chunkSize size of each chunk to split
     * @return List of chunks as D2Frame
     */
    fun chunk(chunkSize: Int) : List<D2Frame> {
        var chunksList = _data.map {
            it.chunked(chunkSize)
        }
        if(chunksList[0].last().size < chunkSize){
            chunksList = chunksList.map {
                it.dropLast(1)
            }
        }
        val chunkedList = mutableListOf<D2Frame>()
        for(i in chunksList[0].indices){
            chunkedList.add(D2Frame(numColumns).apply {
                val singleChunkData = arrayListOf<List<Float>>().also {
                    for(j in chunksList.indices){
                        it.add(chunksList[j][i])
                    }
                }
                putValues(*(singleChunkData.toTypedArray()))
            })
        }
        return chunkedList.toList()
    }

    /**
     * Create a 1D array from the frame's data by appending rows together
     * @return The result 1D float array
     */
    fun flattenByRow() : FloatArray {
        return mutableListOf<Float>().also { flatArray ->
            for(i in _data[0].indices){
                for(j in _data.indices){
                    flatArray.add(_data[j][i])
                }
            }
        }.toFloatArray()
    }

    /**
     * Create a 1D array from the frame's data by appending columns together
     * @return The result 1D float array
     */
    fun flattenByCol() : FloatArray {
        return mutableListOf<Float>().apply {
            for(col in _data){
                addAll(col)
            }
        }.toFloatArray()
    }

    /**
     * Concatenates 2 D2Frame column-wised.
     * @param other the other D2Frame to concat to this one.
     * @return The concatenated frame
     */
    fun concat(other: D2Frame): D2Frame {
        return D2Frame(shape.second + other.shape.second).also {
            it.putValues(*(_data.toTypedArray()), *(other.data.toTypedArray()))
        }
    }

    fun extractMeanAndStd() : FloatArray {
        val res = mutableListOf<Float>()
        _data.forEach { col ->
            val mean = col.sum() / col.size
            res.add(mean)
            val std = sqrt((col.reduce { acc, fl -> acc + (fl.toDouble() - mean.toDouble()).pow(2.0).toFloat()} / col.size).toDouble())
            res.add(std.toFloat())
        }
        return res.toFloatArray()
    }

    fun clearData() {
        for(col in _data){
            col.clear()
        }
    }
}