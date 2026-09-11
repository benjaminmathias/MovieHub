package com.benjamin.moviehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.valentinilk.shimmer.shimmer
import com.benjamin.moviehub.core.theme.ContentHorizontalPadding
import com.benjamin.moviehub.core.theme.PosterAspectRatio

@Composable
fun PosterMovieShimmerItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().shimmer(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(PosterAspectRatio)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.8f)
                            .heightIn(min = 20.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                )
                Box(
                    modifier =
                        Modifier
                            .width(80.dp)
                            .heightIn(min = 18.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

@Composable
fun CompactMovieShimmerItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = ContentHorizontalPadding, vertical = 4.dp).shimmer(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp).padding(8.dp),
        ) {
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
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.8f)
                            .heightIn(min = 20.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                )
                Box(
                    modifier =
                        Modifier
                            .width(80.dp)
                            .heightIn(min = 18.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                )
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

@Composable
fun RowMovieShimmerItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shimmer(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(PosterAspectRatio)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ShimmerBlock(Modifier.fillMaxWidth())
                ShimmerBlock(Modifier.fillMaxWidth(0.8f))
                ShimmerBlock(Modifier.fillMaxWidth(0.5f))
            }
        }
    }
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
