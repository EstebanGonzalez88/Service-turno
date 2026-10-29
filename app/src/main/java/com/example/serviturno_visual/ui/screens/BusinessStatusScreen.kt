package com.example.serviturno_visual.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.serviturno_visual.network.BusinessResponse
import com.example.serviturno_visual.network.RetrofitClient
import com.example.serviturno_visual.network.TicketResponse
import com.example.serviturno_visual.network.TicketStatusUpdateRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessStatusScreen(
    business: BusinessResponse,
    onBackClick: () -> Unit = {},
    onStatusToggled: (BusinessResponse) -> Unit = {}
) {
    var currentBusiness by remember(business) { mutableStateOf(business) }
    var isToggling by remember { mutableStateOf(false) }
    var showTicketsDialog by remember { mutableStateOf(false) }
    var businessTickets by remember { mutableStateOf<List<TicketResponse>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

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
                                                        // Recargar lista de tickets
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
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (currentBusiness.estado == "PENDIENTE") {
                // PASO 5: Muestra mensaje de "Todavía en Aceptación / Evaluación"
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

                // Resumen del negocio enviado
                Text("Resumen de datos enviados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Establecimiento: ${currentBusiness.nombre}", fontWeight = FontWeight.SemiBold)
                        Text("Categoría: ${currentBusiness.categoria}", style = MaterialTheme.typography.bodySmall)
                        Text("Teléfono: ${currentBusiness.telefono}", style = MaterialTheme.typography.bodySmall)
                        Text("Ubicación: ${currentBusiness.direccion}, ${currentBusiness.ciudad}", style = MaterialTheme.typography.bodySmall)
                        Text("Horario: ${currentBusiness.horario}", style = MaterialTheme.typography.bodySmall)
                    }
                }

            } else {
                // PASO 6: ACEPTADO / APROBADO -> Muestra la información del negocio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = currentBusiness.nombre,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentBusiness.categoria,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(" Activo ", modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Bold)
                    }
                }

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
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(currentBusiness.telefono, style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${currentBusiness.direccion}, ${currentBusiness.ciudad}", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(currentBusiness.horario, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Menú de opciones del dueño de negocio
                Text("Panel de Administración de Citas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val tickets = RetrofitClient.instance.getBusinessTickets(currentBusiness.id)
                                    businessTickets = tickets
                                } catch (_: Exception) {
                                    businessTickets = emptyList()
                                } finally {
                                    showTicketsDialog = true
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Citas Recibidas")
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Gestión de Servicios (Próximamente)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Mis Servicios")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // BOTÓN MÁGICO DE MODO DEMO: Permite alternar entre "PENDIENTE" y "APROBADO" en 1 clic para que pruebes ambas vistas
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
                            // Cambio local simulado si no hay red
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
fun PreviewBusinessStatusScreenPending() {
    BusinessStatusScreen(
        business = BusinessResponse(
            id = 1,
            usuario_id = 1,
            nombre = "Taller Móvil Fix",
            categoria = "Reparación y Servicio Técnico",
            descripcion = "Servicio técnico especialista",
            telefono = "9611234567",
            direccion = "Calle Central #45",
            ciudad = "Tuxtla Gutiérrez",
            horario = "Lunes a Sábado: 9am - 7pm",
            estado = "PENDIENTE",
            creado_en = ""
        )
    )
}