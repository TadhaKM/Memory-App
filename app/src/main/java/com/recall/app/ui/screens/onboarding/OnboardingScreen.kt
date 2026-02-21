package com.recall.app.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.app.ui.theme.*

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding")

    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Decorative illustration area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                DecorativeIllustration(animatedOffset)
            }

            // Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Capture Your\nThoughts",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 44.sp,
                        color = LightOnBackground
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your personal memory assistant.\nCapture ideas, tasks, and notes with\nAI-powered organization.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = LightOnSurfaceVariant,
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Get Started button
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DecorativeIllustration(animatedOffset: Float) {
    Box(
        modifier = Modifier
            .size(320.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        LightSurfaceVariant,
                        LightSurface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            val width = size.width
            val height = size.height

            // Draw curved lines (like the reference)
            val path = Path().apply {
                moveTo(width * 0.2f, height * 0.3f + animatedOffset)
                cubicTo(
                    width * 0.4f, height * 0.1f + animatedOffset,
                    width * 0.6f, height * 0.5f + animatedOffset,
                    width * 0.8f, height * 0.3f + animatedOffset
                )
            }

            drawPath(
                path = path,
                color = AccentTeal,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Second curve
            val path2 = Path().apply {
                moveTo(width * 0.15f, height * 0.5f - animatedOffset * 0.5f)
                cubicTo(
                    width * 0.35f, height * 0.3f - animatedOffset * 0.5f,
                    width * 0.55f, height * 0.7f - animatedOffset * 0.5f,
                    width * 0.75f, height * 0.5f - animatedOffset * 0.5f
                )
            }

            drawPath(
                path = path2,
                color = Primary,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Third curve
            val path3 = Path().apply {
                moveTo(width * 0.25f, height * 0.7f + animatedOffset * 0.3f)
                cubicTo(
                    width * 0.45f, height * 0.5f + animatedOffset * 0.3f,
                    width * 0.65f, height * 0.9f + animatedOffset * 0.3f,
                    width * 0.85f, height * 0.7f + animatedOffset * 0.3f
                )
            }

            drawPath(
                path = path3,
                color = AccentBlue,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw decorative circles
            drawCircle(
                color = CardTeal,
                radius = 30.dp.toPx(),
                center = Offset(width * 0.2f, height * 0.25f + animatedOffset)
            )

            drawCircle(
                color = CardPurple,
                radius = 20.dp.toPx(),
                center = Offset(width * 0.8f, height * 0.35f - animatedOffset * 0.5f)
            )

            drawCircle(
                color = CardOrange,
                radius = 25.dp.toPx(),
                center = Offset(width * 0.3f, height * 0.75f + animatedOffset * 0.3f)
            )

            drawCircle(
                color = AccentBlue,
                radius = 15.dp.toPx(),
                center = Offset(width * 0.75f, height * 0.65f - animatedOffset * 0.2f)
            )

            // Draw small dots
            drawCircle(
                color = LightOnSurfaceVariant.copy(alpha = 0.3f),
                radius = 6.dp.toPx(),
                center = Offset(width * 0.5f, height * 0.2f)
            )

            drawCircle(
                color = LightOnSurfaceVariant.copy(alpha = 0.3f),
                radius = 8.dp.toPx(),
                center = Offset(width * 0.6f, height * 0.85f)
            )

            drawCircle(
                color = LightOnSurfaceVariant.copy(alpha = 0.3f),
                radius = 5.dp.toPx(),
                center = Offset(width * 0.15f, height * 0.6f)
            )

            drawCircle(
                color = LightOnSurfaceVariant.copy(alpha = 0.3f),
                radius = 7.dp.toPx(),
                center = Offset(width * 0.9f, height * 0.5f)
            )
        }

        // Center icon/logo
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(y = animatedOffset.dp * 0.5f)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Primary, PrimaryDark)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "R",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}
