package com.example.geminispotifyapp.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.util.Locale


@Composable
fun UserSettingsScreen(
    viewModel: UserSettingsViewModel = hiltViewModel()
) {
    val searchSimilarNum by viewModel.searchSimilarNum.collectAsStateWithLifecycle()
    val userDataNum by viewModel.userDataNum.collectAsStateWithLifecycle()
    val checkMarketIfPlayable by viewModel.checkMarketIfPlayable.collectAsStateWithLifecycle()

    val searchText by viewModel.searchText.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    UserSettingsContent(
        searchSimilarNum = searchSimilarNum,
        userDataNum = userDataNum,
        checkMarketIfPlayable = checkMarketIfPlayable,
        searchText = searchText,
        onSearchSimilarNumChange = { newValue -> scope.launch { viewModel.setSearchSimilarNum(newValue) } },
        onUserDataNumChange = { newValue -> scope.launch { viewModel.setUserDataNum(newValue) } },
        onSearchForMarketChange = { newValue -> scope.launch { viewModel.setCheckMarketIfPlayable(newValue) } },
        onSearchTextChange = { newValue -> viewModel.updateSearchText(newValue) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsContent(
    searchSimilarNum: Int,
    userDataNum: Int,
    checkMarketIfPlayable: String?,
    searchText: String,
    onSearchSimilarNumChange: (Int) -> Unit,
    onUserDataNumChange: (Int) -> Unit,
    onSearchForMarketChange: (String?) -> Unit,
    onSearchTextChange: (String) -> Unit
) {
    val countryCodes = remember { Locale.getISOCountries() }
    val countries = remember(countryCodes) {
        countryCodes.map { code -> Locale("", code).displayCountry }.sorted()
    }

    var expanded by remember { mutableStateOf(false) }
    var expandedExpandable by remember { mutableStateOf(false) }

    // Store the width of OutlinedTextField in pixels
    var textFieldWidthPx by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current

    // Transform pixel to Dp
    val textFieldWidthDp: Dp by remember(textFieldWidthPx) {
        mutableStateOf(with(density) { textFieldWidthPx.toDp() })
    }

    // Simplified height calculation
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    // Assume a height for a DropdownMenuItem is 50.dp
    val assumedDropdownMenuItemHeight = 50.dp
    // Max height of the DropdownMenu is 300.dp or half of the screen height
    val maxDropdownHeight = remember { min(300.dp, screenHeightDp / 2) }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            val annotatedText = buildAnnotatedString {
                append("Number of similar tracks and artists to search: ")
                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = MaterialTheme.colorScheme.onSurface))
                append("$searchSimilarNum")
                pop()
            }
            Text(text = annotatedText, modifier = Modifier.padding(bottom = 8.dp))
        }
        item {
            Slider(
                value = searchSimilarNum.toFloat(),
                onValueChange = { newValue ->
                    onSearchSimilarNumChange(newValue.toInt())
                },
                valueRange = 1f..50f,
                steps = 49,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            val annotatedText = buildAnnotatedString {
                append("Number of user data to retrieve ")
                pushStyle(SpanStyle(textDecoration = TextDecoration.None, color = MaterialTheme.colorScheme.onSurfaceVariant))
                append("(for your Top Tracks/Top Artists/Recently Played data): ")
                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = MaterialTheme.colorScheme.onSurface))
                append("$userDataNum")
                pop()
            }
            Text(
                text = annotatedText,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }
        item {
            Slider(
                value = userDataNum.toFloat(),
                onValueChange = { newValue ->
                    onUserDataNumChange(newValue.toInt())
                },
                valueRange = 1f..50f,
                steps = 49,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val marketText = checkMarketIfPlayable?.let { Locale("", it).displayCountry } ?: "None selected"
                val annotatedText = buildAnnotatedString {
                    append("Quickly check if the selected track is playable in the specified market: ")
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = MaterialTheme.colorScheme.onSurface))
                    append(marketText)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.None, color = MaterialTheme.colorScheme.onSurfaceVariant))
                    append("\n(You can see the result in each track detail page.)\n")
                    pop()
                }
                Text(
                    text = annotatedText,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { expandedExpandable = !expandedExpandable }) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "More Info",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            AnimatedVisibility(visible = expandedExpandable) {
                Column(modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                    Text("Track may have multiple versions for different markets/regions according to each regional agency. " +
                            "You may not be able to check the track in the selected market by only checking the selected track ISRC/ID.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { newValue ->
                        onSearchTextChange(newValue)
                        if (newValue.isEmpty()) {
                            onSearchForMarketChange(null)
                        }
                        if (!expanded) expanded = true
                    },
                    label = { Text("Search For Market") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                        .fillMaxWidth()
                        // Monitor the layout of OutlinedTextField and get its width for anchor point
                        .onGloballyPositioned { coordinates ->
                            textFieldWidthPx = coordinates.size.width
                        }
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    val filteredCountries = remember(countries, searchText) {
                        countries.filter { it.contains(searchText, ignoreCase = true) }
                    }

                    // Calculate the height of LazyColumn based on the number of filtered countries
                    val calculatedLazyColumnHeight = remember(filteredCountries.size, searchText) {
                        if (filteredCountries.isEmpty() && searchText.isNotEmpty()) {
                            // If there's no matching countries, give a height for at least one item
                            assumedDropdownMenuItemHeight
                        } else if (searchText.isEmpty() && countries.isNotEmpty()) {
                            // If there's no search text, show all countries
                            min(assumedDropdownMenuItemHeight * countries.size, maxDropdownHeight)
                        } else {
                            // Display the filtered countries
                            min(assumedDropdownMenuItemHeight * filteredCountries.size, maxDropdownHeight)
                        }
                    }

                    Box(modifier = Modifier
                        .width(textFieldWidthDp) // Use measured width of TextField
                        .height(calculatedLazyColumnHeight) // Use calculated height of LazyColumn
                    ) {
                        LazyColumn {
                            if (filteredCountries.isEmpty() && searchText.isNotEmpty()) {
                                item {
                                    DropdownMenuItem(
                                        text = { Text("No matching countries found") },
                                        onClick = { /* No operation */ },
                                        enabled = false,
                                        modifier = Modifier.height(assumedDropdownMenuItemHeight) // Keep height consistent
                                    )
                                }
                            } else if (searchText.isEmpty() && countries.isNotEmpty()) {
                                items(countries) { countryName ->
                                    val countryCode = countryCodes.find { code -> Locale("", code).displayCountry == countryName }
                                    DropdownMenuItem(
                                        text = { Text(countryName) },
                                        onClick = {
                                            onSearchForMarketChange(countryCode)
                                            onSearchTextChange(countryName)
                                            expanded = false
                                        },
                                        modifier = Modifier.height(assumedDropdownMenuItemHeight) // Keep height consistent
                                    )
                                }
                            } else {
                                items(filteredCountries) { countryName ->
                                    val countryCode = countryCodes.find { code -> Locale("", code).displayCountry == countryName }
                                    DropdownMenuItem(
                                        text = { Text(countryName) },
                                        onClick = {
                                            onSearchForMarketChange(countryCode)
                                            onSearchTextChange(countryName)
                                            expanded = false
                                        },
                                        modifier = Modifier.height(assumedDropdownMenuItemHeight) // Keep height consistent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun UserSettingsScreenPreview() {
    // You can provide a mock ViewModel or use default values for preview
    UserSettingsContent(
        searchSimilarNum = 10,
        userDataNum = 20,
        checkMarketIfPlayable = null,
        searchText = "",
        onSearchSimilarNumChange = {},
        onUserDataNumChange = {},
        onSearchForMarketChange = {},
        onSearchTextChange = {}
    )
}