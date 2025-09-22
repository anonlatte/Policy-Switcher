package com.example.policyswitcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PasswordVisualTransformation
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.policyswitcher.R
import com.example.policyswitcher.model.Client
import com.example.policyswitcher.model.ConnectionStatus
import com.example.policyswitcher.model.CredentialField
import com.example.policyswitcher.model.CustomCommand
import com.example.policyswitcher.model.DragState
import com.example.policyswitcher.model.Policy
import com.example.policyswitcher.model.UiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PolicySwitcherScreen(
    uiState: UiState,
    snackbarHostState: SnackbarHostState,
    onTogglePanel: () -> Unit,
    onUpdateDomain: (String) -> Unit,
    onUpdateUsername: (String) -> Unit,
    onUpdatePassword: (String) -> Unit,
    onUpdateDefaultPolicy: (String) -> Unit,
    onVerifyCredentials: () -> Unit,
    onRefresh: () -> Unit,
    onFocusPolicy: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onApplyPolicy: (String, String) -> Unit,
    onRemovePolicy: (String) -> Unit,
    onApplyPolicyToAll: (String) -> Unit,
    onRemovePolicyFromAll: (String?) -> Unit,
    onRegisterClient: (String, String, String?, String?, (Boolean) -> Unit) -> Unit,
    onRenameClient: (String, String) -> Unit,
    onSetRegistration: (String, Boolean) -> Unit,
    onDragStateChange: (DragState) -> Unit,
    onAssistantCommand: (String) -> Unit,
    onDismissAssistantBanner: () -> Unit,
    onAddCustomCommand: (CustomCommand) -> Unit,
    onUpdateCustomCommand: (CustomCommand) -> Unit,
    onDeleteCustomCommand: (String) -> Unit,
    onImportCommands: (String) -> Result<Unit>,
    onExportCommands: () -> String
)

    var showHelp by remember { mutableStateOf(false) }
    var showAddClient by remember { mutableStateOf(false) }
    var policyPickerClient by remember { mutableStateOf<Client?>(null) }
    var showCommandsSheet by remember { mutableStateOf(false) }
    var editingCommand by remember { mutableStateOf<CustomCommand?>(null) }
    var assistantCommandText by remember { mutableStateOf("") }

    val (assignedClients, otherClients) = uiState.clientsForFocusedPolicy()

    val pullState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            CredentialsAppBar(
                uiState = uiState,
                onTogglePanel = onTogglePanel,
                onUpdateDomain = onUpdateDomain,
                onUpdateUsername = onUpdateUsername,
                onUpdatePassword = onUpdatePassword,
                onUpdateDefaultPolicy = onUpdateDefaultPolicy,
                onVerifyCredentials = onVerifyCredentials,
                onRefresh = onRefresh,
                onShowHelp = { showHelp = true }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            QuickActionsBar(
                isPolicyFocused = uiState.focusedPolicyId != null,
                focusedPolicy = uiState.focusedPolicy,
                isBusy = uiState.bulkOperationInProgress,
                onApplyAll = { uiState.focusedPolicyId?.let(onApplyPolicyToAll) },
                onRemoveAll = {
                    if (uiState.focusedPolicyId != null) {
                        onRemovePolicyFromAll(uiState.focusedPolicyId)
                    } else {
                        onRemovePolicyFromAll(null)
                    }
                },
                onAddClient = { showAddClient = true },
                onOpenCommands = { showCommandsSheet = true }
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                state = pullState,
                onRefresh = onRefresh
            ) {
                Column(Modifier.fillMaxSize()) {
                    uiState.assistantBanner?.let { banner ->
                        AssistantBanner(
                            message = banner.message,
                            isError = banner.isError,
                            onDismiss = onDismissAssistantBanner
                        )
                    }

                    if (uiState.focusedPolicyId == null) {
                        PolicyGrid(
                            policies = uiState.policies,
                            clients = uiState.clients,
                            onPolicyClick = { policy -> onFocusPolicy(policy.id) }
                        )
                    } else {
                        PolicyDetail(
                            policy = uiState.focusedPolicy,
                            assignedClients = assignedClients,
                            otherClients = otherClients,
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            onBack = { onFocusPolicy(null) },
                            onApplyPolicy = { clientId ->
                                uiState.focusedPolicyId?.let { target -> onApplyPolicy(clientId, target) }
                            },
                            onClearPolicy = onRemovePolicy,
                            onRequestPolicyPicker = { client -> policyPickerClient = client },
                            operationsInFlight = uiState.operationsInFlight,
                            onDragStateChange = onDragStateChange,
                            defaultPolicy = uiState.policies.firstOrNull { it.id == uiState.credentials.defaultPolicyId },
                            onRenameClient = onRenameClient,
                            onSetRegistration = onSetRegistration
                        )
                    }

                    AssistantCommandInput(
                        command = assistantCommandText,
                        onCommandChange = { assistantCommandText = it },
                        onSend = {
                            onAssistantCommand(assistantCommandText)
                            assistantCommandText = ""
                        }
                    )
                }
            }

            DragIndicators(dragState = uiState.dragState)
        }
    }

    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }

    if (showAddClient) {
        AddClientDialog(
            onDismiss = { showAddClient = false },
            onSubmit = { name, mac, ip, notes, close ->
                onRegisterClient(name, mac, ip, notes) { success ->
                    if (success) close() else Unit
                }
            }
        )
    }

    policyPickerClient?.let { client ->
        PolicyPickerDialog(
            policies = uiState.policies,
            onDismiss = { policyPickerClient = null },
            onSelect = { policyId ->
                onApplyPolicy(client.id, policyId)
                policyPickerClient = null
            }
        )
    }

    if (showCommandsSheet) {
        CustomCommandsSheet(
            commands = uiState.customCommands,
            policies = uiState.policies,
            clients = uiState.clients,
            onDismiss = { showCommandsSheet = false },
            onAdd = { editingCommand = CustomCommand("", "", com.example.policyswitcher.model.AssistantActionType.APPLY_TO_DEVICE) },
            onExecute = { command ->
                when (command.actionType) {
                    com.example.policyswitcher.model.AssistantActionType.APPLY_TO_DEVICE -> {
                        val policyId = command.policyId
                        val clientId = command.deviceId
                        if (policyId != null && clientId != null) {
                            onApplyPolicy(clientId, policyId)
                        }
                    }
                    com.example.policyswitcher.model.AssistantActionType.REMOVE_FROM_DEVICE -> {
                        val clientId = command.deviceId
                        if (clientId != null) {
                            onRemovePolicy(clientId)
                        }
                    }
                    com.example.policyswitcher.model.AssistantActionType.APPLY_TO_ALL -> {
                        command.policyId?.let(onApplyPolicyToAll)
                    }
                    com.example.policyswitcher.model.AssistantActionType.REMOVE_FROM_ALL -> {
                        onRemovePolicyFromAll(command.policyId)
                    }
                }
            },
            onEdit = { command -> editingCommand = command },
            onDelete = onDeleteCustomCommand,
            onExport = onExportCommands,
            onImport = onImportCommands
        )
    }

    editingCommand?.let { command ->
        CustomCommandEditor(
            command = command,
            policies = uiState.policies,
            clients = uiState.clients,
            onDismiss = { editingCommand = null },
            onSave = { updated ->
                if (updated.id.isEmpty()) {
                    onAddCustomCommand(updated)
                } else {
                    onUpdateCustomCommand(updated)
                }
                editingCommand = null
            }
        )
    }
}

