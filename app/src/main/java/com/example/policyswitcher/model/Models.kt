package com.example.policyswitcher.model

import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * Stores router credentials locally. The password is expected to be persisted securely through
 * [androidx.security.crypto.EncryptedSharedPreferences].
 */
data class Credentials(
    val domainOrIp: String = "",
    val username: String = "",
    val password: String = "",
    val defaultPolicyId: String = ""
) {
    fun isComplete(): Boolean =
        domainOrIp.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

enum class CredentialField { DOMAIN, USERNAME, PASSWORD, DEFAULT_POLICY }

sealed interface ConnectionStatus {
    data object NotSet : ConnectionStatus
    data object Validating : ConnectionStatus
    data class Ready(val lastChecked: Instant) : ConnectionStatus
    data class Error(val reason: String) : ConnectionStatus
}

data class Policy(
    val id: String,
    val name: String,
    val description: String = ""
)

data class Client(
    val id: String,
    val name: String,
    val mac: String,
    val ip: String? = null,
    val policyId: String? = null,
    val registered: Boolean = false,
    val alias: String? = null,
    val notes: String? = null,
    val hasPrivateMacWarning: Boolean = false
)

sealed interface DragState {
    data object Idle : DragState
    data object DraggingOverApply : DragState
    data object DraggingOverRemove : DragState
    data class DraggingOverPolicy(val policyId: String) : DragState
}

data class AssistantBanner(
    val message: String,
    val isError: Boolean,
    val timestamp: Instant = Instant.now()
)

enum class AssistantActionType {
    APPLY_TO_DEVICE,
    REMOVE_FROM_DEVICE,
    APPLY_TO_ALL,
    REMOVE_FROM_ALL
}

@Serializable
data class CustomCommand(
    val id: String,
    val phrase: String,
    val actionType: AssistantActionType,
    val policyId: String? = null,
    val deviceId: String? = null,
    val enabledForAssistant: Boolean = true
)

data class UiState(
    val credentials: Credentials = Credentials(),
    val credentialErrors: Map<CredentialField, String> = emptyMap(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.NotSet,
    val credsPanelExpanded: Boolean = true,
    val lastSuccessfulUrl: String? = null,
    val lastSynced: Instant? = null,
    val policies: List<Policy> = emptyList(),
    val clients: List<Client> = emptyList(),
    val focusedPolicyId: String? = null,
    val dragState: DragState = DragState.Idle,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val assistantBanner: AssistantBanner? = null,
    val customCommands: List<CustomCommand> = emptyList(),
    val operationsInFlight: Set<String> = emptySet(),
    val bulkOperationInProgress: Boolean = false
) {
    val focusedPolicy: Policy? get() = policies.firstOrNull { it.id == focusedPolicyId }

    fun clientsForFocusedPolicy(): Pair<List<Client>, List<Client>> {
        val filtered = applySearchFilter(clients)
        return if (focusedPolicyId == null) {
            filtered to emptyList()
        } else {
            val assigned = filtered.filter { it.policyId == focusedPolicyId }
            val others = filtered.filter { it.policyId != focusedPolicyId }
            assigned to others
        }
    }

    private fun applySearchFilter(clients: List<Client>): List<Client> {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) return clients
        return clients.filter { client ->
            client.name.lowercase().contains(query) ||
                client.mac.lowercase().contains(query) ||
                (client.ip?.lowercase()?.contains(query) ?: false) ||
                (client.alias?.lowercase()?.contains(query) ?: false)
        }
    }
}
