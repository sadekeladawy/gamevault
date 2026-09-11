package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Game
import com.example.data.model.SeriesGameItem
import com.example.data.model.ai.ChatMessage
import com.example.data.model.ai.MessageSender
import com.example.data.remote.ai.GameVaultAiContextBuilder
import com.example.data.remote.rawg.RawgGameDto
import com.example.data.repository.AiChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiActiveContext(
    val title: String,
    val subtitle: String? = null,
    val coverUrl: String? = null,
    val quickPrompts: List<String> = emptyList(),
    val focusedContextPrompt: String? = null
)

class AiChatViewModel(
    private val aiChatRepository: AiChatRepository = AiChatRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _activeContext = MutableStateFlow<AiActiveContext?>(null)
    val activeContext: StateFlow<AiActiveContext?> = _activeContext.asStateFlow()

    private var lastUserPrompt: String? = null
    private var streamingJob: Job? = null

    init {
        val welcomeMsg = ChatMessage(
            sender = MessageSender.AI,
            text = "👋 Hello! I'm your **GameVault AI Gaming Copilot**.\n\n" +
                    "I have real-time awareness of your vault games, franchises, playtime, and backlog. Ask me:\n" +
                    "• *\"What should I play next?\"*\n" +
                    "• *\"Which game in my backlog is the shortest?\"*\n" +
                    "• *\"Analyze my gaming habits\"*"
        )
        _messages.value = listOf(welcomeMsg)
    }

    fun onInputTextChanged(newText: String) {
        _inputText.value = newText
    }

    fun setGameContext(game: Game) {
        val context = AiActiveContext(
            title = game.title,
            subtitle = "${game.platform} • ${game.status.displayName}",
            coverUrl = game.coverUrl,
            quickPrompts = listOf(
                "Is this game worth playing?",
                "Should I play this next?",
                "How long will it take to finish?",
                "Would I like this based on my library?",
                "Compare with similar games"
            ),
            focusedContextPrompt = GameVaultAiContextBuilder.buildGameFocusedContext(game)
        )
        _activeContext.value = context
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "👋 I'm focused on **${game.title}** (${game.status.displayName}).\n\nAsk me anything about its story, gameplay, backlog priority, or whether you should play it next!"
            )
        )
    }

    fun setRawgGameContext(dto: RawgGameDto) {
        val context = AiActiveContext(
            title = dto.name,
            subtitle = "Rating: ${dto.rating ?: 0.0}/5.0 • ${dto.released ?: "TBD"}",
            coverUrl = dto.backgroundImage.orEmpty(),
            quickPrompts = listOf(
                "Is this game worth playing?",
                "How long will it take to finish?",
                "What makes this game good?",
                "Should I add this to my Vault?"
            ),
            focusedContextPrompt = GameVaultAiContextBuilder.buildRawgGameFocusedContext(dto)
        )
        _activeContext.value = context
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "👋 I'm focused on **${dto.name}**.\n\nThinking about playing this? Ask me about reviews, difficulty, playtime, or community consensus!"
            )
        )
    }

    fun setFranchiseContext(franchiseName: String, seriesGames: List<SeriesGameItem>) {
        val context = AiActiveContext(
            title = "$franchiseName Series",
            subtitle = "${seriesGames.size} games in series",
            coverUrl = seriesGames.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl,
            quickPrompts = listOf(
                "Which game should I play first?",
                "Show me the best order to play these",
                "Which games are essential?",
                "Which games are in my Vault?"
            ),
            focusedContextPrompt = GameVaultAiContextBuilder.buildFranchiseFocusedContext(franchiseName, seriesGames)
        )
        _activeContext.value = context
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "👋 I'm focused on the **$franchiseName** series (${seriesGames.size} games).\n\nAsk me about play order, lore, essential entries, or what you should play next!"
            )
        )
    }

    fun clearActiveContext() {
        _activeContext.value = null
    }

    fun cancelGeneration() {
        streamingJob?.cancel()
        _isThinking.value = false
    }

    fun sendMessage(customText: String? = null, userBacklog: List<Game> = emptyList()) {
        val textToSend = customText ?: _inputText.value.trim()
        if (textToSend.isBlank() || _isThinking.value) return

        lastUserPrompt = textToSend
        _inputText.value = ""
        _errorMessage.value = null

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = textToSend
        )

        val initialAiMessage = ChatMessage(
            sender = MessageSender.AI,
            text = "",
            isLoading = true
        )

        val updatedList = _messages.value + userMessage + initialAiMessage
        _messages.value = updatedList
        _isThinking.value = true

        val aiMessageIndex = updatedList.size - 1

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            try {
                var fetchedRawgGames = emptyList<RawgGameDto>()
                var fetchedRawgTag: String? = null

                aiChatRepository.processUserMessageStream(
                    userText = textToSend,
                    conversationHistory = updatedList.dropLast(1),
                    userBacklog = userBacklog,
                    focusedContext = _activeContext.value?.focusedContextPrompt,
                    onRawgMetaDataFetched = { games, tag ->
                        fetchedRawgGames = games
                        fetchedRawgTag = tag
                        val currentList = _messages.value.toMutableList()
                        if (aiMessageIndex < currentList.size) {
                            currentList[aiMessageIndex] = currentList[aiMessageIndex].copy(
                                recommendedGames = fetchedRawgGames,
                                rawgQueryUsed = fetchedRawgTag
                            )
                            _messages.value = currentList
                        }
                    },
                    onVaultRecommendationsFound = { vaultGames, franchise, actions ->
                        val currentList = _messages.value.toMutableList()
                        if (aiMessageIndex < currentList.size) {
                            currentList[aiMessageIndex] = currentList[aiMessageIndex].copy(
                                recommendedVaultGames = vaultGames,
                                recommendedFranchise = franchise,
                                suggestedQuickActions = actions
                            )
                            _messages.value = currentList
                        }
                    }
                ).collect { streamedText ->
                    val currentList = _messages.value.toMutableList()
                    if (aiMessageIndex < currentList.size) {
                        currentList[aiMessageIndex] = currentList[aiMessageIndex].copy(
                            text = streamedText,
                            isLoading = false
                        )
                        _messages.value = currentList
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _errorMessage.value = e.localizedMessage ?: "Failed to get AI response. Please try again."
                val currentList = _messages.value.toMutableList()
                if (aiMessageIndex < currentList.size) {
                    currentList[aiMessageIndex] = ChatMessage(
                        sender = MessageSender.AI,
                        text = "⚠️ Sorry, I encountered an issue processing your request: ${e.localizedMessage ?: "Network error"}",
                        isError = true
                    )
                    _messages.value = currentList
                }
            } finally {
                _isThinking.value = false
            }
        }
    }

    fun retryLastMessage(userBacklog: List<Game> = emptyList()) {
        val prompt = lastUserPrompt ?: return
        val currentList = _messages.value.toMutableList()
        if (currentList.lastOrNull()?.isError == true || currentList.lastOrNull()?.text.isNullOrBlank()) {
            currentList.removeAt(currentList.size - 1)
            _messages.value = currentList
        }
        sendMessage(prompt, userBacklog)
    }

    fun clearChat() {
        val welcomeMsg = ChatMessage(
            sender = MessageSender.AI,
            text = "Chat cleared! How can your GameVault Copilot assist you next?"
        )
        _messages.value = listOf(welcomeMsg)
        _errorMessage.value = null
        lastUserPrompt = null
    }
}

