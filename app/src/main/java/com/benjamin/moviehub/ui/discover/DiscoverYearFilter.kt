package com.benjamin.moviehub.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.DiscoverFilters

internal fun customYearIsInvalid(
    customYearMode: Boolean,
    customYearText: String,
    currentYear: Int,
): Boolean =
    customYearMode &&
        (customYearText.length != 4 || customYearText.toIntOrNull() !in 1870..currentYear)

@Composable
internal fun YearFilterSection(
    filters: DiscoverFilters,
    yearOptions: List<Int?>,
    recentYears: List<Int>,
    currentYear: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    customYearMode: Boolean,
    onCustomYearModeChange: (Boolean) -> Unit,
    customYearText: String,
    onCustomYearTextChange: (String) -> Unit,
    customYearInvalid: Boolean,
    onReleaseYearSelected: (Int?) -> Unit,
    choiceChipColors: SelectableChipColors,
) {
    FilterSection(
        title = stringResource(R.string.discover_year),
        selectedLabel =
            when {
                customYearMode && customYearText.isNotEmpty() -> customYearText
                customYearMode -> stringResource(R.string.discover_other_year)
                filters.releaseYear != null -> filters.releaseYear.toString()
                else -> stringResource(R.string.discover_all_years)
            },
        selected = filters.releaseYear != null || customYearMode,
        expanded = expanded,
        onClick = onToggle,
        modifier = Modifier.testTag("discover_year_section"),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3,
        ) {
            yearOptions.forEach { year ->
                FilterChip(
                    selected = !customYearMode && filters.releaseYear == year,
                    onClick = {
                        onCustomYearModeChange(false)
                        onCustomYearTextChange("")
                        onReleaseYearSelected(year)
                    },
                    colors = choiceChipColors,
                    label = {
                        Text(year?.toString() ?: stringResource(R.string.discover_all_years))
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("discover_year_option_${year ?: "all"}"),
                )
            }
            FilterChip(
                selected = customYearMode,
                onClick = {
                    onCustomYearModeChange(true)
                    onCustomYearTextChange(
                        filters.releaseYear
                            ?.takeIf { it !in recentYears }
                            ?.toString()
                            .orEmpty(),
                    )
                    onReleaseYearSelected(null)
                },
                colors = choiceChipColors,
                label = { Text(stringResource(R.string.discover_other_year)) },
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("discover_year_option_other"),
            )
        }

        if (customYearMode) {
            OutlinedTextField(
                value = customYearText,
                onValueChange = { value ->
                    val digits = value.filter(Char::isDigit).take(4)
                    onCustomYearTextChange(digits)
                    val year = digits.toIntOrNull()
                    if (digits.isEmpty()) {
                        onReleaseYearSelected(null)
                    } else if (digits.length == 4 && year in 1870..currentYear) {
                        onReleaseYearSelected(year)
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("discover_custom_year"),
                label = { Text(stringResource(R.string.discover_other_year)) },
                supportingText = {
                    Text(
                        if (customYearInvalid) {
                            stringResource(R.string.discover_year_error, 1870, currentYear)
                        } else {
                            stringResource(R.string.discover_year_hint, 1870, currentYear)
                        },
                    )
                },
                isError = customYearInvalid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}
