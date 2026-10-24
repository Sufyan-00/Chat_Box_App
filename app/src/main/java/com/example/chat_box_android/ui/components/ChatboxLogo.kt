package com.example.chat_box_android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chat_box_android.R

/**
 * Full UNYTED.CHAT logo with UC icon and text.
 * Use for light backgrounds only (text is black).
 */
@Composable
fun UCLogo(
    modifier: Modifier = Modifier,
    height: Dp = 40.dp
) {
    Image(
        painter = painterResource(id = R.drawable.uc_logo),
        contentDescription = "UNYTED.CHAT Logo",
        modifier = modifier.height(height),
        contentScale = ContentScale.FillHeight
    )
}

/**
 * UC icon only (without text).
 * Use for dark backgrounds or compact displays.
 */
@Composable
fun UCIcon(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp
) {
    Image(
        painter = painterResource(id = R.drawable.uc_icon),
        contentDescription = "UC Icon",
        modifier = modifier
            .height(size)
            .width(size),
        contentScale = ContentScale.Fit
    )
}
