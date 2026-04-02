package com.linea.dialer.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linea.dialer.data.model.RealContact
import com.linea.dialer.data.repository.ContactsRepository
import com.linea.dialer.telecom.CallHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SimSlot(
    val subscriptionId: Int,
    val displayName: String,
    val slotIndex: Int,
    val phoneAccountHandle: PhoneAccountHandle?,
)

class DialViewModel(app: Application) : AndroidViewModel(app) {

    private val repo       = ContactsRepository.getInstance(app)
    private val telecom    = app.getSystemService(TelecomManager::class.java)
    private val subManager = app.getSystemService(SubscriptionManager::class.java)

    private val _input          = MutableStateFlow("")
    private val _matchedContact = MutableStateFlow<RealContact?>(null)
    private val _dialError      = MutableStateFlow<String?>(null)
    private val _availableSims  = MutableStateFlow<List<SimSlot>>(emptyList())
    private val _showSimPicker  = MutableStateFlow(false)
    private val _pendingNumber  = MutableStateFlow<String?>(null)

    val input: StateFlow<String>                = _input.asStateFlow()
    val matchedContact: StateFlow<RealContact?> = _matchedContact.asStateFlow()
    val dialError: StateFlow<String?>           = _dialError.asStateFlow()
    val availableSims: StateFlow<List<SimSlot>> = _availableSims.asStateFlow()
    val showSimPicker: StateFlow<Boolean>       = _showSimPicker.asStateFlow()

    init {
        viewModelScope.launch {
            _input.debounce(300).collectLatest { num ->
                _matchedContact.value =
                    if (num.length >= 4) runCatching { repo.findByNumber(num) }.getOrNull()
                    else null
            }
        }
        loadSims()
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    fun onDigit(digit: String) { if (_input.value.length < 15) _input.value += digit }
    fun onBackspace()          { _input.value = _input.value.dropLast(1) }
    fun onClear()              { _input.value = "" }
    fun clearError()           { _dialError.value = null }

    // ── Dialing ───────────────────────────────────────────────────────────────

    /**
     * Attempts to place a call. Returns the dialled number on success, null on failure.
     *
     * If the device has multiple SIMs, returns null and shows the in-app SIM picker.
     * Navigation to ActiveCallScreen should only happen when this returns non-null,
     * OR after onSimSelected() returns non-null.
     */
    fun dial(): String? {
        val number = _input.value.trim().ifEmpty { return null }
        _dialError.value = null

        val sims = _availableSims.value
        return when {
            sims.size > 1 -> {
                // Multiple SIMs — show branded picker; navigation happens in onSimSelected()
                _pendingNumber.value = number
                _showSimPicker.value = true
                null
            }
            else -> {
                // Single-SIM or unknown — let CallHelper resolve the default account
                var result: String? = null
                CallHelper.placeCall(
                    context            = getApplication(),
                    number             = number,
                    phoneAccountHandle = sims.firstOrNull()?.phoneAccountHandle,
                    onError            = { _dialError.value = it }
                ).also { placed ->
                    if (placed) result = number
                }
                result
            }
        }
    }

    /** Called when the user picks a SIM from the in-app picker. Returns the dialled number. */
    fun onSimSelected(sim: SimSlot): String? {
        _showSimPicker.value = false
        val number = _pendingNumber.value ?: return null
        _pendingNumber.value = null

        var result: String? = null
        CallHelper.placeCall(
            context            = getApplication(),
            number             = number,
            phoneAccountHandle = sim.phoneAccountHandle,
            onError            = { _dialError.value = it }
        ).also { placed ->
            if (placed) result = number
        }
        return result
    }

    fun dismissSimPicker() {
        _showSimPicker.value = false
        _pendingNumber.value = null
    }

    fun dialContact(contact: RealContact) {
        _input.value = contact.primaryNumber
        dial()
    }

    // ── SIM management ────────────────────────────────────────────────────────

    private fun loadSims() {
        viewModelScope.launch(Dispatchers.IO) {
            _availableSims.value = runCatching { readSims() }.getOrDefault(emptyList())
        }
    }

    /**
     * Returns SIM slots only when the device genuinely has multiple active SIMs.
     * On single-SIM devices this returns an empty list, and CallHelper will use
     * getDefaultOutgoingPhoneAccount() automatically — no picker needed.
     */
    private fun readSims(): List<SimSlot> {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPhoneState) return emptyList()

        val subscriptions: List<SubscriptionInfo> = runCatching {
            subManager?.activeSubscriptionInfoList ?: emptyList()
        }.getOrDefault(emptyList())

        // Only build a SIM list if there are genuinely multiple SIMs
        if (subscriptions.size <= 1) return emptyList()

        val phoneAccounts = runCatching {
            telecom?.callCapablePhoneAccounts ?: emptyList()
        }.getOrDefault(emptyList())

        return subscriptions.mapIndexed { index, sub ->
            // Positional matching — reliable for the vast majority of dual-SIM devices
            // across all API levels without needing API-34-only subscriptionId field.
            val handle = phoneAccounts.getOrNull(index)

            SimSlot(
                subscriptionId     = sub.subscriptionId,
                displayName        = sub.displayName?.toString()
                    ?: sub.carrierName?.toString()
                    ?: "SIM ${index + 1}",
                slotIndex          = sub.simSlotIndex,
                phoneAccountHandle = handle,
            )
        }
    }

    // ── Formatting ────────────────────────────────────────────────────────────

    fun formatDisplay(raw: String): String {
        val d = raw.filter(Char::isDigit)
        return when {
            d.length <= 3  -> d
            d.length <= 6  -> "${d.take(3)} ${d.drop(3)}"
            d.length <= 10 -> "${d.take(3)} ${d.drop(3).take(3)} ${d.drop(6)}"
            else           -> "+${d.take(3)} ${d.drop(3).take(3)} ${d.drop(6).take(3)} ${d.drop(9)}"
        }
    }
}