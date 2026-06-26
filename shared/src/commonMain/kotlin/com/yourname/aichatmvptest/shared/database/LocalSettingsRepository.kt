package com.yourname.aichatmvptest.shared.database

import com.yourname.aichatmvptest.shared.model.ModelConfig
import com.yourname.aichatmvptest.shared.model.ModelType
import com.yourname.aichatmvptest.shared.model.VectorStoreConfig
import com.yourname.aichatmvptest.shared.repository.SettingsRepository

class LocalSettingsRepository(
    private val database: RhodesDatabase,
) : SettingsRepository {
    private val queries = database.rhodesDatabaseQueries

    override suspend fun getModelConfigs(): List<ModelConfig> {
        return queries.selectModelConfigs().executeAsList().map { row ->
            ModelConfig(
                id = row.id,
                provider = row.provider,
                modelType = ModelType.valueOf(row.model_type),
                baseUrl = row.base_url,
                apiKeyMasked = row.api_key_encrypted,
                modelName = row.model_name,
                enabled = row.enabled == 1L,
            )
        }
    }

    override suspend fun saveModelConfig(config: ModelConfig) {
        queries.upsertModelConfig(
            id = config.id,
            provider = config.provider,
            model_type = config.modelType.name,
            base_url = config.baseUrl,
            api_key_encrypted = config.apiKeyMasked,
            model_name = config.modelName,
            enabled = if (config.enabled) 1L else 0L,
            updated_at = nowMillis(),
        )
    }

    override suspend fun getVectorStoreConfig(): VectorStoreConfig? {
        return queries.selectEnabledVectorStoreConfig().executeAsOneOrNull()?.let { row ->
            VectorStoreConfig(
                provider = row.provider,
                baseUrl = row.base_url,
                apiKeyMasked = row.api_key_encrypted,
                collectionName = row.collection_name,
                namespace = row.namespace,
            )
        }
    }

    override suspend fun saveVectorStoreConfig(config: VectorStoreConfig) {
        queries.upsertVectorStoreConfig(
            id = "default_vector_store",
            provider = config.provider,
            base_url = config.baseUrl,
            api_key_encrypted = config.apiKeyMasked,
            collection_name = config.collectionName,
            namespace = config.namespace,
            enabled = 1L,
            updated_at = nowMillis(),
        )
    }

    private fun nowMillis(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
}