@Composable
private fun CredentialsAppBar(
    uiState: UiState,
    onTogglePanel: () -> Unit,
    onUpdateDomain: (String) -> Unit,
    onUpdateUsername: (String) -> Unit,
    onUpdatePassword: (String) -> Unit,
    onUpdateDefaultPolicy: (String) -> Unit,
    onVerifyCredentials: () -> Unit,
    onRefresh: () -> Unit,
    onShowHelp: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    LargeTopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIndicator(status = uiState.connectionStatus)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = shortUrl(uiState.lastSuccessfulUrl ?: uiState.credentials.domainOrIp),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                uiState.lastSynced?.let { lastSynced ->
                    Text(
                        text = "Обновлено: ${formatTimestamp(lastSynced)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onTogglePanel) {
                Icon(
                    imageVector = if (uiState.credsPanelExpanded) Icons.Default.ArrowBack else Icons.Default.CloudQueue,
                    contentDescription = null
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = stringResource(id = R.string.refresh))
            }
            IconButton(onClick = onShowHelp) {
                Icon(imageVector = Icons.Outlined.HelpOutline, contentDescription = stringResource(id = R.string.help))
            }
        },
        scrollBehavior = scrollBehavior
    )

    AnimatedVisibility(visible = uiState.credsPanelExpanded, enter = expandVertically(), exit = shrinkVertically()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            CredentialField(
                value = uiState.credentials.domainOrIp,
                onValueChange = onUpdateDomain,
                label = stringResource(id = R.string.domain_hint),
                error = uiState.credentialErrors[CredentialField.DOMAIN]
            )
            CredentialField(
                value = uiState.credentials.username,
                onValueChange = onUpdateUsername,
                label = stringResource(id = R.string.username_hint),
                error = uiState.credentialErrors[CredentialField.USERNAME]
            )
            CredentialField(
                value = uiState.credentials.password,
                onValueChange = onUpdatePassword,
                label = stringResource(id = R.string.password_hint),
                error = uiState.credentialErrors[CredentialField.PASSWORD],
                isPassword = true
            )
            PolicyDropdown(
                policies = uiState.policies,
                selectedId = uiState.credentials.defaultPolicyId,
                onSelected = onUpdateDefaultPolicy,
                error = uiState.credentialErrors[CredentialField.DEFAULT_POLICY]
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onVerifyCredentials, modifier = Modifier.align(Alignment.End)) {
                Text(text = stringResource(id = R.string.verify_connection))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatusIndicator(status: ConnectionStatus) {
    val (icon, tint) = when (status) {
        ConnectionStatus.NotSet -> Icons.Default.CloudQueue to MaterialTheme.colorScheme.onSurfaceVariant
        ConnectionStatus.Validating -> Icons.Default.CloudQueue to MaterialTheme.colorScheme.primary
        is ConnectionStatus.Ready -> Icons.Default.CloudDone to MaterialTheme.colorScheme.primary
        is ConnectionStatus.Error -> Icons.Rounded.CloudOff to MaterialTheme.colorScheme.error
    }
    Icon(icon, contentDescription = null, tint = tint)
}

@Composable
private fun CredentialField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    isPassword: Boolean = false
) {
    val visualTransformation: VisualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        visualTransformation = visualTransformation,
        isError = error != null,
        supportingText = error?.let { { Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PolicyDropdown(
    policies: List<Policy>,
    selectedId: String,
    onSelected: (String) -> Unit,
    error: String?
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedPolicy = policies.firstOrNull { it.id == selectedId }
    Column(Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedPolicy?.name ?: "",
                onValueChange = {},
                label = { Text(stringResource(id = R.string.default_policy)) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                isError = error != null,
                supportingText = error?.let { { Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) } }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                policies.forEach { policy ->
                    DropdownMenuItem(
                        text = { Text(policy.name) },
                        onClick = {
                            onSelected(policy.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicyGrid(
    policies: List<Policy>,
    clients: List<Client>,
    onPolicyClick: (Policy) -> Unit
) {
    if (policies.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(id = R.string.empty_clients), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = if (maxWidth < 600.dp) 2 else 3
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(policies, key = { it.id }) { policy ->
                val count = clients.count { it.policyId == policy.id }
                PolicyCard(policy = policy, clientCount = count, onClick = { onPolicyClick(policy) })
            }
        }
    }
}

@Composable
private fun PolicyCard(policy: Policy, clientCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.1f),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(imageVector = Icons.Outlined.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = policy.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "$clientCount устройств",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PolicyDetail(
    policy: Policy?,
    assignedClients: List<Client>,
    otherClients: List<Client>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onApplyPolicy: (String) -> Unit,
    onClearPolicy: (String) -> Unit,
    onRequestPolicyPicker: (Client) -> Unit,
    operationsInFlight: Set<String>,
    onDragStateChange: (DragState) -> Unit,
    defaultPolicy: Policy?,
    onRenameClient: (String, String) -> Unit,
    onSetRegistration: (String, Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            onClick = {}
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.all_policies))
                }
                Column(Modifier.weight(1f)) {
                    Text(text = policy?.name ?: stringResource(id = R.string.all_policies), style = MaterialTheme.typography.titleLarge)
                    defaultPolicy?.let {
                        Text(
                            text = "Default: ${it.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            label = { Text(stringResource(id = R.string.search)) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stickyHeader {
                SectionHeader(text = stringResource(id = R.string.clients_assigned))
            }
            if (assignedClients.isEmpty()) {
                item {
                    EmptyState(text = stringResource(id = R.string.empty_policy_clients))
                }
            } else {
                items(assignedClients, key = { it.id }) { client ->
                    ClientRow(
                        client = client,
                        isBusy = operationsInFlight.contains(client.id),
                        onApply = { onApplyPolicy(client.id) },
                        onClear = { onClearPolicy(client.id) },
                        onRequestPolicyPicker = { onRequestPolicyPicker(client) },
                        onDragStateChange = onDragStateChange,
                        showPolicyBadge = false,
                        defaultPolicyName = defaultPolicy?.name,
                        onRenameClient = onRenameClient,
                        onSetRegistration = onSetRegistration
                    )
                }
            }

            stickyHeader {
                SectionHeader(text = stringResource(id = R.string.clients_unassigned))
            }
            if (otherClients.isEmpty()) {
                item { EmptyState(text = stringResource(id = R.string.empty_filter)) }
            } else {
                items(otherClients, key = { it.id }) { client ->
                    ClientRow(
                        client = client,
                        isBusy = operationsInFlight.contains(client.id),
                        onApply = { onApplyPolicy(client.id) },
                        onClear = { onClearPolicy(client.id) },
                        onRequestPolicyPicker = { onRequestPolicyPicker(client) },
                        onDragStateChange = onDragStateChange,
                        showPolicyBadge = true,
                        defaultPolicyName = defaultPolicy?.name,
                        onRenameClient = onRenameClient,
                        onSetRegistration = onSetRegistration
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Text(text = text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClientRow(
    client: Client,
    isBusy: Boolean,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onRequestPolicyPicker: () -> Unit,
    onDragStateChange: (DragState) -> Unit,
    showPolicyBadge: Boolean,
    defaultPolicyName: String?,
    onRenameClient: (String, String) -> Unit,
    onSetRegistration: (String, Boolean) -> Unit
) {
    var contextMenuVisible by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = dragOffset, label = "drag")
    val threshold = 120f

    if (renameDialog) {
        RenameClientDialog(
            initialName = client.name,
            onDismiss = { renameDialog = false },
            onConfirm = { name ->
                onRenameClient(client.id, name)
                renameDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .offset { IntOffset(0, animatedOffset.roundToInt()) }
            .combinedClickable(
                onClick = {},
                onLongClick = { contextMenuVisible = true }
            )
            .pointerInput(client.id) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        dragOffset = (dragOffset + dragAmount.y).coerceIn(-200f, 200f)
                        val newState = when {
                            dragOffset < -threshold -> DragState.DraggingOverApply
                            dragOffset > threshold -> DragState.DraggingOverRemove
                            else -> DragState.Idle
                        }
                        onDragStateChange(newState)
                    },
                    onDragEnd = {
                        when {
                            dragOffset < -threshold -> onApply()
                            dragOffset > threshold -> onClear()
                        }
                        dragOffset = 0f
                        onDragStateChange(DragState.Idle)
                    },
                    onDragCancel = {
                        dragOffset = 0f
                        onDragStateChange(DragState.Idle)
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(text = client.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        client.ip?.let {
                            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { contextMenuVisible = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showPolicyBadge && client.policyId != null) {
                        AssistChip(onClick = onRequestPolicyPicker, label = { Text(client.policyId.uppercase(Locale.getDefault())) })
                    }
                    if (!client.registered) {
                        AssistChip(onClick = { onSetRegistration(client.id, true) }, label = { Text("Гость") })
                    }
                    if (client.hasPrivateMacWarning) {
                        AssistChip(onClick = {}, label = { Text("Private MAC") })
                    }
                }

                if (isBusy) {
                    Text(text = "Выполняется…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                DropdownMenu(expanded = contextMenuVisible, onDismissRequest = { contextMenuVisible = false }) {
                    DropdownMenuItem(text = { Text("Применить") }, onClick = {
                        contextMenuVisible = false
                        onApply()
                    })
                    DropdownMenuItem(text = { Text("Снять") }, onClick = {
                        contextMenuVisible = false
                        onClear()
                    })
                    DropdownMenuItem(text = { Text("Изменить…") }, onClick = {
                        contextMenuVisible = false
                        onRequestPolicyPicker()
                    })
                    DropdownMenuItem(text = { Text("Переименовать") }, onClick = {
                        contextMenuVisible = false
                        renameDialog = true
                    })
                    val toggleText = if (client.registered) "Отвязать" else "Зарегистрировать"
                    DropdownMenuItem(text = { Text(toggleText) }, onClick = {
                        contextMenuVisible = false
                        onSetRegistration(client.id, !client.registered)
                    })
                }
            }
        }
    }
}

@Composable
private fun RenameClientDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text(stringResource(id = R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } },
        title = { Text("Переименовать устройство") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") })
        }
    )
}

@Composable
private fun QuickActionsBar(
    isPolicyFocused: Boolean,
    focusedPolicy: Policy?,
    isBusy: Boolean,
    onApplyAll: () -> Unit,
    onRemoveAll: () -> Unit,
    onAddClient: () -> Unit,
    onOpenCommands: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isPolicyFocused) {
                FilledTonalButton(onClick = onApplyAll, enabled = !isBusy) {
                    Text(text = stringResource(id = R.string.apply_all) + (focusedPolicy?.name?.let { " (${it})" } ?: ""))
                }
                FilledTonalButton(onClick = onRemoveAll, enabled = !isBusy) {
                    Text(stringResource(id = R.string.remove_all))
                }
            } else {
                FilledTonalButton(onClick = onAddClient) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.add_client))
                }
                FilledTonalButton(onClick = onOpenCommands) {
                    Icon(imageVector = Icons.Outlined.KeyboardVoice, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.custom_commands))
                }
            }
        }
    }
}

@Composable
private fun AssistantBanner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        label = "bannerColor"
    )
    Surface(color = background, modifier = Modifier.fillMaxWidth(), tonalElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = if (isError) Icons.Default.CloudOff else Icons.Outlined.TaskAlt, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Ок") }
        }
    }
}

@Composable
private fun AssistantCommandInput(
    command: String,
    onCommandChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = command,
                onValueChange = onCommandChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(id = R.string.assistant_prompt)) }
            )
            Spacer(Modifier.width(12.dp))
            Button(onClick = onSend, enabled = command.isNotBlank()) {
                Text(stringResource(id = R.string.execute))
            }
        }
    }
}

