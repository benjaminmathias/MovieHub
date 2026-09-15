package com.benjamin.moviehub.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
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
internal fun YearPicker(
    filters: DiscoverFilters,
    yearOptions: List<Int?>,
    recentYears: List<Int>,
    currentYear: Int,
    customYearMode: Boolean,
    onCustomYearModeChange: (Boolean) -> Unit,
    customYearText: String,
    onCustomYearTextChange: (String) -> Unit,
    customYearInvalid: Boolean,
    onReleaseYearSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag("discover_year_section")
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            yearOptions.forEach { year ->
                DiscoverFilterChip(
                    selected = !customYearMode && filters.releaseYear == year,
                    onClick = {
                        onCustomYearModeChange(false)
                        onCustomYearTextChange("")
                        onReleaseYearSelected(year)
                    },
                    label = year?.toString() ?: stringResource(R.string.discover_all_years),
                    modifier = Modifier.testTag("discover_year_option_${year ?: "all"}"),
                )
            }
            DiscoverFilterChip(
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
                label = stringResource(R.string.discover_other_year),
                modifier = Modifier.testTag("discover_year_option_other"),
            )
        }

        if (customYearMode) {
            val showError = customYearInvalid && customYearText.isNotEmpty()
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
                        if (showError) {
                            stringResource(R.string.discover_year_error, 1870, currentYear)
                        } else {
                            stringResource(R.string.discover_year_hint, 1870, currentYear)
                        },
                    )
                },
                isError = showError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}
