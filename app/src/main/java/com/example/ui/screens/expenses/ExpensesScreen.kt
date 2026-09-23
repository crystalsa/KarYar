package com.example.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ExpenseEntity
import com.example.ui.WorkerViewModel
import com.example.ui.components.HairlineCard
import com.example.ui.components.ModernPillSelector
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate100
import com.example.util.Formatters

@Composable
fun ExpensesScreen(
    viewModel: WorkerViewModel,
    modifier: Modifier = Modifier
) {
    val folder by viewModel.currentFolder.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val workers by viewModel.workers.collectAsState()

    var selectedScopeFilter by remember { mutableStateOf("ALL") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var isAddingExpense by remember { mutableStateOf(false) }
    var deletingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    val filteredList = expenses.filter { exp ->
        val matchesScope = when (selectedScopeFilter) {
            "GROUP" -> exp.scope == "GROUP"
            "INDIVIDUAL" -> exp.scope == "INDIVIDUAL"
            else -> true
        }
        val matchesCategory = when (selectedCategoryFilter) {
            "TRANSIT" -> exp.category == "TRANSIT"
            "ACCOMMODATION" -> exp.category == "ACCOMMODATION"
            "FOOD" -> exp.category == "FOOD"
            "MEDICAL" -> exp.category == "MEDICAL"
            else -> true
        }
        matchesScope && matchesCategory
    }

    val totalAmount = filteredList.sumOf { it.amount }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Header summary
            item {
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مدیریت هزینه‌های جاری این کارگاه",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Formatters.formatCurrency(totalAmount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = RoseAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "تفکیک هزینه‌های ایاب و ذهاب، اسکان و اقامت، خوراک، درمان (جمعی و تکی)",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scope filter pill
            item {
                ModernPillSelector(
                    items = listOf("ALL", "GROUP", "INDIVIDUAL"),
                    selectedItem = selectedScopeFilter,
                    onItemSelected = { selectedScopeFilter = it },
                    labelProvider = {
                        when (it) {
                            "GROUP" -> "هزینه‌های جمعی"
                            "INDIVIDUAL" -> "هزینه‌های تکی"
                            else -> "همه هزینه‌ها"
                        }
                    }
                )
            }

            // Category filter chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categories = listOf(
                        "ALL" to "همه",
                        "TRANSIT" to "ایاب و ذهاب",
                        "ACCOMMODATION" to "اسکان",
                        "FOOD" to "خوراک",
                        "MEDICAL" to "درمان"
                    )
                    categories.forEach { (catKey, catName) ->
                        FilterChip(
                            selected = selectedCategoryFilter == catKey,
                            onClick = { selectedCategoryFilter = catKey },
                            label = { Text(catName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    HairlineCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "هیچ هزینه‌ای برای این فیلتر ثبت نشده است",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "با زدن دکمه + هزینه جدید ثبت کنید",
                                color = AmberAccent,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Expense List Items
            items(filteredList, key = { it.id }) { expense ->
                ExpenseCardItem(
                    expense = expense,
                    onDelete = { deletingExpense = expense }
                )
            }
        }

        // FAB to add expense
        FloatingActionButton(
            onClick = { isAddingExpense = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .testTag("add_expense_fab"),
            containerColor = AmberAccent,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "ثبت هزینه")
        }
    }

    if (isAddingExpense) {
        AddExpenseDialog(
            folder = folder,
            workers = workers,
            onDismiss = { isAddingExpense = false },
            onConfirm = {
                viewModel.addExpense(it)
                isAddingExpense = false
            }
        )
    }

    if (deletingExpense != null) {
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text("حذف هزینه") },
            text = { Text("آیا از حذف هزینه '${deletingExpense?.title}' به مبلغ ${Formatters.formatCurrency(deletingExpense?.amount ?: 0)} اطمینان دارید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingExpense?.let { viewModel.deleteExpense(it) }
                        deletingExpense = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                ) {
                    Text("حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun ExpenseCardItem(
    expense: ExpenseEntity,
    onDelete: () -> Unit
) {
    val (catIcon, catColor, catName) = when (expense.category) {
        "TRANSIT" -> Triple(Icons.Default.DirectionsBus, CyanAccent, "ایاب و ذهاب")
        "ACCOMMODATION" -> Triple(Icons.Default.Home, IndigoAccent, "اسکان")
        "FOOD" -> Triple(Icons.Default.LocalDining, AmberAccent, "خوراک")
        "MEDICAL" -> Triple(Icons.Default.MedicalServices, RoseAccent, "درمان")
        else -> Triple(Icons.Default.MoreHoriz, MaterialTheme.colorScheme.onSurfaceVariant, "سایر")
    }

    val isGroup = expense.scope == "GROUP"
    val isAllowance = expense.impactType == "ALLOWANCE"

    HairlineCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Title, Category Icon, and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = expense.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = catName,
                                fontSize = 11.sp,
                                color = catColor,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (expense.accommodationDays > 0) {
                                Text(
                                    text = " • ${expense.accommodationDays} روز اسکان",
                                    fontSize = 11.sp,
                                    color = IndigoAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = Formatters.formatCurrency(expense.amount),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGroup || isAllowance) AmberAccent else RoseAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Scope pill, worker target, impact badge, delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(
                        text = if (isGroup) "جمعی کارگاه" else "تکی: ${expense.workerName ?: "-"}",
                        dotColor = if (isGroup) IndigoAccent else AmberAccent
                    )
                    if (!isGroup) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(
                            text = if (isAllowance) "+ کمک‌هزینه" else "- کسر از حقوق",
                            dotColor = if (isAllowance) EmeraldAccent else RoseAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = expense.date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = RoseAccent, modifier = Modifier.size(17.dp))
                }
            }

            if (expense.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "یادداشت: ${expense.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
