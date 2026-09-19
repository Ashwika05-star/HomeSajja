package com.homesajja.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.homesajja.app.ui.util.ImageWidth
import com.homesajja.app.ui.util.optimizedImage

/** Swipeable photos with page dots. [images] can be URLs or local Uris. */
@Composable
fun ImageCarousel(images: List<Any>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (images.isEmpty()) {
            Icon(
                imageVector = Icons.Filled.Chair,
                contentDescription = "No photo",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        val pagerState = rememberPagerState { images.size }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            AsyncImage(
                model = optimizedImage(images[page], ImageWidth.FULL),
                contentDescription = "Photo ${page + 1} of ${images.size}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            )
        }
        if (images.size > 1) {
            Row(
                modifier = Modifier.padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(images.size) { index ->
                    val selected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (selected) 8.dp else 6.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}
