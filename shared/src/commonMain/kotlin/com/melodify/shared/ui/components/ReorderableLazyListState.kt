package com.melodify.shared.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): ReorderableLazyListState {
    return remember(lazyListState) {
        ReorderableLazyListState(lazyListState, onMove)
    }
}

class ReorderableLazyListState(
    val lazyListState: LazyListState,
    val onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set
    var draggedDistance by mutableStateOf(0f)
        private set

    fun onDragStart(index: Int) {
        draggingItemIndex = index
        draggedDistance = 0f
    }

    fun onDrag(dragAmount: Offset) {
        draggedDistance += dragAmount.y
        val currentIndex = draggingItemIndex ?: return
        val layoutInfo = lazyListState.layoutInfo
        val currentItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex } ?: return

        val currentCenter = currentItem.offset + currentItem.size / 2 + draggedDistance.toInt()
        val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { item ->
            currentCenter in item.offset..(item.offset + item.size) && item.index != currentIndex
        }

        if (targetItem != null) {
            val targetIndex = targetItem.index
            onMove(currentIndex, targetIndex)
            // Reset drag distance: account for the item moving to the target's position
            val direction = if (targetIndex > currentIndex) 1 else -1
            draggedDistance -= (targetItem.size * direction)
            draggingItemIndex = targetIndex
        }
    }

    fun onDragInterrupted() {
        draggingItemIndex = null
        draggedDistance = 0f
    }
}
