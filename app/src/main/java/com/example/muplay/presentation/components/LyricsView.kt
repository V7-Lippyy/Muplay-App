package com.example.muplay.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muplay.data.model.LyricLine
import kotlinx.coroutines.launch

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    onManualScrollStarted: () -> Unit,
    onManualScrollEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        EmptyLyricsView(modifier)
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Find the current line index
    val currentLineIndex by remember(lyrics) {
        derivedStateOf {
            lyrics.indexOfFirst { it.isCurrent }
        }
    }

    // Scroll to current line when it changes
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            // Adding some delay to make the animation smoother
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = maxOf(0, currentLineIndex - 1),
                    scrollOffset = -150 // Offset to center the current line
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { onManualScrollStarted() },
                    onDragEnd = { onManualScrollEnded() },
                    onDragCancel = { onManualScrollEnded() },
                    onVerticalDrag = { _, dragAmount ->
                        coroutineScope.launch {
                            // Use scroll instead of scrollBy
                            val currentOffset = listState.firstVisibleItemScrollOffset
                            listState.scrollToItem(
                                listState.firstVisibleItemIndex,
                                currentOffset - dragAmount.toInt()
                            )
                        }
                    }
                )
            }
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 80.dp,
                bottom = 80.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lyrics) { index, line ->
                LyricLineItem(
                    lyricLine = line,
                    isCurrentLine = index == currentLineIndex
                )
            }
        }
    }
}

@Composable
fun LyricLineItem(
    lyricLine: LyricLine,
    isCurrentLine: Boolean,
    modifier: Modifier = Modifier
) {
    // Animate the transition between current and non-current lines
    val fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal
    val fontSize = if (isCurrentLine) 20.sp else 18.sp
    val alpha by animateFloatAsState(
        targetValue = if (isCurrentLine) 1f else 0.6f,
        label = "LyricLineAlpha"
    )

    Text(
        text = lyricLine.text,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = fontWeight,
            fontSize = fontSize
        ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .alpha(alpha)
    )
}

@Composable
fun EmptyLyricsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "No lyrics available",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Add lyrics by tapping the button below",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}