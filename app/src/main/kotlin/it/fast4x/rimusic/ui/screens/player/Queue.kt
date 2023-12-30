package it.fast4x.rimusic.ui.screens.player


import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults.colors
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.Log
import androidx.media3.exoplayer.offline.Download
import com.valentinilk.shimmer.shimmer
import it.fast4x.compose.reordering.ReorderingLazyColumn
import it.fast4x.compose.reordering.animateItemPlacement
import it.fast4x.compose.reordering.draggedItem
import it.fast4x.compose.reordering.rememberReorderingState
import it.fast4x.compose.reordering.reorder
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.models.Info
import it.fast4x.rimusic.service.isLocal
import it.fast4x.rimusic.ui.components.BottomSheet
import it.fast4x.rimusic.ui.components.BottomSheetState
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.MusicBars
import it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.components.themed.QueuedMediaItemMenu
import it.fast4x.rimusic.ui.components.themed.SelectorDialog
import it.fast4x.rimusic.ui.items.SongItem
import it.fast4x.rimusic.ui.items.SongItemPlaceholder
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.ui.styling.onOverlay
import it.fast4x.rimusic.ui.styling.px
import it.fast4x.rimusic.utils.DisposableListener
import it.fast4x.rimusic.utils.UiTypeKey
import it.fast4x.rimusic.utils.downloadedStateMedia
import it.fast4x.rimusic.utils.getDownloadState
import it.fast4x.rimusic.utils.manageDownload
import it.fast4x.rimusic.utils.medium
import it.fast4x.rimusic.utils.queueLoopEnabledKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.reorderInQueueEnabledKey
import it.fast4x.rimusic.utils.shouldBePlaying
import it.fast4x.rimusic.utils.shuffleQueue
import it.fast4x.rimusic.utils.smoothScrollToTop
import it.fast4x.rimusic.utils.windows
import kotlinx.coroutines.launch

