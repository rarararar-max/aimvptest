package com.yourname.aichatmvptest

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yourname.aichatmvptest.audio.LocalAudioController
import com.yourname.aichatmvptest.audio.RecordedAudio
import com.yourname.aichatmvptest.security.KeystoreCrypto
import com.yourname.aichatmvptest.settings.EncryptedSettingsRepository
import com.yourname.aichatmvptest.shared.data.SeedData
import com.yourname.aichatmvptest.shared.data.DefaultModelConfigs
import com.yourname.aichatmvptest.shared.database.AndroidDatabaseFactory
import com.yourname.aichatmvptest.shared.database.LocalChatRepository
import com.yourname.aichatmvptest.shared.database.LocalSettingsRepository
import com.yourname.aichatmvptest.shared.database.LocalVectorStoreGateway
import com.yourname.aichatmvptest.shared.model.AiCharacter
import com.yourname.aichatmvptest.shared.model.ChatMessage
import com.yourname.aichatmvptest.shared.model.Conversation
import com.yourname.aichatmvptest.shared.model.MessageContent
import com.yourname.aichatmvptest.shared.model.MessageStatus
import com.yourname.aichatmvptest.shared.model.MessageType
import com.yourname.aichatmvptest.shared.model.ModelConfig
import com.yourname.aichatmvptest.shared.model.ModelType
import com.yourname.aichatmvptest.shared.model.SenderType
import com.yourname.aichatmvptest.shared.modelgateway.ChatModelRequest
import com.yourname.aichatmvptest.shared.modelgateway.FakeModelGateway
import com.yourname.aichatmvptest.shared.modelgateway.ModelGateway
import com.yourname.aichatmvptest.shared.modelgateway.VisionAnalyzeRequest
import com.yourname.aichatmvptest.shared.modelgateway.createAliyunQwenVlGateway
import com.yourname.aichatmvptest.shared.modelgateway.createAliyunTextEmbeddingGateway
import com.yourname.aichatmvptest.shared.modelgateway.createMinimaxTtsGateway
import com.yourname.aichatmvptest.shared.modelgateway.createOpenAiCompatibleGateway
import com.yourname.aichatmvptest.shared.repository.ChatRepository
import com.yourname.aichatmvptest.shared.repository.SettingsRepository
import com.yourname.aichatmvptest.shared.voice.TtsRequest
import com.yourname.aichatmvptest.shared.vector.VectorMemory
import com.yourname.aichatmvptest.shared.vector.VectorStoreGateway
import com.yourname.aichatmvptest.shared.call.CallState
import com.yourname.aichatmvptest.ui.theme.MvptestTheme
import com.yourname.aichatmvptest.notification.RhodesNotificationCenter
import com.yourname.aichatmvptest.worker.ProactiveMessageScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.content.FileProvider

