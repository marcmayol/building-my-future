package com.marc.gymplan100.ui

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Selector tipo ruedita: una columna con desplazamiento que engancha (snap) al elemento
 * centrado. El valor seleccionado es siempre el que queda en el centro, resaltado entre dos
 * líneas. [items] son las etiquetas ya formateadas (p. ej. "72 kg").
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: androidx.compose.ui.unit.Dp = 44.dp,
    visibleCount: Int = 5
) {
    if (items.isEmpty()) return
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    // El centro se logra con un padding de (visibleCount/2) elementos arriba y abajo, así el
    // primer elemento visible (firstVisibleItemIndex) coincide con el que queda centrado.
    val edge = visibleCount / 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    // Mantén la rueda sincronizada si el valor cambia desde fuera (p. ej. importado de Health).
    LaunchedEffect(safeIndex) {
        if (!listState.isScrollInProgress &&
            listState.firstVisibleItemIndex != safeIndex
        ) {
            listState.scrollToItem(safeIndex)
        }
    }

    // Notifica el elemento centrado (primero visible, ya que el padding lo centra).
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2f) - center)
            }?.index ?: listState.firstVisibleItemIndex
        }
            .distinctUntilChanged()
            .collect { idx -> if (idx in items.indices) onSelectedIndexChange(idx) }
    }

    Box(modifier = modifier.height(itemHeight * visibleCount)) {
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = itemHeight * edge)
        ) {
            itemsIndexed(items) { index, label ->
                val centered = index == listState.firstVisibleItemIndex
                Text(
                    text = label,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = if (centered) MaterialTheme.typography.headlineSmall
                    else MaterialTheme.typography.titleMedium,
                    fontWeight = if (centered) FontWeight.Bold else FontWeight.Normal,
                    color = if (centered) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(top = itemHeight / 5)
                )
            }
        }
        // Marco de selección: dos líneas que enmarcan el elemento central.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
        ) {
            HorizontalDivider(Modifier.align(Alignment.TopCenter))
            HorizontalDivider(Modifier.align(Alignment.BottomCenter))
        }
    }
}
