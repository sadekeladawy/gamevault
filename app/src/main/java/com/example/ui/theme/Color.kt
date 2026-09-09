package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// GameVault Modern Dark Gaming Palette (Charcoal / Deep Red / Near-White)
val DarkBg = Color(0xFF0B0B0D)
val DarkSurface = Color(0xFF141416)
val DarkCard = Color(0xFF1C1C1F)
val DarkCardBorder = Color(0xFF27272C)
val DarkCardHover = Color(0xFF242429)

// Accent Colors (Refined & Modern Crimson Red)
val PrimaryRed = Color(0xFFE53935)
val PrimaryRedMuted = Color(0xFFC62828)
val PrimaryRedSubtle = Color(0x28E53935)
val PrimaryAccent = PrimaryRed
val PrimaryAccentVariant = PrimaryRedMuted
val AccentEmerald = Color(0xFF10B981) // Completed
val AccentBlue = Color(0xFF3B82F6)    // Playing
val AccentAmber = Color(0xFFF59E0B)   // Rating / Warning
val AccentRose = PrimaryRed           // Favorite / Delete / Dropped

// Full backward compatibility aliases remapped to modern Black/Red system:
val CyberPurple = PrimaryRed
val CyberPurpleVariant = PrimaryRedMuted
val NeonCyan = Color(0xFFE2E2E8)      // Clean high-contrast neutral highlight (formerly cyan)
val NeonCyanBright = PrimaryRed

// Text Hierarchy
val TextPrimary = Color(0xFFF4F4F6)
val TextSecondary = Color(0xFF9E9EA8)
val TextMuted = Color(0xFF6B6B76)

// Status Specific Tones
val StatusCompletedColor = AccentEmerald
val StatusPlayingColor = AccentBlue
val StatusBacklogColor = Color(0xFF94A3B8)
val StatusDroppedColor = PrimaryRed

// Platform Accent Tones
val PlatformPC = Color(0xFF94A3B8)
val PlatformPlayStation = Color(0xFF3B82F6)
val PlatformXbox = Color(0xFF16A34A)
val PlatformNintendo = PrimaryRed
val PlatformSteamDeck = PrimaryRed
