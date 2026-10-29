package com.example.serviturno_visual.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.serviturno_visual.network.RetrofitClient
import com.example.serviturno_visual.network.TicketCreateRequest
import com.example.serviturno_visual.network.TicketResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTicketScreen(
    userEmail: String = "",
    onBackClick: () -> Unit = {},
    onTicketCreatedSuccess: (TicketResponse) -> Unit = {}
) {
    val context = LocalContext.current
    var problemDescription by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("Reparación / Mantenimiento") }
    var selectedDateText by remember { mutableStateOf("Mañana - 10:00 AM") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> selectedImageUri = uri }

    // Diálogo emergente antes de enviar
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showConfirmDialog = false },
            title = { Text("Confirmar Solicitud") },
            text = { Text("¿Deseas enviar tu ticket de cita para $selectedDateText? El negocio revisará tu solicitud.") },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val ticketResp = RetrofitClient.instance.createTicket(
                                    TicketCreateRequest(
                                        usuario_correo = userEmail.ifBlank { "jose.perez73@unach.mx" },
                                        servicio = selectedService.trim(),
                                        fecha_cita = selectedDateText.trim(),
                                        descripcion = problemDescription.trim(),
                                        foto_referencia = selectedImageUri?.toString()
                                    )
                                )
                                isLoading = false
                                showConfirmDialog = false
                                onTicketCreatedSuccess(ticketResp)
                            } catch (_: Exception) {
                                isLoading = false
                                showConfirmDialog = false
                                android.widget.Toast.makeText(context, "Error de red al crear el ticket", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Aceptar y Enviar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isLoading) showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitar Cita o Ticket") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar a la pantalla anterior"
                        )
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

            Text("Información del Servicio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Selector / Entrada del servicio requerido
            OutlinedTextField(
                value = selectedService,
                onValueChange = { selectedService = it },
                label = { Text("Servicio requerido") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Simulación de selector de fecha/hora
            OutlinedTextField(
                value = selectedDateText,
                onValueChange = { selectedDateText = it },
                label = { Text("Fecha y hora deseada") },
                trailingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                readOnly = false
            )

            // Campo de descripción detallada
            OutlinedTextField(
                value = problemDescription,
                onValueChange = { problemDescription = it },
                label = { Text("Describe el problema o especificaciones") },
                placeholder = { Text("Ej. Mi equipo no enciende tras una caída...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                maxLines = 4
            )

            Text("Fotografía de referencia (opcional)", style = MaterialTheme.typography.bodyMedium)

            // Contenedor de la foto con botón para quitarla si ya se seleccionó
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar foto", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adjuntar Foto")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones de acción inferior
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = problemDescription.isNotBlank()
                ) {
                    Text("Continuar", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCreateTicketScreenInteractive() {
    CreateTicketScreen()
}