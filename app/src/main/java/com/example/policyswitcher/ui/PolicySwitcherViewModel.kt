package com.example.policyswitcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.policyswitcher.assistant.AssistantCommandHandler
import com.example.policyswitcher.assistant.AssistantExecution
import com.example.policyswitcher.data.CredentialStore
import com.example.policyswitcher.data.KeeneticRepository
import com.example.policyswitcher.model.Client
import com.example.policyswitcher.model.ConnectionStatus
import com.example.policyswitcher.model.CredentialField
import com.example.policyswitcher.model.Credentials
import com.example.policyswitcher.model.CustomCommand
import com.example.policyswitcher.model.DragState
import com.example.policyswitcher.model.Policy
import com.example.policyswitcher.model.UiState
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.regex.Pattern
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

sealed interface UiEvent {
    data class Toast(val message: String, val isError: Boolean = false) : UiEvent
    data class Haptic(val type: HapticType) : UiEvent
}

enum class HapticType { APPLY, REMOVE }

class PolicySwitcherViewModel(
    private val repository: KeeneticRepository,
    private val credentialStore: CredentialStore,
    private val assistantHandler: AssistantCommandHandler,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    private var lastFetchedAt: Instant? = null

    init {
        viewModelScope.launch {
            val saved = credentialStore.load()
            val lastUrl = credentialStore.loadLastSuccessfulUrl()
            if (saved != null) {
                _uiState.update {
                    it.copy(
                        credentials = saved,
                        credsPanelExpanded = false,
                        connectionStatus = ConnectionStatus.Ready(lastChecked = Instant.now(clock)),
                        lastSuccessfulUrl = lastUrl
                    )
                }
                refresh(force = true)
            }
        }
    }

    fun toggleCredentialsPanel() {
        _uiState.update { state -> state.copy(credsPanelExpanded = !state.credsPanelExpanded) }
    }

    fun updateDomain(value: String) {
        _uiState.update { it.copy(credentials = it.credentials.copy(domainOrIp = value)) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(credentials = it.credentials.copy(username = value)) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(credentials = it.credentials.copy(password = value)) }
    }

    fun updateDefaultPolicy(policyId: String) {
        _uiState.update { it.copy(credentials = it.credentials.copy(defaultPolicyId = policyId)) }
    }

    fun verifyConnection() {
        val state = _uiState.value
        val errors = validateCredentials(state.credentials, state.policies)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(credentialErrors = errors, connectionStatus = ConnectionStatus.Error("Проверьте введённые данные")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.Validating, credentialErrors = emptyMap()) }
            val result = repository.verifyConnection(_uiState.value.credentials)
            result.onSuccess { normalizedUrl ->
                val now = Instant.now(clock)
                credentialStore.save(_uiState.value.credentials)
                credentialStore.saveLastSuccessfulUrl(normalizedUrl)
                _uiState.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.Ready(now),
                        lastSuccessfulUrl = normalizedUrl,
                        credsPanelExpanded = false,
                    )
                }
                refresh(force = true)
            }.onFailure { error ->
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Error(error.message ?: "Не удалось подключиться")) }
            }
        }
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            if (!force && !isReady()) return@launch
            val now = Instant.now(clock)
            val lastFetch = lastFetchedAt
            if (!force && lastFetch != null && Duration.between(lastFetch, now) < Duration.ofSeconds(30)) {
                return@launch
            }
            _uiState.update { it.copy(isRefreshing = true) }
            val policies = repository.fetchPolicies()
            val clients = repository.fetchClients()
            lastFetchedAt = Instant.now(clock)
            _uiState.update {
                it.copy(
                    policies = policies,
                    clients = clients,
                    isRefreshing = false,
                    lastSynced = lastFetchedAt
                )
            }
        }
    }

    fun focusOnPolicy(policyId: String?) {
        _uiState.update { it.copy(focusedPolicyId = policyId) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onDragStateChange(state: DragState) {
        _uiState.update { it.copy(dragState = state) }
    }

    fun applyPolicyToClient(clientId: String, policyId: String, fromAssistant: Boolean = false) {
        val previous = _uiState.value.clients.firstOrNull { it.id == clientId } ?: return
        val policy = _uiState.value.policies.firstOrNull { it.id == policyId } ?: return
        _uiState.update { state ->
            state.copy(
                clients = state.clients.map { if (it.id == clientId) it.copy(policyId = policyId, registered = true) else it },
                operationsInFlight = state.operationsInFlight + clientId
            )
        }
        viewModelScope.launch {
            val result = repository.applyPolicyToClient(clientId, policyId)
            _uiState.update { it.copy(operationsInFlight = it.operationsInFlight - clientId) }
            result.onSuccess { client ->
                _uiState.update { state ->
                    state.copy(clients = state.clients.map { if (it.id == client.id) client else it })
                }
                if (fromAssistant) {
                    setAssistantBanner("Готово: ${client.name} → ${policy.name}", false)
                }
                _events.emit(UiEvent.Toast("Политика ${policy.name} применена для ${client.name}"))
                _events.emit(UiEvent.Haptic(HapticType.APPLY))
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(clients = state.clients.map { if (it.id == clientId) previous else it })
                }
                if (fromAssistant) {
                    setAssistantBanner(error.message ?: "Ошибка применения", true)
                }
                _events.emit(UiEvent.Toast(error.message ?: "Не удалось применить", isError = true))
            }
        }
    }

    fun clearPolicyForClient(clientId: String, fromAssistant: Boolean = false) {
        val previous = _uiState.value.clients.firstOrNull { it.id == clientId } ?: return
        _uiState.update { state ->
            state.copy(
                clients = state.clients.map { if (it.id == clientId) it.copy(policyId = null) else it },
                operationsInFlight = state.operationsInFlight + clientId
            )
        }
        viewModelScope.launch {
            val result = repository.clearPolicyForClient(clientId)
            _uiState.update { it.copy(operationsInFlight = it.operationsInFlight - clientId) }
            result.onSuccess { client ->
                if (fromAssistant) {
                    setAssistantBanner("Снято: ${client.name}", false)
                }
                _events.emit(UiEvent.Toast("Снято для ${client.name}"))
                _events.emit(UiEvent.Haptic(HapticType.REMOVE))
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(clients = state.clients.map { if (it.id == clientId) previous else it })
                }
                if (fromAssistant) {
                    setAssistantBanner(error.message ?: "Ошибка снятия", true)
                }
                _events.emit(UiEvent.Toast(error.message ?: "Не удалось снять", isError = true))
            }
        }
    }

    fun applyPolicyToAll(policyId: String, fromAssistant: Boolean = false) {
        val policy = _uiState.value.policies.firstOrNull { it.id == policyId } ?: return
        val previous = _uiState.value.clients
        _uiState.update {
            it.copy(
                clients = it.clients.map { client -> client.copy(policyId = policyId, registered = true) },
                bulkOperationInProgress = true
            )
        }
        viewModelScope.launch {
            val result = repository.applyPolicyToAll(policyId)
            _uiState.update { it.copy(bulkOperationInProgress = false) }
            result.onSuccess { clients ->
                _uiState.update { state -> state.copy(clients = clients) }
                if (fromAssistant) {
                    setAssistantBanner("Готово: всем → ${policy.name}", false)
                }
                _events.emit(UiEvent.Toast("${clients.size} устройств → ${policy.name}"))
            }.onFailure { error ->
                _uiState.update { it.copy(clients = previous) }
                if (fromAssistant) {
                    setAssistantBanner(error.message ?: "Ошибка массового применения", true)
                }
                _events.emit(UiEvent.Toast(error.message ?: "Не удалось применить всем", isError = true))
            }
        }
    }

    fun clearPolicyFromAll(policyId: String?, fromAssistant: Boolean = false) {
        val previous = _uiState.value.clients
        _uiState.update { it.copy(clients = it.clients.map { client -> if (policyId == null || client.policyId == policyId) client.copy(policyId = null) else client }, bulkOperationInProgress = true) }
        viewModelScope.launch {
            val result = repository.clearPolicyFromAll(policyId)
            _uiState.update { it.copy(bulkOperationInProgress = false) }
            result.onSuccess { clients ->
                _uiState.update { state -> state.copy(clients = clients) }
                if (fromAssistant) {
                    setAssistantBanner("Сброшено: ${clients.count { it.policyId == null }} устройств", false)
                }
                _events.emit(UiEvent.Toast("Снято у ${clients.count { it.policyId == null }} устройств"))
            }.onFailure { error ->
                _uiState.update { it.copy(clients = previous) }
                if (fromAssistant) {
                    setAssistantBanner(error.message ?: "Ошибка массового снятия", true)
                }
                _events.emit(UiEvent.Toast(error.message ?: "Не удалось снять", isError = true))
            }
        }
    }

    fun registerClient(name: String, mac: String, ip: String?, notes: String?, onComplete: (Boolean) -> Unit) {
        if (name.isBlank() || !MAC_REGEX.matcher(mac).matches()) {
            onComplete(false)
            viewModelScope.launch { _events.emit(UiEvent.Toast("Проверьте имя и MAC", isError = true)) }
            return
        }
        viewModelScope.launch {
            val result = repository.registerClient(name.trim(), mac.trim(), ip?.takeIf { it.isNotBlank() }, notes)
            result.onSuccess { client ->
                _uiState.update { state -> state.copy(clients = state.clients + client) }
                _events.emit(UiEvent.Toast("Клиент ${client.name} зарегистрирован"))
                onComplete(true)
            }.onFailure { error ->
                _events.emit(UiEvent.Toast(error.message ?: "Не удалось зарегистрировать", isError = true))
                onComplete(false)
            }
        }
    }

    fun renameClient(clientId: String, newName: String) {
        if (newName.isBlank()) return
        _uiState.update { state ->
            state.copy(clients = state.clients.map { if (it.id == clientId) it.copy(name = newName.trim()) else it })
        }
    }

    fun setClientRegistration(clientId: String, registered: Boolean) {
        _uiState.update { state ->
            state.copy(clients = state.clients.map { if (it.id == clientId) it.copy(registered = registered) else it })
        }
    }

    fun handleAssistantCommand(command: String) {
        if (command.isBlank()) {
            viewModelScope.launch { setAssistantBanner("Введите команду", true) }
            return
        }
        val intent = assistantHandler.parse(command)
        if (intent == null) {
            setAssistantBanner("Не распознано", true)
            return
        }
        when (val execution = assistantHandler.execute(intent, _uiState.value)) {
            is AssistantExecution.Error -> setAssistantBanner(execution.reason, true)
            is AssistantExecution.Command -> executeAssistantCommand(execution)
        }
    }

    fun dismissAssistantBanner() {
        _uiState.update { it.copy(assistantBanner = null) }
    }

    fun addCustomCommand(command: CustomCommand) {
        _uiState.update { it.copy(customCommands = it.customCommands + command.copy(id = UUID.randomUUID().toString())) }
    }

    fun updateCustomCommand(command: CustomCommand) {
        _uiState.update {
            it.copy(customCommands = it.customCommands.map { existing -> if (existing.id == command.id) command else existing })
        }
    }

    fun removeCustomCommand(commandId: String) {
        _uiState.update { it.copy(customCommands = it.customCommands.filterNot { it.id == commandId }) }
    }

    fun exportCommandsAsJson(): String {
        val commands = _uiState.value.customCommands
        return Json { prettyPrint = true }.encodeToString(commands)
    }

    fun importCommandsFromJson(json: String): Result<Unit> = runCatching {
        val decoded = Json.decodeFromString<List<CustomCommand>>(json)
        _uiState.update { it.copy(customCommands = decoded) }
    }

    private fun executeAssistantCommand(execution: AssistantExecution.Command) {
        when (execution.actionType) {
            com.example.policyswitcher.model.AssistantActionType.APPLY_TO_DEVICE -> {
                val clientId = execution.client?.id ?: return
                val policyId = execution.policy?.id ?: return
                applyPolicyToClient(clientId, policyId, fromAssistant = true)
            }
            com.example.policyswitcher.model.AssistantActionType.REMOVE_FROM_DEVICE -> {
                val clientId = execution.client?.id ?: return
                clearPolicyForClient(clientId, fromAssistant = true)
            }
            com.example.policyswitcher.model.AssistantActionType.APPLY_TO_ALL -> {
                val policyId = execution.policy?.id ?: return
                applyPolicyToAll(policyId, fromAssistant = true)
            }
            com.example.policyswitcher.model.AssistantActionType.REMOVE_FROM_ALL -> {
                val policyId = execution.policy?.id
                clearPolicyFromAll(policyId, fromAssistant = true)
            }
        }
    }

    private fun setAssistantBanner(message: String, isError: Boolean) {
        _uiState.update { it.copy(assistantBanner = assistantHandler.bannerForResult(!isError, message)) }
    }

    private fun validateCredentials(credentials: Credentials, policies: List<Policy>): Map<CredentialField, String> {
        val map = mutableMapOf<CredentialField, String>()
        if (!DOMAIN_REGEX.matcher(credentials.domainOrIp).matches()) {
            map[CredentialField.DOMAIN] = "Некорректный домен/адрес"
        }
        if (credentials.username.isBlank()) {
            map[CredentialField.USERNAME] = "Введите логин"
        }
        if (credentials.password.isBlank()) {
            map[CredentialField.PASSWORD] = "Введите пароль"
        }
        if (credentials.defaultPolicyId.isBlank() && policies.isNotEmpty()) {
            map[CredentialField.DEFAULT_POLICY] = "Выберите политику"
        }
        return map
    }

    private fun isReady(): Boolean = _uiState.value.connectionStatus is ConnectionStatus.Ready

    companion object {
        private val DOMAIN_REGEX = Pattern.compile(
            "^(https?://)?([A-Za-z0-9-]+\\.)*[A-Za-z0-9-]+(\\.[A-Za-z]{2,})?(:\\d+)?|((25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(?1)){3})$"
        )
        private val MAC_REGEX = Pattern.compile("^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}$")
    }
}

class PolicySwitcherViewModelFactory(
    private val repository: KeeneticRepository,
    private val credentialStore: CredentialStore,
    private val assistantCommandHandler: AssistantCommandHandler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PolicySwitcherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PolicySwitcherViewModel(repository, credentialStore, assistantCommandHandler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
