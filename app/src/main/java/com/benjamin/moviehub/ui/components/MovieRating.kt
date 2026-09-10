package com.benjamin.moviehub.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R

/**
 * Star rating label shared by the poster card, the compact row card and the metadata line.
 */
@Composable
fun MovieRating(
    value: Double,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = stringResource(R.string.rating_value, value),
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}
