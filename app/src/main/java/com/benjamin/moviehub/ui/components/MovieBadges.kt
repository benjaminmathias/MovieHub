package com.benjamin.moviehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R

/** Warm rating pill shown over artwork (hero banner). */
@Composable
fun RatingBadge(
    value: Double,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onTertiary,
        )
        Text(
            text = stringResource(R.string.rating_value, value),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiary,
        )
    }
}

/** "● À LA UNE" pill used to label the featured movie. */
@Composable
fun SpotlightBadge(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = stringResource(R.string.spotlight_badge),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Small translucent pill for year / genre metadata over artwork. */
@Composable
fun MetadataChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
            modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
