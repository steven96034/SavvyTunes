package com.example.geminispotifyapp.features.userdatadetail

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import java.util.Locale

/**
 * Handles back navigation in the app. When the current destination is not "home",
 * it navigates back to the "home" destination. If the current destination is "home"
 * and the user presses back, it finishes the activity.
 */
//@Composable
//fun HandleBackToHome(navController: NavController) {
//    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    val backCallback = remember {
//        object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                navController.navigateToHome()
//            }
//        }
//    }
//
//    DisposableEffect (lifecycleOwner, backDispatcher) {
//        backDispatcher?.addCallback(lifecycleOwner, backCallback)
//        onDispose {
//            backCallback.remove()
//        }
//    }
//}
//
//private fun NavController.navigateToHome() {
//    this.navigate("home") {
//        popUpTo(this@navigateToHome.graph.startDestinationId){
//            inclusive = false // keep home page in stack
//        }
//        launchSingleTop = true // avoid multiple copies of the same destination
//        restoreState = true // restore state when reselecting a previously selected item
//    }
//}


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



fun formatEnumPeriodName(period: Period): String {
    return period.name.replace("_", " ").split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

enum class Period {
    SHORT_TERM,
    MEDIUM_TERM,
    LONG_TERM
}