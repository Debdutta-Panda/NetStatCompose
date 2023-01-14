package com.example.netstatcompose

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.IOException

object NetStat{
    data class Stat(
        var available: Boolean = false,
        var metered: Boolean = false,
        var internet: Boolean = false,
        var valid: Boolean = false,
        var lastOnlineCheck: Long = 0,
        var lastKnowOnline: Boolean = false
    ){
        override fun equals(other: Any?): Boolean {
            if(other !is Stat){
                return false
            }
            return available==other.available
                    && metered==other.metered
                    && internet==other.internet
                    && valid==other.valid
        }

        override fun hashCode(): Int {
            var result = available.hashCode()
            result = 31 * result + metered.hashCode()
            result = 31 * result + internet.hashCode()
            result = 31 * result + valid.hashCode()
            result = 31 * result + lastOnlineCheck.hashCode()
            result = 31 * result + lastKnowOnline.hashCode()
            return result
        }
    }

    var stat = Stat()
    private set

    private lateinit var connectivityManager: ConnectivityManager

    fun initialize(context: Context){
        connectivityManager = if(android.os.Build.VERSION.SDK_INT < 23){
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        } else{
            context.getSystemService(ConnectivityManager::class.java)
        }
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            stat.available = true
            callback(stat)
        }
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            val internet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val valid = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            stat.metered = !unmetered
            stat.internet = internet
            stat.valid = valid
            callback(stat)
        }
        override fun onLost(network: Network) {
            super.onLost(network)
            stat.available = false
            callback(stat)
        }
    }

    fun isNetworkOnline(): Boolean {
        var isOnline = false
        try {
            val runtime = Runtime.getRuntime()
            val p = runtime.exec("ping -c 1 8.8.8.8")
            val waitFor = p.waitFor()
            isOnline =
                waitFor == 0
            stat.lastOnlineCheck = System.currentTimeMillis()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        stat.lastKnowOnline = isOnline
        callback(stat)
        return isOnline
    }

    private val callbacks = mutableListOf<(stat: Stat)->Unit>()
    private fun callback(stat: Stat){
        CoroutineScope(
            Dispatchers.IO
        ).launch {
            publish(stat)
        }
        callbacks.forEach {
            it(stat)
        }
    }
    fun add(callback: (stat: Stat)->Unit){
        callbacks.add(callback)
    }
    fun remove(callback: (stat: Stat)->Unit){
        callbacks.remove(callback)
    }
    fun clear(){
        callbacks.clear()
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun waitFor(targetStat: Stat): Stat =
        suspendCancellableCoroutine {
            if(stat == targetStat){
                it.resume(stat){}
            }
            else{
                var block: ((stat: Stat)->Unit)? = null
                block = {stat: Stat->
                    callbacks.remove(block)
                    it.resume(stat){}
                }
                callbacks.add(block)
            }
        }

    private val _events = MutableSharedFlow<Stat>()
    val events = _events.asSharedFlow()

    private suspend fun publish(stat: Stat) = _events.emit(stat)
}