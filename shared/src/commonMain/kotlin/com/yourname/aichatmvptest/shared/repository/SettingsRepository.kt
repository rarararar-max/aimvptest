package com.yourname.aichatmvptest.shared.repository

import com.yourname.aichatmvptest.shared.model.ModelConfig
import com.yourname.aichatmvptest.shared.model.VectorStoreConfig

interface SettingsRepository {
    suspend fun getModelConfigs(): List<ModelConfig>
    suspend fun saveModelConfig(config: ModelConfig)
    suspend fun getVectorStoreConfig(): VectorStoreConfig?
    suspend fun saveVectorStoreConfig(config: VectorStoreConfig)
}
