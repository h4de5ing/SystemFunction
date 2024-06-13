package com.knightboost.cpuprofiler.util

import android.os.Build
import android.system.Os.sysconf
import android.system.OsConstants
import com.knightboost.cpuprofiler.model.CpuCluster
import java.io.File
import java.io.FilenameFilter
import java.util.Arrays
import java.util.regex.Pattern

object CpuUtils {

    val cpuFiles by lazy {
        val cpuFile = File("/sys/devices/system/cpu")
        val filter = FilenameFilter { _, name -> Pattern.matches("cpu[0-9]", name) }
        return@lazy cpuFile.listFiles(filter)
    }

    private val cpuCount = cpuFiles.size

    val boardPlatform by lazy {
        AndroidSysProperties.getSystemProperty("ro.board.platform", "") ?: ""
    }

    val cpuIdleStates by lazy {
        val cpuIdleMaxStates = mutableListOf<Int>()
        for (i in 0 until cpuCount) {
            val file = File("/sys/devices/system/cpu/cpu$i/cpuidle")
            val cpuIdleFiles = file.listFiles { _, name -> Pattern.matches("state[0-9]", name) }
            val maxIdleState = if (cpuIdleFiles == null) 0 else cpuIdleFiles.size - 1
            cpuIdleMaxStates.add(maxIdleState)
        }
        return@lazy cpuIdleMaxStates
    }

    val cpuIdleFiles: List<File> by lazy {
        val idleFiles = mutableListOf<File>()
        for (i in 0 until cpuCount) {
            val file = File("/sys/devices/system/cpu/cpu$i/cpuidle")
            file.listFiles { _, name -> Pattern.matches("state[0-9]", name) }
                ?.let { idleFiles.addAll(it) }
        }
        return@lazy idleFiles
    }


    val eachCpuIdleFiles: List<List<File>> by lazy {
        val files = mutableListOf<List<File>>()
        for (i in 0 until cpuCount) {
            val idleFiles = mutableListOf<File>()
            val file = File("/sys/devices/system/cpu/cpu$i/cpuidle")
            file.listFiles { _, name -> Pattern.matches("state[0-9]", name) }
                ?.let { idleFiles.addAll(it) }
            files.add(idleFiles)
        }
        files
    }

    val cpuClusters: List<CpuCluster> by lazy {
        val cpuClustersInfo = mutableListOf<CpuCluster>()
        val cpuFreqFile = File("/sys/devices/system/cpu/cpufreq")

        val policyFilter = FilenameFilter { _, name
            ->
            return@FilenameFilter Pattern.matches("policy[0-9]", name)
        }

        val policyFiles: Array<File> =
            cpuFreqFile.listFiles(policyFilter) ?: return@lazy cpuClustersInfo
        //sort by character order
        Arrays.sort(policyFiles)

        val cpuClusters = mutableListOf<CpuCluster>()
        for (policyFile in policyFiles) {

            val cpuClusterInfo = CpuCluster(policyFile)
            cpuClusters.add(cpuClusterInfo)
        }
        return@lazy cpuClusters

    }

    val clockTicksPerSeconds by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return@lazy sysconf(OsConstants._SC_CLK_TCK)
        } else {
            100
        }
    }

    /**
     * 每节拍对应的毫秒数
     */
    val millSecondsPerTicks by lazy {
        return@lazy 1000 / clockTicksPerSeconds
    }

    fun isCpuRunningInMaxFrequency(cpuIndex: Int): Boolean {
        return scalingMaxFreq(cpuIndex) == scalingCurFreq(cpuIndex)
    }

    private fun cpuIndexPath(cpuIndex: Int): String {
        return "/sys/devices/system/cpu/cpu$cpuIndex/"
    }

    fun scalingMaxFreq(cpuIndex: Int): Long {
        return readLong(
            cpuIndexPath(cpuIndex),
            "cpufreq/scaling_max_freq"
        )
    }

    fun scalingMinFreq(cpuIndex: Int): Long {
        return readLong(
            cpuIndexPath(cpuIndex),
            "cpufreq/scaling_min_freq"
        )
    }

    fun scalingCurFreq(cpuIndex: Int): Long {
        return readLong(
            cpuIndexPath(cpuIndex),
            "cpufreq/scaling_cur_freq"
        )
    }

    class CpuPesudo(cpuIndex: Int) {
        val basePath = "/sys/devices/system/cpu/cpu$cpuIndex/"
        fun readScalingMaxFreq(): Long = readLong(basePath, "cpufreq/scaling_max_freq")
        fun scalingCurFreq(): Long = readLong(basePath, "cpufreq/scaling_cur_freq")
    }

    fun readLong(basePath: String, childPath: String): Long = File(basePath, childPath).readLong()


    fun File.readLong(): Long = this.readText().trim().toLong()
}