package com.yourname.aichatmvptest.settings

import com.yourname.aichatmvptest.security.KeystoreCrypto
import com.yourname.aichatmvptest.shared.model.ModelConfig
import com.yourname.aichatmvptest.shared.model.VectorStoreConfig
import com.yourname.aichatmvptest.shared.repository.SettingsRepository

class EncryptedSettingsRepository(
    private val delegate: SettingsRepository,
    private val crypto: KeystoreCrypto,
) : SettingsRepository {
    override suspend fun getModelConfigs(): List<ModelConfig> {
        return delegate.getModelConfigs().map { config ->
            config.copy(apiKeyMasked = crypto.decryptIfNeeded(config.apiKeyMasked))
        }
    }

    override suspend fun saveModelConfig(config: ModelConfig) {
        delegate.saveModelConfig(config.copy(apiKeyMasked = crypto.encryptIfNeeded(config.apiKeyMasked)))
    }

    override suspend fun getVectorStoreConfig(): VectorStoreConfig? {
        return delegate.getVectorStoreConfig()?.let { config ->
            config.copy(apiKeyMasked = crypto.decryptIfNeeded(config.apiKeyMasked))
        }
    }

    override suspend fun saveVectorStoreConfig(config: VectorStoreConfig) {
        delegate.saveVectorStoreConfig(config.copy(apiKeyMasked = crypto.encryptIfNeeded(config.apiKeyMasked)))
    }
}
