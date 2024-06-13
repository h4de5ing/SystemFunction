package com.knightboost.cpuprofiler.core.data

class ProcTimeInState {
    private val cpus: MutableList<ProcCpuTimeInState> = ArrayList()

    fun addCpuTimeInState(procCpuTimeInState: ProcCpuTimeInState) {
        cpus.add(procCpuTimeInState)
    }

    fun totalTime(): Long {
        var total: Long = 0
        for (procCpuTimeInState in cpus) total += procCpuTimeInState.totalTime()
        return total
    }

    companion object {
        @JvmField
        var EMPTY: ProcTimeInState = ProcTimeInState()
    }
}
