package com.android.systemlib

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException
import androidx.core.net.toUri

/**
 * 只读提供一个文件（files/tzdistro/distro.zip），供 Android 8.1 的 TzDataInstallReceiver
 * （运行在 system_server）通过 content:// URI 读取。不导出，由 [TzDataInstaller] 按 URI 授权给 "android"。
 */
class TzDistroProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (uri.lastPathSegment != FILE || mode != "r") throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(file(context!!), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply {
        addRow(arrayOf<Any>(FILE, file(context!!).length()))
    }

    override fun getType(uri: Uri): String = "application/zip"

    override fun insert(uri: Uri, values: ContentValues?): Uri = throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException()

    companion object {
        const val DIR = "tzdistro"
        const val FILE = "distro.zip"

        /** 与 AndroidManifest.xml 中的 authorities 保持一致 */
        fun uri(context: Context): Uri =
            "content://${context.packageName}.systemlib.tzdistro/$FILE".toUri()

        fun file(context: Context) = File(File(context.filesDir, DIR), FILE)
    }
}
