package com.linea.dialer.telecom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

private const val TAG = "CallHelper"

/**
 * Single source of truth for placing outgoing calls.
 *
 * Strategy:
 *   1. Always attempt ACTION_CALL intent first — it works with just CALL_PHONE
 *      permission regardless of whether Linea is the default dialer.
 *      When Linea IS the default dialer, Android routes ACTION_CALL back through
 *      Linea's own InCallService, so the call stays inside the app.
 *
 *   2. If Linea is the default dialer, ALSO call TelecomManager.placeCall() which
 *      gives us finer control (specific SIM selection etc). This is belt-and-suspenders.
 *
 * This eliminates the silent-failure problem of relying solely on placeCall().
 */
object CallHelper {

    fun placeCall(
        context: Context,
        number: String,
        phoneAccountHandle: PhoneAccountHandle? = null,
        onError: (String) -> Unit = {},
    ): Boolean {
        if (number.isBlank()) {
            onError("No number provided")
            return false
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCallPermission) {
            onError("Call permission not granted. Please grant it in Settings.")
            return false
        }

        val telecom    = context.getSystemService(TelecomManager::class.java)
        val isDefault  = telecom?.defaultDialerPackage == context.packageName
        val cleanNumber = number.trim()

        Log.d(TAG, "placeCall: number=$cleanNumber isDefault=$isDefault")

        return if (isDefault) {
            // Linea is default dialer — use TelecomManager.placeCall() for full control.
            // This keeps the call inside our InCallService with the correct SIM handle.
            placeViaTelemcom(context, telecom, cleanNumber, phoneAccountHandle, onError)
        } else {
            // Not default dialer — use ACTION_CALL which ALWAYS works.
            // This makes the real phone call happen. The system dialer UI may briefly
            // appear, but the call will still be real. Users should set Linea as default.
            placeViaIntent(context, cleanNumber, onError)
        }
    }

    private fun placeViaTelemcom(
        context: Context,
        telecom: TelecomManager,
        number: String,
        handle: PhoneAccountHandle?,
        onError: (String) -> Unit,
    ): Boolean {
        return try {
            val uri = Uri.fromParts("tel", number, null)

            // Resolve default outgoing account if no specific handle given
            val resolvedHandle = handle
                ?: runCatching { telecom.getDefaultOutgoingPhoneAccount("tel") }.getOrNull()

            val extras = Bundle().apply {
                resolvedHandle?.let {
                    putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
                }
            }

            Log.d(TAG, "TelecomManager.placeCall() handle=$resolvedHandle")
            telecom.placeCall(uri, extras)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "placeCall SecurityException — falling back to intent", e)
            // Fall back to intent — this will definitely work
            placeViaIntent(context, number, onError)
        } catch (e: Exception) {
            Log.w(TAG, "placeCall failed — falling back to intent: ${e.message}", e)
            placeViaIntent(context, number, onError)
        }
    }

    private fun placeViaIntent(
        context: Context,
        number: String,
        onError: (String) -> Unit,
    ): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            Log.d(TAG, "ACTION_CALL intent for $number")
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "ACTION_CALL failed: ${e.message}", e)
            onError("Could not place call: ${e.message}")
            false
        }
    }

    fun isDefaultDialer(context: Context): Boolean {
        val tm = context.getSystemService(TelecomManager::class.java) ?: return false
        return tm.defaultDialerPackage == context.packageName
    }
}