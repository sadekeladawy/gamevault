package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Game
import com.example.data.model.ai.ChatMessage
import com.example.data.model.ai.MessageSender
import com.example.data.remote.rawg.RawgGameDto
import com.example.data.repository.AiChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private var lastUserPrompt: String? = null

    init {
        // Welcome initial message
        val welcomeMsg = ChatMessage(
            sender = MessageSender.AI,
            text = "👋 Hello! I'm **GameVault AI**, powered by Firebase AI Logic.\n\nAsk me 'What should I play tonight?', 'Help me choose from my backlog', compare games, or search RAWG!"
        )
        _messages.value = listOf(welcomeMsg)
    }

    fun onInputTextChanged(newText: String) {
        _inputText.value = newText
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

        viewModelScope.launch {
            try {
                var fetchedRawgGames = emptyList<RawgGameDto>()
                var fetchedRawgTag: String? = null

                aiChatRepository.processUserMessageStream(
                    userText = textToSend,
                    conversationHistory = updatedList.dropLast(1),
                    userBacklog = userBacklog,
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
            text = "Chat cleared! What game would you like to explore or compare next?"
        )
        _messages.value = listOf(welcomeMsg)
        _errorMessage.value = null
        lastUserPrompt = null
    }
}