class MainActivity : ComponentActivity() {
    private var imageResultHandler: ((String) -> Unit)? = null
    private var pendingCameraUri: Uri? = null
    private var recordingResultHandler: ((RecordedAudio?) -> Unit)? = null
    private lateinit var audioController: LocalAudioController

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { imageResultHandler?.invoke(it.toString()) }
        imageResultHandler = null
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { imageResultHandler?.invoke(it.toString()) }
        pendingCameraUri = null
        imageResultHandler = null
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val recordAudioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        recordingResultHandler?.invoke(if (granted && audioController.startRecording()) null else null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        audioController = LocalAudioController(applicationContext)
        RhodesNotificationCenter.ensureChannels(this)
        ProactiveMessageScheduler.schedule(this)
        val database = AndroidDatabaseFactory(applicationContext).createDatabase()
        val chatRepository = LocalChatRepository(database)
        val vectorStoreGateway = LocalVectorStoreGateway(database)
        val settingsRepository = EncryptedSettingsRepository(
            delegate = LocalSettingsRepository(database),
            crypto = KeystoreCrypto(),
        )
        setContent {
            MvptestTheme {
                RhodesApp(
                    chatRepository = chatRepository,
                    settingsRepository = settingsRepository,
                    vectorStoreGateway = vectorStoreGateway,
                    onPickImage = { handler -> pickImage(handler) },
                    onTakePhoto = { handler -> takePhoto(handler) },
                    onPrepareImageForModel = { uri -> prepareImageForModel(uri) },
                    onStartRecording = { handler -> startRecording(handler) },
                    onStopRecording = { stopRecording() },
                    onPlayVoice = { path -> audioController.play(path) },
                    onSaveTtsAudio = { bytes, extension -> audioController.saveTtsAudio(bytes, extension) },
                    onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() },
                )
            }
        }
    }

    private fun pickImage(handler: (String) -> Unit) {
        imageResultHandler = handler
        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun takePhoto(handler: (String) -> Unit) {
        imageResultHandler = handler
        val dir = File(cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        pendingCameraUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        takePictureLauncher.launch(pendingCameraUri)
    }

    private fun prepareImageForModel(uriText: String): String? {
        if (uriText.startsWith("http://") || uriText.startsWith("https://") || uriText.startsWith("data:")) return uriText
        return runCatching {
            val uri = Uri.parse(uriText)
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startRecording(handler: (RecordedAudio?) -> Unit) {
        recordingResultHandler = handler
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            handler(if (audioController.startRecording()) null else null)
        }
    }

    private fun stopRecording(): RecordedAudio? = audioController.stopRecording()
}

private enum class MainTab(val title: String) {
    Chats("聊天"),
    Contacts("通讯录"),
    Features("功能"),
    Settings("设置"),
}

private enum class ActiveCallType(val title: String) {
    Voice("语音通话"),
    Video("视频通话"),
}

private val RhodesInk = Color(0xFF111820)
private val RhodesLine = Color(0x553C6A92)
private val RhodesPanel = Color(0xF7FFFFFF)
private val RhodesBlue = Color(0xFF245C8F)
private val RhodesGreen = Color(0xFF287565)
private val RhodesSoftBg = Brush.verticalGradient(
    listOf(Color(0xFFE7F2FF), Color(0xFFF7F8FA), Color(0xFFEAF5F1))
)
private val RhodesPanelBrush = Brush.linearGradient(
    listOf(Color(0xFFFFFFFF), Color(0xFFEAF3FF))
)

@Composable
private fun RhodesApp(
    chatRepository: ChatRepository,
    settingsRepository: SettingsRepository,
    vectorStoreGateway: VectorStoreGateway,
    onPickImage: ((String) -> Unit) -> Unit,
    onTakePhoto: ((String) -> Unit) -> Unit,
    onPrepareImageForModel: (String) -> String?,
    onStartRecording: ((RecordedAudio?) -> Unit) -> Unit,
    onStopRecording: () -> RecordedAudio?,
    onPlayVoice: (String) -> Unit,
    onSaveTtsAudio: (ByteArray, String) -> RecordedAudio?,
    onRequestNotificationPermission: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(MainTab.Chats) }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var activeCall by remember { mutableStateOf<Pair<Conversation, ActiveCallType>?>(null) }
    var isLoaded by remember { mutableStateOf(false) }
    val characters = remember { mutableStateListOf<AiCharacter>() }
    val conversations = remember { mutableStateListOf<Conversation>() }
    val modelConfigs = remember { mutableStateListOf<ModelConfig>() }
    val messagesByConversation = remember { mutableStateMapOf<String, List<ChatMessage>>() }
    val fakeGateway = remember { FakeModelGateway() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        chatRepository.seedDefaultsIfNeeded()
        seedModelConfigsIfNeeded(settingsRepository)
        characters.replaceAllWith(chatRepository.getCharacters())
        conversations.replaceAllWith(chatRepository.getConversations())
        modelConfigs.replaceAllWith(settingsRepository.getModelConfigs())
        onRequestNotificationPermission()
        isLoaded = true
    }

    activeCall?.let { (conversation, callType) ->
        val character = characters.firstOrNull { it.id == conversation.characterId }
            ?: SeedData.characters.first { it.id == conversation.characterId }
        when (callType) {
            ActiveCallType.Voice -> VoiceCallScreen(character = character, onEnd = { activeCall = null })
            ActiveCallType.Video -> VideoCallScreen(character = character, onEnd = { activeCall = null })
        }
        return
    }

    activeConversation?.let { conversation ->
        LaunchedEffect(conversation.id) {
            messagesByConversation[conversation.id] = chatRepository.getMessages(conversation.id)
        }
        val conversationMessages = messagesByConversation[conversation.id].orEmpty()
        ChatDetailScreen(
            conversation = conversation,
            character = characters.firstOrNull { it.id == conversation.characterId }
                ?: SeedData.characters.first { it.id == conversation.characterId },
            messages = conversationMessages,
            onBack = { activeConversation = null },
            onPickImage = {
                onPickImage { uri ->
                    scope.launch {
                        val message = buildLocalMessage(
                            conversation = conversation,
                            senderType = SenderType.User,
                            senderId = "local_user",
                            messageType = MessageType.Image,
                            content = MessageContent.Image(uri = uri, width = 0, height = 0),
                        )
                        messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + message
                        chatRepository.saveMessage(message)
                        conversations.replaceAllWith(chatRepository.getConversations())
                        analyzeImageAndReply(
                            configs = modelConfigs,
                            imageForModel = onPrepareImageForModel(uri) ?: uri,
                            conversation = conversation,
                            messagesByConversation = messagesByConversation,
                            chatRepository = chatRepository,
                        )
                        conversations.replaceAllWith(chatRepository.getConversations())
                    }
                }
            },
            onTakePhoto = {
                onTakePhoto { uri ->
                    scope.launch {
                        val message = buildLocalMessage(
                            conversation = conversation,
                            senderType = SenderType.User,
                            senderId = "local_user",
                            messageType = MessageType.Image,
                            content = MessageContent.Image(uri = uri, width = 0, height = 0),
                        )
                        messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + message
                        chatRepository.saveMessage(message)
                        conversations.replaceAllWith(chatRepository.getConversations())
                        analyzeImageAndReply(
                            configs = modelConfigs,
                            imageForModel = onPrepareImageForModel(uri) ?: uri,
                            conversation = conversation,
                            messagesByConversation = messagesByConversation,
                            chatRepository = chatRepository,
                        )
                        conversations.replaceAllWith(chatRepository.getConversations())
                    }
                }
            },
            onSendVoice = {
                onStartRecording {
                    // Permission result only starts recording. The actual message is created when user taps stop.
                }
            },
            onStopVoice = {
                onStopRecording()?.let { audio ->
                    scope.launch {
                        val message = buildLocalMessage(
                            conversation = conversation,
                            senderType = SenderType.User,
                            senderId = "local_user",
                            messageType = MessageType.Voice,
                            content = MessageContent.Voice(localPath = audio.path, durationMs = audio.durationMs, text = "本地录音"),
                        )
                        messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + message
                        chatRepository.saveMessage(message)
                        conversations.replaceAllWith(chatRepository.getConversations())
                    }
                }
            },
            onPlayVoice = onPlayVoice,
            onSendGift = {
                scope.launch {
                    val message = buildLocalMessage(
                        conversation = conversation,
                        senderType = SenderType.User,
                        senderId = "local_user",
                        messageType = MessageType.Gift,
                        content = MessageContent.Gift(giftType = "drink", name = "热拿铁"),
                    )
                    messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + message
                    chatRepository.saveMessage(message)
                    conversations.replaceAllWith(chatRepository.getConversations())
                }
            },
            onStartVoiceCall = { activeCall = conversation to ActiveCallType.Voice },
            onStartVideoCall = { activeCall = conversation to ActiveCallType.Video },
            onSend = { text ->
                val now = System.currentTimeMillis()
                val userMessage =
                    ChatMessage(
                        id = "user_$now",
                        conversationId = conversation.id,
                        senderType = SenderType.User,
                        senderId = "local_user",
                        messageType = MessageType.Text,
                        content = MessageContent.Text(text),
                        status = MessageStatus.Sent,
                        createdAtMillis = now,
                    )
                messagesByConversation[conversation.id] = conversationMessages + userMessage
                scope.launch {
                    chatRepository.saveMessage(userMessage)
                    conversations.replaceAllWith(chatRepository.getConversations())
                    val gateway = createChatGateway(modelConfigs, fakeGateway)
                    val reply = gateway.chat(
                        ChatModelRequest(
                            characterId = conversation.characterId,
                            userText = text,
                            recentMessages = messagesByConversation[conversation.id].orEmpty().takeLast(10).mapNotNull { message ->
                                (message.content as? MessageContent.Text)?.text
                            },
                        )
                    )
                    saveMemoryHints(
                        configs = modelConfigs,
                        characterId = conversation.characterId,
                        sourceMessageId = userMessage.id,
                        memoryHints = reply.memoryHints,
                        vectorStoreGateway = vectorStoreGateway,
                    )
                    reply.messages.forEachIndexed { index, segment ->
                        delay(segment.delayMs)
                        val aiMessage = buildAiReplyMessage(
                            configs = modelConfigs,
                            conversation = conversation,
                            segmentText = segment.text.orEmpty(),
                            segmentType = segment.type,
                            saveTtsAudio = onSaveTtsAudio,
                            index = index,
                        )
                        messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + aiMessage
                        chatRepository.saveMessage(aiMessage)
                        conversations.replaceAllWith(chatRepository.getConversations())
                    }
                }
            },
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .border(1.dp, RhodesLine, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                containerColor = Color.White.copy(alpha = 0.96f),
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.title.take(1)) },
                        label = { Text(tab.title) },
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RhodesSoftBg)
            ) {
                if (!isLoaded) {
                LoadingScreen()
                } else when (selectedTab) {
                    MainTab.Chats -> ChatListScreen(
                        conversations = conversations,
                        characters = characters,
                        onOpen = { activeConversation = it },
                    )
                    MainTab.Contacts -> ContactsScreen(characters = characters)
                    MainTab.Features -> FeaturesScreen()
                    MainTab.Settings -> SettingsScreen(
                        modelConfigs = modelConfigs,
                        onSaveModelConfig = { config ->
                            scope.launch {
                                settingsRepository.saveModelConfig(config)
                                modelConfigs.replaceAllWith(settingsRepository.getModelConfigs())
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatListScreen(
    conversations: List<Conversation>,
    characters: List<AiCharacter>,
    onOpen: (Conversation) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopTitle("罗德岛通讯端")
        LazyColumn {
            items(conversations) { conversation ->
                ConversationRow(
                    conversation = conversation,
                    character = characters.first { it.id == conversation.characterId },
                    onClick = { onOpen(conversation) },
                )
                HorizontalDivider(color = Color(0xFFE1E5EA))
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, character: AiCharacter, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, RhodesLine, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .background(RhodesPanel)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(character.name)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(conversation.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(character.title, color = Color(0xFF7A8794), fontSize = 12.sp, maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            Text(conversation.lastMessage, color = Color(0xFF7A8794), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(conversation.updatedAtText, color = Color(0xFF98A2AD), fontSize = 12.sp)
            if (conversation.unreadCount > 0) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE54D42)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(conversation.unreadCount.toString(), color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ContactsScreen(characters: List<AiCharacter>) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopTitle("通讯录")
        LazyColumn {
            items(characters) { character ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, RhodesLine, RoundedCornerShape(18.dp))
                        .background(RhodesPanel)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(character.name)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(character.name, fontWeight = FontWeight.SemiBold)
                        Text(character.description, color = Color(0xFF6F7B87), fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = Color(0xFFE1E5EA))
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("正在接入本地通讯数据库...")
    }
}

@Composable
private fun FeaturesScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopTitle("功能")
        FeatureCard("图片识别", "接入第三方识图模型后可分析图片和视频帧。")
        FeatureCard("语音服务", "ASR 语音识别与 TTS 语音条播放预留。")
        FeatureCard("向量记忆", "Embedding 和第三方向量库配置后启用长期记忆检索。")
        FeatureCard("语音/视频通话", "半双工通话、2D 帧动画和摄像头抽帧识图预留。")
    }
}

@Composable
private fun SettingsScreen(
    modelConfigs: List<ModelConfig>,
    onSaveModelConfig: (ModelConfig) -> Unit,
) {
    var proactiveEnabled by remember { mutableStateOf(true) }
    var callLowLatencyTts by remember { mutableStateOf(true) }
    var editingConfig by remember { mutableStateOf<ModelConfig?>(null) }

    editingConfig?.let { config ->
        ModelConfigEditor(
            config = config,
            onBack = { editingConfig = null },
            onSave = { saved ->
                onSaveModelConfig(saved)
                editingConfig = null
            },
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopTitle("设置")
        LazyColumn {
            item { SectionTitle("模型服务配置") }
            items(modelConfigs) { config ->
                ModelConfigCard(
                    title = "${config.provider} · ${config.modelType.name}",
                    body = "Model: ${config.modelName.ifBlank { "未配置" }}\nBase URL: ${config.baseUrl.ifBlank { "待填写" }}\n状态: ${if (config.enabled) "启用" else "待配置 API Key"}",
                    onClick = { editingConfig = config },
                )
            }
            item { SectionTitle("第三方向量库") }
            item {
                FeatureCard(
                    "${DefaultModelConfigs.vectorStore.provider} · ${DefaultModelConfigs.vectorStore.collectionName}",
                    "Base URL: ${DefaultModelConfigs.vectorStore.baseUrl.ifBlank { "待填写" }}\nNamespace: ${DefaultModelConfigs.vectorStore.namespace.orEmpty()}\nEmbedding 默认：阿里 text-embedding-v4",
                )
            }
            item { SectionTitle("语音策略") }
            item {
                SettingSwitchRow(
                    title = "通话优先低延迟 TTS",
                    body = "语音条用同步/异步合成；语音和视频通话优先流式或短句同步队列。",
                    checked = callLowLatencyTts,
                    onCheckedChange = { callLowLatencyTts = it },
                )
            }
            item { SectionTitle("通知与后台") }
            item {
                SettingSwitchRow(
                    title = "允许主动消息",
                    body = "WorkManager 每 15 分钟检查一次。后续会接入角色频率、免打扰和 LLM 主动消息。",
                    checked = proactiveEnabled,
                    onCheckedChange = { proactiveEnabled = it },
                )
            }
            item { FeatureCard("数据管理", "后续加入本地导入导出、清空聊天、清空记忆和 API Key 清理。") }
        }
    }
}

@Composable
private fun ModelConfigEditor(config: ModelConfig, onBack: () -> Unit, onSave: (ModelConfig) -> Unit) {
    var provider by remember { mutableStateOf(config.provider) }
    var baseUrl by remember { mutableStateOf(config.baseUrl) }
    var apiKey by remember { mutableStateOf(config.apiKeyMasked) }
    var modelName by remember { mutableStateOf(config.modelName) }
    var enabled by remember { mutableStateOf(config.enabled) }
    var testResult by remember { mutableStateOf("未测试") }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RhodesSoftBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("返回") }
                Text("模型配置", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        item { Text("类型：${config.modelType.name}", color = Color(0xFF5D6875)) }
        item { ConfigTextField("服务商", provider, { provider = it }) }
        item { ConfigTextField("Base URL", baseUrl, { baseUrl = it }) }
        item { ConfigTextField("API Key / Token", apiKey, { apiKey = it }) }
        item { ConfigTextField("Model Name / Voice ID", modelName, { modelName = it }) }
        item {
            SettingSwitchRow(
                title = "启用此配置",
                body = "保存后将作为 ${config.modelType.name} 的候选配置。API Key 后续会接入 Android Keystore 加密。",
                checked = enabled,
                onCheckedChange = { enabled = it },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        scope.launch {
                            testResult = "测试中..."
                            testResult = testModelConfig(
                                config.copy(
                                    provider = provider,
                                    baseUrl = baseUrl,
                                    apiKeyMasked = apiKey,
                                    modelName = modelName,
                                    enabled = enabled,
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("测试连接") }
                Button(
                    onClick = {
                        onSave(
                            config.copy(
                                provider = provider,
                                baseUrl = baseUrl,
                                apiKeyMasked = apiKey,
                                modelName = modelName,
                                enabled = enabled,
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("保存配置") }
            }
        }
        item {
            FeatureCard("测试结果", testResult)
        }
    }
}

@Composable
private fun ConfigTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = false,
    )
}

@Composable
private fun ModelConfigCard(title: String, body: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, RhodesLine, RoundedCornerShape(16.dp))
            .background(RhodesPanel)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("编辑", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(body, color = Color(0xFF6F7B87), fontSize = 14.sp)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        color = Color(0xFF5D6875),
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingSwitchRow(title: String, body: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, RhodesLine, RoundedCornerShape(16.dp))
            .background(RhodesPanel)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(body, color = Color(0xFF6F7B87), fontSize = 14.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FeatureCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, RhodesLine, RoundedCornerShape(16.dp))
            .background(RhodesPanel)
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text(body, color = Color(0xFF6F7B87), fontSize = 14.sp)
    }
}

@Composable
private fun ChatDetailScreen(
    conversation: Conversation,
    character: AiCharacter,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onSendVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onPlayVoice: (String) -> Unit,
    onSendGift: () -> Unit,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onSend: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var showPlusPanel by remember { mutableStateOf(false) }
    var voiceMode by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(RhodesPanelBrush)
                    .border(1.dp, RhodesLine)
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("返回") }
                Avatar(character.name)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(conversation.title, fontWeight = FontWeight.SemiBold)
                    Text("在线 · ${character.title}", color = Color(0xFF6F7B87), fontSize = 12.sp)
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RhodesPanelBrush)
                    .border(1.dp, RhodesLine)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { voiceMode = !voiceMode }) { Text(if (voiceMode) "键盘" else "语音") }
                    if (voiceMode) {
                        Button(
                            onClick = {
                                if (recording) {
                                    recording = false
                                    onStopVoice()
                                } else {
                                    recording = true
                                    onSendVoice()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(if (recording) "正在录音，点击发送" else "点击开始录音") }
                    } else {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("发送消息") },
                            singleLine = true,
                        )
                    }
                    TextButton(onClick = { showPlusPanel = !showPlusPanel }) { Text("+") }
                    Button(
                        onClick = {
                            val text = input.trim()
                            if (text.isNotEmpty()) {
                                input = ""
                                onSend(text)
                            }
                        },
                        enabled = !voiceMode,
                    ) {
                        Text("发送")
                    }
                }
                if (showPlusPanel) {
                    PlusPanel(
                        onAlbum = onPickImage,
                        onCamera = onTakePhoto,
                        onGift = onSendGift,
                        onVoiceCall = onStartVoiceCall,
                        onVideoCall = onStartVideoCall,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(RhodesSoftBg)
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages) { message ->
                MessageBubble(message, onPlayVoice = onPlayVoice)
            }
        }
    }
}

@Composable
private fun PlusPanel(
    onAlbum: () -> Unit,
    onCamera: () -> Unit,
    onGift: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.55f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { PlusAction("相册", "图片", onAlbum) }
        item { PlusAction("拍摄", "相机", onCamera) }
        item { PlusAction("饮品", "拿铁", onGift) }
        item { PlusAction("语音通话", "Call", onVoiceCall) }
        item { PlusAction("视频通话", "Video", onVideoCall) }
    }
}

@Composable
private fun PlusAction(title: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, RhodesLine, RoundedCornerShape(16.dp))
                .background(RhodesPanel)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(6.dp))
        Text(title, fontSize = 12.sp, color = Color(0xFF5D6875))
    }
}

@Composable
private fun VoiceCallScreen(character: AiCharacter, onEnd: () -> Unit) {
    var state by remember { mutableStateOf(CallState.Calling) }
    LaunchedEffect(Unit) {
        delay(800)
        state = CallState.Connected
    }
    CallScaffold(
        title = character.name,
        subtitle = "${state.name} · 半双工语音通话",
        primary = "AI 正在等待你的语音输入。MVP 下一步接入 ASR + LLM + Minimax TTS。",
        showCameraPreview = false,
        onEnd = onEnd,
    )
}

@Composable
private fun VideoCallScreen(character: AiCharacter, onEnd: () -> Unit) {
    var state by remember { mutableStateOf(CallState.Connected) }
    CallScaffold(
        title = character.name,
        subtitle = "${state.name} · 视频通话",
        primary = "AI 画面区域：2D 图片序列帧动画占位。\n用户小窗：CameraX 预览下一步接入。\n视觉理解：默认 qwen3-vl-plus 抽帧识图。",
        showCameraPreview = true,
        onEnd = onEnd,
    )
}

@Composable
private fun CallScaffold(
    title: String,
    subtitle: String,
    primary: String,
    showCameraPreview: Boolean,
    onEnd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1420), Color(0xFF17263A), Color(0xFF0F1B17))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(28.dp))
            Avatar(title)
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFFB8C7D9), fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showCameraPreview) 360.dp else 260.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x66B8C7D9), RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF263445), Color(0xFF1B2A3A))))
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(primary, color = Color.White, lineHeight = 22.sp)
            if (showCameraPreview) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(96.dp, 132.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x77FFFFFF), RoundedCornerShape(16.dp))
                        .background(Color(0xFF3C4E63)),
                    contentAlignment = Alignment.Center,
                ) { Text("用户\n摄像头", color = Color.White, fontSize = 12.sp) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {}) { Text("静音") }
            Button(onClick = {}) { Text("扬声器") }
            Button(onClick = onEnd) { Text("挂断") }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onPlayVoice: (String) -> Unit) {
    val isUser = message.senderType == SenderType.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        val bubbleColor = if (isUser) Color(0xFF95EC69) else Color.White
        val content = message.content
        val text = when (content) {
            is MessageContent.Text -> content.text
            is MessageContent.Image -> "图片已发送\n${content.uri.take(42)}"
            is MessageContent.Voice -> "语音 ${content.durationMs / 1000}s  ▶"
            is MessageContent.Gift -> "${content.name}  已送达"
            is MessageContent.Call -> "${content.callType} · ${content.status}"
        }
        val shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 16.dp,
        )
        Column(
            modifier = Modifier
                .clip(shape)
                .border(1.dp, if (isUser) Color(0x663C8B34) else RhodesLine, shape)
                .background(bubbleColor)
                .clickable(enabled = content is MessageContent.Voice) {
                    val voice = content as? MessageContent.Voice
                    voice?.let { onPlayVoice(it.localPath) }
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .width(260.dp),
        ) {
            if (content is MessageContent.Image) {
                AsyncImage(
                    model = content.uri,
                    contentDescription = "图片消息",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFBBD7FF), Color(0xFFDCE8F8))))
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(text = text, color = Color(0xFF111820), fontSize = 15.sp)
        }
    }
}

