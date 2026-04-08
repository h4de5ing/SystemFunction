package com.android.systemlib

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.RecoverySystem
import android.os.SystemProperties
import android.os.UpdateEngine
import android.os.UpdateEngineCallback
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Enumeration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.math.floor
import kotlin.math.min

class OtaInstaller(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val isRunning = AtomicBoolean(false)
    private val targetPackageFile = File("/data/ota_package/selected_ota.zip")

    data class Callbacks(
        val onStatus: (String) -> Unit = {},
        val onError: (String) -> Unit = {},
        val onSuccess: (String) -> Unit = {},
    )

    fun install(sourceFile: File, callbacks: Callbacks) {
        if (!isRunning.compareAndSet(false, true)) {
            callbacks.onError("OTA is already running")
            return
        }

        executor.execute {
            try {
                val displayName = sourceFile.name
                callbacks.onStatus("SELECTED $displayName")
                copyPackage(sourceFile, callbacks)

                if (isAbDevice()) {
                    installAbPackage(callbacks)
                } else {
                    installRecoveryPackage(callbacks)
                }
            } catch (e: Exception) {
                callbacks.onError(e.message ?: e.toString())
            } finally {
                isRunning.set(false)
            }
        }
    }

    private fun copyPackage(sourceFile: File, callbacks: Callbacks) {
        val sourceSize = sourceFile.length()
        targetPackageFile.parentFile?.mkdirs()
        if (targetPackageFile.exists() && !targetPackageFile.delete()) {
            throw IOException("Failed to delete old package: ${targetPackageFile.absolutePath}")
        }

        var copiedBytes = 0L
        var lastPercent = -1
        BufferedInputStream(sourceFile.inputStream()).use { bufferedInput ->
            FileOutputStream(targetPackageFile).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = bufferedInput.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copiedBytes += read

                    if (sourceSize > 0) {
                        val percent = ((copiedBytes * 100) / sourceSize).toInt().coerceIn(0, 100)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            callbacks.onStatus("COPYING $percent%")
                        }
                    }
                }
                output.fd.sync()
            }
        }

        if (sourceSize > 0 && copiedBytes != sourceSize) {
            throw IOException("OTA Copy File Error: copied $copiedBytes/$sourceSize bytes")
        }
        if (!targetPackageFile.setReadable(true, false)) {
            runCatching {
                Runtime.getRuntime().exec(arrayOf("chmod", "644", targetPackageFile.absolutePath))
                    .waitFor()
            }
        }
    }

    private fun installRecoveryPackage(callbacks: Callbacks) {
        callbacks.onStatus("INSTALLING")
        RecoverySystem.installPackage(context, targetPackageFile)
        callbacks.onSuccess("INSTALL_PACKAGE_RETURNED")
    }

    @SuppressLint("MissingPermission")
    private fun installAbPackage(callbacks: Callbacks) {
        val payloadSpec = parsePayloadSpec(targetPackageFile)
        val updateEngine = UpdateEngine()
        val doneSignal = CountDownLatch(1)
        var terminalError: String? = null
        var terminalStatus: String? = null

        try {
            runCatching { updateEngine.cancel() }

            val bound = updateEngine.bind(object : UpdateEngineCallback() {
                override fun onStatusUpdate(status: Int, percent: Float) {
                    val progress = floor(min(1.0, percent.toDouble()) * 100).toInt()
                    val statusMessage = resolveConstantName(
                        UpdateEngine.UpdateStatusConstants(),
                        status,
                    ) ?: "STATUS_$status"
                    val rawStatus = "$statusMessage [$status] $progress%"
                    terminalStatus = rawStatus
                    val isComplete =
                        status == UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT ||
                                ((Build.VERSION.SDK_INT == 34) && (percent >= 1 && status == UpdateEngine.UpdateStatusConstants.FINALIZING))
                    if (isComplete) {
                        try {
                            updateEngine.unbind()
                        } catch (_: Exception) {
                        }
                        Thread.sleep(3000)
                        reboot()
                    }
                    callbacks.onStatus(rawStatus)
                }

                override fun onPayloadApplicationComplete(errorCode: Int) {
                    val errorMessage = resolveConstantName(
                        UpdateEngine.ErrorCodeConstants(),
                        errorCode,
                    )?.let { "$it [$errorCode]" } ?: "ERROR_$errorCode"

                    if (errorCode == UpdateEngine.ErrorCodeConstants.SUCCESS) {
                        terminalStatus = terminalStatus ?: errorMessage
                    } else {
                        terminalError = errorMessage
                    }
                    doneSignal.countDown()
                }
            })
            if (!bound) {
                throw IOException("UpdateEngine bind failed")
            }

            callbacks.onStatus("APPLYING_PAYLOAD")
            updateEngine.applyPayload(
                payloadSpec.url,
                payloadSpec.offset,
                payloadSpec.size,
                payloadSpec.properties.toTypedArray(),
            )

            doneSignal.await()
            terminalError?.let { throw IOException(it) }
            callbacks.onSuccess(terminalStatus ?: "SUCCESS [0]")
        } finally {
            runCatching { updateEngine.unbind() }
        }
    }

    private fun isAbDevice(): Boolean = !SystemProperties.get("ro.boot.slot_suffix").isNullOrEmpty()

    private fun resolveConstantName(constants: Any, value: Int): String? {
        return constants.javaClass.declaredFields.firstNotNullOfOrNull { field ->
            field.isAccessible = true
            val constantValue = field[constants] as? Int ?: return@firstNotNullOfOrNull null
            if (constantValue == value) field.name else null
        }
    }

    private fun parsePayloadSpec(packageFile: File): PayloadSpec {
        ZipFile(packageFile).use { zipFile ->
            val entries: Enumeration<out ZipEntry> = zipFile.entries()
            var payloadOffset = 0L
            var payloadSize = 0L
            var payloadFound = false
            val properties = mutableListOf<String>()
            var offset = 0L

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                val extraSize = entry.extra?.size?.toLong() ?: 0L
                offset += 30 + name.toByteArray(Charsets.UTF_8).size + extraSize

                if (entry.isDirectory) {
                    continue
                }

                val length = entry.compressedSize
                when (name) {
                    "payload.bin" -> {
                        if (entry.method != ZipEntry.STORED) {
                            throw IOException("Invalid compression method.")
                        }
                        payloadFound = true
                        payloadOffset = offset
                        payloadSize = length
                    }

                    "payload_properties.txt" -> {
                        zipFile.getInputStream(entry)?.bufferedReader()?.useLines { lines ->
                            properties.addAll(lines.toList())
                        }
                    }
                }
                offset += length
            }

            if (!payloadFound) {
                throw IOException("Failed to find payload entry in the given package.")
            }

            return PayloadSpec(
                url = "file://${packageFile.absolutePath}",
                offset = payloadOffset,
                size = payloadSize,
                properties = properties,
            )
        }
    }

    private data class PayloadSpec(
        val url: String,
        val offset: Long,
        val size: Long,
        val properties: List<String>,
    )
}
