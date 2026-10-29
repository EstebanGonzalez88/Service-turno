package com.example.serviturno_visual.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.serviturno_visual.network.ProfileUpdateRequest
import com.example.serviturno_visual.network.RetrofitClient
import kotlinx.coroutines.launch

data class UserProfileData(
    val nombre: String = "",
    val apellidos: String = "",
    val telefono: String = "",
    val codigoPostal: String = "",
    val direccion: String = "",
    val ciudad: String = "",
    val fotoPerfil: String = "",
    val contactoEmergenciaNombre: String = "",
    val contactoEmergenciaTelefono: String = "",
    val notifTickets: Boolean = true,
    val notifRecordatorios: Boolean = true,
    val notifPromociones: Boolean = false
) {
    val isComplete: Boolean
        get() = nombre.isNotBlank() && apellidos.isNotBlank() && telefono.isNotBlank() && codigoPostal.isNotBlank() && direccion.isNotBlank() && ciudad.isNotBlank()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userEmail: String = "",
    profileData: UserProfileData = UserProfileData(),
    isFirstSetup: Boolean = false,
    onSaveProfile: (UserProfileData) -> Unit = {},
    onSkipClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(isFirstSetup || !profileData.isComplete) }
    var isLoading by remember { mutableStateOf(false) }

    // Diálogos emergentes
    var showQrDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // Campos del formulario
    var nombre by remember(profileData) { mutableStateOf(profileData.nombre) }
    var apellidos by remember(profileData) { mutableStateOf(profileData.apellidos) }
    var telefono by remember(profileData) { mutableStateOf(profileData.telefono) }
    var codigoPostal by remember(profileData) { mutableStateOf(profileData.codigoPostal) }
    var direccion by remember(profileData) { mutableStateOf(profileData.direccion) }
    var ciudad by remember(profileData) { mutableStateOf(profileData.ciudad) }
    var fotoUri by remember(profileData) { mutableStateOf<Uri?>(if (profileData.fotoPerfil.isNotBlank()) Uri.parse(profileData.fotoPerfil) else null) }

    // Contacto secundario / emergencia
    var contactoNombre by remember(profileData) { mutableStateOf(profileData.contactoEmergenciaNombre) }
    var contactoTelefono by remember(profileData) { mutableStateOf(profileData.contactoEmergenciaTelefono) }

    // Notificaciones
    var notifTickets by remember(profileData) { mutableStateOf(profileData.notifTickets) }
    var notifRecordatorios by remember(profileData) { mutableStateOf(profileData.notifRecordatorios) }
    var notifPromociones by remember(profileData) { mutableStateOf(profileData.notifPromociones) }

    val coroutineScope = rememberCoroutineScope()

    // Selector de foto de perfil desde la galería
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            fotoUri = uri
        }
    }

    // Diálogo del Código QR ("Mi ID ServiTurno")
    if (showQrDialog) {
        QRCodeDialog(
            userEmail = userEmail.ifBlank { "usuario@serviturno.com" },
            onDismiss = { showQrDialog = false }
        )
    }

    // Diálogo de Cambio de Contraseña
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isFirstSetup) "Completar Perfil" else "Mi Información Personal") },
                navigationIcon = {
                    if (!isFirstSetup) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                        }
                    }
                },
                actions = {
                    if (isFirstSetup) {
                        TextButton(onClick = onSkipClick) {
                            Text("Omitir", fontWeight = FontWeight.Bold)
                        }
                    } else if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. FOTO DE PERFIL CON BADGE DE EDICIÓN
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            ) {
                if (fotoUri != null) {
                    AsyncImage(
                        model = fotoUri,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Foto por defecto",
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Cambiar foto",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }

            Text(
                text = "Toca la imagen para seleccionar tu foto de perfil",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )

            // Botón rápido para abrir código QR
            OutlinedButton(
                onClick = { showQrDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver mi Código QR (ID ServiTurno)")
            }

            HorizontalDivider()

            if (isEditing) {
                // MODO EDICIÓN / FORMULARIO
                Text(
                    text = if (isFirstSetup) "¡Queremos conocerte mejor!" else "Editar Datos Personales",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                // Datos Principales
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre(s)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it },
                    label = { Text("Apellidos") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Número telefónico") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = codigoPostal,
                    onValueChange = { codigoPostal = it },
                    label = { Text("Código Postal (CP)") },
                    leadingIcon = { Icon(Icons.Default.MarkunreadMailbox, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección (Calle, Número, Colonia)") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = ciudad,
                    onValueChange = { ciudad = it },
                    label = { Text("Ciudad / Municipio") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Sección Contacto Secundario / Emergencia
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Contacto Secundario (Opcional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactoNombre,
                    onValueChange = { contactoNombre = it },
                    label = { Text("Nombre del contacto secundario") },
                    leadingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = contactoTelefono,
                    onValueChange = { contactoTelefono = it },
                    label = { Text("Teléfono de emergencia") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val updated = UserProfileData(
                            nombre = nombre.trim(),
                            apellidos = apellidos.trim(),
                            telefono = telefono.trim(),
                            codigoPostal = codigoPostal.trim(),
                            direccion = direccion.trim(),
                            ciudad = ciudad.trim(),
                            fotoPerfil = fotoUri?.toString() ?: "",
                            contactoEmergenciaNombre = contactoNombre.trim(),
                            contactoEmergenciaTelefono = contactoTelefono.trim(),
                            notifTickets = notifTickets,
                            notifRecordatorios = notifRecordatorios,
                            notifPromociones = notifPromociones
                        )
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                if (userEmail.isNotBlank()) {
                                    RetrofitClient.instance.updateProfile(
                                        ProfileUpdateRequest(
                                            correo = userEmail,
                                            nombre = updated.nombre,
                                            apellidos = updated.apellidos,
                                            telefono = updated.telefono,
                                            codigo_postal = updated.codigoPostal,
                                            direccion = updated.direccion,
                                            ciudad = updated.ciudad,
                                            foto_perfil = updated.fotoPerfil,
                                            contacto_emergencia_nombre = updated.contactoEmergenciaNombre,
                                            contacto_emergencia_telefono = updated.contactoEmergenciaTelefono,
                                            notif_tickets = updated.notifTickets,
                                            notif_recordatorios = updated.notifRecordatorios,
                                            notif_promociones = updated.notifPromociones
                                        )
                                    )
                                }
                            } catch (_: Exception) {
                            } finally {
                                isLoading = false
                                onSaveProfile(updated)
                                isEditing = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (isFirstSetup) "Hecho" else "Guardar Cambios", fontWeight = FontWeight.Bold)
                    }
                }

            } else {
                // MODO VISTA / LECTURA
                Text(
                    text = "Información Personal Registrada",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProfileInfoRow(
                            icon = Icons.Default.Person,
                            label = "Nombre Completo",
                            value = "${profileData.nombre} ${profileData.apellidos}".trim().ifBlank { "No especificado" }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        ProfileInfoRow(
                            icon = Icons.Default.Phone,
                            label = "Teléfono Principal",
                            value = profileData.telefono.ifBlank { "No especificado" }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        ProfileInfoRow(
                            icon = Icons.Default.MarkunreadMailbox,
                            label = "Código Postal",
                            value = profileData.codigoPostal.ifBlank { "No especificado" }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        ProfileInfoRow(
                            icon = Icons.Default.Home,
                            label = "Dirección Principal",
                            value = profileData.direccion.ifBlank { "No especificado" }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        ProfileInfoRow(
                            icon = Icons.Default.LocationCity,
                            label = "Ciudad / Municipio",
                            value = profileData.ciudad.ifBlank { "No especificado" }
                        )

                        if (profileData.contactoEmergenciaNombre.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            ProfileInfoRow(
                                icon = Icons.Default.ContactPhone,
                                label = "Contacto Secundario",
                                value = "${profileData.contactoEmergenciaNombre} (${profileData.contactoEmergenciaTelefono})"
                            )
                        }
                    }
                }

                // Sección Preferencias de Notificaciones
                Text(
                    text = "Preferencias de Notificaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Notificaciones de Tickets", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = notifTickets, onCheckedChange = { notifTickets = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recordatorios de Citas", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = notifRecordatorios, onCheckedChange = { notifRecordatorios = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Promociones y Ofertas", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = notifPromociones, onCheckedChange = { notifPromociones = it })
                        }
                    }
                }

                // Sección Seguridad
                Text(
                    text = "Seguridad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { showPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cambiar Contraseña")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar Información", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QRCodeDialog(
    userEmail: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Mi ID ServiTurno",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Muestra este código QR en el negocio físico para que escaneen tu cita al instante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray, shape = RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridSize = 11
                        val cellSize = size.width / gridSize
                        val seed = userEmail.hashCode()

                        for (row in 0 until gridSize) {
                            for (col in 0 until gridSize) {
                                val isTopLeft = row < 3 && col < 3
                                val isTopRight = row < 3 && col >= gridSize - 3
                                val isBottomLeft = row >= gridSize - 3 && col < 3

                                if (isTopLeft || isTopRight || isBottomLeft) {
                                    val isBorder = row == 0 || row == 2 || row == gridSize - 3 || row == gridSize - 1 ||
                                            col == 0 || col == 2 || col == gridSize - 3 || col == gridSize - 1
                                    val isCenter = (row == 1 && col == 1) || (row == 1 && col == gridSize - 2) || (row == gridSize - 2 && col == 1)

                                    if (isBorder || isCenter) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                                            size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                        )
                                    }
                                } else {
                                    val cellHash = (seed + row * 31 + col * 17) % 2 == 0
                                    if (cellHash) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                                            size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPass,
                    onValueChange = { oldPass = it },
                    label = { Text("Contraseña Actual") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("Nueva Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (message != null) {
                    Text(text = message ?: "", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldPass.isNotBlank() && newPass.isNotBlank()) {
                        message = "Contraseña actualizada exitosamente"
                    }
                }
            ) {
                Text("Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUserProfileScreen() {
    UserProfileScreen()
}