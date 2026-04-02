package com.linea.dialer.telecom

import android.telecom.Call
import com.linea.dialer.data.model.ActiveCallState
import com.linea.dialer.data.model.RealContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton bridge between LineaInCallService (which holds the real Call object)
 * and the Compose UI (which observes callState).
 *
 * Only LineaInCallService writes here. Screens only read.
 */
object CallManager {

    private val _callState = MutableStateFlow<ActiveCallState>(ActiveCallState.Idle)
    val callState: StateFlow<ActiveCallState> = _callState.asStateFlow()

    @Volatile private var activeCall: Call?    = null
    @Volatile private var activeContact: RealContact? = null

    // ── Called by LineaInCallService ──────────────────────────────────────────

    fun onCallAdded(call: Call, contact: RealContact?) {
        activeCall    = call
        activeContact = contact

        val number = call.details?.handle?.schemeSpecificPart ?: ""

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                updateState(c, state, contact, number)
            }
        })

        // Post initial state
        updateState(call, call.state, contact, number)
    }

    fun onCallRemoved(call: Call) {
        if (activeCall == call) {
            val duration = run {
                val start = call.details?.connectTimeMillis ?: return@run 0L
                if (start == 0L) 0L else (System.currentTimeMillis() - start) / 1000
            }
            _callState.value = ActiveCallState.Ended(activeContact, duration)
            activeCall    = null
            activeContact = null
        }
    }

    // ── Called by UI ──────────────────────────────────────────────────────────

    /**
     * Answer an incoming ringing call.
     * Requires Linea to be the default dialer.
     */
    fun answer() {
        activeCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
    }

    fun mute(muted: Boolean) {
        // AudioManager muting is handled at the system level via TelecomManager.
        // For a full implementation, inject AudioManager and call setMicrophoneMute().
    }

    fun hold() {
        activeCall?.hold()
        val cur = _callState.value
        if (cur is ActiveCallState.Connected) {
            _callState.value = ActiveCallState.OnHold(cur.contact, cur.number)
        }
    }

    fun unhold() {
        activeCall?.unhold()
        val cur = _callState.value
        if (cur is ActiveCallState.OnHold) {
            _callState.value = ActiveCallState.Connected(cur.contact, cur.number, System.currentTimeMillis())
        }
    }

    fun endCall() {
        activeCall?.disconnect()
    }

    fun sendDtmf(digit: Char) {
        activeCall?.playDtmfTone(digit)
    }

    fun stopDtmf() {
        activeCall?.stopDtmfTone()
    }

    /** Reset to Idle after the ended state has been consumed by the UI */
    fun reset() {
        _callState.value = ActiveCallState.Idle
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun updateState(call: Call, state: Int, contact: RealContact?, number: String) {
        _callState.value = when (state) {
            Call.STATE_DIALING,
            Call.STATE_RINGING,
            Call.STATE_NEW          -> ActiveCallState.Calling(contact, number)

            Call.STATE_ACTIVE       -> ActiveCallState.Connected(
                contact, number,
                call.details?.connectTimeMillis ?: System.currentTimeMillis()
            )

            Call.STATE_HOLDING      -> ActiveCallState.OnHold(contact, number)

            Call.STATE_DISCONNECTED,
            Call.STATE_DISCONNECTING -> {
                val duration = run {
                    val start = call.details?.connectTimeMillis ?: return@run 0L
                    if (start == 0L) 0L else (System.currentTimeMillis() - start) / 1000
                }
                ActiveCallState.Ended(contact, duration)
            }

            else -> _callState.value
        }
    }
}