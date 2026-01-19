package com.benjamin.moviehub.ui.movie_detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.benjamin.moviehub.domain.model.Movie

@Composable
fun MovieDetailContent(movie: Movie) {
    Column(
        modifier = Modifier.Companion
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        Box {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w780${movie.backdropPath}",
                contentDescription = null,
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .height(280.dp),
                contentScale = ContentScale.Companion.Crop
            )

            Surface(
                modifier = Modifier.Companion
                    .padding(16.dp)
                    .align(Alignment.Companion.BottomEnd),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "⭐ ${String.format("%.1f", movie.voteAverage)}",
                    modifier = Modifier.Companion.padding(8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }


        Column(modifier = Modifier.Companion.padding(20.dp)) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Companion.ExtraBold
            )

            Spacer(modifier = Modifier.Companion.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.Companion.height(16.dp))

            Text(
                text = "Synopsis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Companion.SemiBold
            )

            Spacer(modifier = Modifier.Companion.height(8.dp))

            Text(
                text = movie.overview,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
        }
    }
}