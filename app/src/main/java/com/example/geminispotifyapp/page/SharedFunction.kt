package com.example.geminispotifyapp.page

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.geminispotifyapp.ui.theme.SpotifyBlack

/**
 * Handles back navigation in the app. When the current destination is not "home",
 * it navigates back to the "home" destination. If the current destination is "home"
 * and the user presses back, it finishes the activity.
 */
@Composable
fun HandleBackToHome(navController: NavController) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current

    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navController.navigateToHome()
            }
        }
    }

    DisposableEffect (lifecycleOwner, backDispatcher) {
        backDispatcher?.addCallback(lifecycleOwner, backCallback)
        onDispose {
            backCallback.remove()
        }
    }
}

private fun NavController.navigateToHome() {
    this.navigate("home") {
        popUpTo(this@navigateToHome.graph.startDestinationId){
            inclusive = false // keep home page in stack
        }
        launchSingleTop = true // avoid multiple copies of the same destination
        restoreState = true // restore state when reselecting a previously selected item
    }
}


@Composable
fun DropDownMenuTemplate(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    options: List<String>
) {
    Box (contentAlignment = Alignment.CenterEnd) {
        IconButton(onClick = { onExpandChange(true) }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Period Selection"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) }
        ) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    modifier = Modifier.padding(2.dp),
                    onClick = {
                        onExpandChange(false)
                        onValueChange(index)
                    },
                    trailingIcon = {
                        if (selectedValue == index) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected"
                            )
                        }
                    }
                )
            }
        }
    }
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

    if (selectedValue == null) {
        return
    }

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
                        onDismiss()
                    }
                }
            }.pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount < -30 || dragAmount > 30) {
                        // Swipe to the left || Swipe to the right, then exit detail layout.
                        onDismiss()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ){
        val scrollState = rememberScrollState()

        Surface(
            color = SpotifyBlack,
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.75f)
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
                content(selectedValue, onDismiss)
            }
        }
    }
}

