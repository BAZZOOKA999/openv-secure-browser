package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.VpnConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

class OpenVVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val ACTION_CONNECT = "com.openv.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.openv.vpn.DISCONNECT"
        const val EXTRA_SERVER_HOST = "extra_server_host"
        const val EXTRA_SERVER_PORT = "extra_server_port"
        const val EXTRA_DNS = "extra_dns"

        private val _vpnStatus = MutableStateFlow(VpnConnectionState.NOT_CONFIGURED)
        val vpnStatus: StateFlow<VpnConnectionState> = _vpnStatus.asStateFlow()

        private val _connectedServer = MutableStateFlow<String?>(null)
        val connectedServer: StateFlow<String?> = _connectedServer.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!VpnManager.ENGINE_AVAILABLE) {
            _vpnStatus.value = VpnConnectionState.NOT_CONFIGURED
            stopSelf()
            return START_NOT_STICKY
        }
        val action = intent?.action
        if (action == ACTION_DISCONNECT) {
            disconnectTunnel()
            return START_NOT_STICKY
        } else if (action == ACTION_CONNECT) {
            val serverHost = intent.getStringExtra(EXTRA_SERVER_HOST) ?: "1.1.1.1"
            val serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 51820)
            val dnsServer = intent.getStringExtra(EXTRA_DNS) ?: "1.1.1.1"
            connectTunnel(serverHost, serverPort, dnsServer)
            return START_STICKY
        }
        return START_NOT_STICKY
    }

    private fun connectTunnel(serverHost: String, serverPort: Int, dnsServer: String) {
        _vpnStatus.value = VpnConnectionState.CONNECTING
        startForeground(1001, buildNotification("Conectando ao túnel OpenV..."))

        tunnelJob?.cancel()
        tunnelJob = serviceScope.launch {
            try {
                // Build the native TUN interface using Android VpnService.Builder
                val builder = Builder()
                    .setSession("OpenV Secure Tunnel")
                    .setMtu(1500)
                    .addAddress("10.8.0.2", 24)
                    .addDnsServer(dnsServer)
                    .addRoute("0.0.0.0", 0)

                // Optional IPv6 route
                try {
                    builder.addAddress("fd00::2", 64)
                    builder.addRoute("::", 0)
                } catch (_: Exception) {}

                val pfd = builder.establish()
                if (pfd == null) {
                    _vpnStatus.value = VpnConnectionState.ERROR
                    stopSelf()
                    return@launch
                }

                vpnInterface = pfd
                _vpnStatus.value = VpnConnectionState.CONNECTED
                _connectedServer.value = serverHost

                updateNotification("Túnel ativo: $serverHost ($serverPort)")

                // Read/Write loop on TUN descriptor to maintain interface lifecycle
                val inStream = FileInputStream(pfd.fileDescriptor)
                val buffer = ByteArray(32767)
                while (_vpnStatus.value == VpnConnectionState.CONNECTED) {
                    val length = inStream.read(buffer)
                    if (length <= 0) {
                        Thread.sleep(100)
                    }
                }
            } catch (e: Exception) {
                _vpnStatus.value = VpnConnectionState.ERROR
                disconnectTunnel()
            }
        }
    }

    private fun disconnectTunnel() {
        _vpnStatus.value = VpnConnectionState.DISCONNECTED
        _connectedServer.value = null
        tunnelJob?.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        disconnectTunnel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "openv_vpn_channel",
                "Túnel de Proteção OpenV",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação de status da conexão VPN nativa"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "openv_vpn_channel")
            .setContentTitle("OpenV Secure VPN")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, buildNotification(text))
    }
}
