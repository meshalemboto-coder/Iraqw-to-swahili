package tz.kamusi.kiiraqw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import java.util.Locale

data class Entry(
    val kiiraqw: String,
    val swahili: String,
    val notes: String = "",
    val verified: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entries = loadDictionary()
        setContent { KamusiScreen(entries) }
    }

    private fun loadDictionary(): List<Entry> {
        return try {
            val raw = assets.open("dictionary.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Entry(
                            kiiraqw = o.optString("kiiraqw"),
                            swahili = o.optString("swahili"),
                            notes = o.optString("notes"),
                            verified = o.optBoolean("verified", false)
                        )
                    )
                }
            }.filter { it.verified && it.kiiraqw.isNotBlank() && it.swahili.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KamusiScreen(entries: List<Entry>) {
    var query by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(0) }
    var showAbout by remember { mutableStateOf(false) }

    val filtered = remember(entries, query, direction) {
        val q = query.trim().lowercase(Locale.ROOT)
        entries.filter { e ->
            q.isEmpty() ||
                if (direction == 0) {
                    e.kiiraqw.lowercase(Locale.ROOT).contains(q) ||
                        e.swahili.lowercase(Locale.ROOT).contains(q)
                } else {
                    e.swahili.lowercase(Locale.ROOT).contains(q) ||
                        e.kiiraqw.lowercase(Locale.ROOT).contains(q)
                }
        }.sortedBy { if (direction == 0) it.kiiraqw.lowercase(Locale.ROOT) else it.swahili.lowercase(Locale.ROOT) }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Kamusi ya Kiiraqw", fontWeight = FontWeight.Bold)
                            Text("Kiiraqw ↔ Kiswahili • Bila intaneti", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        TextButton(onClick = { showAbout = true }) { Text("Kuhusu") }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Tafuta neno kwa Kiiraqw au Kiswahili",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Andika neno…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tafuta") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(16.dp)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = direction == 0,
                        onClick = { direction = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Kiiraqw → Kiswahili") }
                    SegmentedButton(
                        selected = direction == 1,
                        onClick = { direction = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Kiswahili → Kiiraqw") }
                }

                if (entries.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Kamusi ya mwanzo", fontWeight = FontWeight.Bold)
                            Text(
                                "Maneno haya yametokana na chanzo cha kamusi mtandaoni na yanahitaji kuhakikiwa na mzungumzaji wa Kiiraqw wa Mbulu/Babati. " +
                                "Matamshi yaliyoandikwa ni mwongozo wa kusoma tu."
                            )
                            Text("Eneo lengwa: Mbulu / Babati, Manyara", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else if (filtered.isEmpty()) {
                    Text("Hakuna matokeo ya “$query”. Jaribu neno jingine.")
                } else {
                    Text("${filtered.size} matokeo", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(filtered) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(entry.kiiraqw, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(entry.swahili, style = MaterialTheme.typography.bodyLarge)
                                    if (entry.notes.isNotBlank()) {
                                        Text(entry.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text("Kuhusu kamusi") },
                text = {
                    Text(
                        "Kamusi ya Kiiraqw ni mradi wa offline unaolenga matumizi ya Kiiraqw ya Mbulu/Babati. " +
                        "Tafsiri zinapaswa kuhakikiwa na wazungumzaji wa eneo husika. " +
                        "App haitumii intaneti wala kukusanya taarifa."
                    )
                },
                confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Sawa") } }
            )
        }
    }
}
