package com.hisaab.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val category: String,
    val paymentType: String,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Expense>>

    @Insert
    suspend fun insert(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [Expense::class], version = 1, exportSchema = false)
abstract class HisaabDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var INSTANCE: HisaabDatabase? = null
        fun get(context: android.content.Context): HisaabDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context, HisaabDatabase::class.java, "hisaab.db").build().also { INSTANCE = it }
        }
    }
}

class HisaabViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = HisaabDatabase.get(application).expenseDao()
    val expenses: Flow<List<Expense>> = dao.observeAll()

    fun add(amountPaise: Long, category: String, paymentType: String, note: String) {
        viewModelScope.launch { dao.insert(Expense(amountPaise = amountPaise, category = category, paymentType = paymentType, note = note)) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this)[HisaabViewModel::class.java]
        setContent { HisaabApp(vm) }
    }
}

private val ink = Color(0xFF17352D)
private val green = Color(0xFF1F765A)
private val paleGreen = Color(0xFFE7F3ED)
private val muted = Color(0xFF68756E)

@Composable
fun HisaabApp(vm: HisaabViewModel) {
    val expenses by vm.expenses.collectAsStateWithLifecycle(initialValue = emptyList())
    var tab by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = green,
        onPrimary = Color.White,
        secondary = ink,
        background = Color(0xFFF7F8F5),
        surface = Color.White,
        onSurface = ink
    )) {
        Scaffold(
            containerColor = Color(0xFFF7F8F5),
            topBar = { TopAppBar(title = { Text("Hisaab", fontWeight = FontWeight.Bold, color = ink) }) },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Filled.ReceiptLong, null) }, label = { Text("Activity") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Filled.Analytics, null) }, label = { Text("Insights") })
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("About") })
                }
            },
            floatingActionButton = {
                if (tab < 2) FloatingActionButton(onClick = { showAdd = true }, containerColor = green, contentColor = Color.White) {
                    Icon(Icons.Filled.Add, contentDescription = "Add expense")
                }
            }
        ) { padding ->
            when (tab) {
                0 -> HomeScreen(expenses, Modifier.padding(padding), onAdd = { showAdd = true }, onDelete = vm::delete)
                1 -> ActivityScreen(expenses, Modifier.padding(padding), onDelete = vm::delete)
                2 -> InsightsScreen(expenses, Modifier.padding(padding))
                else -> AboutScreen(Modifier.padding(padding))
            }
        }
        if (showAdd) AddExpenseDialog(onDismiss = { showAdd = false }, onSave = { amount, category, type, note ->
            vm.add(amount, category, type, note)
            showAdd = false
        })
    }
}

