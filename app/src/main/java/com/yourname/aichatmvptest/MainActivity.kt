package com.yourname.aichatmvptest

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Base64
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.viewinterop.AndroidView
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
import com.yourname.aichatmvptest.shared.modelgateway.AiReplySegment
import com.yourname.aichatmvptest.shared.modelgateway.ModelGateway
import com.yourname.aichatmvptest.shared.modelgateway.VideoCallReply
import com.yourname.aichatmvptest.shared.modelgateway.VideoCallAction
import com.yourname.aichatmvptest.shared.modelgateway.MemoryExtractionRequest
import com.yourname.aichatmvptest.shared.modelgateway.VoiceCallRequest
import com.yourname.aichatmvptest.shared.modelgateway.VisionAnalyzeRequest
import com.yourname.aichatmvptest.shared.modelgateway.createAliyunDashScopeAsrGateway
import com.yourname.aichatmvptest.shared.modelgateway.createAliyunQwenVlGateway
import com.yourname.aichatmvptest.shared.modelgateway.createAliyunTextEmbeddingGateway
import com.yourname.aichatmvptest.shared.modelgateway.createMinimaxTtsGateway
import com.yourname.aichatmvptest.shared.modelgateway.createOpenAiCompatibleGateway
import com.yourname.aichatmvptest.shared.repository.ChatRepository
import com.yourname.aichatmvptest.shared.repository.SettingsRepository
import com.yourname.aichatmvptest.shared.voice.AsrRequest
import com.yourname.aichatmvptest.shared.voice.TtsRequest
import com.yourname.aichatmvptest.shared.vector.VectorMemory
import com.yourname.aichatmvptest.shared.vector.VectorStoreGateway
import com.yourname.aichatmvptest.shared.call.CallState
import com.yourname.aichatmvptest.ui.theme.MvptestTheme
import com.yourname.aichatmvptest.notification.RhodesNotificationCenter
import com.yourname.aichatmvptest.worker.ProactiveMessageScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.File
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private var imageResultHandler: ((String) -> Unit)? = null
    private var pendingCameraUri: Uri? = null
    private var currentImageCapture: ImageCapture? = null
    private var recordingResultHandler: ((Boolean) -> Unit)? = null
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
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val recordAudioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        recordingResultHandler?.invoke(granted && audioController.startRecording())
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
                    onReadPcmFromWav = { path -> audioController.readPcmFromWav(path) },
                    onPlayVoice = { path -> audioController.play(path) },
                    onPlayVoiceAndWait = { path, onComplete -> audioController.play(path, onComplete) },
                    onStopPlayback = { audioController.stopPlayback() },
                    onHasRecentSpeech = { path -> audioController.hasRecentSpeech(path) },
                    onHasRecordingBeenSilent = { audioController.hasRecordingBeenSilent() },
                    onSaveTtsAudio = { bytes, extension -> audioController.saveTtsAudio(bytes, extension) },
                    onSetSpeakerEnabled = { enabled -> audioController.setSpeakerEnabled(enabled) },
                    onCaptureVideoFrame = { callback -> captureVideoFrame(callback) },
                    onBindCameraPreview = { previewView, imageCapture -> bindCameraPreview(previewView, imageCapture) },
                    onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() },
                    onRequestCameraPermission = { requestCameraPermissionIfNeeded() },
                    onLoadProactiveEnabled = { loadProactiveEnabled() },
                    onSaveProactiveEnabled = { enabled -> saveProactiveEnabled(enabled) },
                    onLoadModelTestPassed = { id -> loadModelTestPassed(id) },
                    onSaveModelTestPassed = { id, passed -> saveModelTestPassed(id, passed) },
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

    private fun startRecording(handler: (Boolean) -> Unit) {
        recordingResultHandler = handler
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            handler(audioController.startRecording())
        }
    }

    private fun requestCameraPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun stopRecording(): RecordedAudio? = audioController.stopRecording()

    private fun captureVideoFrame(callback: (String?) -> Unit) {
        val dir = File(cacheDir, "video_frames").apply { mkdirs() }
        val file = File(dir, "frame_${System.currentTimeMillis()}.jpg")
        val imageCapture = currentImageCapture ?: run {
            callback(null)
            return
        }
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    callback(Uri.fromFile(file).toString())
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("RhodesVideoCall", "capture frame failed", exception)
                    callback(null)
                }
            }
        )
    }

    private fun bindCameraPreview(previewView: PreviewView, imageCapture: ImageCapture) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = CameraPreview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            currentImageCapture = imageCapture
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageCapture,
                )
            }.onFailure { error ->
                Log.e("RhodesVideoCall", "bind camera failed", error)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun loadProactiveEnabled(): Boolean {
        return getSharedPreferences("rhodes_settings", MODE_PRIVATE).getBoolean("proactive_enabled", true)
    }

    private fun saveProactiveEnabled(enabled: Boolean) {
        getSharedPreferences("rhodes_settings", MODE_PRIVATE)
            .edit()
            .putBoolean("proactive_enabled", enabled)
            .apply()
        if (enabled) {
            ProactiveMessageScheduler.schedule(this)
        } else {
            ProactiveMessageScheduler.cancel(this)
        }
    }

    private fun loadModelTestPassed(id: String): Boolean {
        return getSharedPreferences("rhodes_settings", MODE_PRIVATE).getBoolean("model_test_passed_$id", false)
    }

    private fun saveModelTestPassed(id: String, passed: Boolean) {
        getSharedPreferences("rhodes_settings", MODE_PRIVATE)
            .edit()
            .putBoolean("model_test_passed_$id", passed)
            .apply()
    }
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

private enum class VoiceCallInteractionMode(val title: String) {
    Manual("手动发送"),
    AutoVad("自动检测"),
}

private data class CallTurn(
    val speaker: String,
    val text: String,
)

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
    onStartRecording: ((Boolean) -> Unit) -> Unit,
    onStopRecording: () -> RecordedAudio?,
    onReadPcmFromWav: (String) -> ByteArray,
    onPlayVoice: (String) -> Unit,
    onPlayVoiceAndWait: (String, (Boolean) -> Unit) -> Unit,
    onStopPlayback: () -> Unit,
    onHasRecentSpeech: (String) -> Boolean,
    onHasRecordingBeenSilent: () -> Boolean,
    onSaveTtsAudio: (ByteArray, String) -> RecordedAudio?,
    onSetSpeakerEnabled: (Boolean) -> Unit,
    onCaptureVideoFrame: ((String?) -> Unit) -> Unit,
    onBindCameraPreview: (PreviewView, ImageCapture) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,

    onLoadProactiveEnabled: () -> Boolean,
    onSaveProactiveEnabled: (Boolean) -> Unit,
    onLoadModelTestPassed: (String) -> Boolean,
    onSaveModelTestPassed: (String, Boolean) -> Unit,
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
        val gateway = createChatGateway(modelConfigs, fakeGateway)
        when (callType) {
            ActiveCallType.Voice -> VoiceCallScreen(
                character = character,
                gateway = gateway,
                configs = modelConfigs,
                onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onReadPcmFromWav = onReadPcmFromWav,
                onSaveTtsAudio = onSaveTtsAudio,
                onPlayVoice = onPlayVoice,
                onPlayVoiceAndWait = onPlayVoiceAndWait,
                onStopPlayback = onStopPlayback,
                onHasRecentSpeech = onHasRecentSpeech,
                onHasRecordingBeenSilent = onHasRecordingBeenSilent,
                onSetSpeakerEnabled = onSetSpeakerEnabled,
                onEnd = { turns, durationSeconds ->
                    scope.launch {
                        val callMessage = buildLocalMessage(
                            conversation = conversation,
                            senderType = SenderType.System,
                            senderId = "system",
                            messageType = MessageType.Call,
                            content = MessageContent.Call(
                                callType = "voice",
                                status = buildCallSummary(turns),
                                durationSeconds = durationSeconds,
                            ),
                        )
                        messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + callMessage
                        chatRepository.saveMessage(callMessage)
                        conversations.replaceAllWith(chatRepository.getConversations())
                    }
                    activeCall = null
                },
            )
            ActiveCallType.Video -> VideoCallScreen(
                character = character,
                gateway = gateway,
                configs = modelConfigs,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onReadPcmFromWav = onReadPcmFromWav,
                onSaveTtsAudio = onSaveTtsAudio,
                onPlayVoice = onPlayVoice,
                onPlayVoiceAndWait = onPlayVoiceAndWait,
                onStopPlayback = onStopPlayback,
                onHasRecordingBeenSilent = onHasRecordingBeenSilent,
                onSetSpeakerEnabled = onSetSpeakerEnabled,
                onPrepareImageForModel = onPrepareImageForModel,
                onCaptureVideoFrame = onCaptureVideoFrame,
                onBindCameraPreview = onBindCameraPreview,
                onRequestCameraPermission = onRequestCameraPermission,
                onEnd = { turns, durationSeconds ->
                    scope.launch {
                        val callMessage = buildLocalMessage(
                            conversation = conversation,
                            senderType = SenderType.System,
                            senderId = "system",
                            messageType = MessageType.Call,
                            content = MessageContent.Call(
                                callType = "video",
                                status = buildCallSummary(turns),
                                durationSeconds = durationSeconds,
                            ),
                        )
                        messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + callMessage
                        chatRepository.saveMessage(callMessage)
                        conversations.replaceAllWith(chatRepository.getConversations())
                    }
                    activeCall = null
                },
            )
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
            onSendVoice = { started ->
                onStartRecording { success ->
                    // Permission result only starts recording. The actual message is created when user taps stop.
                    started(success)
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
                            content = MessageContent.Voice(
                                localPath = audio.path,
                                durationMs = audio.durationMs,
                                text = "正在识别...",
                            ),
                        )
                        messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + message
                        chatRepository.saveMessage(message)
                        conversations.replaceAllWith(chatRepository.getConversations())
                        val transcript = transcribeRecordedAudio(
                            configs = modelConfigs,
                            recordedAudio = audio,
                            readPcmFromWav = onReadPcmFromWav,
                        )
                        val updatedMessage = message.copy(
                            content = MessageContent.Voice(
                                localPath = audio.path,
                                durationMs = audio.durationMs,
                                text = transcript,
                            )
                        )
                        messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty().map {
                            if (it.id == message.id) updatedMessage else it
                        }
                        chatRepository.saveMessage(updatedMessage)
                        conversations.replaceAllWith(chatRepository.getConversations())
                        if (isUsableTranscript(transcript)) {
                            replyToUserInput(
                                configs = modelConfigs,
                                fallback = fakeGateway,
                                conversation = conversation,
                                userText = transcript,
                                userTextForModel = "用户通过语音说：$transcript",
                                messagesByConversation = messagesByConversation,
                                chatRepository = chatRepository,
                                vectorStoreGateway = vectorStoreGateway,
                                saveTtsAudio = onSaveTtsAudio,
                            )
                            conversations.replaceAllWith(chatRepository.getConversations())
                        } else if (transcript.startsWith("识别失败") || transcript == "识别为空") {
                            val failedMessage = buildLocalMessage(
                                conversation = conversation,
                                senderType = SenderType.System,
                                senderId = "system",
                                messageType = MessageType.Text,
                                content = MessageContent.Text("语音未能识别为可发送文本，已只保存语音条。"),
                            ).copy(status = MessageStatus.Failed)
                            messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + failedMessage
                            chatRepository.saveMessage(failedMessage)
                            conversations.replaceAllWith(chatRepository.getConversations())
                        }
                    }
                }
            },
            onPlayVoice = onPlayVoice,
            onRetryVoiceAsr = { message ->
                scope.launch {
                    val voice = message.content as? MessageContent.Voice ?: return@launch
                    val recognizing = message.copy(content = voice.copy(text = "正在识别..."))
                    messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty().map {
                        if (it.id == message.id) recognizing else it
                    }
                    chatRepository.saveMessage(recognizing)
                    val transcript = transcribeRecordedAudio(
                        configs = modelConfigs,
                        recordedAudio = RecordedAudio(path = voice.localPath, durationMs = voice.durationMs),
                        readPcmFromWav = onReadPcmFromWav,
                    )
                    val updated = message.copy(content = voice.copy(text = transcript))
                    messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty().map {
                        if (it.id == message.id) updated else it
                    }
                    chatRepository.saveMessage(updated)
                    if (isUsableTranscript(transcript)) {
                        replyToUserInput(
                            configs = modelConfigs,
                            fallback = fakeGateway,
                            conversation = conversation,
                            userText = transcript,
                            userTextForModel = "用户通过语音说：$transcript",
                            messagesByConversation = messagesByConversation,
                            chatRepository = chatRepository,
                            vectorStoreGateway = vectorStoreGateway,
                            saveTtsAudio = onSaveTtsAudio,
                        )
                    }
                    conversations.replaceAllWith(chatRepository.getConversations())
                }
            },
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
                    replyToUserInput(
                        configs = modelConfigs,
                        fallback = fakeGateway,
                        conversation = conversation,
                        userText = text,
                        userTextForModel = text,
                        messagesByConversation = messagesByConversation,
                        chatRepository = chatRepository,
                        vectorStoreGateway = vectorStoreGateway,
                        saveTtsAudio = onSaveTtsAudio,
                    )
                    conversations.replaceAllWith(chatRepository.getConversations())
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
                        modelConfigs = modelConfigs,
                        onOpen = { activeConversation = it },
                        onOpenSettings = { selectedTab = MainTab.Settings },
                    )
                    MainTab.Contacts -> ContactsScreen(characters = characters)
                    MainTab.Features -> FeaturesScreen()
                    MainTab.Settings -> SettingsScreen(
                        modelConfigs = modelConfigs,
                        proactiveEnabledInitial = onLoadProactiveEnabled(),
                        onSaveModelConfig = { config ->
                            scope.launch {
                                settingsRepository.saveModelConfig(config)
                                modelConfigs.replaceAllWith(settingsRepository.getModelConfigs())
                            }
                        },
                        onSaveProactiveEnabled = onSaveProactiveEnabled,
                        onLoadModelTestPassed = onLoadModelTestPassed,
                        onSaveModelTestPassed = onSaveModelTestPassed,
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
    modelConfigs: List<ModelConfig>,
    onOpen: (Conversation) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopTitle("罗德岛通讯端")
        LazyColumn {
            item {
                ModelReadinessCard(
                    modelConfigs = modelConfigs,
                    onOpenSettings = onOpenSettings,
                )
            }
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
private fun ModelReadinessCard(modelConfigs: List<ModelConfig>, onOpenSettings: () -> Unit) {
    val llmReady = modelConfigs.any { it.modelType == ModelType.Llm && it.enabled && it.apiKeyMasked.isNotBlank() && it.baseUrl.isNotBlank() }
    val ttsReady = modelConfigs.any { it.modelType == ModelType.Tts && it.enabled && it.apiKeyMasked.isNotBlank() && it.baseUrl.isNotBlank() }
    val memoryReady = modelConfigs.any { it.modelType == ModelType.Embedding && it.enabled && it.apiKeyMasked.isNotBlank() && it.baseUrl.isNotBlank() }
    val title = if (llmReady) "模型服务已可用" else "离线测试模式"
    val body = if (llmReady) {
        "文字聊天将调用你配置的 LLM。${if (ttsReady) "语音条/TTS 已可用。" else "TTS 未配置，语音会降级为文字。"}${if (memoryReady) "向量记忆已增强。" else "未配置 Embedding 时使用本地关键词记忆。"}"
    } else {
        "当前未启用可用 LLM，会使用内置假回复。要给真实用户使用，请先在设置里填写 Base URL、API Key，并测试连接。"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, if (llmReady) Color(0x66338A5C) else Color(0x88D68B00), RoundedCornerShape(18.dp))
            .background(if (llmReady) Color(0xFFEAF7F0) else Color(0xFFFFF6E3))
            .padding(16.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, color = RhodesInk)
        Spacer(Modifier.height(6.dp))
        Text(body, color = Color(0xFF5D6875), fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onOpenSettings) { Text("打开设置") }
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
    proactiveEnabledInitial: Boolean,
    onSaveModelConfig: (ModelConfig) -> Unit,
    onSaveProactiveEnabled: (Boolean) -> Unit,
    onLoadModelTestPassed: (String) -> Boolean,
    onSaveModelTestPassed: (String, Boolean) -> Unit,
) {
    var proactiveEnabled by remember { mutableStateOf(proactiveEnabledInitial) }
    var callLowLatencyTts by remember { mutableStateOf(true) }
    var editingConfig by remember { mutableStateOf<ModelConfig?>(null) }
    val testPassedByConfig = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(modelConfigs.map { it.id }) {
        modelConfigs.forEach { config ->
            testPassedByConfig[config.id] = onLoadModelTestPassed(config.id)
        }
    }

    editingConfig?.let { config ->
        ModelConfigEditor(
            config = config,
            testPassedInitial = testPassedByConfig[config.id] ?: onLoadModelTestPassed(config.id),
            onBack = { editingConfig = null },
            onSave = { saved ->
                onSaveModelTestPassed(saved.id, false)
                testPassedByConfig[saved.id] = false
                onSaveModelConfig(saved)
                editingConfig = null
            },
            onSaveTestPassed = { passed ->
                onSaveModelTestPassed(config.id, passed)
                testPassedByConfig[config.id] = passed
            },
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopTitle("设置")
        LazyColumn {
            item { SectionTitle("模型服务配置") }
            item {
                FeatureCard(
                    "MVP 可用性检查",
                    buildReadinessText(modelConfigs),
                )
            }
            items(modelConfigs) { config ->
                ModelConfigCard(
                    title = "${config.provider} · ${config.modelType.name}",
                    config = config,
                    testPassed = testPassedByConfig[config.id] ?: onLoadModelTestPassed(config.id),
                    onClick = { editingConfig = config },
                )
            }
            item { SectionTitle("第三方向量库") }
            item {
                FeatureCard(
                    "${DefaultModelConfigs.vectorStore.provider} · ${DefaultModelConfigs.vectorStore.collectionName}",
                    "当前 MVP 使用本地 SQLite 存储记忆，并用 Embedding 做本地相似度检索。第三方远程向量库网关尚未实现，不需要填写。",
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
                    onCheckedChange = {
                        proactiveEnabled = it
                        onSaveProactiveEnabled(it)
                    },
                )
            }
            item { FeatureCard("数据管理", "聊天、模型配置和记忆当前保存在本机数据库。API Key 使用 Android Keystore 加密；卸载应用会清除本地数据。") }
        }
    }
}

private fun buildReadinessText(modelConfigs: List<ModelConfig>): String {
    fun status(type: ModelType, label: String): String {
        val config = modelConfigs.firstOrNull { it.modelType == type }
        val ready = config != null && isModelReady(config)
        return "$label：${if (ready) "可用" else "未配置"}"
    }
    return listOf(
        status(ModelType.Llm, "文字聊天"),
        status(ModelType.Tts, "语音条/TTS"),
        status(ModelType.Asr, "语音识别"),
        status(ModelType.Vision, "图片识别"),
        status(ModelType.Embedding, "向量记忆"),
    ).joinToString("\n")
}

private fun isModelReady(config: ModelConfig): Boolean {
    if (!config.enabled || config.apiKeyMasked.isBlank()) return false
    return when (config.modelType) {
        ModelType.Asr -> config.modelName.isNotBlank()
        ModelType.VectorStore -> true
        else -> config.baseUrl.isNotBlank()
    }
}

private fun buildModelConfigCardBody(config: ModelConfig): String {
    return buildString {
        appendLine("状态: ${modelConfigStatus(config)}")
        appendLine("Model: ${config.modelName.ifBlank { recommendedModelName(config.modelType) }}")
        appendLine("Base URL: ${displayBaseUrl(config)}")
        append(modelTypeHelp(config.modelType))
    }
}

private fun displayBaseUrl(config: ModelConfig): String {
    return if (config.modelType == ModelType.Asr) {
        "固定：wss://dashscope.aliyuncs.com/api-ws/v1/realtime"
    } else {
        config.baseUrl.ifBlank { recommendedBaseUrl(config.modelType) }
    }
}

private fun modelConfigStatus(config: ModelConfig): String {
    if (!config.enabled) return "未启用"
    if (config.modelType != ModelType.Asr && config.baseUrl.isBlank()) return "缺少 Base URL"
    if (config.apiKeyMasked.isBlank()) return "缺少 API Key"
    if (config.baseUrl.contains("{") || config.baseUrl.contains("}")) return "Base URL 仍包含占位符"
    return "已启用，建议测试连接"
}

private fun modelTypeHelp(type: ModelType): String {
    return when (type) {
        ModelType.Llm -> "协议: OpenAI-compatible。DeepSeek 可填 https://api.deepseek.com 或 /v1。Anthropic URL 当前不支持。"
        ModelType.Vision -> "协议: 阿里百炼多模态 generation endpoint。必须使用你自己 Workspace 下有权限的 endpoint。"
        ModelType.Embedding -> "协议: 阿里 text-embedding-v4 endpoint。不要保留 {WorkspaceId} 占位符。"
        ModelType.Tts -> "协议: Minimax WebSocket TTS。Model 格式: speech-2.8-hd|voiceId。"
        ModelType.Asr -> "协议: 阿里百炼 Qwen-Omni Realtime 转写。Base URL 固定为 wss://dashscope.aliyuncs.com/api-ws/v1/realtime。你只需要填写 DashScope API Key。Model 格式：实时模型|转写模型。"
        ModelType.VectorStore -> "远程向量库网关未实现。"
    }
}

private fun recommendedBaseUrl(type: ModelType): String {
    return when (type) {
        ModelType.Llm -> "https://api.deepseek.com/v1"
        ModelType.Vision -> "https://llm-imxtee9l3et45y6z.cn-beijing.maas.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
        ModelType.Embedding -> "https://llm-imxtee9l3et45y6z.cn-beijing.maas.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding"
        ModelType.Tts -> "wss://api.minimaxi.com/ws/v1/t2a_v2"
        ModelType.Asr -> ""
        ModelType.VectorStore -> "暂不需要"
    }
}

private fun recommendedModelName(type: ModelType): String {
    return when (type) {
        ModelType.Llm -> "deepseek-v4-flash"
        ModelType.Vision -> "qwen3-vl-plus"
        ModelType.Embedding -> "text-embedding-v4"
        ModelType.Tts -> "speech-2.8-hd|male-qn-qingse"
        ModelType.Asr -> "qwen3.5-omni-flash-realtime|qwen3-asr-flash-realtime"
        ModelType.VectorStore -> ""
    }
}

private fun recommendedProvider(type: ModelType): String {
    return when (type) {
        ModelType.Llm -> "DeepSeek / OpenAI兼容"
        ModelType.Vision -> "阿里百炼"
        ModelType.Embedding -> "阿里百炼"
        ModelType.Tts -> "Minimax"
        ModelType.Asr -> "阿里百炼"
        ModelType.VectorStore -> "本地"
    }
}

@Composable
private fun ModelConfigEditor(
    config: ModelConfig,
    testPassedInitial: Boolean,
    onBack: () -> Unit,
    onSave: (ModelConfig) -> Unit,
    onSaveTestPassed: (Boolean) -> Unit,
) {
    var provider by remember { mutableStateOf(config.provider) }
    var baseUrl by remember { mutableStateOf(config.baseUrl) }
    var apiKey by remember { mutableStateOf(config.apiKeyMasked) }
    var modelName by remember { mutableStateOf(config.modelName) }
    var enabled by remember { mutableStateOf(config.enabled) }
    var testResult by remember { mutableStateOf("未测试") }
    var testPassed by remember { mutableStateOf(testPassedInitial) }
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
        item {
            FeatureCard(
                "填写说明",
                modelTypeHelp(config.modelType) + "\n\n推荐 Base URL: ${recommendedBaseUrl(config.modelType).ifBlank { "不需要填写" }}\n${recommendedValueHelp(config.modelType)}",
            )
        }
        item { FeatureCard("测试状态", if (testPassed) "测试已通过" else "未测试。修改配置并保存后会重置为未测试。") }
        item { ConfigTextField("服务商", provider, { provider = it }) }
        item {
            if (config.modelType == ModelType.Asr) {
                FeatureCard("固定 Base URL", "DashScope ASR 使用固定 Realtime WebSocket 地址，保存时会自动保持为空；这里只需要填写 API Key。")
            } else {
                ConfigTextField("Base URL", baseUrl, { baseUrl = it })
            }
        }
        item { ConfigTextField("API Key / Token", apiKey, { apiKey = it }) }
        item { ConfigTextField(modelConfigValueLabel(config.modelType), modelName, { modelName = it }) }
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
                        val suggestedBaseUrl = recommendedBaseUrl(config.modelType)
                        baseUrl = suggestedBaseUrl.takeUnless { it.startsWith("请填写") || it == "暂不需要" }.orEmpty()
                        modelName = recommendedModelName(config.modelType)
                        if (provider.isBlank()) provider = recommendedProvider(config.modelType)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("填入推荐") }
                Button(
                    onClick = {
                        scope.launch {
                            testResult = "测试中..."
                            val result = testModelConfig(
                                normalizeModelConfigForSave(config.copy(
                                    provider = provider,
                                    baseUrl = baseUrl,
                                    apiKeyMasked = apiKey,
                                    modelName = modelName,
                                    enabled = enabled,
                                ))
                            )
                            testResult = result
                            val passed = result.startsWith("成功")
                            testPassed = passed
                            onSaveTestPassed(passed)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("测试连接") }
                Button(
                    onClick = {
                        onSave(
                            normalizeModelConfigForSave(config.copy(
                                provider = provider,
                                baseUrl = baseUrl,
                                apiKeyMasked = apiKey,
                                modelName = modelName,
                                enabled = enabled,
                            ))
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
private fun ModelConfigCard(title: String, config: ModelConfig, testPassed: Boolean, onClick: () -> Unit) {
    val statusColor = if (testPassed) Color(0xFF217A4A) else Color(0xFF9A6500)
    val statusBg = if (testPassed) Color(0xFFE7F6ED) else Color(0xFFFFF3D6)
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
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(statusBg)
                .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(if (testPassed) "测试已通过" else "未测试", color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Text(buildModelConfigCardBody(config), color = Color(0xFF6F7B87), fontSize = 14.sp)
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
    onSendVoice: ((Boolean) -> Unit) -> Unit,
    onStopVoice: () -> Unit,
    onPlayVoice: (String) -> Unit,
    onRetryVoiceAsr: (ChatMessage) -> Unit,
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
                                    onSendVoice { started -> recording = started }
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
                MessageBubble(message, onPlayVoice = onPlayVoice, onRetryVoiceAsr = onRetryVoiceAsr)
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
private fun VoiceCallScreen(
    character: AiCharacter,
    gateway: ModelGateway,
    configs: List<ModelConfig>,
    onStartRecording: ((Boolean) -> Unit) -> Unit,
    onStopRecording: () -> RecordedAudio?,
    onReadPcmFromWav: (String) -> ByteArray,
    onSaveTtsAudio: (ByteArray, String) -> RecordedAudio?,
    onPlayVoice: (String) -> Unit,
    onPlayVoiceAndWait: (String, (Boolean) -> Unit) -> Unit,
    onStopPlayback: () -> Unit,
    onHasRecentSpeech: (String) -> Boolean,
    onHasRecordingBeenSilent: () -> Boolean,
    onSetSpeakerEnabled: (Boolean) -> Unit,
    onEnd: (List<CallTurn>, Int) -> Unit,
) {
    var state by remember { mutableStateOf(CallState.Calling) }
    var input by remember { mutableStateOf("今晚有点累，陪我聊两句") }
    var transcript by remember { mutableStateOf("可以直接输入文字模拟 ASR，也可以录一段语音后发送。") }
    var aiText by remember { mutableStateOf("AI 正在等待你的语音输入。") }
    var recording by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var speakerEnabled by remember { mutableStateOf(false) }
    var interactionMode by remember { mutableStateOf(VoiceCallInteractionMode.Manual) }
    val callTurns = remember { mutableStateListOf<CallTurn>() }
    val callStartedAt = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        delay(800)
        state = CallState.Connected
    }
    LaunchedEffect(recording, interactionMode) {
        if (recording && interactionMode == VoiceCallInteractionMode.AutoVad) {
            while (recording) {
                delay(250)
                if (onHasRecordingBeenSilent()) {
                    val audio = onStopRecording()
                    recording = false
                    if (audio == null) {
                        transcript = "自动检测未生成录音，请重试"
                    } else {
                        transcript = "检测到停顿，正在识别..."
                        state = CallState.Thinking
                        val text = transcribeRecordedAudio(
                            configs = configs,
                            recordedAudio = audio,
                            readPcmFromWav = onReadPcmFromWav,
                        )
                        transcript = text
                        input = text
                        if (isUsableTranscript(text)) {
                            callTurns += CallTurn("用户", text)
                            val reply = runCatching {
                                gateway.voiceCall(
                                    VoiceCallRequest(
                                        characterId = character.id,
                                        userText = "用户通过自动语音检测说：$text",
                                        recentMessages = callTurns.takeLast(8).map { "${it.speaker}：${it.text}" },
                                    )
                                )
                            }.getOrDefault("我听到了，但刚才回复生成失败了。")
                            aiText = reply
                            callTurns += CallTurn("AI", reply)
                            state = CallState.AiSpeaking
                            val played = synthesizeAndPlayCallReply(
                                configs = configs,
                                text = reply,
                                onSaveTtsAudio = onSaveTtsAudio,
                                onPlayVoiceAndWait = onPlayVoiceAndWait,
                            )
                            if (!played) aiText = "$reply\n（语音播放失败，请检查 TTS 配置）"
                            state = CallState.Listening
                        } else {
                            state = CallState.Listening
                        }
                    }
                    break
                }
            }
        }
    }
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
            Avatar(character.name)
            Spacer(Modifier.height(12.dp))
            Text(character.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("${state.name} · 半双工语音通话", color = Color(0xFFB8C7D9), fontSize = 13.sp)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x66B8C7D9), RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF263445), Color(0xFF1B2A3A))))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("用户：$transcript", color = Color(0xFFB8C7D9), lineHeight = 21.sp)
            Text("AI：$aiText", color = Color.White, lineHeight = 22.sp)
            Text(
                text = when (interactionMode) {
                    VoiceCallInteractionMode.Manual -> "当前模式：手动半双工。点开始录音，再点停止并发送给 AI。AI 说话时可点录音打断。"
                    VoiceCallInteractionMode.AutoVad -> "当前模式：自动检测第一版。点击开始说话，再点停止检测；若末尾已安静会自动发送给 AI。"
                },
                color = Color(0xFF9FB0C4),
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                VoiceCallInteractionMode.entries.forEach { mode ->
                    Button(
                        onClick = { interactionMode = mode },
                        modifier = Modifier.weight(1f),
                    ) { Text(if (interactionMode == mode) "✓ ${mode.title}" else mode.title) }
                }
            }
            CallTranscriptPanel(turns = callTurns)
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("通话输入 / ASR 结果") },
                singleLine = false,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (muted) {
                            transcript = "已静音，取消静音后再录音。"
                        } else if (state == CallState.AiSpeaking) {
                            onStopPlayback()
                            state = CallState.Listening
                            aiText = "$aiText\n（已被你打断）"
                            onStartRecording { started ->
                                recording = started
                                transcript = if (started) "已打断 AI，正在录音...再次点击停止并发送" else "录音启动失败，请检查麦克风权限"
                            }
                        } else if (recording) {
                            val audio = onStopRecording()
                            recording = false
                            if (audio == null) {
                                transcript = "录音未生成，请重试"
                            } else if (interactionMode == VoiceCallInteractionMode.AutoVad && onHasRecentSpeech(audio.path)) {
                                transcript = "检测到你可能还没说完，请继续录音或切回手动发送。"
                            } else {
                                transcript = "正在识别..."
                                scope.launch {
                                    state = CallState.Thinking
                                    val text = transcribeRecordedAudio(
                                        configs = configs,
                                        recordedAudio = audio,
                                        readPcmFromWav = onReadPcmFromWav,
                                    )
                                    transcript = text
                                    input = text
                                    if (isUsableTranscript(text)) {
                                        callTurns += CallTurn("用户", text)
                                        val reply = runCatching {
                                            gateway.voiceCall(
                                                VoiceCallRequest(
                                                    characterId = character.id,
                                                    userText = "用户通过语音通话说：$text",
                                                    recentMessages = listOf("用户语音：$text"),
                                                )
                                            )
                                        }.getOrDefault("我听到了，但刚才回复生成失败了。")
                                        aiText = reply
                                        callTurns += CallTurn("AI", reply)
                                        state = CallState.AiSpeaking
                                        val played = synthesizeAndPlayCallReply(
                                            configs = configs,
                                            text = reply,
                                            onSaveTtsAudio = onSaveTtsAudio,
                                            onPlayVoiceAndWait = onPlayVoiceAndWait,
                                        )
                                        if (!played) aiText = "$reply\n（语音播放失败，请检查 TTS 配置）"
                                        state = CallState.Listening
                                    } else {
                                        state = CallState.Listening
                                    }
                                }
                            }
                        } else {
                            onStartRecording { started ->
                                recording = started
                                transcript = if (started) "正在录音...再次点击停止并发送" else "录音启动失败，请检查麦克风权限"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            recording && interactionMode == VoiceCallInteractionMode.AutoVad -> "正在听，停顿后自动发送"
                            recording -> "停止录音并发送"
                            interactionMode == VoiceCallInteractionMode.AutoVad -> "开始自动检测"
                            else -> "开始录音"
                        }
                    )
                }
                Button(
                    onClick = {
                        input = ""
                        transcript = "已清空输入"
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("清空") }
            }
            Button(
                onClick = {
                    val userText = input.trim()
                    if (userText.isNotEmpty()) {
                        transcript = userText
                        input = ""
                        scope.launch {
                            state = CallState.Thinking
                            val reply = runCatching {
                                gateway.voiceCall(
                                    VoiceCallRequest(
                                        characterId = character.id,
                                        userText = userText,
                                        recentMessages = listOf("用户：$userText"),
                                    )
                                )
                            }.getOrDefault("我在，慢慢说。")
                            aiText = reply
                            callTurns += CallTurn("用户", userText)
                            callTurns += CallTurn("AI", reply)
                            state = CallState.AiSpeaking
                            val played = synthesizeAndPlayCallReply(
                                configs = configs,
                                text = reply,
                                onSaveTtsAudio = onSaveTtsAudio,
                                onPlayVoiceAndWait = onPlayVoiceAndWait,
                            )
                            if (!played) aiText = "$reply\n（语音播放失败，请检查 TTS 配置）"
                            state = CallState.Listening
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("发送给 AI 并播放") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                muted = !muted
                if (muted && recording) {
                    onStopRecording()
                    recording = false
                }
            }) { Text(if (muted) "取消静音" else "静音") }
            Button(onClick = {
                speakerEnabled = !speakerEnabled
                onSetSpeakerEnabled(speakerEnabled)
            }) { Text(if (speakerEnabled) "听筒" else "扬声器") }
            Button(onClick = {
                if (recording) onStopRecording()
                recording = false
                val durationSeconds = ((System.currentTimeMillis() - callStartedAt) / 1000).toInt().coerceAtLeast(1)
                onSetSpeakerEnabled(false)
                onEnd(callTurns.toList().ifEmpty { listOf(CallTurn("用户", transcript), CallTurn("AI", aiText)) }, durationSeconds)
            }) { Text("挂断") }
        }
    }
}

@Composable
private fun CallTranscriptPanel(turns: List<CallTurn>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x445E7898), RoundedCornerShape(16.dp))
            .background(Color(0x33222F42))
            .padding(10.dp),
    ) {
        Text("通话转录", color = Color(0xFFD7E2F0), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        if (turns.isEmpty()) {
            Text("本次通话内容会显示在这里。", color = Color(0xFF9FB0C4), fontSize = 12.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(turns.takeLast(20)) { turn ->
                    Text(
                        text = "${turn.speaker}：${turn.text}",
                        color = if (turn.speaker == "用户") Color(0xFFB8C7D9) else Color.White,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
        }
    }
}

private fun recommendedValueHelp(type: ModelType): String {
    return when (type) {
        ModelType.Asr -> "Model Name: 推荐 qwen3.5-omni-flash-realtime|qwen3-asr-flash-realtime；API Key 使用 DashScope Key。"
        ModelType.Tts -> "推荐 Model | Voice ID: ${recommendedModelName(type)}"
        else -> "推荐 Model: ${recommendedModelName(type).ifBlank { "无" }}"
    }
}

private fun modelConfigValueLabel(type: ModelType): String {
    return when (type) {
        ModelType.Tts -> "Model | Voice ID"
        ModelType.Asr -> "DashScope ASR Model"
        ModelType.VectorStore -> "Collection / Namespace"
        else -> "Model Name"
    }
}

@Composable
private fun VideoCallScreen(
    character: AiCharacter,
    gateway: ModelGateway,
    configs: List<ModelConfig>,
    onStartRecording: ((Boolean) -> Unit) -> Unit,
    onStopRecording: () -> RecordedAudio?,
    onReadPcmFromWav: (String) -> ByteArray,
    onSaveTtsAudio: (ByteArray, String) -> RecordedAudio?,
    onPlayVoice: (String) -> Unit,
    onPlayVoiceAndWait: (String, (Boolean) -> Unit) -> Unit,
    onStopPlayback: () -> Unit,
    onHasRecordingBeenSilent: () -> Boolean,
    onSetSpeakerEnabled: (Boolean) -> Unit,
    onPrepareImageForModel: (String) -> String?,
    onCaptureVideoFrame: ((String?) -> Unit) -> Unit,
    onBindCameraPreview: (PreviewView, ImageCapture) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onEnd: (List<CallTurn>, Int) -> Unit,
) {
    var state by remember { mutableStateOf(CallState.Connected) }
    var actionText by remember { mutableStateOf("idle") }
    var lastVisionText by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("摄像头已启动，可以语音对话。") }
    var aiText by remember { mutableStateOf("我能看到你这边了。") }
    var recording by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var speakerEnabled by remember { mutableStateOf(false) }
    val callTurns = remember { mutableStateListOf<CallTurn>() }
    val callStartedAt = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    val imageCapture = remember { ImageCapture.Builder().build() }
    LaunchedEffect(Unit) { onRequestCameraPermission() }
    LaunchedEffect(Unit) {
        delay(800)
        state = CallState.Connected
        transcript = "摄像头已启动。按住录音按钮说话，松开后自动识别画面和语音。"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1420), Color(0xFF17263A), Color(0xFF0F1B17))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            Avatar(character.name)
            Spacer(Modifier.height(6.dp))
            Text(character.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("${state.name} · 视频通话 · 动作:{$actionText}", color = Color(0xFF9FB0C4), fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0x66B8C7D9), RoundedCornerShape(20.dp))
                .background(Color(0xFF1B2A3A)),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).also { previewView -> onBindCameraPreview(previewView, imageCapture) }
                }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(10.dp),
            ) {
                Text("AI：${aiText.take(120)}", color = Color.White, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        CallTranscriptPanel(turns = callTurns)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0x445E7898), RoundedCornerShape(20.dp))
                .background(Color(0x33222F42))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("用户：$transcript", color = Color(0xFFB8C7D9), fontSize = 12.sp, lineHeight = 17.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (muted) {
                            transcript = "已静音，取消静音后再录音。"
                        } else if (state == CallState.AiSpeaking) {
                            onStopPlayback()
                            state = CallState.Listening
                            transcript = "已打断 AI，点击录音重新说话。"
                        } else if (recording) {
                            val audio = onStopRecording()
                            recording = false
                            if (audio == null) {
                                transcript = "录音未生成，请重试"
                            } else {
                                transcript = "正在识别语音和分析画面..."
                                state = CallState.Thinking
                                scope.launch {
                                    val asrText = transcribeRecordedAudio(
                                        configs = configs,
                                        recordedAudio = audio,
                                        readPcmFromWav = onReadPcmFromWav,
                                    )
                                    transcript = asrText
                                    val visionText = suspendCancellableCoroutine<String?> { cont ->
                                        onCaptureVideoFrame { uri ->
                                            if (uri != null && cont.isActive) {
                                                scope.launch {
                                                    val vt = analyzeVideoFrameForCall(
                                                        configs = configs,
                                                        imageForModel = onPrepareImageForModel(uri) ?: uri,
                                                    )
                                                    if (cont.isActive) cont.resumeWith(Result.success(vt))
                                                }
                                            } else {
                                                if (cont.isActive) cont.resumeWith(Result.success("画面未捕获"))
                                            }
                                        }
                                    }
                                    lastVisionText = visionText ?: "画面未捕获"
                                    if (isUsableTranscript(asrText)) {
                                        callTurns += CallTurn("用户", "$asrText（画面：${(visionText ?: "无").take(60)}）")
                                        val reply = videoVoiceCall(
                                            gateway = gateway,
                                            characterId = character.id,
                                            asrText = asrText,
                                            visionText = visionText ?: "无",
                                            recentTurns = callTurns.toList(),
                                        )
                                        aiText = reply.text
                                        actionText = reply.action.name
                                        if (reply.action.durationMs > 0) {
                                            delay(reply.action.durationMs)
                                            actionText = "idle"
                                        }
                                        callTurns += CallTurn("AI", reply.text)
                                        state = CallState.AiSpeaking
                                        val played = synthesizeAndPlayCallReply(
                                            configs = configs,
                                            text = reply.text,
                                            onSaveTtsAudio = onSaveTtsAudio,
                                            onPlayVoiceAndWait = onPlayVoiceAndWait,
                                        )
                                        if (!played) aiText = "${reply.text}\n（语音播放失败，请检查 TTS 配置）"
                                        state = CallState.Listening
                                    } else {
                                        state = CallState.Listening
                                    }
                                }
                            }
                        } else {
                            onStartRecording { started ->
                                recording = started
                                transcript = if (started) "正在录音，松手停止并发送" else "录音启动失败，请检查麦克风权限"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            recording -> "停止录音并发送"
                            else -> "开始录音"
                        }
                    )
                }
                Button(
                    onClick = {
                        state = CallState.Thinking
                        onCaptureVideoFrame { uri ->
                            if (uri == null) {
                                transcript = "没有拿到摄像头画面，请确认相机权限。"
                                state = CallState.Connected
                                return@onCaptureVideoFrame
                            }
                            scope.launch {
                                val visionText = analyzeVideoFrameForCall(
                                    configs = configs,
                                    imageForModel = onPrepareImageForModel(uri) ?: uri,
                                )
                                lastVisionText = visionText
                                aiText = visionText.ifBlank { "画面已捕获，但视觉模型没有返回内容。" }
                                actionText = "observe"
                                transcript = "分析完成：${visionText.take(80)}"
                                state = CallState.Connected
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("分析当前画面") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                muted = !muted
                if (muted && recording) { onStopRecording(); recording = false }
            }) { Text(if (muted) "取消静音" else "静音") }
            Button(onClick = {
                speakerEnabled = !speakerEnabled
                onSetSpeakerEnabled(speakerEnabled)
            }) { Text(if (speakerEnabled) "听筒" else "扬声器") }
            Button(onClick = {
                if (recording) onStopRecording()
                recording = false
                val durationSeconds = ((System.currentTimeMillis() - callStartedAt) / 1000).toInt().coerceAtLeast(1)
                onSetSpeakerEnabled(false)
                onEnd(callTurns.toList().ifEmpty { listOf(CallTurn("用户", transcript), CallTurn("AI", aiText)) }, durationSeconds)
            }) { Text("挂断") }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onPlayVoice: (String) -> Unit, onRetryVoiceAsr: (ChatMessage) -> Unit) {
    val isUser = message.senderType == SenderType.User
    var isPlaying by remember(message.id) { mutableStateOf(false) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val duration = (message.content as? MessageContent.Voice)?.durationMs ?: 1200L
            delay(duration.coerceIn(800L, 120_000L))
            isPlaying = false
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        val bubbleColor = if (isUser) Color(0xFF95EC69) else Color.White
        val content = message.content
        val text = when (content) {
            is MessageContent.Text -> content.text
            is MessageContent.Image -> content.caption ?: content.prompt ?: "图片已发送\n${content.uri.take(42)}"
            is MessageContent.Voice -> buildString {
                append("语音 ${content.durationMs / 1000}s  ${if (isPlaying) "播放中..." else "▶"}")
                content.text?.takeIf { it.isNotBlank() }?.let { append("\n识别：$it") }
                if (content.text == "识别为空" || content.text?.startsWith("识别失败") == true) append("\n点击重试识别")
            }
            is MessageContent.Sticker -> "表情 ${content.stickerId}\n${content.alt.orEmpty()}"
            is MessageContent.Gift -> "${content.name}  已送达"
            is MessageContent.Call -> "${if (content.callType == "voice") "语音通话" else "视频通话"} ${content.durationSeconds}s\n${content.status}"
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
                    voice?.let {
                        Log.d("RhodesAudio", "keyword=RHODES_AUDIO_PLAYBACK stage=bubble_click path=${it.localPath} duration=${it.durationMs} text=${it.text.orEmpty().take(120)}")
                        isPlaying = true
                        if (it.text == "识别为空" || it.text?.startsWith("识别失败") == true) {
                            onRetryVoiceAsr(message)
                        } else {
                            onPlayVoice(it.localPath)
                        }
                    }
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
            onStartRecording = { handler -> handler(true) },
            onStopRecording = { RecordedAudio(path = "preview://voice", durationMs = 3200L) },
            onReadPcmFromWav = { ByteArray(0) },
            onPlayVoice = {},
            onPlayVoiceAndWait = { _, done -> done(true) },
            onStopPlayback = {},
            onHasRecentSpeech = { false },
            onHasRecordingBeenSilent = { false },
            onSaveTtsAudio = { _, _ -> null },
            onSetSpeakerEnabled = {},
            onCaptureVideoFrame = { callback -> callback(null) },
            onBindCameraPreview = { _, _ -> },
            onRequestNotificationPermission = {},
            onRequestCameraPermission = {},
            onLoadProactiveEnabled = { true },
            onSaveProactiveEnabled = {},
            onLoadModelTestPassed = { false },
            onSaveModelTestPassed = { _, _ -> },
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
    override suspend fun countMessages(conversationId: String): Long = 1L
    override suspend fun getConversationSummary(conversationId: String): String? = null
    override suspend fun saveConversationSummary(conversationId: String, summary: String) = Unit
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
    val existing = settingsRepository.getModelConfigs()
    if (existing.isNotEmpty()) {
        normalizeSeededModelConfigs(settingsRepository, existing)
        return
    }
    DefaultModelConfigs.allModelConfigs.forEach { settingsRepository.saveModelConfig(it) }
    settingsRepository.saveVectorStoreConfig(DefaultModelConfigs.vectorStore)
}

private suspend fun normalizeSeededModelConfigs(settingsRepository: SettingsRepository, configs: List<ModelConfig>) {
    configs.forEach { config ->
        val normalized = when (config.id) {
            DefaultModelConfigs.llm.id -> {
                if (config.apiKeyMasked.isBlank() && config.baseUrl.contains("openai-compatible.example")) {
                    config.copy(
                        provider = DefaultModelConfigs.llm.provider,
                        baseUrl = DefaultModelConfigs.llm.baseUrl,
                        modelName = DefaultModelConfigs.llm.modelName,
                    )
                } else config
            }
            DefaultModelConfigs.vision.id -> {
                if (config.apiKeyMasked.isBlank() && config.baseUrl.contains("llm-imxtee9l3et45y6z")) {
                    config.copy(baseUrl = "")
                } else config
            }
            DefaultModelConfigs.embedding.id -> {
                if (config.apiKeyMasked.isBlank() && config.baseUrl.contains("{WorkspaceId}")) {
                    config.copy(baseUrl = "")
                } else config
            }
            DefaultModelConfigs.tts.id -> {
                if (config.apiKeyMasked.isBlank() && !config.modelName.contains("|")) {
                    config.copy(modelName = DefaultModelConfigs.tts.modelName)
                } else config
            }
            DefaultModelConfigs.asr.id, "asr_aliyun_qwen3_realtime" -> {
                if (config.apiKeyMasked.isBlank() || isLegacyAsrModelName(config.modelName)) {
                    config.copy(
                        provider = DefaultModelConfigs.asr.provider,
                        baseUrl = DefaultModelConfigs.asr.baseUrl,
                        modelName = DefaultModelConfigs.asr.modelName,
                    )
                } else config
            }
            "asr_aliyun_nls_short" -> {
                config.copy(
                    id = DefaultModelConfigs.asr.id,
                    provider = DefaultModelConfigs.asr.provider,
                    baseUrl = DefaultModelConfigs.asr.baseUrl,
                    modelName = DefaultModelConfigs.asr.modelName,
                )
            }
            else -> config
        }
        if (normalized != config) settingsRepository.saveModelConfig(normalized)
    }
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

private fun normalizedAsrModelName(modelName: String): String {
    return if (modelName.isBlank() || isLegacyAsrModelName(modelName)) {
        DefaultModelConfigs.asr.modelName
    } else {
        modelName
    }
}

private fun isLegacyAsrModelName(modelName: String): Boolean {
    return modelName == "fun-asr-realtime" ||
        modelName == "qwen3-asr-flash-realtime" ||
        modelName == "qwen3-asr-flash"
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
        val gateway = createAliyunQwenVlGateway(
            endpoint = vision.baseUrl,
            apiKey = vision.apiKeyMasked,
            modelName = vision.modelName.ifBlank { "qwen3-vl-plus" },
        )
        var response = gateway.analyzeImage(
            com.yourname.aichatmvptest.shared.modelgateway.VisionAnalyzeRequest(
                imageUrlOrBase64 = imageForModel,
                prompt = "请用自然聊天语气简要描述这张图片，并指出你观察到的重点。",
            )
        )
        if (isProviderTransientVisionError(response.text)) {
            delay(800)
            response = gateway.analyzeImage(
                com.yourname.aichatmvptest.shared.modelgateway.VisionAnalyzeRequest(
                    imageUrlOrBase64 = imageForModel,
                    prompt = "请用自然聊天语气简要描述这张图片，并指出你观察到的重点。",
                )
            )
        }
        response
    }.getOrNull()

    val text = if (result == null || result.text.isBlank()) {
        "我看了一下图片，但分析过程遇到服务端错误，这可能是一个临时问题，请稍后重试。"
    } else if (looksLikeProviderError(result.text)) {
        "我看了一下图片，但分析过程遇到服务端错误，这可能是一个临时问题，请稍后重试。"
    } else {
        "我看了一下图片：${result.text}"
    }

    val aiMessage = buildLocalMessage(
        conversation = conversation,
        senderType = SenderType.Ai,
        senderId = conversation.characterId,
        messageType = MessageType.Text,
        content = MessageContent.Text(text),
    )
    messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + aiMessage
    chatRepository.saveMessage(aiMessage)
}

private suspend fun analyzeVideoFrameForCall(
    configs: List<ModelConfig>,
    imageForModel: String,
): String {
    val vision = configs.firstOrNull { it.modelType == ModelType.Vision && it.enabled }
    if (vision == null || vision.baseUrl.isBlank() || vision.apiKeyMasked.isBlank()) {
        return "视觉模型未配置，已保留摄像头预览。"
    }
    return runCatching {
        createAliyunQwenVlGateway(
            endpoint = vision.baseUrl,
            apiKey = vision.apiKeyMasked,
            modelName = vision.modelName.ifBlank { "qwen3-vl-plus" },
        ).analyzeImage(
            VisionAnalyzeRequest(
                imageUrlOrBase64 = imageForModel,
                prompt = "你正在视频通话中。请用一句自然中文描述你从当前画面观察到的重点，并给出一个简短动作建议。",
            )
        ).text
    }.getOrElse { error ->
        "视觉分析失败：${error.message ?: error::class.simpleName}"
    }
}

private suspend fun videoVoiceCall(
    gateway: ModelGateway,
    characterId: String,
    asrText: String,
    visionText: String,
    recentTurns: List<CallTurn>,
): VideoCallReply {
    val context = recentTurns.takeLast(6).joinToString("\n") { "${it.speaker}：${it.text.take(120)}" }
    val userContent = "用户通过语音说：$asrText\n当前画面：$visionText\n最近对话：\n$context"
    val reply = gateway.chat(
        ChatModelRequest(
            characterId = characterId,
            userText = userContent,
            recentMessages = listOf("当前是视频通话。用户提供了语音和画面信息。"),
        )
    )
    val raw = reply.messages.firstOrNull()?.text.orEmpty()
    return try {
        val json = org.json.JSONObject(raw)
        val act = json.optJSONObject("action") ?: org.json.JSONObject()
        VideoCallReply(
            text = json.optString("text", raw),
            action = VideoCallAction(
                name = act.optString("name", "idle"),
                intensity = act.optDouble("intensity", 0.5),
                durationMs = act.optLong("durationMs", 1000L),
                loop = act.optBoolean("loop", false),
            ),
            mood = json.optString("mood", "neutral"),
        )
    } catch (e: Exception) {
        VideoCallReply(text = raw.ifBlank { asrText }, action = VideoCallAction(name = "idle"), mood = "neutral")
    }
}

private suspend fun transcribeRecordedAudio(
    configs: List<ModelConfig>,
    recordedAudio: RecordedAudio,
    readPcmFromWav: (String) -> ByteArray,
): String {
    val asr = configs.firstOrNull { it.modelType == ModelType.Asr && it.enabled }
    if (asr == null || asr.apiKeyMasked.isBlank()) return "本地录音（ASR 未配置）"
    val pcm = runCatching { readPcmFromWav(recordedAudio.path) }.getOrElse { error ->
        logVoiceChat("asr_read_pcm_failed", error.message ?: error::class.simpleName.orEmpty())
        return "识别失败：录音文件读取失败"
    }
    if (pcm.isEmpty()) return "识别失败：录音为空"
    return runCatching {
        createAliyunDashScopeAsrGateway(
            apiKey = asr.apiKeyMasked,
            modelName = normalizedAsrModelName(asr.modelName),
        ).transcribe(
            AsrRequest(
                pcm16kMonoAudio = pcm,
                inputAudioFormat = "pcm",
                sampleRate = 16000,
            )
        ).text
    }.getOrElse { error ->
        logVoiceChat("asr_failed", error.stackTraceToString())
        "识别失败：${error.message ?: error::class.simpleName}"
    }.ifBlank { "识别为空" }
}

private fun isUsableTranscript(text: String): Boolean {
    return text.isNotBlank() &&
        !text.startsWith("本地录音") &&
        !text.startsWith("识别失败") &&
        text != "识别为空" &&
        text != "正在识别..."
}

private suspend fun replyToUserInput(
    configs: List<ModelConfig>,
    fallback: FakeModelGateway,
    conversation: Conversation,
    userText: String,
    userTextForModel: String,
    messagesByConversation: MutableMap<String, List<ChatMessage>>,
    chatRepository: ChatRepository,
    vectorStoreGateway: VectorStoreGateway,
    saveTtsAudio: (ByteArray, String) -> RecordedAudio?,
) {
    val gateway = createChatGateway(configs, fallback)
    val contextLines = buildChatContext(
        conversation = conversation,
        messages = messagesByConversation[conversation.id].orEmpty(),
        chatRepository = chatRepository,
        vectorStoreGateway = vectorStoreGateway,
    )
    val reply = runCatching {
        gateway.chat(
            ChatModelRequest(
                characterId = conversation.characterId,
                userText = userTextForModel,
                recentMessages = contextLines,
            )
        )
    }.getOrElse { error ->
        val failedMessage = ChatMessage(
            id = "failed_${System.currentTimeMillis()}",
            conversationId = conversation.id,
            senderType = SenderType.System,
            senderId = "system",
            messageType = MessageType.Text,
            content = MessageContent.Text("模型调用失败：${error.message ?: error::class.simpleName}。请检查设置里的 LLM 配置，或暂时关闭该模型配置使用离线测试回复。"),
            status = MessageStatus.Failed,
            createdAtMillis = System.currentTimeMillis(),
        )
        messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + failedMessage
        chatRepository.saveMessage(failedMessage)
        return
    }
    logAiReply(
        stage = "chat_reply",
        message = "sourceText=${userText.take(120)}, segments=${reply.messages.size}, mood=${reply.mood}, raw=${reply.messages.joinToString { segment -> "${segment.type}:${segment.text ?: segment.stickerId ?: segment.prompt}" }.take(500)}",
    )
    saveMemoryHints(
        configs = configs,
        characterId = conversation.characterId,
        sourceMessageId = "input_${System.currentTimeMillis()}",
        memoryHints = reply.memoryHints,
        vectorStoreGateway = vectorStoreGateway,
    )
    reply.messages.forEachIndexed { index, segment ->
        delay(segment.delayMs)
        val aiMessage = buildAiReplyMessage(
            configs = configs,
            conversation = conversation,
            segment = segment,
            saveTtsAudio = saveTtsAudio,
            index = index,
        )
        messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + aiMessage
        chatRepository.saveMessage(aiMessage)
        if (segment.type.lowercase() == "voice" && aiMessage.messageType == MessageType.Text) {
            val ttsFailedMessage = ChatMessage(
                id = "tts_failed_${System.currentTimeMillis()}_$index",
                conversationId = conversation.id,
                senderType = SenderType.System,
                senderId = "system",
                messageType = MessageType.Text,
                content = MessageContent.Text("语音合成失败，已降级为文字。请检查 TTS 配置、voiceId 或 Minimax 额度。"),
                status = MessageStatus.Failed,
                createdAtMillis = System.currentTimeMillis(),
            )
            messagesByConversation[conversation.id] = messagesByConversation[conversation.id].orEmpty() + ttsFailedMessage
            chatRepository.saveMessage(ttsFailedMessage)
        }
    }
    extractMemoryIfNeeded(
        configs = configs,
        conversation = conversation,
        gateway = gateway,
        chatRepository = chatRepository,
        vectorStoreGateway = vectorStoreGateway,
    )
}

private fun buildCallSummary(transcript: String, aiText: String): String {
    val userPart = transcript.takeIf { it.isNotBlank() }?.let { "用户：${it.take(80)}" }.orEmpty()
    val aiPart = aiText.takeIf { it.isNotBlank() }?.let { "AI：${it.take(80)}" }.orEmpty()
    return listOf(userPart, aiPart).filter { it.isNotBlank() }.joinToString("\n").ifBlank { "已结束" }
}

private fun buildCallSummary(turns: List<CallTurn>): String {
    return turns.takeLast(8).joinToString("\n") { "${it.speaker}：${it.text.take(100)}" }.ifBlank { "已结束" }
}

private suspend fun synthesizeAndPlayCallReply(
    configs: List<ModelConfig>,
    text: String,
    onSaveTtsAudio: (ByteArray, String) -> RecordedAudio?,
    onPlayVoiceAndWait: (String, (Boolean) -> Unit) -> Unit,
): Boolean {
    val tts = configs.firstOrNull { it.modelType == ModelType.Tts && it.enabled } ?: return false
    if (tts.baseUrl.isBlank() || tts.apiKeyMasked.isBlank()) return false
    val ttsModel = tts.modelName.substringBefore("|", "speech-2.8-hd").ifBlank { "speech-2.8-hd" }
    val voiceId = tts.modelName.substringAfter("|", "male-qn-qingse").ifBlank { "male-qn-qingse" }
    val synthesized = synthesizeTtsAudio(tts.baseUrl, tts.apiKeyMasked, ttsModel, voiceId, text) ?: return false
    val recorded = onSaveTtsAudio(synthesized.first, synthesized.second) ?: return false
    return suspendCancellableCoroutine { continuation ->
        onPlayVoiceAndWait(recorded.path) { success ->
            if (continuation.isActive) continuation.resumeWith(Result.success(success))
        }
    }
}

private suspend fun synthesizeTtsAudio(
    endpoint: String,
    apiKey: String,
    modelName: String,
    voiceId: String,
    text: String,
): Pair<ByteArray, String>? {
    val gateway = createMinimaxTtsGateway(
        endpoint = endpoint,
        apiKey = apiKey,
        modelName = modelName,
    )
    val result = runCatching {
        gateway.synthesize(
            TtsRequest(
                text = text,
                voiceId = voiceId,
                format = "mp3",
            )
        )
    }.getOrElse { error ->
        logAiReply("tts_mp3_failed", error.message ?: error::class.simpleName.orEmpty())
        null
    }
    val bytes = result?.audioBytes
    if (bytes != null && bytes.isNotEmpty()) return bytes to "mp3"
    return null
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

private suspend fun buildChatContext(
    conversation: Conversation,
    messages: List<ChatMessage>,
    chatRepository: ChatRepository,
    vectorStoreGateway: VectorStoreGateway,
): List<String> {
    val summary = chatRepository.getConversationSummary(conversation.id)
        ?.takeIf { it.isNotBlank() }
        ?.let { "滚动摘要：$it" }
    val latestUserText = messages.asReversed().firstOrNull { it.senderType == SenderType.User }
        ?.let { (it.content as? MessageContent.Text)?.text }
        .orEmpty()
    val memories = if (latestUserText.isBlank()) {
        emptyList()
    } else {
        vectorStoreGateway.search(
            com.yourname.aichatmvptest.shared.vector.VectorSearchRequest(
                characterId = conversation.characterId,
                query = latestUserText,
                limit = 4,
            )
        ).map { "相关记忆：${it.content}" }
    }
    val recentMessages = messages.takeLast(12).mapNotNull { message ->
        val speaker = when (message.senderType) {
            SenderType.User -> "用户"
            SenderType.Ai -> "AI"
            SenderType.System -> "系统"
        }
        when (val content = message.content) {
            is MessageContent.Text -> "$speaker：${content.text}"
            is MessageContent.Voice -> "${if (message.senderType == SenderType.User) "用户语音" else "AI语音"}：${content.text.orEmpty()}"
            is MessageContent.Image -> "$speaker：[图片] ${content.caption ?: content.prompt ?: content.uri.take(42)}"
            is MessageContent.Sticker -> "$speaker：[表情] ${content.alt ?: content.stickerId}"
            is MessageContent.Gift -> "$speaker：[礼物] ${content.name}"
            is MessageContent.Call -> "$speaker：[通话] ${content.callType} ${content.status}"
        }
    }
    return listOfNotNull(summary) + memories + recentMessages
}

private suspend fun extractMemoryIfNeeded(
    configs: List<ModelConfig>,
    conversation: Conversation,
    gateway: ModelGateway,
    chatRepository: ChatRepository,
    vectorStoreGateway: VectorStoreGateway,
) {
    val count = chatRepository.countMessages(conversation.id)
    if (count < MEMORY_EXTRACTION_INTERVAL || count % MEMORY_EXTRACTION_INTERVAL != 0L) return
    val messages = chatRepository.getMessages(conversation.id).takeLast(MEMORY_EXTRACTION_INTERVAL.toInt())
    val dialogue = messages.mapNotNull { message ->
        val speaker = if (message.senderType == SenderType.User) "用户" else "AI"
        when (val content = message.content) {
            is MessageContent.Text -> "$speaker：${content.text}"
            is MessageContent.Voice -> "${if (message.senderType == SenderType.User) "用户语音" else "AI语音"}：${content.text.orEmpty()}"
            is MessageContent.Image -> "$speaker：[图片] ${content.caption ?: content.prompt ?: "图片"}"
            is MessageContent.Sticker -> "$speaker：[表情] ${content.alt ?: content.stickerId}"
            is MessageContent.Gift -> "$speaker：[礼物] ${content.name}"
            is MessageContent.Call -> "$speaker：[通话] ${content.callType} ${content.status}"
        }
    }
    val summaryText = dialogue.joinToString("\n").take(1800)
    if (summaryText.isBlank()) return
    val extraction = runCatching {
        gateway.extractMemory(
            MemoryExtractionRequest(
                characterId = conversation.characterId,
                currentSummary = chatRepository.getConversationSummary(conversation.id),
                dialogue = dialogue,
            )
        )
    }.getOrNull()
    chatRepository.saveConversationSummary(
        conversation.id,
        extraction?.rollingSummary?.takeIf { it.isNotBlank() } ?: summaryText,
    )

    val hints = extraction?.memories?.takeIf { it.isNotEmpty() } ?: extractSimpleMemoryHints(messages)
    saveMemoryHints(
        configs = configs,
        characterId = conversation.characterId,
        sourceMessageId = "summary_${System.currentTimeMillis()}",
        memoryHints = hints,
        vectorStoreGateway = vectorStoreGateway,
    )
}

private fun extractSimpleMemoryHints(messages: List<ChatMessage>): List<com.yourname.aichatmvptest.shared.modelgateway.MemoryHint> {
    return messages.mapNotNull { message ->
        val text = message.memoryCandidateText().trim()
        if (text.length < 6) return@mapNotNull null
        val type = when {
            listOf("喜欢", "讨厌", "不喜欢", "爱吃", "不爱").any { it in text } -> "preference_expression"
            listOf("想", "希望", "打算").any { it in text } -> "intent_wish"
            listOf("答应", "约好", "明天", "下次", "以后").any { it in text } -> "agreement_commitment"
            listOf("难受", "头疼", "困", "累", "饿", "疼").any { it in text } -> "physiological_state"
            listOf("开心", "难过", "焦虑", "生气", "害怕", "沮丧").any { it in text } -> "emotion_state"
            else -> return@mapNotNull null
        }
        val subject = if (message.senderType == SenderType.User) "user" else "ai"
        com.yourname.aichatmvptest.shared.modelgateway.MemoryHint(
            type = type,
            subject = subject,
            content = if (subject == "user") "用户说：$text" else "AI说：$text",
            importance = if (type == "preference_expression" || type == "agreement_commitment") 0.8 else 0.6,
            confidence = 0.7,
        )
    }.take(8)
}

private fun ChatMessage.memoryCandidateText(): String {
    return when (val content = content) {
        is MessageContent.Text -> content.text
        is MessageContent.Voice -> content.text.orEmpty()
        is MessageContent.Call -> content.status
        is MessageContent.Image -> content.caption ?: content.prompt.orEmpty()
        is MessageContent.Sticker -> content.alt.orEmpty()
        is MessageContent.Gift -> content.name
    }
}

private suspend fun buildAiReplyMessage(
    configs: List<ModelConfig>,
    conversation: Conversation,
    segment: AiReplySegment,
    saveTtsAudio: (ByteArray, String) -> RecordedAudio?,
    index: Int,
): ChatMessage {
    val segmentText = segment.text.orEmpty()
    val segmentType = segment.type.lowercase()
    logAiReply(
        stage = "build_segment",
        message = "type=$segmentType text=${segmentText.take(160)} sticker=${segment.stickerId} prompt=${segment.prompt}",
    )
    if (segmentType == "voice" && segmentText.isNotBlank()) {
        val tts = configs.firstOrNull { it.modelType == ModelType.Tts && it.enabled }
        if (tts != null && tts.baseUrl.isNotBlank() && tts.apiKeyMasked.isNotBlank()) {
            val ttsModel = tts.modelName.substringBefore("|", "speech-2.8-hd").ifBlank { "speech-2.8-hd" }
            val voiceId = tts.modelName.substringAfter("|", "male-qn-qingse").ifBlank { "male-qn-qingse" }
            val synthesized = synthesizeTtsAudio(tts.baseUrl, tts.apiKeyMasked, ttsModel, voiceId, segmentText)
            val recorded = synthesized?.let { (bytes, extension) -> saveTtsAudio(bytes, extension) }
            if (recorded != null) {
                logAiReply(
                    stage = "voice_segment_saved",
                    message = "path=${recorded.path} duration=${recorded.durationMs} text=${segmentText.take(160)}",
                )
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
            logAiReply(
                stage = "voice_segment_tts_failed",
                message = "fallback=text",
            )
        } else {
            logAiReply(stage = "voice_segment_no_tts", message = "fallback=text")
        }
    }

    if (segmentType == "sticker") {
        return ChatMessage(
            id = "ai_sticker_${System.currentTimeMillis()}_$index",
            conversationId = conversation.id,
            senderType = SenderType.Ai,
            senderId = conversation.characterId,
            messageType = MessageType.Sticker,
            content = MessageContent.Sticker(
                stickerId = segment.stickerId ?: "smile_01",
                alt = segment.alt,
            ),
            status = MessageStatus.Sent,
            createdAtMillis = System.currentTimeMillis(),
        )
    }

    if (segmentType == "image") {
        return ChatMessage(
            id = "ai_image_${System.currentTimeMillis()}_$index",
            conversationId = conversation.id,
            senderType = SenderType.Ai,
            senderId = conversation.characterId,
            messageType = MessageType.Image,
            content = MessageContent.Image(
                uri = "generated://pending/${System.currentTimeMillis()}",
                prompt = segment.prompt,
                caption = segment.caption ?: segment.prompt,
            ),
            status = MessageStatus.Sent,
            createdAtMillis = System.currentTimeMillis(),
        )
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

private const val MEMORY_EXTRACTION_INTERVAL = 8L

private suspend fun testModelConfig(config: ModelConfig): String {
    val cleanedConfig = config.copy(baseUrl = cleanModelBaseUrl(config.baseUrl))
    if (cleanedConfig.modelType != ModelType.Asr && cleanedConfig.baseUrl.isBlank()) return "失败：Base URL 不能为空"
    if (config.apiKeyMasked.isBlank()) return "失败：API Key 不能为空"
    if (cleanedConfig.baseUrl.contains("{") || cleanedConfig.baseUrl.contains("}") || cleanedConfig.baseUrl.contains("你的WorkspaceId")) {
        return "失败：Base URL 仍包含占位符，请把 {WorkspaceId} 替换为阿里百炼控制台里的真实 WorkspaceId"
    }

    logModelTest(cleanedConfig, "start", "baseUrl=${cleanedConfig.baseUrl}, modelName=${cleanedConfig.modelName}, enabled=${cleanedConfig.enabled}, key=${maskKeyForLog(cleanedConfig.apiKeyMasked)}")
    return runCatching {
        withTimeout(30_000L) {
            when (cleanedConfig.modelType) {
            ModelType.Llm -> {
                val normalizedBaseUrl = normalizeOpenAiBaseUrl(cleanedConfig.baseUrl)
                logModelTest(cleanedConfig, "llm_request", "normalizedBaseUrl=$normalizedBaseUrl, model=${cleanedConfig.modelName.ifBlank { "deepseek-v4-flash" }}")
                val reply = createOpenAiCompatibleGateway(
                    baseUrl = normalizedBaseUrl,
                    apiKey = cleanedConfig.apiKeyMasked,
                    modelName = cleanedConfig.modelName.ifBlank { "deepseek-v4-flash" },
                ).voiceCall(
                    VoiceCallRequest(
                        characterId = "test",
                        userText = "请用一句中文回复：连接成功。",
                        recentMessages = emptyList(),
                    )
                )
                if (reply.isBlank()) {
                    logModelTest(cleanedConfig, "llm_empty", "response is blank")
                    "失败：LLM 已返回但内容为空，请检查模型名是否可用"
                } else {
                    logModelTest(cleanedConfig, "llm_success", reply.take(200))
                    "成功：${reply.take(80)}\n实际请求 Base URL: $normalizedBaseUrl"
                }
            }
            ModelType.Vision -> {
                logModelTest(cleanedConfig, "vision_request", "endpoint=${cleanedConfig.baseUrl}, model=${cleanedConfig.modelName.ifBlank { "qwen3-vl-plus" }}")
                var result = testVisionOnce(cleanedConfig)
                if (isProviderTransientVisionError(result.text)) {
                    logModelTest(cleanedConfig, "vision_retry", "provider returned transient server error, retrying once: ${result.text.take(240)}")
                    delay(800)
                    result = testVisionOnce(cleanedConfig)
                }
                if (result.text.isBlank()) {
                    logModelTest(cleanedConfig, "vision_empty", "response is blank")
                    "失败：视觉模型返回为空"
                } else if (looksLikeProviderError(result.text)) {
                    logModelTest(cleanedConfig, "vision_provider_error", result.text.take(500))
                    when {
                        result.text.contains("Endpoint.AccessDenied", ignoreCase = true) -> {
                            "失败：Endpoint.AccessDenied。当前 DashScope API Key 无权访问这个 Workspace endpoint。请确认 API Key 的地域、业务空间ID llm-imxtee9l3et45y6z、endpoint 与模型权限属于同一阿里百炼业务空间。原始返回：${result.text.take(160)}"
                        }
                        isProviderTransientVisionError(result.text) -> {
                            "失败：阿里视觉模型服务端返回 500/InternalError.Algo。这表示请求已到达模型服务，但模型处理内部失败，通常是服务端临时异常或该模型当前不可用。请稍后重试，或在阿里控制台确认 qwen3-vl-plus 当前可用。原始返回：${result.text.take(180)}"
                        }
                        else -> {
                            "失败：服务商返回错误：${result.text.take(160)}"
                        }
                    }
                } else {
                    logModelTest(cleanedConfig, "vision_success", result.text.take(200))
                    "成功：${result.text.take(80)}"
                }
            }
            ModelType.Embedding -> {
                logModelTest(cleanedConfig, "embedding_request", "endpoint=${cleanedConfig.baseUrl}, model=${cleanedConfig.modelName.ifBlank { "text-embedding-v4" }}")
                if (!cleanedConfig.provider.contains("阿里") && !cleanedConfig.baseUrl.contains("aliyuncs.com")) {
                    logModelTest(cleanedConfig, "embedding_rejected", "unsupported endpoint/provider")
                    "失败：当前 Embedding 测试只支持阿里 text-embedding-v4，不支持 DeepSeek/OpenAI 兼容地址"
                } else {
                    val embedding = createAliyunTextEmbeddingGateway(
                        endpoint = cleanedConfig.baseUrl,
                        apiKey = cleanedConfig.apiKeyMasked,
                        modelName = cleanedConfig.modelName.ifBlank { "text-embedding-v4" },
                    ).embed("连接测试")
                    if (embedding.isEmpty()) {
                        logModelTest(cleanedConfig, "embedding_empty", "empty vector")
                        "失败：Embedding 返回空向量"
                    } else {
                        logModelTest(cleanedConfig, "embedding_success", "dimension=${embedding.size}")
                        "成功：向量维度 ${embedding.size}"
                    }
                }
            }
            ModelType.Tts -> {
                logModelTest(cleanedConfig, "tts_request", "endpoint=${cleanedConfig.baseUrl}, modelNameField=${cleanedConfig.modelName}")
                if (!cleanedConfig.baseUrl.startsWith("wss://") || !cleanedConfig.baseUrl.contains("minimax", ignoreCase = true)) {
                    logModelTest(cleanedConfig, "tts_rejected", "unsupported endpoint")
                    "失败：当前 TTS 仅支持 Minimax WebSocket，请填写 wss://api.minimaxi.com/ws/v1/t2a_v2；Model 可填 speech-2.8-hd|male-qn-qingse"
                } else {
                    val model = cleanedConfig.modelName.substringBefore("|", "speech-2.8-hd").ifBlank { "speech-2.8-hd" }
                    val voiceId = cleanedConfig.modelName.substringAfter("|", "male-qn-qingse").ifBlank { "male-qn-qingse" }
                    val result = createMinimaxTtsGateway(
                        endpoint = cleanedConfig.baseUrl,
                        apiKey = cleanedConfig.apiKeyMasked,
                        modelName = model,
                    ).synthesize(TtsRequest(text = "连接测试", voiceId = voiceId))
                    val bytes = result.audioBytes?.size ?: 0
                    if (bytes <= 0) {
                        logModelTest(cleanedConfig, "tts_empty_audio", "model=$model, voiceId=$voiceId, bytes=$bytes")
                        "失败：Minimax 已连接但未返回音频数据，请检查 API Key、模型名和 voiceId。建议 Model 填 speech-2.8-hd|male-qn-qingse"
                    } else {
                        logModelTest(cleanedConfig, "tts_success", "model=$model, voiceId=$voiceId, bytes=$bytes")
                        "成功：音频 $bytes bytes"
                    }
                }
            }
            ModelType.Asr -> {
                logModelTest(cleanedConfig, "asr_request", "provider=DashScope, model=${normalizedAsrModelName(cleanedConfig.modelName)}")
                val wav = generatePcm16ToneWav()
                val result = createAliyunDashScopeAsrGateway(
                    apiKey = cleanedConfig.apiKeyMasked,
                    modelName = normalizedAsrModelName(cleanedConfig.modelName),
                ).transcribe(
                    AsrRequest(
                        pcm16kMonoAudio = wav.copyOfRange(44, wav.size),
                        inputAudioFormat = "pcm",
                        sampleRate = 16000,
                    )
                )
                logModelTest(cleanedConfig, "asr_success", "text=${result.text}, emotion=${result.emotion}")
                "成功：DashScope ASR WebSocket 已连通。测试音频为 440Hz 蜂鸣音，识别结果：${result.text.ifBlank { "（无文本，属正常）" }}"
            }
            ModelType.VectorStore -> {
                logModelTest(cleanedConfig, "vector_store_skipped", "no remote vector store gateway implemented")
                "已保存：向量库测试将在记忆检索流程内执行"
            }
            }
        }
    }.getOrElse { error ->
        logModelTest(config, "error", error.stackTraceToString())
        "失败：${error.message ?: error::class.simpleName}"
    }
}

private suspend fun testVisionOnce(config: ModelConfig): com.yourname.aichatmvptest.shared.modelgateway.VisionAnalyzeResponse {
    return createAliyunQwenVlGateway(
        endpoint = config.baseUrl,
        apiKey = config.apiKeyMasked,
        modelName = config.modelName.ifBlank { "qwen3-vl-plus" },
    ).analyzeImage(
        VisionAnalyzeRequest(
            imageUrlOrBase64 = "https://img.alicdn.com/imgextra/i1/O1CN01gDEY8M1W114Hi3XcN_!!6000000002727-0-tps-1024-406.jpg",
            prompt = "请用一句话描述这张图。",
        )
    )
}

private fun logModelTest(config: ModelConfig, stage: String, message: String) {
    Log.d(
        MODEL_TEST_LOG_TAG,
        "keyword=$MODEL_TEST_LOG_KEYWORD stage=$stage type=${config.modelType} provider=${config.provider} model=${config.modelName} $message",
    )
}

private fun logAiReply(stage: String, message: String) {
    Log.d(AI_REPLY_LOG_TAG, "keyword=$AI_REPLY_LOG_KEYWORD stage=$stage $message")
}

private fun maskKeyForLog(apiKey: String): String {
    if (apiKey.isBlank()) return "blank"
    if (apiKey.length <= 8) return "***"
    return apiKey.take(4) + "..." + apiKey.takeLast(4)
}

private const val MODEL_TEST_LOG_TAG = "RhodesModelTest"
private const val MODEL_TEST_LOG_KEYWORD = "RHODES_MODEL_TEST"
private const val AI_REPLY_LOG_TAG = "RhodesAiReply"
private const val AI_REPLY_LOG_KEYWORD = "RHODES_AI_REPLY"
private const val VOICE_CHAT_LOG_TAG = "RhodesVoiceChat"
private const val VOICE_CHAT_LOG_KEYWORD = "RHODES_VOICE_CHAT"

private fun logVoiceChat(stage: String, message: String) {
    Log.d(VOICE_CHAT_LOG_TAG, "keyword=$VOICE_CHAT_LOG_KEYWORD stage=$stage $message")
}

private fun generatePcm16ToneWav(sampleRate: Int = 16_000, durationMs: Int = 700): ByteArray {
    val sampleCount = sampleRate * durationMs / 1000
    val pcm = ByteArray(sampleCount * 2)
    repeat(sampleCount) { index ->
        val value = (sin(2.0 * PI * 440.0 * index / sampleRate) * Short.MAX_VALUE * 0.25).toInt().toShort()
        pcm[index * 2] = (value.toInt() and 0xff).toByte()
        pcm[index * 2 + 1] = ((value.toInt() shr 8) and 0xff).toByte()
    }
    return buildWavFile(pcm = pcm, sampleRate = sampleRate, channels = 1, bitsPerSample = 16)
}

private fun buildWavFile(pcm: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8
    val output = ByteArray(44 + pcm.size)
    fun writeAscii(offset: Int, value: String) {
        value.forEachIndexed { index, char -> output[offset + index] = char.code.toByte() }
    }
    fun writeIntLe(offset: Int, value: Int) {
        output[offset] = (value and 0xff).toByte()
        output[offset + 1] = ((value shr 8) and 0xff).toByte()
        output[offset + 2] = ((value shr 16) and 0xff).toByte()
        output[offset + 3] = ((value shr 24) and 0xff).toByte()
    }
    fun writeShortLe(offset: Int, value: Int) {
        output[offset] = (value and 0xff).toByte()
        output[offset + 1] = ((value shr 8) and 0xff).toByte()
    }
    writeAscii(0, "RIFF")
    writeIntLe(4, 36 + pcm.size)
    writeAscii(8, "WAVE")
    writeAscii(12, "fmt ")
    writeIntLe(16, 16)
    writeShortLe(20, 1)
    writeShortLe(22, channels)
    writeIntLe(24, sampleRate)
    writeIntLe(28, byteRate)
    writeShortLe(32, blockAlign)
    writeShortLe(34, bitsPerSample)
    writeAscii(36, "data")
    writeIntLe(40, pcm.size)
    pcm.copyInto(output, destinationOffset = 44)
    return output
}

private fun normalizeOpenAiBaseUrl(baseUrl: String): String {
    val trimmed = cleanModelBaseUrl(baseUrl).trimEnd('/')
    return if (trimmed == "https://api.deepseek.com" || trimmed == "http://api.deepseek.com") {
        "$trimmed/v1"
    } else {
        trimmed
    }
}

private fun cleanModelBaseUrl(baseUrl: String): String {
    return baseUrl
        .trim()
        .trimEnd('\\')
        .trim()
        .replace("\n", "")
        .replace("\r", "")
}

private fun normalizeModelConfigForSave(config: ModelConfig): ModelConfig {
    val normalizedModel = when (config.modelType) {
        ModelType.Asr -> normalizedAsrModelName(config.modelName)
        else -> config.modelName.ifBlank { recommendedModelName(config.modelType) }
    }
    return config.copy(
        baseUrl = if (config.modelType == ModelType.Asr) "" else cleanModelBaseUrl(config.baseUrl),
        modelName = normalizedModel,
    )
}

private fun looksLikeProviderError(text: String): Boolean {
    val lower = text.lowercase()
    return lower.contains("\"code\"") && lower.contains("\"message\"") ||
        lower.contains("accessdenied") ||
        lower.contains("access denied") ||
        lower.contains("internalerror") ||
        lower.contains("http_status/500") ||
        lower.contains("modelservingerror") ||
        lower.contains("invalid api") ||
        lower.contains("unauthorized")
}

private fun isProviderTransientVisionError(text: String): Boolean {
    val lower = text.lowercase()
    return lower.contains("http_status/500") ||
        lower.contains("internalerror.algo") ||
        lower.contains("modelservingerror")
}