@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@androidx.media3.common.util.UnstableApi
@Composable
fun Queue(
    backgroundColorProvider: () -> Color,
    layoutState: BottomSheetState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    val uiType  by rememberPreference(UiTypeKey, UiType.RiMusic)
    val windowInsets = WindowInsets.systemBars

    val horizontalBottomPaddingValues = windowInsets
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues()

    val context = LocalContext.current

    BottomSheet(
        state = layoutState,
        modifier = modifier,
        collapsedContent = {
/*
            Box(
                modifier = Modifier
                    .drawBehind { drawRect(backgroundColorProvider()) }
                    .fillMaxSize()
                    .padding(horizontalBottomPaddingValues)
            ) {
                Image(
                    painter = painterResource(R.drawable.chevron_up),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.text),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(18.dp)
                )

                content()
            }
*/

           content()
        }
    ) {
        val binder = LocalPlayerServiceBinder.current

        binder?.player ?: return@BottomSheet

        val player = binder.player

        var queueLoopEnabled by rememberPreference(queueLoopEnabledKey, defaultValue = true)

        val menuState = LocalMenuState.current

        val thumbnailSizeDp = Dimensions.thumbnails.song
        val thumbnailSizePx = thumbnailSizeDp.px

        var mediaItemIndex by remember {
            mutableStateOf(if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex)
        }

        var windows by remember {
            mutableStateOf(player.currentTimeline.windows)
        }

        var shouldBePlaying by remember {
            mutableStateOf(binder.player.shouldBePlaying)
        }

        player.DisposableListener {
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItemIndex =
                        if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    windows = timeline.windows
                    mediaItemIndex =
                        if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    shouldBePlaying = binder.player.shouldBePlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    shouldBePlaying = binder.player.shouldBePlaying
                }
            }
        }

        val reorderingState = rememberReorderingState(
            lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = mediaItemIndex),
            key = windows,
            onDragEnd = player::moveMediaItem,
            extraItemCount = 0
        )

        val rippleIndication = rememberRipple(bounded = false)

        val musicBarsTransition = updateTransition(targetState = mediaItemIndex, label = "")

        /*
        var isReorderDisabled by rememberSaveable {
            mutableStateOf(false)
        }
         */
        var isReorderDisabled by rememberPreference(reorderInQueueEnabledKey, defaultValue = true)

        var downloadState by remember {
            mutableStateOf(Download.STATE_STOPPED)
        }

        var listMediaItems = remember {
            mutableListOf<Int>()
        }

        var selectQueueItems by remember {
            mutableStateOf(false)
        }

        var showSelectTypeClearQueue by remember {
            mutableStateOf(false)
        }

        Column {
            Box(
                modifier = Modifier
                    .background(colorPalette.background1)
                    .weight(1f)
            ) {

                ReorderingLazyColumn(
                    reorderingState = reorderingState,
                    contentPadding = windowInsets
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        .asPaddingValues(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .nestedScroll(layoutState.preUpPostDownNestedScrollConnection)

                ) {
                    items(
                        items = windows,
                        key = { it.uid.hashCode() }
                    ) { window ->
                        var deltaX by remember { mutableStateOf(0f) }
                        val isPlayingThisMediaItem = mediaItemIndex == window.firstPeriodIndex
                        val currentItem by rememberUpdatedState(window)
                        val isLocal by remember { derivedStateOf { window.mediaItem.isLocal } }
                        downloadState = getDownloadState(window.mediaItem.mediaId)
                        val isDownloaded = if (!isLocal) downloadedStateMedia(window.mediaItem.mediaId) else true
                        SongItem(
                            song = window.mediaItem,
                            isDownloaded = isDownloaded,
                            onDownloadClick = {
                                binder?.cache?.removeResource(window.mediaItem.mediaId)
                                if (!isLocal)
                                manageDownload(
                                    context = context,
                                    songId = window.mediaItem.mediaId,
                                    songTitle = window.mediaItem.mediaMetadata.title.toString(),
                                    downloadState = isDownloaded
                                )
                            },
                            downloadState = downloadState,
                            thumbnailSizePx = thumbnailSizePx,
                            thumbnailSizeDp = thumbnailSizeDp,
                            onThumbnailContent = {
                                musicBarsTransition.AnimatedVisibility(
                                    visible = { it == window.firstPeriodIndex },
                                    enter = fadeIn(tween(800)),
                                    exit = fadeOut(tween(800)),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .background(
                                                color = Color.Black.copy(alpha = 0.25f),
                                                shape = thumbnailShape
                                            )
                                            .size(Dimensions.thumbnails.song)
                                    ) {
                                        if (shouldBePlaying) {
                                            MusicBars(
                                                color = colorPalette.onOverlay,
                                                modifier = Modifier
                                                    .height(24.dp)
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(R.drawable.play),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(colorPalette.onOverlay),
                                                modifier = Modifier
                                                    .size(24.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            trailingContent = {

                                val checkedState = remember { mutableStateOf(false) }
                                if (selectQueueItems)
                                Checkbox(
                                    checked = checkedState.value,
                                    onCheckedChange = {
                                        checkedState.value = it
                                        if (it) listMediaItems.add(window.firstPeriodIndex) else
                                            listMediaItems.remove(window.firstPeriodIndex)
                                    },
                                    colors = colors(
                                        checkedColor = colorPalette.accent,
                                        uncheckedColor = colorPalette.text
                                    )
                                )

                                if (!isReorderDisabled) {
                                    IconButton(
                                        icon = R.drawable.reorder,
                                        color = colorPalette.textDisabled,
                                        //indication = rippleIndication,
                                        onClick = {},
                                        modifier = Modifier
                                            .reorder(
                                                reorderingState = reorderingState,
                                                index = window.firstPeriodIndex
                                            )
                                            .size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            QueuedMediaItemMenu(
                                                mediaItem = window.mediaItem,
                                                indexInQueue = if (isPlayingThisMediaItem) null else window.firstPeriodIndex,
                                                onDismiss = menuState::hide,
                                                onDownload = {
                                                    manageDownload(
                                                        context = context,
                                                        songId = window.mediaItem.mediaId,
                                                        songTitle = window.mediaItem.mediaMetadata.title.toString(),
                                                        downloadState = isDownloaded
                                                    )
                                                }

                                            )
                                        }
                                    },
                                    onClick = {
                                        if (isPlayingThisMediaItem) {
                                            if (shouldBePlaying) {
                                                player.pause()
                                            } else {
                                                player.play()
                                            }
                                        } else {
                                            player.seekToDefaultPosition(window.firstPeriodIndex)
                                            player.playWhenReady = true
                                        }
                                    }
                                )
                                .pointerInput(Unit) {

                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, dragAmount ->
                                            deltaX = dragAmount
                                        },

                                        onDragEnd = {
                                            if (!isReorderDisabled)
                                                player?.removeMediaItem(currentItem.firstPeriodIndex)
                                        }

                                    )

                                }
                                .animateItemPlacement(reorderingState = reorderingState)
                                .draggedItem(
                                    reorderingState = reorderingState,
                                    index = window.firstPeriodIndex
                                )
                        )
                    }

                    item {
                        if (binder.isLoadingRadio) {
                            Column(
                                modifier = Modifier
                                    .shimmer()
                            ) {
                                repeat(3) { index ->
                                    SongItemPlaceholder(
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier
                                            .alpha(1f - index * 0.125f)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                if(uiType == UiType.RiMusic)
                FloatingActionsContainerWithScrollToTop(
                    lazyListState = reorderingState.lazyListState,
                    iconId = R.drawable.shuffle,
                    visible = !reorderingState.isDragging,
                    windowInsets = windowInsets.only(WindowInsetsSides.Horizontal),
                    onClick = {
                        reorderingState.coroutineScope.launch {
                            reorderingState.lazyListState.smoothScrollToTop()
                        }.invokeOnCompletion {
                            player.shuffleQueue()
                        }
                    }
                )


            }


            Box(
                modifier = Modifier
                    .clickable(onClick = layoutState::collapseSoft)
                    .background(colorPalette.background1)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 20.dp)
                    .padding(horizontalBottomPaddingValues)
                    .height(40.dp) //bottom bar queue
            ) {
                /*
                Image(
                    painter = painterResource(R.drawable.chevron_down),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.text),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(18.dp)
                )
                 */

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .align(Alignment.CenterStart)

            ) {

                IconButton(
                    icon = R.drawable.trash,
                    color = colorPalette.text,
                    onClick = {
                        if (!selectQueueItems)
                        showSelectTypeClearQueue = true else {
                            val mediacount = listMediaItems.size - 1
                            listMediaItems.sort()
                            for (i in mediacount.downTo(0)) {
                                //if (i == mediaItemIndex) null else
                                binder.player.removeMediaItem(listMediaItems[i])
                            }
                            listMediaItems.clear()
                            selectQueueItems = false
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(24.dp)
                )

                if (showSelectTypeClearQueue)
                    SelectorDialog(
                        title = stringResource(R.string.clear_queue),
                        onDismiss = { showSelectTypeClearQueue = false },
                        values = listOf(
                            Info("a", stringResource(R.string.remove_all)),
                            Info("s", stringResource(R.string.remove_selected))
                        ),
                        onValueSelected = {
                            if (it == "a") {
                                val mediacount = binder.player.mediaItemCount - 1
                                for (i in mediacount.downTo(0)) {
                                    if (i == mediaItemIndex) null else binder.player.removeMediaItem(i)
                                }
                            } else selectQueueItems = true

                            showSelectTypeClearQueue = false
                        }
                    )

                IconButton(
                    icon = R.drawable.chevron_forward,
                    color = colorPalette.text,
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp)
                )
                BasicText(
                    text = "${binder.player.mediaItemCount} " + stringResource(R.string.songs), //+ " " + stringResource(R.string.on_queue),
                    style = typography.xxs.medium,
                )

            }


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(horizontal = 4.dp)
                       // .fillMaxHeight()

                ) {

                    IconButton(
                        icon = if (isReorderDisabled) R.drawable.locked else R.drawable.unlocked,
                        color = colorPalette.text,
                        onClick = { isReorderDisabled = !isReorderDisabled },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(24.dp)
                    )

                    Spacer(
                        modifier = Modifier
                            .width(12.dp)
                    )
                    IconButton(
                        icon = R.drawable.repeat,
                        color = if (queueLoopEnabled) colorPalette.text else colorPalette.textDisabled,
                        onClick = { queueLoopEnabled = !queueLoopEnabled },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(24.dp)
                    )

                    Spacer(
                        modifier = Modifier
                            .width(12.dp)
                    )

                    IconButton(
                        icon = R.drawable.shuffle,
                        color = colorPalette.text,
                        enabled = !reorderingState.isDragging,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(24.dp),
                        onClick = {
                            reorderingState.coroutineScope.launch {
                                reorderingState.lazyListState.smoothScrollToTop()
                            }.invokeOnCompletion {
                                player.shuffleQueue()
                            }
                        }
                    )

                    Spacer(
                        modifier = Modifier
                            .width(12.dp)
                    )

                    IconButton(
                        icon = R.drawable.chevron_down,
                        color = colorPalette.text,
                        onClick = { layoutState.collapseSoft() },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(24.dp)
                    )
                    


                }
            }
        }
    }
}
