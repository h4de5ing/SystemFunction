package com.knightboost.cpuprofiler.core.data

class TimeInState {
    private val frequencyTimes = LinkedHashMap<Long, Long>()

    fun setTime(frequency: Long, time: Long) {
        frequencyTimes[frequency] = time
    }

    fun getTimeOfFrequency(frequency: Long): Long = frequencyTimes[frequency] ?: 0

    /**
     * 总的 ji
     *
     * @return
     */
    fun spendTime(): Long {
        var total: Long = 0
        //usertime 时间单位为10ms
        for (value in frequencyTimes.values) total += value * 10
        return total
    }

    companion object {
        @JvmField
        val EMPTY: TimeInState = TimeInState()
    }
}
