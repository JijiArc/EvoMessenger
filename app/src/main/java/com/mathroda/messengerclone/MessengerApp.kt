package com.mathroda.messengerclone

import android.app.Application
import android.content.pm.ApplicationInfo
import android.webkit.WebView

class MessengerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Remote WebView debugging (chrome://inspect) only in the debuggable build.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
