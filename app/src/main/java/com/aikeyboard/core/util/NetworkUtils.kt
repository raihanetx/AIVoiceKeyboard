package com.aikeyboard.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "NetworkUtils"

/**
 * Network connectivity state
 */
sealed class NetworkState {
    object Available : NetworkState()
    object Unavailable : NetworkState()
    object Lost : NetworkState()
}

/**
 * Network utility functions
 */
object NetworkUtils {

    /**
     * Check if network is currently available
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Observe network state changes as a Flow
     */
    fun observeNetworkState(context: Context): Flow<NetworkState> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                trySend(NetworkState.Available)
            }

            override fun onUnavailable() {
                Log.d(TAG, "Network unavailable")
                trySend(NetworkState.Unavailable)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
                trySend(NetworkState.Lost)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        if (isNetworkAvailable(context)) {
            trySend(NetworkState.Available)
        } else {
            trySend(NetworkState.Unavailable)
        }

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    /**
     * Get error message for network exception
     */
    fun getNetworkErrorMessage(exception: Exception): String {
        return when (exception) {
            is java.net.SocketTimeoutException -> "Request timed out. Please try again."
            is java.net.UnknownHostException -> "No internet connection. Please check your network."
            else -> "Network error: ${exception.message}"
        }
    }
}
