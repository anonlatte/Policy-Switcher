package com.example.policyswitcher.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.policyswitcher.model.Credentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CredentialStore {
    suspend fun load(): Credentials?
    suspend fun save(credentials: Credentials)
    suspend fun saveLastSuccessfulUrl(url: String)
    suspend fun loadLastSuccessfulUrl(): String?
}

class SecureCredentialStore(context: Context) : CredentialStore {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun load(): Credentials? = withContext(Dispatchers.IO) {
        if (!prefs.contains(KEY_DOMAIN)) return@withContext null
        Credentials(
            domainOrIp = prefs.getString(KEY_DOMAIN, "") ?: "",
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            password = prefs.getString(KEY_PASSWORD, "") ?: "",
            defaultPolicyId = prefs.getString(KEY_DEFAULT_POLICY, "") ?: ""
        )
    }

    override suspend fun save(credentials: Credentials) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_DOMAIN, credentials.domainOrIp)
            .putString(KEY_USERNAME, credentials.username)
            .putString(KEY_PASSWORD, credentials.password)
            .putString(KEY_DEFAULT_POLICY, credentials.defaultPolicyId)
            .apply()
    }

    override suspend fun saveLastSuccessfulUrl(url: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_LAST_URL, url).apply()
    }

    override suspend fun loadLastSuccessfulUrl(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_LAST_URL, null)
    }

    companion object {
        private const val PREFS_NAME = "keenetic_credentials"
        private const val KEY_DOMAIN = "domain"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_DEFAULT_POLICY = "default_policy"
        private const val KEY_LAST_URL = "last_url"
    }
}

class InMemoryCredentialStore : CredentialStore {
    private var credentials: Credentials? = null
    private var lastUrl: String? = null

    override suspend fun load(): Credentials? = credentials

    override suspend fun save(credentials: Credentials) {
        this.credentials = credentials
    }

    override suspend fun saveLastSuccessfulUrl(url: String) {
        lastUrl = url
    }

    override suspend fun loadLastSuccessfulUrl(): String? = lastUrl
}
