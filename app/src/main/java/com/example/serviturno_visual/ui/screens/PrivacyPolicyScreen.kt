package com.example.serviturno_visual.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Políticas y Privacidad") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Aviso de Privacidad",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "En ServiTurno, la privacidad y protección de tus datos personales es una prioridad. " +
                        "Esta política describe cómo recopilamos, utilizamos y protegemos la información que nos proporcionas al utilizar nuestra aplicación.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "1. Uso de la Información",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tus datos personales (nombre, teléfono, correo electrónico y dirección) se utilizan exclusivamente para gestionar la reserva de turnos, confirmaciones de citas y comunicación directa con los establecimientos de servicio que elijas.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "2. Protección de Datos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Implementamos medidas de seguridad técnicas y organizativas para evitar el acceso no autorizado, alteración, divulgación o destrucción de tu información personal.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "3. Compartir con Terceros",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "No vendemos ni alquilamos tus datos personales a terceros. Únicamente compartimos la información estrictamente necesaria con el negocio donde agendaste tu ticket o cita.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPrivacyPolicyScreen() {
    PrivacyPolicyScreen()
}