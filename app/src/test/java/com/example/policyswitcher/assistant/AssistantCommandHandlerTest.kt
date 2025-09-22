package com.example.policyswitcher.assistant

import com.example.policyswitcher.model.AssistantActionType
import com.example.policyswitcher.model.Client
import com.example.policyswitcher.model.Policy
import com.example.policyswitcher.model.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantCommandHandlerTest {
    private val handler = AssistantCommandHandler()

    private val state = UiState(
        policies = listOf(
            Policy(id = "vpn", name = "WireGuard"),
            Policy(id = "office", name = "Office")
        ),
        clients = listOf(
            Client(id = "c1", name = "Ноутбук", mac = "AA:BB:CC:00:11:22"),
            Client(id = "c2", name = "Телефон", mac = "AA:BB:CC:00:11:23")
        )
    )

    @Test
    fun `parses apply command`() {
        val intent = handler.parse("Включи политику WireGuard на ноутбук")
        require(intent is AssistantIntent.ApplyToDevice)
        val execution = handler.execute(intent, state)
        require(execution is AssistantExecution.Command)
        assertEquals(AssistantActionType.APPLY_TO_DEVICE, execution.actionType)
        assertEquals("vpn", execution.policy?.id)
        assertEquals("c1", execution.client?.id)
    }

    @Test
    fun `returns error for unknown policy`() {
        val intent = handler.parse("Включи политику Unknown на ноутбук")
        require(intent is AssistantIntent.ApplyToDevice)
        val execution = handler.execute(intent, state)
        assertTrue(execution is AssistantExecution.Error)
    }
}
