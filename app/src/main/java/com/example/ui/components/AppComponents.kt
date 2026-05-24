package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) {
        Color(0x331F213A) // Very sleek semi-transparent navy-dark
    } else {
        Color(0x99FFFFFF) // Pristine elegant light glass
    }

    val borderColor = if (isDark) {
        Color(0x2BFFFFFF) // Smooth light-colored white glow
    } else {
        Color(0x336C00D9) // Smooth light purple border for light mode
    }

    Card(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(DarkPurple, ElectricBlue)
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) gradient else Brush.horizontalGradient(listOf(Color.Gray, Color.LightGray)))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun TypeBadge(type: String) {
    val (label, bgColor, textColor) = when (type.uppercase()) {
        "YAPE" -> Triple("YAPE PAGO", Color(0xFF00FFCC), Color(0xFF131522))
        "LINK" -> Triple("ENLACE WEB", Color(0xFF007AFF), Color.White)
        "WIFI" -> Triple("RED WI-FI", Color(0xFFFFCC00), Color(0xFF1C1C1E))
        "WHATSAPP" -> Triple("WHATSAPP", Color(0xFF25D366), Color.White)
        "PHONE" -> Triple("TELÉFONO", Color(0xFFFF5E5B), Color.White)
        "EMAIL" -> Triple("CORREO", Color(0xFF9E00FF), Color.White)
        else -> Triple("TEXTO PLANO", Color(0xFF8E8E93), Color.White)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}
