package com.example.policyswitcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.policyswitcher.assistant.AssistantCommandHandler
import com.example.policyswitcher.data.FakeKeeneticRepository
import com.example.policyswitcher.data.SecureCredentialStore
import com.example.policyswitcher.ui.PolicySwitcherScreen
import com.example.policyswitcher.ui.PolicySwitcherViewModel
import com.example.policyswitcher.ui.PolicySwitcherViewModelFactory
import com.example.policyswitcher.ui.UiEvent
import com.example.policyswitcher.ui.HapticType
import com.example.policyswitcher.ui.theme.PolicySwitcherTheme

class MainActivity : ComponentActivity() {
    private val viewModelFactory by lazy {
        PolicySwitcherViewModelFactory(
            repository = FakeKeeneticRepository(),
            credentialStore = SecureCredentialStore(this),
            assistantCommandHandler = AssistantCommandHandler()
        )
    }

    private val viewModel: PolicySwitcherViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PolicySwitcherTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                val snackbarHostState = remember { SnackbarHostState() }
                val haptic = LocalHapticFeedback.current

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is UiEvent.Toast -> snackbarHostState.showSnackbar(
                                message = event.message,
                                withDismissAction = true
                            )
                            is UiEvent.Haptic -> when (event.type) {
                                HapticType.APPLY -> haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                HapticType.REMOVE -> haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }
                        }
                    }
                }

                PolicySwitcherScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onTogglePanel = viewModel::toggleCredentialsPanel,
                    onUpdateDomain = viewModel::updateDomain,
                    onUpdateUsername = viewModel::updateUsername,
                    onUpdatePassword = viewModel::updatePassword,
                    onUpdateDefaultPolicy = viewModel::updateDefaultPolicy,
                    onVerifyCredentials = viewModel::verifyConnection,
                    onRefresh = { viewModel.refresh(force = true) },
                    onFocusPolicy = viewModel::focusOnPolicy,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onApplyPolicy = { clientId, policyId -> viewModel.applyPolicyToClient(clientId, policyId) },
                    onRemovePolicy = { clientId -> viewModel.clearPolicyForClient(clientId) },
                    onApplyPolicyToAll = { policyId -> viewModel.applyPolicyToAll(policyId) },
                    onRemovePolicyFromAll = { policyId -> viewModel.clearPolicyFromAll(policyId) },
                    onRegisterClient = { name, mac, ip, notes, close -> viewModel.registerClient(name, mac, ip, notes, close) },
                    onRenameClient = viewModel::renameClient,
                    onSetRegistration = viewModel::setClientRegistration,
                    onDragStateChange = viewModel::onDragStateChange,
                    onAssistantCommand = viewModel::handleAssistantCommand,
                    onDismissAssistantBanner = viewModel::dismissAssistantBanner,
                    onAddCustomCommand = viewModel::addCustomCommand,
                    onUpdateCustomCommand = viewModel::updateCustomCommand,
                    onDeleteCustomCommand = viewModel::removeCustomCommand,
                    onImportCommands = viewModel::importCommandsFromJson,
                    onExportCommands = viewModel::exportCommandsAsJson
                )
            }
        }
    }
}
