package com.example.policyswitcher.assistant

import com.example.policyswitcher.model.AssistantActionType
import com.example.policyswitcher.model.AssistantBanner
import com.example.policyswitcher.model.Client
import com.example.policyswitcher.model.Policy
import com.example.policyswitcher.model.UiState
import java.text.Normalizer
import java.util.Locale

sealed interface AssistantIntent {
    data class ApplyToDevice(val policyName: String, val deviceName: String) : AssistantIntent
    data class RemoveFromDevice(val policyName: String?, val deviceName: String) : AssistantIntent
    data class ApplyToAll(val policyName: String) : AssistantIntent
    data class RemoveFromAll(val policyName: String) : AssistantIntent
}

class AssistantCommandHandler {
    private val applyRegex = Regex("включи политику (.+) на (.+)")
    private val disableRegex = Regex("отключи политику (.+) на (.+)")
    private val enableAllRegex = Regex("включи всем политику (.+)")
    private val disableAllRegex = Regex("(отключи|выключи) всем политику (.+)")

    fun parse(command: String): AssistantIntent? {
        val normalized = normalize(command)
        return when {
            applyRegex.matches(normalized) -> {
                val (policy, device) = applyRegex.find(normalized)!!.destructured
                AssistantIntent.ApplyToDevice(policy.trim(), device.trim())
            }
            disableRegex.matches(normalized) -> {
                val (policy, device) = disableRegex.find(normalized)!!.destructured
                AssistantIntent.RemoveFromDevice(policy.trim(), device.trim())
            }
            enableAllRegex.matches(normalized) -> {
                val (policy) = enableAllRegex.find(normalized)!!.destructured
                AssistantIntent.ApplyToAll(policy.trim())
            }
            disableAllRegex.matches(normalized) -> {
                val (_, policy) = disableAllRegex.find(normalized)!!.destructured
                AssistantIntent.RemoveFromAll(policy.trim())
            }
            else -> null
        }
    }

    fun execute(intent: AssistantIntent, state: UiState): AssistantExecution {
        return when (intent) {
            is AssistantIntent.ApplyToDevice -> resolveDevice(state, intent.deviceName)?.let { client ->
                val policy = resolvePolicy(state, intent.policyName)
                    ?: return AssistantExecution.Error("Политика \"${intent.policyName}\" не найдена")
                AssistantExecution.Command(
                    actionType = AssistantActionType.APPLY_TO_DEVICE,
                    client = client,
                    policy = policy
                )
            } ?: AssistantExecution.Error("Устройство \"${intent.deviceName}\" не найдено")

            is AssistantIntent.RemoveFromDevice -> resolveDevice(state, intent.deviceName)?.let { client ->
                val policy = intent.policyName?.let { resolvePolicy(state, it) }
                AssistantExecution.Command(
                    actionType = AssistantActionType.REMOVE_FROM_DEVICE,
                    client = client,
                    policy = policy
                )
            } ?: AssistantExecution.Error("Устройство \"${intent.deviceName}\" не найдено")

            is AssistantIntent.ApplyToAll -> {
                val policy = resolvePolicy(state, intent.policyName)
                    ?: return AssistantExecution.Error("Политика \"${intent.policyName}\" не найдена")
                AssistantExecution.Command(
                    actionType = AssistantActionType.APPLY_TO_ALL,
                    policy = policy
                )
            }

            is AssistantIntent.RemoveFromAll -> {
                val policy = resolvePolicy(state, intent.policyName)
                    ?: return AssistantExecution.Error("Политика \"${intent.policyName}\" не найдена")
                AssistantExecution.Command(
                    actionType = AssistantActionType.REMOVE_FROM_ALL,
                    policy = policy
                )
            }
        }
    }

    fun bannerForResult(success: Boolean, message: String): AssistantBanner =
        AssistantBanner(message = message, isError = !success)

    private fun resolveDevice(state: UiState, rawName: String): Client? {
        val normalized = normalize(rawName)
        return state.clients.firstOrNull { client ->
            normalize(client.name) == normalized ||
                normalize(client.alias ?: "") == normalized ||
                client.mac.equals(rawName.trim(), ignoreCase = true)
        }
    }

    private fun resolvePolicy(state: UiState, rawName: String): Policy? {
        val normalized = normalize(rawName)
        return state.policies.firstOrNull { policy ->
            normalize(policy.name) == normalized || policy.id.equals(rawName.trim(), ignoreCase = true)
        }
    }

    private fun normalize(input: String): String {
        if (input.isBlank()) return input
        val lower = input.lowercase(Locale.getDefault())
        val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return normalized.replace("[^a-z0-9а-яё ]".toRegex(), " ").replace("\s+".toRegex(), " ").trim()
    }
}

sealed interface AssistantExecution {
    data class Command(
        val actionType: AssistantActionType,
        val client: Client? = null,
        val policy: Policy? = null
    ) : AssistantExecution

    data class Error(val reason: String) : AssistantExecution
}
