package com.example.geminispotifyapp.features

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(uiEventManager: UiEventManager) : ViewModel() {

    val snackbarEvent = uiEventManager.snackbarEvent

    private val _selectedItemForDetail = MutableStateFlow<Any?>(null)
    val selectedItemForDetail = _selectedItemForDetail.asStateFlow()

    fun showItemDetail(item: Any?) {
        _selectedItemForDetail.value = item
    }

    fun dismissItemDetail() {
        _selectedItemForDetail.value = null
    }

    @Composable
    fun <T> DetailBox(
        selectedValue: T?, // Generic Type
        onDismiss: () -> Unit, // Dismiss the detail box
        modifier: Modifier = Modifier,
        content: @Composable (T, () -> Unit) -> Unit // Content to be displayed
    ) {
//    // 1. Custom NestedScrollConnection, intercept scroll events
//    val nestedScrollConnection = remember {
//        object : NestedScrollConnection {
//            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
//                // 2. Intercept scroll events, don't pass them to outside
//                return Offset(0f, available.y)
//            }
//        }
//    }
        Log.d("DetailBox", "DetailBox recomposing. selectedValue is null: ${selectedValue == null}") // 追蹤重組和 selectedValue
        if (selectedValue == null) {
            Log.d("DetailBox", "selectedValue is NULL, returning.")
            return
        }

        Log.d("DetailBox", "selectedValue is NOT NULL, proceeding to show Box. Value: $selectedValue")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Transparent background
                //.nestedScroll(nestedScrollConnection)
//            .pointerInput(Unit) {
//                awaitPointerEventScope {
//                    while (true) {
//                        val event = awaitPointerEvent()
//                        if (event.type == PointerEventType.Scroll) {
//                           event.changes.forEach { it.consume() }
//                        }
//                    }
//                }
//            }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val innerBoxWidth = size.width * 0.8f
                        val innerBoxHeight = size.height * 0.75f
                        val innerBoxLeft = (size.width - innerBoxWidth) / 2
                        val innerBoxTop = (size.height - innerBoxHeight) / 2

                        if (offset.x < innerBoxLeft || offset.x > innerBoxLeft + innerBoxWidth ||
                            offset.y < innerBoxTop || offset.y > innerBoxTop + innerBoxHeight
                        ) {
                            Log.d("DetailBox", "Outside tap detected, dismissing.")
                            onDismiss()
                        } else Log.d("DetailBox", "Inside tap detected.")
                    }
                }.pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount < -30 || dragAmount > 30) {
                            // Swipe to the left || Swipe to the right, then exit detail layout.
                            Log.d("DetailBox", "Swipe detected, dismissing.")
                            onDismiss()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ){
            Log.d("DetailBox", "Inside outer Box, about to show Surface.")
            val scrollState = rememberScrollState()

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.75f)
                //.border(2.dp, Color.Red) // <<--- For test
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize() // Ensure Column fills the Surface
                        .wrapContentSize(Alignment.TopCenter)
                        .verticalScroll(scrollState)
                    , // Align the content to top center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Log.d("DetailBox", "About to call content lambda.")
                    content(selectedValue, onDismiss)
                    Log.d("DetailBox", "Finished calling content lambda.")
                }
            }
        }
    }
}