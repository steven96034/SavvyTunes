package com.example.geminispotifyapp.presentation.features.main.userdatadetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.Period.Companion.formatEnumPeriodName
import com.example.geminispotifyapp.presentation.ui.theme.GeminiSpotifyAppTheme
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

        TextButton(
            onClick = { onExpandChange(true) },
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.background),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = formatEnumPeriodName(Period.entries[selectedValue]),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = "Period Selection",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

@Preview(showBackground = true)
@Composable
fun DropDownMenuTemplatePreview() {
    GeminiSpotifyAppTheme {
        DropDownMenuTemplate(
            expanded = false,
            onExpandChange = {},
            selectedValue = 0,
            onValueChange = {},
            options = Period.entries.map { formatEnumPeriodName(it) }
        )
    }
}





enum class Period(val apiValue: String) {
    SHORT_TERM("short_term"),
    MEDIUM_TERM("medium_term"),
    LONG_TERM("long_term");
    companion object {
        fun fromString(value: String): Period? {
            return entries.firstOrNull { it.name.lowercase() == value.lowercase() }
        }
        fun formatEnumPeriodName(period: Period): String {
            return period.apiValue.replace("_", " ").split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        }
    }
}

