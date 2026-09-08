package com.artillery.fehelper.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SectionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Border),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = TextStyle(color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall.copy(color = MutedInk))
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
internal fun NumberField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    suffix: String,
    error: String?,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = label) },
        suffix = { Text(text = suffix) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { message -> { Text(text = message) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
