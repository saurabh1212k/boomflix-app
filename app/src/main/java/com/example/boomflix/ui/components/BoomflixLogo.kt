package com.example.boomflix.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boomflix.R
import com.example.boomflix.theme.BoomflixRed

@Composable
fun BoomflixLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp,
    fontSize: TextUnit = 20.sp,
    showEmblem: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        if (showEmblem) {
            Image(
                painter = painterResource(id = R.drawable.boomflix_b),
                contentDescription = "BOOMFLIX Emblem",
                modifier = Modifier.size(iconSize)
            )
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = BoomflixRed, fontWeight = FontWeight.Black)) {
                    append("BOOM")
                }
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Black)) {
                    append("FLIX")
                }
            },
            fontSize = fontSize,
            fontFamily = FontFamily.SansSerif,
            fontStyle = FontStyle.Italic,
            letterSpacing = 1.8.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0x88000000),
                    offset = Offset(0f, 2f),
                    blurRadius = 4f
                )
            )
        )
    }
}
