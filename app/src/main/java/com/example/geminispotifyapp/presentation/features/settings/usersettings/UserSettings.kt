package com.example.geminispotifyapp.presentation.features.settings.usersettings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.geminispotifyapp.core.utils.findActivity
import com.example.geminispotifyapp.core.utils.openAppNotificationSettings
import com.example.geminispotifyapp.presentation.WELCOME_ROUTE
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale


@Composable
fun UserSettingsScreen(
    viewModel: UserSettingsViewModel = hiltViewModel(),
    navController: NavController
) {
    val searchSimilarNum by viewModel.searchSimilarNum.collectAsStateWithLifecycle()
    val userDataNum by viewModel.userDataNum.collectAsStateWithLifecycle()
    val checkMarketIfPlayable by viewModel.checkMarketIfPlayable.collectAsStateWithLifecycle()
    val numOfShowCaseSearch by viewModel.numOfShowCaseSearch.collectAsStateWithLifecycle()
    val languageOfShowCaseSearch by viewModel.languageOfShowCaseSearch.collectAsStateWithLifecycle()
    val genreOfShowCaseSearch by viewModel.genreOfShowCaseSearch.collectAsStateWithLifecycle()
    val yearOfShowCaseSearch by viewModel.yearOfShowCaseSearch.collectAsStateWithLifecycle()
    val isRandomYearOfShowCaseSelection by viewModel.isRandomYearOfShowCaseSelection.collectAsStateWithLifecycle()

    val searchText by viewModel.searchText.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isSystemPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showEnableDialog by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isSystemPermissionGranted = isGranted
        viewModel.syncNotificationPermissionState(isGranted)

        if (!isGranted) {
            val activity = context.findActivity()
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity!!,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (!shouldShowRationale) {
                showEnableDialog = true
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentStatus = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                isSystemPermissionGranted = currentStatus
                viewModel.syncNotificationPermissionState(currentStatus)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
        isRandomYearOfShowCaseSelection = isRandomYearOfShowCaseSelection,
        onNumOfShowCaseSearchChange = { newValue -> scope.launch { viewModel.setNumOfShowCaseSearch(newValue) } },
        onLanguageOfShowCaseSearchChange = { newValue -> scope.launch { viewModel.setLanguageOfShowCaseSearch(newValue) } },
        onGenreOfShowCaseSearchChange = { newValue -> scope.launch { viewModel.setGenreOfShowCaseSearch(newValue) } },
        onYearOfShowCaseSearchChange = { newValue -> scope.launch { viewModel.setYearOfShowCaseSearch(newValue) } },
        onIsRandomYearOfShowCaseSelection = { newValue -> scope.launch { viewModel.setIsRandomYearOfShowCaseSelection(newValue) } },
        onResetWelcomeFlowClick = {
            viewModel.resetWelcomeFlowCompleted()
            navController.navigate(WELCOME_ROUTE) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true } 
            }
        },
        isNotificationEnabled = isSystemPermissionGranted,
        onNotificationToggle = { shouldCheck ->
            if (shouldCheck) {
                if (!isSystemPermissionGranted) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                showDisableDialog = true
            }
        }
    )

    if (showEnableDialog) {
        EnableNotificationDialog(
            onDismiss = { showEnableDialog = false },
            onConfirm = {
                showEnableDialog = false
                context.openAppNotificationSettings()
            }
        )
    }

    if (showDisableDialog) {
        DisableNotificationDialog(
            onDismiss = { showDisableDialog = false },
            onConfirm = {
                showDisableDialog = false
                context.openAppNotificationSettings()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    languageOfShowCaseSearch: String,
    genreOfShowCaseSearch: String,
    yearOfShowCaseSearch: String,
    isRandomYearOfShowCaseSelection: Boolean,
    onNumOfShowCaseSearchChange: (Int) -> Unit,
    onLanguageOfShowCaseSearchChange: (String) -> Unit,
    onGenreOfShowCaseSearchChange: (String) -> Unit,
    onYearOfShowCaseSearchChange: (String) -> Unit,
    onIsRandomYearOfShowCaseSelection: (Boolean) -> Unit,
    onResetWelcomeFlowClick: () -> Unit,
    isNotificationEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit
) {
    val countryCodes = remember { Locale.getISOCountries() }
    val countries = remember(countryCodes) {
        countryCodes.map { code -> Locale("", code).displayCountry }.sorted()
    }
    val languageLocales = remember {
        Locale.getAvailableLocales()
            .filter { it.language.isNotEmpty() && it.displayLanguage.isNotEmpty() }
            .distinctBy { it.language }
    }
    val languagePairs = remember(languageLocales) {
        languageLocales.map { locale ->
            // For each locale, get its display name in the device's language and in English
            Pair(locale.getDisplayLanguage(Locale.getDefault()), locale.getDisplayLanguage(Locale.ENGLISH))
        }
            .distinct() // Remove duplicates that may arise from identical display names
            .sortedBy { it.first } // Sort by the display name that the user sees
    }

    var languageSearchText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val genres = remember {
        listOf(
            "Pop",
            "Rock",
            "Hip Hop",
            "Jazz",
            "Classical",
            "Country",
            "Electronic",
            "Instrumental",
            "R&B",
            "Reggae",
            "Blues",
            "Metal",
            "Folk",
            "Indie",
            "Punk",
            "Soul",
            "Funk",
            "Disco",
            "Techno",
            "House",
            "Ambient",
        )
    }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

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

    val tabTitles = listOf("Showcase", "Search", "Others")
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val coroutineScope = rememberCoroutineScope()


    Column(modifier = Modifier.fillMaxSize()) { // Let Column occupy the whole layout to take effect the gesture of HorizontalPager
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f) // Let HorizontalPager occupy all remaining vertical space
        ) { page ->
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)) { // Let LazyColumn occupy its parent space
                when (page) {
                    0 -> {
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
                            val useLanguage = languageOfShowCaseSearch != ""
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Enable Language Filter")
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = useLanguage,
                                    onCheckedChange = { isChecked ->
                                        if (!isChecked) {
                                            onLanguageOfShowCaseSearchChange("")
                                            languageSearchText = "" // Clear search text when turned off
                                        } else {
                                            onLanguageOfShowCaseSearchChange("English")
                                            // When turned on, set the search box text to the currently selected language
                                            languageSearchText =
                                                languagePairs.find { it.second == languageOfShowCaseSearch }?.first
                                                    ?: languageOfShowCaseSearch
                                        }
                                    }
                                )
                            }

                            AnimatedVisibility(visible = useLanguage) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    ExposedDropdownMenuBox(
                                        expanded = expandedLanguage,
                                        onExpandedChange = {
                                            expandedLanguage = !expandedLanguage
                                            if (!expandedLanguage) {
                                                focusManager.clearFocus() // Clear focus when the menu is closed
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            // value is now controlled by languageSearchText
                                            value = languageSearchText,
                                            onValueChange = { newValue ->
                                                languageSearchText = newValue
                                                if (!expandedLanguage) expandedLanguage = true
                                            },
                                            label = {
                                                val currentDisplayName =
                                                    languageOfShowCaseSearch.let { englishName ->
                                                        languagePairs.find { it.second == englishName }?.first
                                                    }
                                                if (languageSearchText.isNotEmpty() || expandedLanguage) {
                                                    Text("Select a language")
                                                } else {
                                                    Text(currentDisplayName ?: "Select a language")
                                                }
                                            },
                                            placeholder = {
                                                val currentDisplayName =
                                                    languageOfShowCaseSearch.let { englishName ->
                                                        languagePairs.find { it.second == englishName }?.first
                                                    }
                                                Text(currentDisplayName ?: "Select or input language")
                                            },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguage) },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedLanguage,
                                            onDismissRequest = { expandedLanguage = false },
                                        ) {
                                            // Filter the list based on the search text
                                            val filteredLanguages = remember(languagePairs, languageSearchText) {
                                                if (languageSearchText.isEmpty()) {
                                                    languagePairs
                                                } else {
                                                    languagePairs.filter { (display, english) ->
                                                        display.contains(languageSearchText, ignoreCase = true) ||
                                                                english.contains(languageSearchText, ignoreCase = true)
                                                    }
                                                }
                                            }

                                            if (filteredLanguages.isNotEmpty()) {
                                                filteredLanguages.forEach { (displayLanguage, englishLanguage) ->
                                                    DropdownMenuItem(
                                                        text = { Text(displayLanguage) },
                                                        onClick = {
                                                            onLanguageOfShowCaseSearchChange(englishLanguage) // Update search box text
                                                            languageSearchText = displayLanguage
                                                            expandedLanguage = false
                                                            focusManager.clearFocus()
                                                        },
                                                    )
                                                }
                                            } else {
                                                DropdownMenuItem(
                                                    text = { Text("No matching languages found") },
                                                    onClick = { },
                                                    enabled = false
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
                            var useGenre = genreOfShowCaseSearch != ""

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Enable Genre Filter")
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = useGenre,
                                    onCheckedChange = { isChecked ->
                                        useGenre = isChecked
                                        if (!isChecked) {
                                            onGenreOfShowCaseSearchChange("")
                                        } else {
                                            onGenreOfShowCaseSearchChange("Country")
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
                                            value = genreOfShowCaseSearch,
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
                            var useYear = yearOfShowCaseSearch != ""

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Enable Year Filter")
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = useYear,
                                    onCheckedChange = { isChecked ->
                                        useYear = isChecked
                                        if (!isChecked) {
                                            onYearOfShowCaseSearchChange("")
                                        } else {
                                            onYearOfShowCaseSearchChange("2015")
                                        }
                                    }
                                )
                            }

                            AnimatedVisibility(visible = useYear) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    val annotatedText = buildAnnotatedString {
                                        append("Year of showcase tracks to search: ")
                                        pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = MaterialTheme.colorScheme.onSurface))
                                        append(yearOfShowCaseSearch)
                                        pop()
                                    }
                                    Text(text = annotatedText, modifier = Modifier.padding(bottom = 8.dp))
                                    Slider(
                                        value = if (yearOfShowCaseSearch != "") yearOfShowCaseSearch.toFloat() else 2015f,
                                        onValueChange = { newValue ->
                                            onYearOfShowCaseSearchChange(newValue.toInt().toString())
                                        },
                                        valueRange = 1900f..currentYear.toFloat(),
                                        steps = currentYear - 1900 - 1,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Randomize the Selected Year in Small Range!",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = isRandomYearOfShowCaseSelection,
                                            onCheckedChange = {
                                                onIsRandomYearOfShowCaseSelection(it)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onResetWelcomeFlowClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Tap to Regenerate Your Music Taste!")
                            }
                        }
                    }
                    1 -> {
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
                    }
                    2 -> {
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
                            NotificationSettingItem(
                                isChecked = isNotificationEnabled,
                                onCheckedChange = onNotificationToggle,
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationSettingItem(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Daily Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isChecked)
                    "You'll get notified when your daily mix is ready."
                else "Enable to get notified about your daily mix.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun EnableNotificationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Notifications") },
        text = { Text("Notifications are blocked by system settings. Please enable them manually to receive updates.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Open Settings", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DisableNotificationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disable Notifications") },
        text = { Text("To disable notifications, you need to modify permissions in your device settings.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Go to Settings", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
        isRandomYearOfShowCaseSelection = false,
        onNumOfShowCaseSearchChange = {},
        onLanguageOfShowCaseSearchChange = {},
        onGenreOfShowCaseSearchChange = {},
        onYearOfShowCaseSearchChange = {},
        onIsRandomYearOfShowCaseSelection = {},
        onResetWelcomeFlowClick = {},
        isNotificationEnabled = false,
        onNotificationToggle = {}
    )
}
