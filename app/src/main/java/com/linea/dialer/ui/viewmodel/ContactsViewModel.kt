package com.linea.dialer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linea.dialer.data.model.ContactTag
import com.linea.dialer.data.model.RealContact
import com.linea.dialer.data.repository.ContactsRepository
import com.linea.dialer.data.repository.NotesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ContactsUiState(
    val contacts: List<RealContact> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class ContactsViewModel(app: Application) : AndroidViewModel(app) {

    private val contactsRepo = ContactsRepository.getInstance(app)
    private val notesRepo    = NotesRepository.getInstance(app)

    private val _search      = MutableStateFlow("")
    private val _tagFilter   = MutableStateFlow<ContactTag?>(null)
    private val _allContacts = MutableStateFlow<List<RealContact>>(emptyList())
    private val _isLoading   = MutableStateFlow(true)
    private val _error       = MutableStateFlow<String?>(null)

    val search:    StateFlow<String>        = _search.asStateFlow()
    val tagFilter: StateFlow<ContactTag?>   = _tagFilter.asStateFlow()

    val uiState: StateFlow<ContactsUiState> = combine(
        _allContacts, _search, _tagFilter, _isLoading, _error
    ) { contacts, query, tag, loading, error ->
        val filtered = contacts
            .filter { c -> tag == null || c.tag == tag }
            .filter { c ->
                query.isBlank() ||
                c.name.contains(query, ignoreCase = true) ||
                c.numbers.any { it.number.contains(query) }
            }
        ContactsUiState(filtered, loading, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactsUiState())

    init { loadContacts() }

    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            runCatching {
                val rawContacts = contactsRepo.loadAll()
                // Enrich with Room tags (overrides the SharedPreferences tags)
                val roomTags = notesRepo.getAllTags()
                rawContacts.map { c ->
                    roomTags[c.lookupKey]?.let { c.copy(tag = it) } ?: c
                }
            }
            .onSuccess  { _allContacts.value = it; _isLoading.value = false }
            .onFailure  { _error.value = it.message; _isLoading.value = false }
        }
    }

    fun onSearch(query: String)       { _search.value    = query }
    fun onTagFilter(tag: ContactTag?) { _tagFilter.value = tag }

    fun saveTag(contact: RealContact, tag: ContactTag) {
        viewModelScope.launch {
            notesRepo.saveTag(contact.lookupKey, tag)
            _allContacts.value = _allContacts.value.map {
                if (it.id == contact.id) it.copy(tag = tag) else it
            }
        }
    }

    fun getContact(id: Long): RealContact? = _allContacts.value.firstOrNull { it.id == id }
}
