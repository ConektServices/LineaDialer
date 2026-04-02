package com.linea.dialer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import android.os.Bundle

/**
 * Generic factory for ViewModels that require both [Application] and [SavedStateHandle].
 *
 * Usage inside a composable that has a [NavBackStackEntry] as its owner:
 *
 *   val vm: ContactDetailViewModel = viewModel()
 *
 * When called inside a NavHost composable block, navigation-compose automatically
 * uses the BackStackEntry as the ViewModelStoreOwner + SavedStateRegistryOwner,
 * so [SavedStateHandle] is populated from navArguments for free.
 * No custom factory is needed when using navigation-compose ≥ 2.7.
 *
 * This file is kept for reference and for any screens launched outside NavHost.
 */
class SavedStateViewModelFactory(
    private val app: Application,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null,
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        return when {
            modelClass.isAssignableFrom(ContactDetailViewModel::class.java) ->
                ContactDetailViewModel(app, handle) as T
            modelClass.isAssignableFrom(ContactsViewModel::class.java) ->
                ContactsViewModel(app) as T
            modelClass.isAssignableFrom(RecentsViewModel::class.java) ->
                RecentsViewModel(app) as T
            modelClass.isAssignableFrom(DialViewModel::class.java) ->
                DialViewModel(app) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
