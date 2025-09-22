package com.example.policyswitcher.data

import com.example.policyswitcher.model.Client
import com.example.policyswitcher.model.Credentials
import com.example.policyswitcher.model.Policy
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

interface KeeneticRepository {
    suspend fun verifyConnection(credentials: Credentials): Result<String>
    suspend fun fetchPolicies(): List<Policy>
    suspend fun fetchClients(): List<Client>
    suspend fun applyPolicyToClient(clientId: String, policyId: String): Result<Client>
    suspend fun clearPolicyForClient(clientId: String): Result<Client>
    suspend fun applyPolicyToAll(policyId: String): Result<List<Client>>
    suspend fun clearPolicyFromAll(policyId: String?): Result<List<Client>>
    suspend fun registerClient(name: String, mac: String, ip: String?, notes: String?): Result<Client>
}

class FakeKeeneticRepository : KeeneticRepository {
    private val mutex = Mutex()
    private val policies = mutableListOf(
        Policy(id = "conform", name = "Conform", description = "Дефолтный профиль Keenetic"),
        Policy(id = "office", name = "Office"),
        Policy(id = "kids", name = "Kids"),
        Policy(id = "vpn", name = "WireGuard VPN"),
        Policy(id = "guest", name = "Guest"),
    )

    private val clients = mutableListOf(
        Client(
            id = "c1",
            name = "Ноутбук Макс",
            mac = "AA:BB:CC:DD:EE:01",
            ip = "192.168.10.20",
            policyId = "office",
            registered = true
        ),
        Client(
            id = "c2",
            name = "Планшет",
            mac = "AA:BB:CC:DD:EE:02",
            ip = "192.168.10.30",
            policyId = null,
            registered = false,
            hasPrivateMacWarning = true
        ),
        Client(
            id = "c3",
            name = "Smart TV",
            mac = "AA:BB:CC:DD:EE:03",
            ip = "192.168.10.40",
            policyId = "vpn",
            registered = true
        ),
        Client(
            id = "c4",
            name = "Смартфон Ани",
            mac = "AA:BB:CC:DD:EE:04",
            ip = "192.168.10.50",
            policyId = "kids",
            registered = true
        ),
        Client(
            id = "c5",
            name = "Printer",
            mac = "AA:BB:CC:DD:EE:05",
            ip = "192.168.10.60",
            policyId = null,
            registered = true
        )
    )

    override suspend fun verifyConnection(credentials: Credentials): Result<String> {
        delay(450)
        return if (credentials.domainOrIp.isNotBlank() && credentials.username.isNotBlank() && credentials.password.isNotBlank()) {
            Result.success(normalizeDomain(credentials.domainOrIp))
        } else {
            Result.failure(IllegalArgumentException("Введите домен, логин и пароль"))
        }
    }

    override suspend fun fetchPolicies(): List<Policy> {
        delay(250)
        return mutex.withLock { policies.map { it.copy() } }
    }

    override suspend fun fetchClients(): List<Client> {
        delay(350)
        return mutex.withLock { clients.map { it.copy() } }
    }

    override suspend fun applyPolicyToClient(clientId: String, policyId: String): Result<Client> = mutex.withLock {
        val index = clients.indexOfFirst { it.id == clientId }
        if (index == -1) {
            Result.failure(IllegalArgumentException("Клиент не найден"))
        } else {
            val updated = clients[index].copy(policyId = policyId, registered = true)
            clients[index] = updated
            delay(200)
            Result.success(updated)
        }
    }

    override suspend fun clearPolicyForClient(clientId: String): Result<Client> = mutex.withLock {
        val index = clients.indexOfFirst { it.id == clientId }
        if (index == -1) {
            Result.failure(IllegalArgumentException("Клиент не найден"))
        } else {
            val updated = clients[index].copy(policyId = null)
            clients[index] = updated
            delay(200)
            Result.success(updated)
        }
    }

    override suspend fun applyPolicyToAll(policyId: String): Result<List<Client>> = mutex.withLock {
        val updated = clients.map { client ->
            if (client.policyId == policyId) client else client.copy(policyId = policyId, registered = true)
        }
        clients.clear()
        clients.addAll(updated)
        delay(450)
        Result.success(updated.map { it.copy() })
    }

    override suspend fun clearPolicyFromAll(policyId: String?): Result<List<Client>> = mutex.withLock {
        val updated = clients.map { client ->
            if (policyId == null || client.policyId == policyId) client.copy(policyId = null) else client
        }
        clients.clear()
        clients.addAll(updated)
        delay(450)
        Result.success(updated.map { it.copy() })
    }

    override suspend fun registerClient(
        name: String,
        mac: String,
        ip: String?,
        notes: String?
    ): Result<Client> = mutex.withLock {
        if (clients.any { it.mac.equals(mac, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("MAC уже зарегистрирован"))
        }
        val client = Client(
            id = UUID.randomUUID().toString(),
            name = name,
            mac = mac.uppercase(),
            ip = ip,
            policyId = null,
            registered = true,
            notes = notes
        )
        clients += client
        delay(350)
        Result.success(client)
    }

    private fun normalizeDomain(input: String): String {
        val trimmed = input.trim()
        return if (trimmed.startsWith("http")) trimmed else "https://$trimmed"
    }
}
