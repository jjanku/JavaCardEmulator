package com.example.javacardemulator

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class EmulatorApduService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray? {
        TODO()
    }

    override fun onDeactivated(reason: Int) {
        TODO()
    }
}
