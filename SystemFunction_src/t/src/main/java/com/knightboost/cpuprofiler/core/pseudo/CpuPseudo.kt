package com.knightboost.cpuprofiler.core.pseudo

import com.knightboost.cpuprofiler.core.data.TimeInState
import com.knightboost.cpuprofiler.core.readutil.CpuPseudoReadUtil
import com.knightboost.cpuprofiler.util.CpuUtils.readLong
import java.io.File
import java.util.regex.Pattern

class CpuPseudo(private val cpuIndex: Int) {
    private val basePath = "/sys/devices/system/cpu/cpu${cpuIndex}/"

    private val cpuIdleStates by lazy {
        val idleStates = mutableListOf<CpuIdleState>()
        val file = File("${basePath}/cpuidle")
        val stateFiles = file.listFiles { _, name -> Pattern.matches("state[0-9]", name) }
        for (cpuIdleFile in stateFiles!!) {
            val state = cpuIdleFile.name.replace("state", "").toInt()
            val cpuIdle = CpuIdleState(cpuIndex, state)
            idleStates.add(cpuIdle)
        }
        return@lazy idleStates
    }

    private val timeInStateFile by lazy {
        return@lazy File(basePath + "cpufreq/stats/time_in_state")
    }

    private var lastState0Time = 0L
    private var lastState1Time = 0L
    fun idleTime(): Long {
        var total = 0L
        for (cpuIdleState in cpuIdleStates) {
            val time = cpuIdleState.time()
            if (cpuIdleState.state == 0) {
                lastState0Time = time
            } else if (cpuIdleState.state == 1) {
                lastState1Time = time
            }
            total += time
        }
        return total
    }

    fun timeInState(): TimeInState = CpuPseudoReadUtil.readCpuTimeInState(timeInStateFile)
}

class CpuIdleState(cpuIndex: Int, val state: Int) {
    val path = "/sys/devices/system/cpu/cpu${cpuIndex}/cpuidle/state${state}"

    val name by lazy {
        return@lazy File(path, "name").readText()
    }

    val timeFile by lazy {
        return@lazy File(path, "time")
    }
    val usageFile by lazy {
        return@lazy File(path, "usage")
    }

    fun time(): Long = timeFile.readLong()

    fun usage(): Long = usageFile.readLong()
}