@Composable
private fun TopTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(RhodesPanelBrush)
            .border(1.dp, RhodesLine)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun Avatar(name: String) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF245C8F), Color(0xFF287565)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

private fun <T> MutableList<T>.replaceAllWith(values: List<T>) {
    clear()
    addAll(values)
}

private fun buildLocalMessage(
    conversation: Conversation,
    senderType: SenderType,
    senderId: String,
    messageType: MessageType,
    content: MessageContent,
): ChatMessage {
    val now = System.currentTimeMillis()
    return ChatMessage(
        id = "msg_$now",
        conversationId = conversation.id,
        senderType = senderType,
        senderId = senderId,
        messageType = messageType,
        content = content,
        status = MessageStatus.Sent,
        createdAtMillis = now,
    )
}

@Preview(showBackground = true)
@Composable
private fun RhodesAppPreview() {
    MvptestTheme {
        RhodesApp(
            chatRepository = PreviewChatRepository(),
            settingsRepository = PreviewSettingsRepository(),
            vectorStoreGateway = PreviewVectorStoreGateway(),
            onPickImage = { handler -> handler("preview://album") },
            onTakePhoto = { handler -> handler("preview://camera") },
            onPrepareImageForModel = { it },
            onStartRecording = { handler -> handler(null) },
            onStopRecording = { RecordedAudio(path = "preview://voice", durationMs = 3200L) },
            onPlayVoice = {},
            onSaveTtsAudio = { _, _ -> null },
            onRequestNotificationPermission = {},
        )
    }
}

