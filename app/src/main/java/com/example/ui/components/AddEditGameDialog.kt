package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.data.sample.MasterGameCatalog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardHover
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val CommonPlatforms = listOf("PC", "PlayStation 5", "Xbox Series X", "Nintendo Switch", "Steam Deck", "PlayStation 4", "Retro")
val CommonGenres = listOf("Action RPG", "Open World", "Action Adventure", "Metroidvania", "Roguelike", "CRPG", "JRPG", "Shooter", "Survival Horror", "Indie", "Strategy")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditGameDialog(
    game: Game? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        coverUrl: String,
        platform: String,
        genre: String,
        releaseYear: Int,
        status: GameStatus,
        completionDate: String?,
        playtimeHours: Double,
        rating: Int,
        notes: String,
        isFavorite: Boolean
    ) -> Unit
) {
    val isEditing = game != null

    var title by remember { mutableStateOf(game?.title ?: "") }
    var coverUrl by remember { mutableStateOf(game?.coverUrl ?: "") }
    var platform by remember { mutableStateOf(game?.platform ?: "PC") }
    var customPlatform by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf(game?.genre ?: "Action RPG") }
    var customGenre by remember { mutableStateOf("") }
    var releaseYearStr by remember { mutableStateOf(game?.releaseYear?.toString() ?: Calendar.getInstance().get(Calendar.YEAR).toString()) }
    var status by remember { mutableStateOf(game?.status ?: GameStatus.BACKLOG) }
    var completionDate by remember {
        mutableStateOf(
            game?.completionDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
    }
    var playtimeHours by remember { mutableDoubleStateOf(game?.playtimeHours ?: 0.0) }
    var rating by remember { mutableIntStateOf(game?.rating ?: 0) }
    var notes by remember { mutableStateOf(game?.notes ?: "") }
    var isFavorite by remember { mutableStateOf(game?.isFavorite ?: false) }

    var titleError by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CyberPurple,
        unfocusedBorderColor = DarkCardBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        cursorColor = NeonCyan,
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkBg),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Game" else "Add New Game",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("add_edit_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title Field
                    Text(
                        text = "GAME TITLE *",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        placeholder = { Text("e.g. Elden Ring, Baldur's Gate 3", color = TextMuted) },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text("Title is required", color = AccentRose) }
                        } else null,
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_game_title")
                    )

                    if (!isEditing && title.length >= 2) {
                        val matchingCatalog = MasterGameCatalog.defaultCatalog.filter {
                            it.title.contains(title, ignoreCase = true)
                        }.take(3)
                        if (matchingCatalog.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "MATCHED IN GAME DATABASE (TAP TO AUTOFILL):",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                matchingCatalog.forEach { master ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkCardHover,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberPurple.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                title = master.title
                                                coverUrl = master.coverUrl
                                                platform = master.platform
                                                genre = master.genre
                                                releaseYearStr = master.releaseYear.toString()
                                                if (notes.isBlank()) notes = master.description
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Cloud,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${master.title} • ${master.genre} (${master.releaseYear})",
                                                color = TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cover Image URL
                    Text(
                        text = "COVER IMAGE URL",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        placeholder = { Text("https://...", color = TextMuted) },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_cover_url")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status Picker
                    Text(
                        text = "COMPLETION STATUS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GameStatus.entries.forEach { s ->
                            val selected = status == s
                            FilterChip(
                                selected = selected,
                                onClick = { status = s },
                                label = { Text(s.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberPurple.copy(alpha = 0.3f),
                                    selectedLabelColor = NeonCyan,
                                    containerColor = DarkCard,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = DarkCardBorder,
                                    selectedBorderColor = CyberPurple
                                )
                            )
                        }
                    }

                    // Completion Date (if Completed)
                    if (status == GameStatus.COMPLETED) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "COMPLETION DATE (YYYY-MM-DD)",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = completionDate,
                            onValueChange = { completionDate = it },
                            placeholder = { Text("YYYY-MM-DD", color = TextMuted) },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_completion_date")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Platform Selection Chips + Custom
                    Text(
                        text = "PLATFORM",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CommonPlatforms.forEach { p ->
                            val selected = platform == p
                            FilterChip(
                                selected = selected,
                                onClick = { platform = p },
                                label = { Text(p) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = NeonCyan,
                                    containerColor = DarkCard,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = DarkCardBorder,
                                    selectedBorderColor = NeonCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Genre Selection Chips
                    Text(
                        text = "GENRE",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CommonGenres.forEach { g ->
                            val selected = genre == g
                            FilterChip(
                                selected = selected,
                                onClick = { genre = g },
                                label = { Text(g) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberPurple.copy(alpha = 0.25f),
                                    selectedLabelColor = CyberPurple,
                                    containerColor = DarkCard,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = DarkCardBorder,
                                    selectedBorderColor = CyberPurple
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Release Year & Playtime (Side-by-side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RELEASE YEAR",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = releaseYearStr,
                                onValueChange = { releaseYearStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = textFieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_release_year")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PLAYTIME (HOURS)",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = if (playtimeHours == 0.0) "" else playtimeHours.toString(),
                                onValueChange = {
                                    playtimeHours = it.toDoubleOrNull() ?: 0.0
                                },
                                placeholder = { Text("0", color = TextMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = textFieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_playtime")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Rating Slider (1 to 10)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RATING (1 - 10)",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (rating > 0) "$rating / 10" else "Unrated",
                            color = if (rating > 0) AccentAmber else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = rating.toFloat(),
                        onValueChange = { rating = it.toInt() },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentAmber,
                            activeTrackColor = AccentAmber,
                            inactiveTrackColor = DarkCardBorder
                        ),
                        modifier = Modifier.testTag("input_rating_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Favorite Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) AccentRose else TextMuted
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Mark as Favorite",
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                        Switch(
                            checked = isFavorite,
                            onCheckedChange = { isFavorite = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentRose,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkCardBorder
                            ),
                            modifier = Modifier.testTag("input_favorite_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Personal Notes Field
                    Text(
                        text = "NOTES & REVIEW",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Write your review, memorable moments, build info...", color = TextMuted) },
                        minLines = 3,
                        maxLines = 6,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_notes")
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Bottom Action Buttons (Cancel, Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_edit_cancel_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }
                            val relYear = releaseYearStr.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                            onSave(
                                game?.id ?: 0L,
                                title.trim(),
                                coverUrl.trim(),
                                platform,
                                genre,
                                relYear,
                                status,
                                if (status == GameStatus.COMPLETED) completionDate else null,
                                playtimeHours,
                                rating,
                                notes.trim(),
                                isFavorite
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_edit_save_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                    ) {
                        Text(
                            text = if (isEditing) "Save Changes" else "Add Game",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
