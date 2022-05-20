package com.android.systemfunction.bean

import com.google.gson.JsonArray
import com.google.gson.JsonObject

data class SettingsBean(
    val global: List<Pair<String, String>>,
    val system: List<Pair<String, String>>,
    val secure: List<Pair<String, String>>,
) {
    fun toJson(): String = JsonObject()
        .addArray("global", global)
        .addArray("system", system)
        .addArray("secure", secure)
        .toString()


    private fun JsonObject.addArray(name: String, list: List<Pair<String, String>>): JsonObject {
        this.add(name, JsonArray().toJsonArray(list))
        return this
    }

    private fun JsonArray.toJsonArray(list: List<Pair<String, String>>): JsonArray {
        list.forEach { this.add(JsonObject().addPair(it)) }
        return this
    }

    private fun JsonObject.addPair(pair: Pair<String, String>): JsonObject {
        this.addProperty(pair.first, pair.second)
        return this
    }
}