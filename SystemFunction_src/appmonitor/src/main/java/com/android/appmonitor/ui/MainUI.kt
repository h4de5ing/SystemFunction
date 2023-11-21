package com.android.appmonitor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.appmonitor.R
import com.android.appmonitor.getFileUageRecordList
import com.android.appmonitor.getNetworkRecordList
import com.android.appmonitor.getTimesMonthMorning
import com.android.appmonitor.getTrafficByPackageName
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Composable
fun MainUI() {
    val context = LocalContext.current
    var log by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    BackMenu {
        Row {
            MyButton("创建文件") {
                try {
                    File("/sdcard/text1").createNewFile()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            MyButton("删除文件") {
                try {
                    File("/sdcard/text1").delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            MyButton("读文件文件") {
                try {
                    val fis =
                        FileInputStream(File("/sdcard/text1"))
                    fis.bufferedReader()
                        .lines().forEach {
                            println("读文件内容:${it}")
                        }
                    fis.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            MyButton("写文件文件") {
                try {
                    val fos = FileOutputStream(File("/sdcard/text1"))
                    fos.write("text write\n".toByteArray())
                    fos.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            MyButton("获取文件操作记录") {
                log = getFileUageRecordList(
                    listOf("/storage/", "/sdcard/"),
                    getTimesMonthMorning()
                ).toString()
                scope.launch { scrollState.scrollTo(scrollState.maxValue) }
            }
        }
        Row {
            MyButton("获取URL访问列表") {
                log = getNetworkRecordList("com.android.browser", getTimesMonthMorning()).toString()
                scope.launch { scrollState.scrollTo(scrollState.maxValue) }
            }
            MyButton("获取流量统计") {
                log = getTrafficByPackageName(context, "com.scanner.hardware", 0).toString()
                scope.launch { scrollState.scrollTo(scrollState.maxValue) }
            }
        }
        Text(
            text = log, modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        )
    }
}

@Composable
fun MyButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) { Text(text = text) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackMenu(
    backClick: () -> Unit = {},
    title: String = stringResource(id = R.string.app_name),
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        TopAppBar(title = { Text(text = title) }, navigationIcon = {
            IconButton(onClick = backClick, enabled = false) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Navigation icon",
                )
            }
        })
        content()
    }
}