@Composable
private fun HomeScreen(expenses: List<Expense>, modifier: Modifier = Modifier, onAdd: () -> Unit, onDelete: (Long) -> Unit) {
    val monthTotal = expenses.filter { isThisMonth(it.createdAt) }.sumOf { it.amountPaise }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("YOUR SPENDING THIS MONTH", color = muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ink)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("Total expenses", color = Color(0xFFCADBD2), fontSize = 14.sp)
                Text(money(monthTotal), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                Text("${expenses.count { isThisMonth(it.createdAt) }} transactions", color = Color(0xFFCADBD2), modifier = Modifier.padding(top = 8.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recent activity", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ink)
            TextButton(onClick = onAdd) { Text("+ Add expense") }
        }
        if (expenses.isEmpty()) EmptyState("Your money story starts here", "Add an expense to see it show up here.", onAdd)
        else expenses.take(6).forEach { ExpenseRow(it, onDelete) }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ActivityScreen(expenses: List<Expense>, modifier: Modifier = Modifier, onDelete: (Long) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = expenses.filter {
        query.isBlank() || it.category.contains(query, true) || it.note.contains(query, true) || it.paymentType.contains(query, true)
    }
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp),
            label = { Text("Search transactions") }, singleLine = true, shape = RoundedCornerShape(16.dp)
        )
        if (filtered.isEmpty()) {
            EmptyState("No expenses found", if (query.isBlank()) "Add your first expense from Home." else "Try another search.", {})
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(filtered, key = { it.id }) { ExpenseRow(it, onDelete) }
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, onDelete: (Long) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = paleGreen, modifier = Modifier.size(46.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(expense.category.take(1).uppercase(), color = green, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(expense.category, color = ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = listOf(expense.note.takeIf { it.isNotBlank() }, expense.paymentType, dateLabel(expense.createdAt)).filterNotNull().joinToString(" · ")
            Text(subtitle, color = muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("−${money(expense.amountPaise)}", color = ink, fontWeight = FontWeight.Bold)
        IconButton(onClick = { onDelete(expense.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete ${expense.category}", tint = muted) }
    }
    Divider(color = Color(0xFFE9EDE9))
}

@Composable
private fun InsightsScreen(expenses: List<Expense>, modifier: Modifier = Modifier) {
    val month = expenses.filter { isThisMonth(it.createdAt) }
    val total = month.sumOf { it.amountPaise }
    val categories = month.groupBy { it.category }.mapValues { (_, rows) -> rows.sumOf { it.amountPaise } }.entries.sortedByDescending { it.value }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Insights", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ink)
        Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()), color = muted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = paleGreen), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Spent this month", color = muted)
                Text(money(total), color = ink, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                Text("${month.size} transactions", color = muted, modifier = Modifier.padding(top = 5.dp))
            }
        }
        Text("By category", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = ink, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
        if (categories.isEmpty()) Text("No expenses this month yet.", color = muted)
        categories.forEach { entry ->
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.key, color = ink)
                Column(horizontalAlignment = Alignment.End) {
                    Text(money(entry.value), color = ink, fontWeight = FontWeight.SemiBold)
                    Text(if (total == 0L) "0%" else "${(entry.value * 100 / total)}%", color = muted, fontSize = 12.sp)
                }
            }
            Divider(color = Color(0xFFE9EDE9))
        }
    }
}

@Composable
private fun AboutScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Your money, your phone.", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ink)
        Text("Hisaab stores your expenses locally on this device using an encrypted-by-Android app sandbox and a Room database. It does not need an internet connection or an account.", color = muted, modifier = Modifier.padding(top = 12.dp))
        Text("Add expenses manually", fontWeight = FontWeight.SemiBold, color = ink, modifier = Modifier.padding(top = 24.dp))
        Text("Choose a category and whether you paid with cash or online. Search or remove transactions in Activity; view your monthly totals in Insights.", color = muted, modifier = Modifier.padding(top = 6.dp))
        Text("Hisaab · version 1.0", color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 28.dp))
    }
}

@Composable
private fun EmptyState(title: String, detail: String, onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontWeight = FontWeight.SemiBold, color = ink, fontSize = 17.sp)
        Text(detail, color = muted, modifier = Modifier.padding(top = 6.dp))
        if (title.startsWith("Your money")) Button(onClick = onAdd, modifier = Modifier.padding(top = 14.dp)) { Text("Add expense") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(onDismiss: () -> Unit, onSave: (Long, String, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("Online") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val suggestions = listOf("Food", "Travel", "Shopping", "Bills", "Health", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 540.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = amount, onValueChange = { amount = it; error = null }, label = { Text("Amount (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("SUGGESTED CATEGORIES", color = muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                suggestions.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { item ->
                            if (category.equals(item, true)) Button(onClick = { category = item }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) { Text(item) }
                            else OutlinedButton(onClick = { category = item }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) { Text(item) }
                        }
                    }
                }
                OutlinedTextField(value = category, onValueChange = { category = it; error = null }, label = { Text("Category (or type a new one)") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Text("HOW DID YOU PAY?", color = muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("Online", "Cash").forEach { option ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            RadioButton(selected = payment == option, onClick = { payment = option })
                            Text(option, color = ink)
                        }
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = try { BigDecimal(amount.trim()).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact() } catch (_: Exception) { null }
                when {
                    parsed == null || parsed <= 0L -> error = "Enter a valid amount greater than zero."
                    category.isBlank() -> error = "Add a category to continue."
                    else -> onSave(parsed, category.trim(), payment, note.trim())
                }
            }) { Text("Save expense") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun isThisMonth(timestamp: Long): Boolean {
    val now = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return now.get(Calendar.YEAR) == date.get(Calendar.YEAR) && now.get(Calendar.MONTH) == date.get(Calendar.MONTH)
}

private fun money(paise: Long): String {
    val rupees = paise / 100.0
    return "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(rupees)
}

private fun dateLabel(timestamp: Long): String = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
