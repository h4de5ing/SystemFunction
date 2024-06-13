package com.knightboost.cpuprofiler.core.data

class ProcCpuTimeInState {
    private val frequencyTimes = LinkedHashMap<Long, Long>()

    fun setTime(frequency: Long, time: Long) {
        frequencyTimes[frequency] = time
    }

    fun totalTime(): Long {
        var total: Long = 0
        for (value in frequencyTimes.values) total += value
        return total
    }
}
