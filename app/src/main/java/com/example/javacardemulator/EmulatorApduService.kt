package com.example.javacardemulator

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.licel.jcardsim.base.Simulator
import com.licel.jcardsim.utils.AIDUtil
import com.licel.jcardsim.samples.HelloWorldApplet
import javacard.framework.AID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EmulatorApduService : HostApduService() {

    /* when using the jCardSim API,
     * keep in mind that Android does NOT provide the javax.smartcardio
     * which is used in some parts of the package
     * and thus these parts must be avoided
     */

    private lateinit var simulator: Simulator
    private lateinit var serviceWorkerScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        serviceWorkerScope = CoroutineScope(Dispatchers.Default)

        simulator = Simulator()
        simulator.installApplet(appletAID, appletClass)
    }

    private fun logCommand(commandApdu: ByteArray) {
        val (cla, ins, p1, p2) = commandApdu.take(4).map { "%#04x".format(it) }
        val dataLength = commandApdu.size - 4

        val msg = "Command: CLA=$cla, INS=$ins, P1=$p1, P2=$p2, data=$dataLength B"
        Log.d(TAG, msg)
    }

    private fun logResponse(responseApdu: ByteArray) {
        val status = responseApdu
            .takeLast(2)
            .map { it.toUByte() }
            .fold(0u) { acc, byte: UByte -> (acc shl 8) + byte }
            .toInt()
            .let { "%#06x".format(it) }
        val dataLength = responseApdu.size - 2

        Log.d(TAG, "Response: status=$status, data=$dataLength B")
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray? {
        logCommand(commandApdu)

        serviceWorkerScope.launch {
            val responseApdu = simulator.transmitCommand(commandApdu)
            logResponse(responseApdu)
            sendResponseApdu(responseApdu)
        }

        return null
    }

    override fun onDeactivated(reason: Int) {
        val reasonMessage = when (reason) {
            DEACTIVATION_LINK_LOSS -> "NFC link lost"
            DEACTIVATION_DESELECTED -> "applet deselected"
            else -> "Unknown reason"
        }
        Log.i(TAG, "Emulator deactivated: $reasonMessage")
    }

    override fun onDestroy() {
        serviceWorkerScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val TAG = "EmulatorApduService"
        const val APPLET_NAME = "helloworld"
        /* TODO: replace with your applet's AID */
        val appletAID: AID = AIDUtil.create(APPLET_NAME.toByteArray())
        /* TODO: replace with your applet's class */
        val appletClass = HelloWorldApplet::class.java
    }
}
