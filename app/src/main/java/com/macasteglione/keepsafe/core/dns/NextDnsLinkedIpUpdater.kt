package com.macasteglione.keepsafe.core.dns

import android.util.Log
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

/**
 * Actualiza el Linked IP en NextDNS cuando cambia la red.
 * Así NextDNS reconoce el dispositivo sin importar la IP pública.
 */
object NextDnsLinkedIpUpdater {

    private val tag = "NextDnsLinkedIpUpdater"

    // Reemplaza con tu ID y token de NextDNS
    private const val NEXTDNS_ID = "4162c1"
    private const val NEXTDNS_TOKEN = "08f8d1ab7dcec767"

    /**
     * Actualiza el Linked IP en NextDNS.
     * Llama a esta función cada vez que detectes cambio de red.
     */
    fun updateLinkedIp() {
        thread {
            try {
                val url = URL("https://link-ip.nextdns.io/$NEXTDNS_ID/$NEXTDNS_TOKEN")

                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode

                if (responseCode == 200) {
                    Log.d(tag, "Linked IP actualizado en NextDNS")
                } else {
                    Log.w(tag, "NextDNS respondió con código: $responseCode")
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e(tag, "Error actualizando Linked IP: ${e.message}")
            }
        }
    }
}