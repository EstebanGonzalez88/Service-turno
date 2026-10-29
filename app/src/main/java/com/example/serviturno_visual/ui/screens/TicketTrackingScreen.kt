package com.example.serviturno_visual.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.serviturno_visual.network.RetrofitClient
import com.example.serviturno_visual.network.TicketResponse
import com.example.serviturno_visual.network.TicketStatusUpdateRequest
import kotlinx.coroutines.launch

enum class ServiceStatus(val title: String, val subtitle: String) {
    PENDIENTE("Solicitud Enviada", "Esperando confirmación del taller"),
    CONFIRMADA("Cita Confirmada", "Horario apartado en la agenda"),
    EN_PROCESO("En Reparación", "El técnico está trabajando en el equipo"),
    LISTO("Listo para Entrega", "Ya puedes pasar a recogerlo"),
    COMPLETADO("Completado", "Servicio concluido")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketTrackingScreen(
    ticket: TicketResponse? = null,
    onBackClick: () -> Unit = {},
    onCancelTicket: () -> Unit = {}
) {
    val statuses = ServiceStatus.entries
    var showCancelDialog by remember { mutableStateOf(false) }

    val currentStep = ticket?.estado_index ?: 0
    val ticketCode = ticket?.codigo_ticket ?: "Sin Ticket"
    val businessName = ticket?.negocio_nombre ?: "Negocio"
    val serviceName = ticket?.servicio ?: "Servicio Solicitado"
    val dateText = ticket?.fecha_cita ?: "Fecha por confirmar"

    val coroutineScope = rememberCoroutineScope()

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar Solicitud") },
            text = { Text("¿Estás seguro de cancelar este ticket ($ticketCode)? Esta acción liberará tu turno.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        coroutineScope.launch {
                            try {
                                if (ticket != null) {
                                    RetrofitClient.instance.updateTicketStatus(
                                        TicketStatusUpdateRequest(
                                            ticket_id = ticket.id,
                                            nuevo_estado_index = -1
                                        )
                                    )
                                }
                            } catch (_: Exception) {}
                            onCancelTicket()
                        }
                    }
                ) {
                    Text("Sí, cancelar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Button(onClick = { showCancelDialog = false }) {
                    Text("Mantener cita")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket $ticketCode") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Acción para marcar o llamar */ }) {
                        Icon(Icons.Default.Call, contentDescription = "Llamar al local")
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
                .verticalScroll(rememberScrollState())
        ) {
            // Ficha del Negocio
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(businessName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Servicio: $serviceName", style = MaterialTheme.typography.bodyMedium)
                    Text("Turno agendado: $dateText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Línea de Progreso", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (currentStep == -1) {
                // Estado CANCELADO
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "⚠️ Este ticket ha sido cancelado.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // Línea de tiempo con conector vertical continuo
                Column {
                    statuses.forEachIndexed { index, status ->
                        val isCompleted = index <= currentStep
                        val isCurrent = index == currentStep
                        val isLast = index == statuses.size - 1

                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Columna del ícono y la línea vertical
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCompleted) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Línea vertical que conecta los círculos
                                if (!isLast) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(38.dp)
                                            .background(
                                                if (index < currentStep) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Textos del estado
                            Column(modifier = Modifier.padding(top = 2.dp)) {
                                Text(
                                    text = status.title,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = status.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (!isLast) Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de cancelar cita (solo disponible si no está completado ni cancelado)
            if (currentStep in 0 until (statuses.size - 1)) {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancelar esta Cita")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTicketTrackingScreenInteractive() {
    TicketTrackingScreen()
}