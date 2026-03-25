package com.aikeyboard.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EnglishKeyboard() {
    val rows = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫"),
        listOf("?123","😊","Space",".","↵")
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    Key(key, key == "Space")
                }
            }
        }
    }
}

@Composable
fun BanglaKeyboard() {
    val rows = listOf(
        listOf("১","২","৩","৪","৫","৬","৭","৮","৯","০"),
        listOf("ৌ","ৈ","া","ী","ূ","ব","হ","গ","দ","জ"),
        listOf("ো","ে","্","ি","ু","প","র","ক","ত","চ"),
        listOf("ং","ম","ন","ণ","স","ও","য","শ","খ","ঃ")
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key -> Key(key, false) }
            }
        }
    }
}

@Composable
fun RowScope.Key(key: String, wide: Boolean) {
    Surface(
        modifier = Modifier
            .padding(2.dp)
            .height(40.dp)
            .weight(if (wide) 3f else 1f),
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF2D2D2D)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(key, color = Color.White, fontSize = 16.sp)
        }
    }
}
