package com.android.applicationoperation.ui.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.android.applicationoperation.R
import com.android.applicationoperation.ext.sendGet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@Composable
fun MainUI() {
    val context = LocalContext.current
    val activity = context as Activity?
    val scope = rememberCoroutineScope()
    BackMenu {
        Row {
            MyButton(text = "创建文件") {
                try {
                    File("/sdcard/text").createNewFile()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            MyButton(text = "删除文件") {
                try {
                    File("/sdcard/text").delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        Row {
            MyButton(text = "权限") {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                } else {
                    ActivityCompat.requestPermissions(
                        context, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0
                    )
                }
            }
        }
        Row {
            MyButton(text = "UrlConnect") {
                scope.launch(Dispatchers.IO) {
                    try {
                        val response = sendGet("https://www.bing.com", null, null)
                        println("UrlConnect:${response}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            MyButton(text = "OkHttp") {
                scope.launch(Dispatchers.IO) {
                    try {
                        val client = OkHttpClient()
                        val getRequest = Request.Builder().url("https://www.qq.com")
                            .build()
                        val response = client.newCall(getRequest).execute()
                        println("网络请求返回${response.body?.string()}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
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