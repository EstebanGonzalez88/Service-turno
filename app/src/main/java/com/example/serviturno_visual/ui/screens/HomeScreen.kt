package com.example.serviturno_visual.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class BusinessItem(
    val id: String,
    val name: String,
    val category: String,
    val rating: Double,
    val address: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onBusinessClick: (String) -> Unit = {}) {
    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("Todos", "Reparación", "Cerrajería", "Estética", "Pastelería")
    var selectedCategory by remember { mutableStateOf("Todos") }

    val sampleBusinesses = listOf(
        BusinessItem("1", "Taller Móvil Fix", "Reparación", 4.8, "Calle Central #45"),
        BusinessItem("2", "Cerrajería El Chispazo", "Cerrajería", 4.5, "Av. 5 de Mayo #12"),
        BusinessItem("3", "Pastelería Delicias", "Pastelería", 4.9, "Boulevard Belisario #89")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ServiTurno", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("¿Qué servicio buscas hoy?") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selector horizontal de categorías
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = (selectedCategory == cat),
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Negocios destacados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Lista de negocios
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sampleBusinesses) { business ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBusinessClick(business.id) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(business.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(business.rating.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(business.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            Text(business.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    HomeScreen()
}