private class PreviewChatRepository : ChatRepository {
    override suspend fun seedDefaultsIfNeeded() = Unit
    override suspend fun getCharacters(): List<AiCharacter> = SeedData.characters
    override suspend fun getConversations(): List<Conversation> = SeedData.conversations
    override suspend fun getMessages(conversationId: String): List<ChatMessage> = listOf(
        ChatMessage(
            id = "preview_msg",
            conversationId = conversationId,
            senderType = SenderType.Ai,
            senderId = "medic",
            messageType = MessageType.Text,
            content = MessageContent.Text("预览通讯链路已建立。"),
            status = MessageStatus.Sent,
            createdAtMillis = 0L,
        )
    )
    override suspend fun saveMessage(message: ChatMessage) = Unit
}

private class PreviewSettingsRepository : SettingsRepository {
    private val configs = DefaultModelConfigs.allModelConfigs.toMutableList()
    override suspend fun getModelConfigs(): List<ModelConfig> = configs
    override suspend fun saveModelConfig(config: ModelConfig) {
        configs.removeAll { it.id == config.id }
        configs.add(config)
    }
    override suspend fun getVectorStoreConfig() = DefaultModelConfigs.vectorStore
    override suspend fun saveVectorStoreConfig(config: com.yourname.aichatmvptest.shared.model.VectorStoreConfig) = Unit
}

