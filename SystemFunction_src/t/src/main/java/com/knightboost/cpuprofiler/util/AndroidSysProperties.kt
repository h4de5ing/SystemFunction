package com.knightboost.cpuprofiler.util

import java.lang.reflect.Method

object AndroidSysProperties {

    private var getSystemPropertyMethod: Method? = null
    private var getLongMethod: Method? = null
    private var getIntMethod: Method? = null
    private var setMethod: Method? = null

    init {
        try {
            val methods = Class.forName("android.os.SystemProperties").methods
            for (method in methods) {
                val name = method.name
                when (name) {
                    "get" -> getSystemPropertyMethod = method
                    "set" -> setMethod = method
                    "getLong" -> getLongMethod = method
                    "getInt" -> getIntMethod = method
                }
            }
        } catch (_: Exception) {
        }
    }

    fun getSystemProperty(property: String, defaultValue: String?): String? {
        val method = getSystemPropertyMethod ?: return defaultValue
        return try {
            method.invoke(null, arrayOf(property, defaultValue)) as String?
        } catch (e: Exception) {
            defaultValue
        }
    }
}