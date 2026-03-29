package com.aikeyboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aikeyboard.core.di.AppModule
import com.aikeyboard.core.theme.PixelProKeyboardTheme
import com.aikeyboard.feature.keyboard.ui.KeyboardScreen

/**
 * Preview Activity for rapid UI iteration.
 * Renders a mock text field above the keyboard so you can
 * test typing without enabling the IME system-wide.
 *
 * The real entry point for keyboard usage is [PixelProIME].
 */
class MainActivity : ComponentActivity() {

    private val viewModel by lazy { AppModule.provideKeyboardViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsState()
            PixelProKeyboardTheme(isDark = state.isDarkTheme) {
                val bgColor = if (state.isDarkTheme) Color(0xFF121212) else Color(0xFFF2F4F8)

                Column(
                    modifier            = Modifier.fillMaxSize().background(bgColor),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Mock text display
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            text  = "Pixel Pro Keyboard",
                            style = TextStyle(
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (state.isDarkTheme) Color(0xFFE8EAED) else Color(0xFF202124),
                            ),
                        )
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 80.dp)
                                .background(
                                    color = if (state.isDarkTheme) Color(0xFF2D2D2D) else Color.White,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .padding(16.dp),
                        ) {
                            Text(
                                text  = state.bufferText.ifEmpty { "Start typing…" },
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color    = if (state.bufferText.isEmpty()) Color(0xFF9AA0A6)
                                               else if (state.isDarkTheme)    Color(0xFFE8EAED)
                                               else                           Color(0xFF202124),
                                ),
                            )
                        }
                    }

                    // Keyboard surface
                    KeyboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