private class PreviewVectorStoreGateway : VectorStoreGateway {
    override suspend fun upsert(memory: VectorMemory) = Unit
    override suspend fun search(request: com.yourname.aichatmvptest.shared.vector.VectorSearchRequest): List<VectorMemory> = emptyList()
    override suspend fun delete(memoryId: String) = Unit
    override suspend fun clearCharacterMemory(characterId: String) = Unit
}

private suspend fun seedModelConfigsIfNeeded(settingsRepository: SettingsRepository) {
    if (settingsRepository.getModelConfigs().isNotEmpty()) return
    DefaultModelConfigs.allModelConfigs.forEach { settingsRepository.saveModelConfig(it) }
    settingsRepository.saveVectorStoreConfig(DefaultModelConfigs.vectorStore)
}

private fun createChatGateway(configs: List<ModelConfig>, fallback: FakeModelGateway): ModelGateway {
    val llm = configs.firstOrNull { it.modelType == ModelType.Llm && it.enabled }
    if (llm == null || llm.apiKeyMasked.isBlank() || llm.baseUrl.isBlank() || llm.modelName.isBlank()) {
        return fallback
    }
    return createOpenAiCompatibleGateway(
        baseUrl = llm.baseUrl,
        apiKey = llm.apiKeyMasked,
        modelName = llm.modelName,
    )
}

