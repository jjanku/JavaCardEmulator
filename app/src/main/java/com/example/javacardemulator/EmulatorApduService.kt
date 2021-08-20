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

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray? {
        val header = commandApdu.take(4).toTypedArray()
        Log.d(TAG, "Processing new APDU(CLA=%x, INS=%x, P1=%x, P2=%x".format(*header))

        serviceWorkerScope.launch {
            val responseApdu = simulator.transmitCommand(commandApdu)
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
