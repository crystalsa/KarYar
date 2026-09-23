package com.example.ui.screens.workers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.DateFolderEntity
import com.example.ui.components.HairlineCard
import com.example.ui.components.IconicsBox
import com.example.ui.components.IconicsSize
import com.example.ui.components.LoadingButton
import com.example.ui.components.LoadingOutlinedButton
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.Slate100
import com.example.util.JalaliCalendar

private val DAYS_OF_WEEK = listOf(
    "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
)

@Composable
fun AddEditDateFolderDialog(
    initialDateFolder: DateFolderEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (date: String, dayOfWeek: String, title: String) -> Unit
) {
    val initialDate = initialDateFolder?.date ?: JalaliCalendar.todayString()
    var date by remember { mutableStateOf(initialDate) }
    var dayOfWeek by remember {
        mutableStateOf(initialDateFolder?.dayOfWeek ?: JalaliCalendar.getDayOfWeek(initialDate))
    }
    var title by remember {
        mutableStateOf(initialDateFolder?.title ?: "شیفت کاری")
    }

    Dialog(onDismissRequest = onDismiss) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Header with Android-Iconics box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconicsBox(
                            icon = Icons.Default.Folder,
                            color = AmberAccent,
                            size = IconicsSize.SMALL
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (initialDateFolder == null) "پوشه جدید تاریخ و روز" else "ویرایش پوشه روز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date input
                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                        val detectedDay = JalaliCalendar.getDayOfWeek(it)
                        if (detectedDay in DAYS_OF_WEEK) {
                            dayOfWeek = detectedDay
                        }
                    },
                    label = { Text("تاریخ شمسی (مثلاً ۱۴۰۵/۰۱/۱۵)", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Day of Week selector
                Text(
                    text = "روز هفته:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DAYS_OF_WEEK.forEach { day ->
                        val isSelected = dayOfWeek == day
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) AmberAccent else Slate100,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { dayOfWeek = day }
                        ) {
                            Text(
                                text = day,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                fontSize = 11.5.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان یا توضیح پوشه (اختیاری)", fontSize = 12.sp) },
                    placeholder = { Text("مثلاً شیفت صبح، بتن‌ریزی سقف", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Actions with LoadingButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoadingOutlinedButton(
                        text = "انصراف",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                    LoadingButton(
                        text = if (initialDateFolder == null) "ایجاد پوشه" else "ذخیره تغییرات",
                        icon = Icons.Default.Folder,
                        onClick = {
                            if (date.isNotBlank() && dayOfWeek.isNotBlank()) {
                                onConfirm(date.trim(), dayOfWeek.trim(), title.trim())
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        containerColor = AmberAccent,
                        height = 40.dp,
                        testTag = "submit_date_folder_button"
                    )
                }
            }
        }
    }
}