private suspend fun analyzeImageAndReply(
    configs: List<ModelConfig>,
    imageForModel: String,
    conversation: Conversation,
    messagesByConversation: MutableMap<String, List<ChatMessage>>,
    chatRepository: ChatRepository,
) {
    val vision = configs.firstOrNull { it.modelType == ModelType.Vision && it.enabled }
    if (vision == null || vision.baseUrl.isBlank() || vision.apiKeyMasked.isBlank()) return

    val result = runCatching {
        createAliyunQwenVlGateway(
            endpoint = vision.baseUrl,
            apiKey = vision.apiKeyMasked,
            modelName = vision.modelName.ifBlank { "qwen3-vl-plus" },
        ).analyzeImage(
            com.yourname.aichatmvptest.shared.modelgateway.VisionAnalyzeRequest(
                imageUrlOrBase64 = imageForModel,
                prompt = "请用自然聊天语气简要描述这张图片，并指出你观察到的重点。",
            )
        )
    }.getOrNull() ?: return

    val aiMessage = buildLocalMessage(
        conversation = conversation,
        senderType = SenderType.Ai,
        senderId = conversation.characterId,
        messageType = MessageType.Text,
        content = MessageContent.Text("我看了一下图片：${result.text}"),
    )
    messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + aiMessage
    chatRepository.saveMessage(aiMessage)
}

