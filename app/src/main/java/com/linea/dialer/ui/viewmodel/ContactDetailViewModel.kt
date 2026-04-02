package com.linea.dialer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.linea.dialer.data.db.NoteEntity
import com.linea.dialer.data.db.ReminderEntity
import com.linea.dialer.data.model.ContactTag
import com.linea.dialer.data.model.RealCallLog
import com.linea.dialer.data.model.RealContact
import com.linea.dialer.data.repository.CallLogRepository
import com.linea.dialer.data.repository.ContactsRepository
import com.linea.dialer.data.repository.NotesRepository
import com.linea.dialer.work.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ContactDetailState(
    val contact: RealContact? = null,
    val notes: List<NoteEntity> = emptyList(),
    val callHistory: List<RealCallLog> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val isLoading: Boolean = true,
)

class ContactDetailViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val contactId = savedStateHandle.get<Long>("contactId") ?: -1L

    private val contactsRepo = ContactsRepository.getInstance(app)
    private val notesRepo    = NotesRepository.getInstance(app)
    private val callLogRepo  = CallLogRepository.getInstance(app)

    private val _contact = MutableStateFlow<RealContact?>(null)
    private val _history = MutableStateFlow<List<RealCallLog>>(emptyList())
    private val _loading = MutableStateFlow(true)

    val notes: StateFlow<List<NoteEntity>> = _contact
        .filterNotNull()
        .flatMapLatest { notesRepo.observeNotesForContact(it.lookupKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = _contact
        .filterNotNull()
        .flatMapLatest { notesRepo.observeRemindersForContact(it.lookupKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<ContactDetailState> = combine(
        _contact, notes, _history, reminders, _loading
    ) { contact, notes, history, reminders, loading ->
        ContactDetailState(
            contact     = contact,
            notes       = notes,
            callHistory = history,
            reminders   = reminders,
            isLoading   = loading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactDetailState())

    init {
        loadContact()
    }

    private fun loadContact() {
        viewModelScope.launch {
            _loading.value = true
            val c = runCatching { contactsRepo.loadById(contactId) }.getOrNull()
            _contact.value = c

            if (c != null) {
                // Load call history on IO
                _history.value = runCatching {
                    callLogRepo.loadForContact(c.numbers.map { it.number })
                }.getOrDefault(emptyList())
            }

            _loading.value = false
        }
    }

    fun addNote(body: String) {
        val contact = _contact.value ?: return
        viewModelScope.launch {
            notesRepo.addNote(lookupKey = contact.lookupKey, body = body)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { notesRepo.deleteNote(noteId) }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch { notesRepo.updateNote(note) }
    }

    fun saveTag(tag: ContactTag) {
        val contact = _contact.value ?: return
        viewModelScope.launch {
            notesRepo.saveTag(contact.lookupKey, tag)
            _contact.value = contact.copy(tag = tag)
        }
    }

    fun scheduleReminder(message: String, atMs: Long) {
        val contact = _contact.value ?: return
        viewModelScope.launch {
            ReminderScheduler.schedule(
                context      = getApplication(),
                contact      = contact,
                message      = message,
                scheduledAtMs = atMs,
            )
        }
    }

    fun cancelReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            ReminderScheduler.cancelEntity(getApplication(), reminder)
        }
    }
}
