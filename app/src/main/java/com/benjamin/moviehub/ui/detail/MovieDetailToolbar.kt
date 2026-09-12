package com.benjamin.moviehub.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.benjamin.moviehub.R

@Composable
internal fun DetailToolbar(
    title: String,
    toolbarVisible: Boolean,
    progressProvider: () -> Float,
    onBackClick: () -> Unit,
    onShareClick: (() -> Unit)?,
) {
    val toolbarColor = MaterialTheme.colorScheme.surface
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(toolbarColor.copy(alpha = progressProvider())) }
                .statusBarsPadding()
                .zIndex(1f)
                .semantics {
                    isTraversalGroup = true
                    traversalIndex = -1f
                },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailControlButton(
                contentDescription = stringResource(R.string.back),
                onClick = onBackClick,
                elevated = !toolbarVisible,
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .graphicsLayer { alpha = progressProvider() }
                            .then(if (toolbarVisible) Modifier else Modifier.clearAndSetSemantics {}),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            onShareClick?.let { share ->
                DetailControlButton(
                    contentDescription = stringResource(R.string.share),
                    onClick = share,
                    elevated = !toolbarVisible,
                ) {
                    Icon(imageVector = Icons.Outlined.Share, contentDescription = null)
                }
            }
        }
        if (toolbarVisible) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        } else {
            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

@Composable
private fun DetailControlButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevated: Boolean = true,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .size(48.dp)
                .semantics {
                    this.contentDescription = contentDescription
                },
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (elevated) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                        } else {
                            Color.Transparent
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}
