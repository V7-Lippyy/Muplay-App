package com.example.muplay.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.muplay.data.model.MusicWithPlayCount

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MostPlayedCarousel(
    mostPlayedSongs: List<MusicWithPlayCount>,
    onMusicClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = mostPlayedSongs.chunked(9) // Group songs into chunks of 9 (3x3 grid per page)
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pages.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                val songsOnPage = pages[pageIndex]
                MostPlayedGrid(
                    songs = songsOnPage,
                    onMusicClick = onMusicClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Page indicator
            if (pages.size > 1) {
                Row(
                    Modifier
                        .padding(top = 8.dp, bottom = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)

                        androidx.compose.material3.Surface(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            shape = MaterialTheme.shapes.small,
                            color = color
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(
                                    text = "${iteration + 1}",
                                    color = MaterialTheme.colorScheme.surface,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MostPlayedGrid(
    songs: List<MusicWithPlayCount>,
    onMusicClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Divide songs into three rows with up to 3 songs per row
        val rows = songs.chunked(3)

        rows.forEach { rowSongs ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowSongs.forEach { musicWithCount ->
                    MusicCard(
                        music = musicWithCount.music,
                        onClick = { onMusicClick(musicWithCount.music.id) },
                        isCompact = true,
                        modifier = Modifier
                            .weight(1f)
                            .width(120.dp) // Fixed width to ensure proper sizing
                    )
                }

                // Add spacers for any missing slots in the row
                repeat(3 - rowSongs.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}