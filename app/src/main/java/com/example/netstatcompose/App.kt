package com.example.netstatcompose

import android.app.Application

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        NetStat.initialize(this)
    }
}