package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.data.RawgApiService
import com.example.data.RawgGame

@Composable
fun GameSearchScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var gameResult by remember { mutableStateOf<RawgGame?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { RawgApiService.create() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("اكتب اسم اللعبة هنا") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (searchQuery.isNotEmpty()) {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            // 👇 حط الـ API Key بتاعك هنا 👇
                            val response = apiService.searchGames(
                                apiKey = "f63b923ffe2c4982a204425ec383a3e4",
                                query = searchQuery
                            )
                            gameResult = response.results.firstOrNull()
                        } catch (e: Exception) {
                            // تم تجاهل الخطأ مؤقتاً
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("بحث")
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (gameResult != null) {
            Text(text = "النتيجة: ${gameResult!!.name}", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            AsyncImage(
                model = gameResult!!.backgroundImage,
                contentDescription = "صورة اللعبة",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
        }
    }
}