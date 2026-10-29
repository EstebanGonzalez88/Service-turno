package com.example.serviturno_visual.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.serviturno_visual.network.BusinessResponse
import com.example.serviturno_visual.network.BusinessUpdateRequest
import com.example.serviturno_visual.network.RetrofitClient
import com.example.serviturno_visual.network.TicketResponse
import com.example.serviturno_visual.network.TicketStatusUpdateRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    business: BusinessResponse,
    onBackClick: () -> Unit = {},
    onStatusToggled: (BusinessResponse) -> Unit = {}
) {
    var currentBusiness by remember(business) { mutableStateOf(business) }

    // Estados editables
    var businessName by remember(currentBusiness) { mutableStateOf(currentBusiness.nombre) }
    var category by remember(currentBusiness) { mutableStateOf(currentBusiness.categoria) }
    var phone by remember(currentBusiness) { mutableStateOf(currentBusiness.telefono) }
    var address by remember(currentBusiness) { mutableStateOf(currentBusiness.direccion) }
    var schedule by remember(currentBusiness) { mutableStateOf(currentBusiness.horario) }
    var isOpenNow by remember(currentBusiness) { mutableStateOf(currentBusiness.abierto) }

    // Coordenadas fijadas en el mapa con validación de rango (Tuxtla Gutiérrez, Chiapas)
    val rawLat = currentBusiness.latitud.toDoubleOrNull() ?: 16.7533
    val rawLng = currentBusiness.longitud.toDoubleOrNull() ?: -93.1165
    val validLat = if (rawLat in 14.0..32.0) rawLat else 16.7533
    val validLng = if (rawLng in -118.0..-86.0) rawLng else -93.1165

    var markerPosition by remember(currentBusiness) { mutableStateOf(LatLng(validLat, validLng)) }
    var isLocationConfirmed by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(markerPosition, 16f)
    }

    LaunchedEffect(markerPosition) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(markerPosition, 16f)
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showTicketsDialog by remember { mutableStateOf(false) }
    var businessTickets by remember { mutableStateOf<List<TicketResponse>>(emptyList()) }
    var isToggling by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Permisos de Ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            try {
                @Suppress("MissingPermission")
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                @Suppress("MissingPermission")
                fusedClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        markerPosition = LatLng(loc.latitude, loc.longitude)
                        Toast.makeText(context, "Ubicación aproximada detectada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Posición no disponible de inmediato, usa el mapa para colocar tu pin.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {}
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    // Diálogo Modal para editar información básica del perfil
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Información del Negocio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Nombre del negocio") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono de contacto") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Dirección escrita") },
                        maxLines = 2
                    )
                    OutlinedTextField(
                        value = schedule,
                        onValueChange = { schedule = it },
                        label = { Text("Horario de atención") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditDialog = false
                        coroutineScope.launch {
                            try {
                                val updated = RetrofitClient.instance.updateBusiness(
                                    BusinessUpdateRequest(
                                        business_id = currentBusiness.id,
                                        nombre = businessName.trim(),
                                        telefono = phone.trim(),
                                        direccion = address.trim(),
                                        horario = schedule.trim(),
                                        latitud = markerPosition.latitude.toString(),
                                        longitud = markerPosition.longitude.toString(),
                                        abierto = isOpenNow
                                    )
                                )
                                currentBusiness = updated
                                Toast.makeText(context, "Perfil guardado con éxito", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {
                                Toast.makeText(context, "Cambios guardados localmente", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo emergente para gestionar las citas recibidas en tiempo real
    if (showTicketsDialog) {
        AlertDialog(
            onDismissRequest = { showTicketsDialog = false },
            title = {
                Text(
                    "Citas Recibidas (${businessTickets.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (businessTickets.isEmpty()) {
                        Text(
                            "Aún no has recibido solicitudes de citas de clientes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        businessTickets.forEach { t ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(t.codigo_ticket, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(t.fecha_cita, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Cliente: ${t.usuario_correo}", style = MaterialTheme.typography.bodySmall)
                                    Text("Servicio: ${t.servicio}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text("Nota: ${t.descripcion}", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val statusLabels = listOf("Solicitud Enviada", "Cita Confirmada", "En Reparación", "Listo para Entrega", "Completado")
                                    val statusText = if (t.estado_index in statusLabels.indices) statusLabels[t.estado_index] else "Cancelado"

                                    Text("Estado Actual: $statusText", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                                    if (t.estado_index in 0..3) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        val nextStep = t.estado_index + 1
                                                        RetrofitClient.instance.updateTicketStatus(
                                                            TicketStatusUpdateRequest(ticket_id = t.id, nuevo_estado_index = nextStep)
                                                        )
                                                        businessTickets = RetrofitClient.instance.getBusinessTickets(currentBusiness.id)
                                                        Toast.makeText(context, "Progreso de cita avanzado", Toast.LENGTH_SHORT).show()
                                                    } catch (_: Exception) {}
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Avanzar Progreso")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTicketsDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentBusiness.estado == "APROBADO") "Mi Negocio" else "Estado de Solicitud") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    if (currentBusiness.estado == "APROBADO") {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Perfil")
                        }
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            if (currentBusiness.estado == "PENDIENTE") {
                // Estado en Evaluación
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Solicitud en Proceso de Evaluación",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Plazo de revisión: 24h a 48h hábiles", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }

                            Text(
                                text = "Estamos revisando la información de '${currentBusiness.nombre}'. En cuanto nuestro equipo apruebe tu solicitud, tu negocio aparecerá públicamente y podrás empezar a recibir citas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

            } else {
                // 1. Cabecera Principal con Avatar y Estado de Apertura Interactivo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = "Logo Negocio",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = businessName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Switch de Disponibilidad Inmediata
                    FilterChip(
                        selected = isOpenNow,
                        onClick = {
                            isOpenNow = !isOpenNow
                            coroutineScope.launch {
                                try {
                                    RetrofitClient.instance.updateBusiness(
                                        BusinessUpdateRequest(business_id = currentBusiness.id, abierto = isOpenNow)
                                    )
                                } catch (_: Exception) {}
                            }
                        },
                        label = { Text(if (isOpenNow) "Abierto" else "Cerrado") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOpenNow) Color(0xFF2E7D32) else Color.Gray)
                            )
                        }
                    )
                }

                // Métricas Rápidas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("4.8", fontWeight = FontWeight.Bold)
                            }
                            Text("Calificación", style = MaterialTheme.typography.labelSmall)
                        }

                        VerticalDivider(modifier = Modifier.height(32.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("+150", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Completados", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // 2. Tarjeta con Datos de Contacto y Horarios
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(phone, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(address, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(schedule, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // 3. Mini Mapa Interactivo con Solicitud de Permiso y Confirmación
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ubicación en el mapa",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (isLocationConfirmed) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fijada", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mi Ubicación", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(mapType = MapType.NORMAL),
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = !isLocationConfirmed,
                                scrollGesturesEnabled = !isLocationConfirmed,
                                zoomGesturesEnabled = !isLocationConfirmed,
                                rotationGesturesEnabled = !isLocationConfirmed,
                                myLocationButtonEnabled = false,
                                compassEnabled = true
                            ),
                            onMapClick = { newLatLng ->
                                if (!isLocationConfirmed) {
                                    markerPosition = newLatLng
                                }
                            }
                        ) {
                            Marker(
                                state = rememberMarkerState(position = markerPosition),
                                title = businessName,
                                snippet = if (isLocationConfirmed) "Ubicación Confirmada" else "Toca el mapa para mover",
                                draggable = !isLocationConfirmed
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coords: ${String.format(java.util.Locale.US, "%.4f", markerPosition.latitude)}, ${String.format(java.util.Locale.US, "%.4f", markerPosition.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        if (isLocationConfirmed) {
                            TextButton(onClick = { isLocationConfirmed = false }) {
                                Icon(Icons.Default.EditLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reubicar Pin", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Botón de Confirmación de Ubicación
                    if (!isLocationConfirmed) {
                        Button(
                            onClick = {
                                isLocationConfirmed = true
                                coroutineScope.launch {
                                    try {
                                        RetrofitClient.instance.updateBusiness(
                                            BusinessUpdateRequest(
                                                business_id = currentBusiness.id,
                                                latitud = markerPosition.latitude.toString(),
                                                longitud = markerPosition.longitude.toString()
                                            )
                                        )
                                        Toast.makeText(context, "¡Ubicación del negocio confirmada y fijada!", Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirmar Ubicación del Negocio", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 4. Panel de Administración de Citas
                Text(
                    text = "Panel de Administración",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    businessTickets = RetrofitClient.instance.getBusinessTickets(currentBusiness.id)
                                } catch (_: Exception) {
                                    businessTickets = emptyList()
                                } finally {
                                    showTicketsDialog = true
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Citas", fontWeight = FontWeight.Bold)
                                Text("Recibidas", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Gestión de Servicios (Próximamente)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mis Servicios", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // BOTÓN MÁGICO DE MODO DEMO
            OutlinedButton(
                onClick = {
                    isToggling = true
                    coroutineScope.launch {
                        try {
                            val toggled = RetrofitClient.instance.toggleBusinessStatus(currentBusiness.id)
                            currentBusiness = toggled
                            onStatusToggled(toggled)
                            Toast.makeText(context, "Estado de negocio actualizado a: ${toggled.estado}", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            val newStatus = if (currentBusiness.estado == "PENDIENTE") "APROBADO" else "PENDIENTE"
                            val toggled = currentBusiness.copy(estado = newStatus)
                            currentBusiness = toggled
                            onStatusToggled(toggled)
                            Toast.makeText(context, "Estado alternado a: $newStatus", Toast.LENGTH_SHORT).show()
                        } finally {
                            isToggling = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isToggling
            ) {
                Icon(Icons.Default.PublishedWithChanges, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simular Aprobación/Evaluación (Modo Demo)")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBusinessProfileScreen() {
    BusinessProfileScreen(
        business = BusinessResponse(
            id = 1,
            usuario_id = 1,
            nombre = "Cerrajería El Chispazo",
            categoria = "Cerrajería",
            descripcion = "Servicio de cerrajería residencial y automotriz",
            telefono = "9613000004",
            direccion = "Av. 5 de Mayo #12, Tuxtla Gutiérrez",
            ciudad = "Tuxtla Gutiérrez",
            horario = "Servicio 24 Horas",
            estado = "APROBADO",
            creado_en = ""
        )
    )
}