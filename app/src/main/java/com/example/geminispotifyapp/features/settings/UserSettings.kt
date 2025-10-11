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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalFocusManager
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
    val numOfShowCaseSearch by viewModel.numOfShowCaseSearch.collectAsStateWithLifecycle()
    val languageOfShowCaseSearch by viewModel.languageOfShowCaseSearch.collectAsStateWithLifecycle()
    val genreOfShowCaseSearch by viewModel.genreOfShowCaseSearch.collectAsStateWithLifecycle()
    val yearOfShowCaseSearch by viewModel.yearOfShowCaseSearch.collectAsStateWithLifecycle()

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
        onSearchTextChange = { newValue -> viewModel.updateSearchText(newValue) },
        numOfShowCaseSearch = numOfShowCaseSearch,
        languageOfShowCaseSearch = languageOfShowCaseSearch,
        genreOfShowCaseSearch = genreOfShowCaseSearch,
        yearOfShowCaseSearch = yearOfShowCaseSearch,
        onNumOfShowCaseSearchChange = { newValue -> scope.launch { viewModel.setNumOfShowCaseSearch(newValue) } },
        onLanguageOfShowCaseSearchChange = { newValue -> scope.launch { viewModel.setLanguageOfShowCaseSearch(newValue) } },
        onGenreOfShowCaseSearchChange = { newValue -> scope.launch { viewModel.setGenreOfShowCaseSearch(newValue) } },
        onYearOfShowCaseSearchChange = { newValue -> scope.launch { viewModel.setYearOfShowCaseSearch(newValue) } }
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
    onSearchTextChange: (String) -> Unit,
    numOfShowCaseSearch: Int,
    languageOfShowCaseSearch: String?,
    genreOfShowCaseSearch: String?,
    yearOfShowCaseSearch: String?,
    onNumOfShowCaseSearchChange: (Int) -> Unit,
    onLanguageOfShowCaseSearchChange: (String?) -> Unit,
    onGenreOfShowCaseSearchChange: (String?) -> Unit,
    onYearOfShowCaseSearchChange: (String?) -> Unit
) {
    val countryCodes = remember { Locale.getISOCountries() }
    val countries = remember(countryCodes) {
        countryCodes.map { code -> Locale("", code).displayCountry }.sorted()
    }
    val languages = remember {
        Locale.getAvailableLocales().map { it.displayLanguage }.distinct().sorted()
    }
    val genres = remember {
        listOf("Pop", "Rock", "Hip Hop", "Jazz", "Classical", "Country", "Electronic", "R&B", "Reggae", "Blues")
    }

    var expanded by remember { mutableStateOf(false) }
    var expandedExpandable by remember { mutableStateOf(false) }
    var expandedLanguage by remember { mutableStateOf(false) }
    var expandedGenre by remember { mutableStateOf(false) }

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
            val focusManager = LocalFocusManager.current
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
                    label = { Text("Select/Search For Market") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                        .fillMaxWidth()
                        // Monitor the layout of OutlinedTextField and get its width for anchor point
                        .onGloballyPositioned { coordinates ->
                            textFieldWidthPx = coordinates.size.width
                        },
                    placeholder = { Text(checkMarketIfPlayable?.let { Locale("", it).displayCountry } ?: "Market") }
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
                                            focusManager.clearFocus()
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
                                            focusManager.clearFocus()
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
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            val annotatedText = buildAnnotatedString {
                append("Number of showcase tracks to search: ")
                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = MaterialTheme.colorScheme.onSurface))
                append("$numOfShowCaseSearch")
                pop()
            }
            Text(text = annotatedText, modifier = Modifier.padding(bottom = 8.dp))
        }
        item {
            Slider(
                value = numOfShowCaseSearch.toFloat(),
                onValueChange = { newValue ->
                    onNumOfShowCaseSearchChange(newValue.toInt())
                },
                valueRange = 1f..50f,
                steps = 49,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
        // Language Filter
        item {
            var useLanguage = languageOfShowCaseSearch != null

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Language Filter")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = useLanguage,
                    onCheckedChange = { isChecked ->
                        useLanguage = isChecked
                        if (!isChecked) {
                            onLanguageOfShowCaseSearchChange(null) // If disabled, set to null
                        } else {
                            // If enabled, set to a default value, e.g., "English",
                            // or keep the previous value if it's not null.
                            onLanguageOfShowCaseSearchChange(languageOfShowCaseSearch ?: "English")
                        }
                    }
                )
            }

            AnimatedVisibility(visible = useLanguage) {
                // We wrap the Dropdown in a Column to ensure proper spacing and layout
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expandedLanguage,
                        onExpandedChange = { expandedLanguage = !expandedLanguage },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            // Show the selected language, or a default value if somehow null while enabled
                            value = languageOfShowCaseSearch ?: "English",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Language of showcase tracks to search") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguage) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLanguage,
                            onDismissRequest = { expandedLanguage = false },
                        ) {
                            languages.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(language) },
                                    onClick = {
                                        onLanguageOfShowCaseSearchChange(language)
                                        expandedLanguage = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Genre Filter
        item {
            var useGenre = genreOfShowCaseSearch != null

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Genre Filter")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = useGenre,
                    onCheckedChange = { isChecked ->
                        useGenre = isChecked
                        if (!isChecked) {
                            onGenreOfShowCaseSearchChange(null)
                        } else {
                            onGenreOfShowCaseSearchChange(genreOfShowCaseSearch ?: "Pop")
                        }
                    }
                )
            }

            AnimatedVisibility(visible = useGenre) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expandedGenre,
                        onExpandedChange = { expandedGenre = !expandedGenre },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = genreOfShowCaseSearch ?: "Pop",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Genre of showcase tracks to search") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGenre) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedGenre,
                            onDismissRequest = { expandedGenre = false },
                        ) {
                            genres.forEach { genre ->
                                DropdownMenuItem(
                                    text = { Text(genre) },
                                    onClick = {
                                        onGenreOfShowCaseSearchChange(genre)
                                        expandedGenre = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // MARK: Year Filter (remains the same)
        item {
            var useYear = yearOfShowCaseSearch != null

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Year Filter")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = useYear,
                    onCheckedChange = { isChecked ->
                        useYear = isChecked
                        if (!isChecked) {
                            onYearOfShowCaseSearchChange(null)
                        } else {
                            onYearOfShowCaseSearchChange(yearOfShowCaseSearch ?: "2015")
                        }
                    }
                )
            }

            AnimatedVisibility(visible = useYear) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    val annotatedText = buildAnnotatedString {
                        append("Year of showcase tracks to search: ")
                        pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = MaterialTheme.colorScheme.onSurface))
                        append(yearOfShowCaseSearch ?: "Any")
                        pop()
                    }
                    Text(text = annotatedText, modifier = Modifier.padding(bottom = 8.dp))
                    Slider(
                        value = yearOfShowCaseSearch?.toFloat() ?: 2015f,
                        onValueChange = { newValue ->
                            onYearOfShowCaseSearchChange(newValue.toInt().toString())
                        },
                        valueRange = 1900f..2024f,
                        steps = 123,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun UserSettingsScreenPreview() {
    UserSettingsContent(
        searchSimilarNum = 10,
        userDataNum = 20,
        checkMarketIfPlayable = null,
        searchText = "",
        onSearchSimilarNumChange = {},
        onUserDataNumChange = {},
        onSearchForMarketChange = {},
        onSearchTextChange = {},
        numOfShowCaseSearch = 15,
        languageOfShowCaseSearch = "English",
        genreOfShowCaseSearch = "Country",
        yearOfShowCaseSearch = "2015",
        onNumOfShowCaseSearchChange = {},
        onLanguageOfShowCaseSearchChange = {},
        onGenreOfShowCaseSearchChange = {},
        onYearOfShowCaseSearchChange = {}
    )
}