@Composable
private fun AddClientDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String?, String?, close: () -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSubmit(name, mac, ip.takeIf { it.isNotBlank() }, notes.takeIf { it.isNotBlank() }) {
                    onDismiss()
                }
            }) { Text(stringResource(id = R.string.register)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } },
        title = { Text(stringResource(id = R.string.add_client)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(id = R.string.device)) })
                OutlinedTextField(value = mac, onValueChange = { mac = it }, label = { Text(stringResource(id = R.string.mac_address)) })
                OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text(stringResource(id = R.string.ip_address)) })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(id = R.string.comment)) })
            }
        }
    )
}

@Composable
private fun PolicyPickerDialog(
    policies: List<Policy>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } },
        title = { Text("Выберите политику") },
        text = {
            Column {
                policies.forEach { policy ->
                    TextButton(onClick = { onSelect(policy.id) }) {
                        Text(policy.name)
                    }
                }
            }
        }
    )
}

@Composable
private fun ImportCommandsDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onImport(text) }) { Text(stringResource(id = R.string.import_json)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } },
        title = { Text(stringResource(id = R.string.import_json)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("[{…}]") }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomCommandsSheet(
    commands: List<CustomCommand>,
    policies: List<Policy>,
    clients: List<Client>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onExecute: (CustomCommand) -> Unit,
    onEdit: (CustomCommand) -> Unit,
    onDelete: (String) -> Unit,
    onExport: () -> String,
    onImport: (String) -> Result<Unit>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }

    if (showImportDialog) {
        ImportCommandsDialog(
            onDismiss = { showImportDialog = false },
            onImport = { json ->
                onImport(json)
                showImportDialog = false
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(id = R.string.custom_commands), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onAdd) { Text(stringResource(id = R.string.add_command)) }
            }
            commands.forEach { command ->
                CommandCard(
                    command = command,
                    policies = policies,
                    clients = clients,
                    onExecute = onExecute,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
            Divider()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val json = onExport()
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("commands", json))
                }) { Text(stringResource(id = R.string.export_json)) }
                FilledTonalButton(onClick = { showImportDialog = true }) { Text(stringResource(id = R.string.import_json)) }
            }
        }
    }
}

