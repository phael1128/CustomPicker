package com.example.custompicker.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.custompicker.R
import com.example.custompicker.constants.PickerDefine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaOptionSettingBottomSheet(
    currentSortingType: Int,
    onDismissRequest: () -> Unit,
    onSaveClick: (Int) -> Unit,
) {
    var selectedSortingType by remember(currentSortingType) { mutableIntStateOf(currentSortingType) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(id = R.string.sorting_dialog_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.sorting),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(id = R.string.descending_order),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MediaOptionChip(
                        text = stringResource(id = R.string.modified_date),
                        selected = selectedSortingType == PickerDefine.TYPE_SORTING_MODIFIED_DATE,
                        onClick = { selectedSortingType = PickerDefine.TYPE_SORTING_MODIFIED_DATE },
                        modifier = Modifier.weight(1f),
                    )
                    MediaOptionChip(
                        text = stringResource(id = R.string.created_date),
                        selected = selectedSortingType == PickerDefine.TYPE_SORTING_CREATE_DATE,
                        onClick = { selectedSortingType = PickerDefine.TYPE_SORTING_CREATE_DATE },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
                Button(
                    onClick = {
                        onSaveClick(selectedSortingType)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.dialog_option_saved))
                }
            }
        }
    }
}

@Composable
private fun MediaOptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlightColor = Color(0xFF2F6BFF)
    val borderColor =
        if (selected) {
            highlightColor
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        }
    val backgroundColor =
        if (selected) {
            highlightColor.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        }

    Surface(
        modifier =
            modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
