package com.swappy.picora

import android.content.Context
import android.content.SharedPreferences

object PortNameStorage {
    private const val PREFS_NAME = "port_name_prefs"
    private const val KEY_PREFIX = "port_name_"

    fun savePortName(context: Context, date: String, portName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PREFIX + date, portName).apply()
    }

    fun getPortName(context: Context, date: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PREFIX + date, "") ?: ""
    }

    fun clearPortName(context: Context, date: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PREFIX + date).apply()
    }
}

