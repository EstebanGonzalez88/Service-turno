package com.example.serviturno_visual.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.serviturno_visual.network.BusinessResponse
import com.example.serviturno_visual.network.RetrofitClient
import kotlinx.coroutines.launch

data class BusinessItem(
    val id: String,
    val name: String,
    val category: String,
    val rating: Double,
    val address: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userEmail: String = "usuario@serviturno.com",
    userProfile: UserProfileData = UserProfileData(),
    userBusiness: BusinessResponse? = null,
    onBusinessClick: (BusinessItem) -> Unit = {},
    onMyAccountClick: () -> Unit = {},
    onMyBusinessClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("Todos", "Reparación", "Cerrajería", "Estética", "Pastelería", "Mecánica", "Climas")
    var selectedCategory by remember { mutableStateOf("Todos") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var apiBusinesses by remember { mutableStateOf<List<BusinessItem>>(emptyList()) }
    var isLoadingBusinesses by remember { mutableStateOf(true) }

    // Consultar exclusivamente negocios reales registrados en la base de datos de SQLite
    LaunchedEffect(Unit) {
        try {
            val list = RetrofitClient.instance.getAllBusinesses()
            apiBusinesses = list.map { b ->
                BusinessItem(
                    id = b.id.toString(),
                    name = b.nombre,
                    category = b.categoria,
                    rating = 5.0,
                    address = "${b.direccion}, ${b.ciudad}"
                )
            }
        } catch (_: Exception) {
            apiBusinesses = emptyList()
        } finally {
            isLoadingBusinesses = false
        }
    }

    val allBusinesses = apiBusinesses

    // Filtrar en tiempo real por búsqueda y por categoría seleccionada (Optimizado con remember)
    val filteredBusinesses = remember(searchQuery, selectedCategory, allBusinesses) {
        allBusinesses.filter { business ->
            val matchesSearch = searchQuery.isBlank() ||
                    business.name.contains(searchQuery, ignoreCase = true) ||
                    business.category.contains(searchQuery, ignoreCase = true) ||
                    business.address.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == "Todos" ||
                    business.category.contains(selectedCategory, ignoreCase = true)

            matchesSearch && matchesCategory
        }
    }

    val displayName = if (userProfile.nombre.isNotBlank()) {
        "${userProfile.nombre} ${userProfile.apellidos}".trim()
    } else {
        "Mi Perfil"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Cabecera del Menú Lateral
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Avatar de Usuario",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                HorizontalDivider()

                Spacer(modifier = Modifier.height(12.dp))

                // Opciones del Menú
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Mi Cuenta") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onMyAccountClick()
                    }
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    label = { Text(if (userBusiness != null) "Mi Negocio (${userBusiness.nombre})" else "Mi Negocio") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onMyBusinessClick()
                    }
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ajustes") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSettingsClick()
                    }
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.PrivacyTip, contentDescription = null) },
                    label = { Text("Políticas y Privacidad") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onPrivacyClick()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    label = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogoutClick()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ServiTurno", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú de opciones (3 rayitas)")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Buscador interactivo
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("¿Qué servicio buscas hoy?") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // TARJETA DE "MI NEGOCIO" EN EL DASHBOARD PRINCIPAL (Si el usuario tiene un negocio registrado)
                if (userBusiness != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mi Negocio: ${userBusiness.nombre}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${userBusiness.categoria} • Estado: ${userBusiness.estado}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Button(
                                onClick = onMyBusinessClick,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Administrar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Selector horizontal de categorías
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it }) { cat ->
                        FilterChip(
                            selected = (selectedCategory == cat),
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Negocios destacados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("(${filteredBusinesses.size})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Lista de negocios filtrada en tiempo real
                if (filteredBusinesses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron negocios con '$searchQuery'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filteredBusinesses, key = { it.id }) { business ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBusinessClick(business) },
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
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    HomeScreen()
}