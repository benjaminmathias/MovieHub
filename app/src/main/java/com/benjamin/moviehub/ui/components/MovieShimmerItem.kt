package com.benjamin.moviehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.core.theme.ContentHorizontalPadding
import com.benjamin.moviehub.core.theme.POSTER_ASPECT_RATIO
import com.valentinilk.shimmer.shimmer

/** Poster-card placeholder used by the discover grid, recommendation rows and home rows. */
@Composable
fun MovieCardShimmer(modifier: Modifier = Modifier) {
    ShimmerCard(modifier) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(POSTER_ASPECT_RATIO)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShimmerBlock(Modifier.fillMaxWidth(0.8f))
                ShimmerBlock(Modifier.width(80.dp))
            }
        }
    }
}

/** Full-width compact row placeholder used by search results and the library. */
@Composable
fun CompactMovieShimmerItem(modifier: Modifier = Modifier) {
    ShimmerCard(modifier.fillMaxWidth().padding(horizontal = ContentHorizontalPadding, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp).padding(8.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(width = 64.dp, height = 96.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShimmerBlock(Modifier.fillMaxWidth(0.8f))
                ShimmerBlock(Modifier.width(80.dp))
                Box(
                    modifier =
                        Modifier
                            .size(18.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                )
            }
        }
    }
}

/** Full-width taller placeholder used while the featured movie of the home feed loads. */
@Composable
fun HeroMovieShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(min = 300.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                .shimmer(),
    )
}

@Composable
private fun ShimmerCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.shimmer(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        content = content,
    )
}

@Composable
private fun ShimmerBlock(modifier: Modifier) {
    Box(
        modifier =
            modifier
                .heightIn(min = 14.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
    )
}