@Composable
private fun CommandCard(
    command: CustomCommand,
    policies: List<Policy>,
    clients: List<Client>,
    onExecute: (CustomCommand) -> Unit,
    onEdit: (CustomCommand) -> Unit,
    onDelete: (String) -> Unit
) {
    val policyName = policies.firstOrNull { it.id == command.policyId }?.name
    val clientName = clients.firstOrNull { it.id == command.deviceId }?.name
    Card(onClick = { onExecute(command) }) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(command.phrase.ifBlank { "Команда" }, style = MaterialTheme.typography.titleMedium)
            Text(commandDescription(command, policyName, clientName), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { onEdit(command) }) { Text(stringResource(id = R.string.edit)) }
                TextButton(onClick = { onDelete(command.id) }) { Text(stringResource(id = R.string.delete)) }
            }
        }
    }
}

@Composable
private fun CustomCommandEditor(
    command: CustomCommand,
    policies: List<Policy>,
    clients: List<Client>,
    onDismiss: () -> Unit,
    onSave: (CustomCommand) -> Unit
) {
    var phrase by remember { mutableStateOf(command.phrase) }
    var actionType by remember { mutableStateOf(command.actionType) }
    var policyId by remember { mutableStateOf(command.policyId) }
    var deviceId by remember { mutableStateOf(command.deviceId) }
    var enabled by remember { mutableStateOf(command.enabledForAssistant) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(command.copy(phrase = phrase, actionType = actionType, policyId = policyId, deviceId = deviceId, enabledForAssistant = enabled))
            }) { Text(stringResource(id = R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } },
        title = {
            val titleRes = if (command.id.isEmpty()) R.string.add_command else R.string.edit
            Text(stringResource(id = titleRes))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = phrase, onValueChange = { phrase = it }, label = { Text(stringResource(id = R.string.phrase)) })
                ActionTypeSelector(actionType = actionType, onActionChange = { actionType = it })
                if (requiresPolicy(actionType)) {
                    PolicySelector(policies = policies, selected = policyId, onSelect = { policyId = it })
                }
                if (requiresDevice(actionType)) {
                    DeviceSelector(clients = clients, selected = deviceId, onSelect = { deviceId = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                    Text("Доступно ассистенту")
                }
            }
        }
    )
}

