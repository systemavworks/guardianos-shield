package com.guardianos.shield.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.guardianos.shield.ui.SafeBrowserActivity

class SafeBrowsingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Lanza el navegador seguro al iniciar el servicio
        val browserIntent = Intent(this, SafeBrowserActivity::class.java)
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(browserIntent)
        // El servicio puede quedarse en background si se desea
        return START_STICKY
    }
}
