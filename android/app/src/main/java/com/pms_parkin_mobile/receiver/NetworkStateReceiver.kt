package com.pms_parkin_mobile.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import timber.log.Timber

class NetworkStateReceiver : BroadcastReceiver() {
    @SuppressLint("LongLogTag")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getAction()
        if (action == null) return

        if (action == ConnectivityManager.CONNECTIVITY_ACTION) {
            try {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                @SuppressLint("MissingPermission") val activeNetwork =
                    connectivityManager.getActiveNetworkInfo()

                val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting()
            } catch (e: Exception) {
                Timber.e("onReceive Error: %s", e.message)
            }
        }
    }
}