private suspend fun saveMemoryHints(
    configs: List<ModelConfig>,
    characterId: String,
    sourceMessageId: String,
    memoryHints: List<com.yourname.aichatmvptest.shared.modelgateway.MemoryHint>,
    vectorStoreGateway: VectorStoreGateway,
) {
    if (memoryHints.isEmpty()) return
    val embeddingConfig = configs.firstOrNull { it.modelType == ModelType.Embedding && it.enabled }

    memoryHints.forEach { hint ->
        val embedding = if (embeddingConfig != null && embeddingConfig.baseUrl.isNotBlank() && embeddingConfig.apiKeyMasked.isNotBlank()) {
            runCatching {
                createAliyunTextEmbeddingGateway(
                    endpoint = embeddingConfig.baseUrl,
                    apiKey = embeddingConfig.apiKeyMasked,
                    modelName = embeddingConfig.modelName.ifBlank { "text-embedding-v4" },
                ).embed(hint.content)
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        vectorStoreGateway.upsert(
            VectorMemory(
                id = "memory_${System.currentTimeMillis()}_${hint.type}",
                characterId = characterId,
                content = hint.content,
                importance = hint.importance,
                embedding = embedding,
                metadata = mapOf(
                    "type" to hint.type,
                    "source_message_id" to sourceMessageId,
                ),
            )
        )
    }
}

private suspend fun buildAiReplyMessage(
    configs: List<ModelConfig>,
    conversation: Conversation,
    segmentText: String,
    segmentType: String,
    saveTtsAudio: (ByteArray, String) -> RecordedAudio?,
    index: Int,
): ChatMessage {
    if (segmentType == "voice" && segmentText.isNotBlank()) {
        val tts = configs.firstOrNull { it.modelType == ModelType.Tts && it.enabled }
        if (tts != null && tts.baseUrl.isNotBlank() && tts.apiKeyMasked.isNotBlank()) {
            val ttsModel = tts.modelName.substringBefore("|", "speech-2.8-hd").ifBlank { "speech-2.8-hd" }
            val voiceId = tts.modelName.substringAfter("|", "male-qn-qingse").ifBlank { "male-qn-qingse" }
            val audio = runCatching {
                createMinimaxTtsGateway(
                    endpoint = tts.baseUrl,
                    apiKey = tts.apiKeyMasked,
                    modelName = ttsModel,
                ).synthesize(
                    TtsRequest(
                        text = segmentText,
                        voiceId = voiceId,
                        format = "mp3",
                    )
                )
            }.getOrNull()
            val recorded = audio?.audioBytes?.let { saveTtsAudio(it, "mp3") }
            if (recorded != null) {
                return ChatMessage(
                    id = "ai_voice_${System.currentTimeMillis()}_$index",
                    conversationId = conversation.id,
                    senderType = SenderType.Ai,
                    senderId = conversation.characterId,
                    messageType = MessageType.Voice,
                    content = MessageContent.Voice(
                        localPath = recorded.path,
                        durationMs = recorded.durationMs,
                        text = segmentText,
                    ),
                    status = MessageStatus.Sent,
                    createdAtMillis = System.currentTimeMillis(),
                )
            }
        }
    }

    return ChatMessage(
        id = "ai_${System.currentTimeMillis()}_$index",
        conversationId = conversation.id,
        senderType = SenderType.Ai,
        senderId = conversation.characterId,
        messageType = MessageType.Text,
        content = MessageContent.Text(segmentText),
        status = MessageStatus.Sent,
        createdAtMillis = System.currentTimeMillis(),
    )
}

private suspend fun testModelConfig(config: ModelConfig): String {
    if (config.baseUrl.isBlank()) return "失败：Base URL 不能为空"
    if (config.apiKeyMasked.isBlank()) return "失败：API Key 不能为空"

    return runCatching {
        when (config.modelType) {
            ModelType.Llm -> {
                val reply = createOpenAiCompatibleGateway(
                    baseUrl = config.baseUrl,
                    apiKey = config.apiKeyMasked,
                    modelName = config.modelName.ifBlank { "deepseek-chat" },
                ).chat(
                    ChatModelRequest(
                        characterId = "test",
                        userText = "请只回复一个 JSON，用一条短消息说连接成功。",
                        recentMessages = emptyList(),
                    )
                )
                "成功：收到 ${reply.messages.size} 条回复段"
            }
            ModelType.Vision -> {
                val result = createAliyunQwenVlGateway(
                    endpoint = config.baseUrl,
                    apiKey = config.apiKeyMasked,
                    modelName = config.modelName.ifBlank { "qwen3-vl-plus" },
                ).analyzeImage(
                    VisionAnalyzeRequest(
                        imageUrlOrBase64 = "https://img.alicdn.com/imgextra/i1/O1CN01gDEY8M1W114Hi3XcN_!!6000000002727-0-tps-1024-406.jpg",
                        prompt = "请用一句话描述这张图。",
                    )
                )
                "成功：${result.text.take(80)}"
            }
            ModelType.Embedding -> {
                val embedding = createAliyunTextEmbeddingGateway(
                    endpoint = config.baseUrl,
                    apiKey = config.apiKeyMasked,
                    modelName = config.modelName.ifBlank { "text-embedding-v4" },
                ).embed("连接测试")
                "成功：向量维度 ${embedding.size}"
            }
            ModelType.Tts -> {
                val model = config.modelName.substringBefore("|", "speech-2.8-hd").ifBlank { "speech-2.8-hd" }
                val voiceId = config.modelName.substringAfter("|", "male-qn-qingse").ifBlank { "male-qn-qingse" }
                val result = createMinimaxTtsGateway(
                    endpoint = config.baseUrl,
                    apiKey = config.apiKeyMasked,
                    modelName = model,
                ).synthesize(TtsRequest(text = "连接测试", voiceId = voiceId))
                "成功：音频 ${result.audioBytes?.size ?: 0} bytes"
            }
            ModelType.Asr -> {
                "已保存：ASR 需要 PCM16k 音频样本，稍后在语音识别流程内测试"
            }
            ModelType.VectorStore -> {
                "已保存：向量库测试将在记忆检索流程内执行"
            }
        }
    }.getOrElse { error ->
        "失败：${error.message ?: error::class.simpleName}"
    }
}