@Composable
private fun ActionTypeSelector(actionType: com.example.policyswitcher.model.AssistantActionType, onActionChange: (com.example.policyswitcher.model.AssistantActionType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = com.example.policyswitcher.model.AssistantActionType.values()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = actionTypeLabel(actionType),
            onValueChange = {},
            label = { Text(stringResource(id = R.string.action_type)) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(actionTypeLabel(option)) }, onClick = {
                    onActionChange(option)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun PolicySelector(policies: List<Policy>, selected: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedPolicy = policies.firstOrNull { it.id == selected }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedPolicy?.name ?: "",
            onValueChange = {},
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            label = { Text(stringResource(id = R.string.policy)) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            policies.forEach { policy ->
                DropdownMenuItem(text = { Text(policy.name) }, onClick = {
                    onSelect(policy.id)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun DeviceSelector(clients: List<Client>, selected: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedClient = clients.firstOrNull { it.id == selected }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedClient?.name ?: "",
            onValueChange = {},
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            label = { Text(stringResource(id = R.string.device)) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            clients.forEach { client ->
                DropdownMenuItem(text = { Text(client.name) }, onClick = {
                    onSelect(client.id)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun DragIndicators(dragState: DragState) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        AnimatedVisibility(visible = dragState == DragState.DraggingOverApply) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp
            ) {
                Text("Отпустите, чтобы применить", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        AnimatedVisibility(visible = dragState == DragState.DraggingOverRemove) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 4.dp
            ) {
                Text("Отпустите, чтобы снять", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Понятно") } },
        title = { Text("Подсказки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Перетащите устройство вверх, чтобы применить политику.")
                Text("• Перетащите вниз, чтобы вернуть на политику по умолчанию.")
                Text("• Нажмите на карточку политики, чтобы увидеть связанных клиентов.")
                Text("• Ассистент: скажите \"Включи политику WireGuard на Ноутбук\".")
            }
        }
    )
}

private fun formatTimestamp(instant: Instant): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

private fun shortUrl(url: String): String {
    if (url.isBlank()) return "Не настроено"
    return url.removePrefix("https://").removePrefix("http://")
}

private fun commandDescription(command: CustomCommand, policyName: String?, clientName: String?): String {
    return when (command.actionType) {
        com.example.policyswitcher.model.AssistantActionType.APPLY_TO_DEVICE -> "${clientName ?: "Устройство"} → ${policyName ?: "Политика"}"
        com.example.policyswitcher.model.AssistantActionType.REMOVE_FROM_DEVICE -> "Снять с ${clientName ?: "устройства"}"
        com.example.policyswitcher.model.AssistantActionType.APPLY_TO_ALL -> "Всем → ${policyName ?: "Политика"}"
        com.example.policyswitcher.model.AssistantActionType.REMOVE_FROM_ALL -> "Снять у всех ${policyName ?: ""}".trim()
    }
}

private fun actionTypeLabel(type: com.example.policyswitcher.model.AssistantActionType): String = when (type) {
    com.example.policyswitcher.model.AssistantActionType.APPLY_TO_DEVICE -> "На устройство"
    com.example.policyswitcher.model.AssistantActionType.REMOVE_FROM_DEVICE -> "Снять с устройства"
    com.example.policyswitcher.model.AssistantActionType.APPLY_TO_ALL -> "Для всех"
    com.example.policyswitcher.model.AssistantActionType.REMOVE_FROM_ALL -> "Снять у всех"
}

private fun requiresPolicy(type: com.example.policyswitcher.model.AssistantActionType): Boolean = when (type) {
    com.example.policyswitcher.model.AssistantActionType.REMOVE_FROM_DEVICE -> false
    com.example.policyswitcher.model.AssistantActionType.APPLY_TO_DEVICE -> true
    com.example.policyswitcher.model.AssistantActionType.APPLY_TO_ALL -> true
    com.example.policyswitcher.model.AssistantActionType.REMOVE_FROM_ALL -> true
}

private fun requiresDevice(type: com.example.policyswitcher.model.AssistantActionType): Boolean = when (type) {
    com.example.policyswitcher.model.AssistantActionType.APPLY_TO_DEVICE -> true
    com.example.policyswitcher.model.AssistantActionType.REMOVE_FROM_DEVICE -> true
    else -> false
}
