package com.swappy.picora

import android.app.Application

class PicoraApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}