package com.homesajja.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import kotlinx.coroutines.launch

/** One slide of the intro: an illustration (an icon on layered circles) and a title with one line of text. */
private data class IntroSlide(val icon: ImageVector, val title: String, val line: String)

private val SLIDES = listOf(
    IntroSlide(Icons.Filled.ShoppingBag, "Buy & Sell Furniture", "Find pre-loved pieces in your city, or turn your own into cash."),
    IntroSlide(Icons.Filled.SwapHoriz, "Exchange Instead of Spending", "Swap what you have for something you need, with no money involved."),
    IntroSlide(Icons.Filled.Recycling, "Repair, Recycle, Give It a New Life", "Fix it with a local carpenter, or recycle it responsibly."),
)

/**
 * The first screen for someone who isn't signed in: three swipeable slides, a Skip button, page dots, and Sign In / Sign Up on the
 * last slide. Skip jumps to the last slide, where the sign-in buttons are.
 */
@Composable
fun IntroScreen(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    onPreviewComponentsClick: (() -> Unit)? = null,
) {
    val pagerState = rememberPagerState { SLIDES.size }
    val scope = rememberCoroutineScope()
    val lastPage = SLIDES.lastIndex
    val onLast = pagerState.currentPage == lastPage
    fun goTo(page: Int) {
        scope.launch { pagerState.animateScrollToPage(page) }
    }

    Column(modifier = modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (!onLast) TextButton(onClick = { goTo(lastPage) }) { Text("Skip") }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            SlideContent(SLIDES[page])
        }

        PageDots(pagerState)
        Spacer(Modifier.height(24.dp))

        // Both buttons always take up space, so the dots and slide don't jump when the last slide shows Sign In.
        PrimaryButton(
            text = if (onLast) "Sign Up" else "Next",
            onClick = if (onLast) onSignUp else ({ goTo(pagerState.currentPage + 1) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        if (onLast) {
            OutlinedButton(text = "Sign In", onClick = onSignIn, modifier = Modifier.fillMaxWidth())
        } else {
            Spacer(Modifier.height(52.dp))
        }
        if (onPreviewComponentsClick != null && onLast) {
            TextButton(onClick = onPreviewComponentsClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("View component library (debug)")
            }
        } else {
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SlideContent(slide: IntroSlide) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SlideArt(slide.icon)
        Spacer(Modifier.height(40.dp))
        Text(
            slide.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            slide.line,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Decorative: layered circles in the brand colours behind a large icon. The title says what the slide is about. */
@Composable
private fun SlideArt(icon: ImageVector) {
    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(210.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape))
        Box(Modifier.size(140.dp).background(MaterialTheme.colorScheme.surface, CircleShape))
        Box(Modifier.size(36.dp).offset(x = 78.dp, y = (-78).dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
        Box(Modifier.size(22.dp).offset(x = (-86).dp, y = 70.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(76.dp))
    }
}

@Composable
private fun PageDots(state: PagerState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Page ${state.currentPage + 1} of ${state.pageCount}" },
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(state.pageCount) { index ->
            val selected = index == state.currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (selected) 24.dp else 8.dp, height = 8.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
            )
        }
    }
}
