package com.example.serviturno_visual

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.serviturno_visual.network.BusinessResponse
import com.example.serviturno_visual.network.RetrofitClient
import com.example.serviturno_visual.network.TicketResponse
import com.example.serviturno_visual.ui.screens.*
import com.example.serviturno_visual.ui.theme.ServiTurnoVisualTheme
import com.example.serviturno_visual.utils.UserSessionManager
import kotlinx.coroutines.launch

enum class AppScreen { Welcome, Login, Register, TwoFactor, UserProfile, MyAccount, PrivacyPolicy, Home, BusinessDetail, CreateTicket, Tracking, BusinessOnboarding, BusinessRegister, BusinessStatus }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ServiTurnoVisualTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.Welcome) }
                    var previousAuthScreen by remember { mutableStateOf(AppScreen.Welcome) }
                    var userEmail by remember { mutableStateOf("") }
                    var userProfile by remember { mutableStateOf(UserProfileData()) }
                    var userBusiness by remember { mutableStateOf<BusinessResponse?>(null) }
                    var selectedBusinessForDetail by remember { mutableStateOf<BusinessItem?>(null) }
                    var currentTicket by remember { mutableStateOf<TicketResponse?>(null) }
                    var showIncompleteNotice by remember { mutableStateOf(false) }

                    val coroutineScope = rememberCoroutineScope()

                    // Restaurar sesión guardada automáticamente al abrir la app
                    LaunchedEffect(Unit) {
                        val savedEmail = UserSessionManager.getSavedEmail(this@MainActivity)
                        if (!savedEmail.isNullOrBlank()) {
                            userEmail = savedEmail
                            try {
                                val profileResp = RetrofitClient.instance.getProfile(savedEmail)
                                userProfile = UserProfileData(
                                    nombre = profileResp.nombre ?: "",
                                    apellidos = profileResp.apellidos ?: "",
                                    telefono = profileResp.telefono ?: "",
                                    codigoPostal = profileResp.codigo_postal ?: "",
                                    direccion = profileResp.direccion ?: "",
                                    ciudad = profileResp.ciudad ?: "",
                                    fotoPerfil = profileResp.foto_perfil ?: "",
                                    contactoEmergenciaNombre = profileResp.contacto_emergencia_nombre ?: "",
                                    contactoEmergenciaTelefono = profileResp.contacto_emergencia_telefono ?: "",
                                    notifTickets = profileResp.notif_tickets,
                                    notifRecordatorios = profileResp.notif_recordatorios,
                                    notifPromociones = profileResp.notif_promociones
                                )
                            } catch (_: Exception) {}

                            try {
                                val bus = RetrofitClient.instance.getMyBusiness(savedEmail)
                                userBusiness = bus
                            } catch (_: Exception) {}

                            currentScreen = AppScreen.Home
                        }
                    }

                    // Diálogo de advertencia si la información del usuario no está completa
                    if (showIncompleteNotice) {
                        AlertDialog(
                            onDismissRequest = { showIncompleteNotice = false },
                            title = { Text("Información Incompleta") },
                            text = { Text("Es muy recomendable completar tus datos personales (Teléfono, Dirección, CP) para agilizar tus citas con los negocios.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showIncompleteNotice = false
                                        currentScreen = AppScreen.UserProfile
                                    }
                                ) {
                                    Text("Completar Ahora")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showIncompleteNotice = false
                                        currentScreen = AppScreen.Home
                                    }
                                ) {
                                    Text("Más Tarde")
                                }
                            }
                        )
                    }

                    when (currentScreen) {
                        AppScreen.Welcome -> {
                            WelcomeScreen(
                                onLoginClick = { currentScreen = AppScreen.Login },
                                onRegisterClick = { currentScreen = AppScreen.Register }
                            )
                        }
                        AppScreen.Login -> {
                            LoginScreen(
                                onLoginSuccess = { emailIngresado ->
                                    userEmail = emailIngresado.trim()
                                    previousAuthScreen = AppScreen.Login
                                    currentScreen = AppScreen.TwoFactor
                                },
                                onNavigateToRegister = { currentScreen = AppScreen.Register }
                            )
                        }
                        AppScreen.Register -> {
                            RegisterScreen(
                                onRegisterSuccess = { registeredEmail ->
                                    userEmail = registeredEmail.trim()
                                    previousAuthScreen = AppScreen.Register
                                    currentScreen = AppScreen.TwoFactor
                                },
                                onNavigateToLogin = { currentScreen = AppScreen.Login }
                            )
                        }
                        AppScreen.TwoFactor -> {
                            TwoFactorScreen(
                                userEmail = userEmail,
                                onVerifySuccess = {
                                    // Guardar sesión persistente en disco (SharedPreferences)
                                    UserSessionManager.saveSession(this@MainActivity, userEmail)

                                    // Consultar perfil y datos de negocio en SQLite
                                    coroutineScope.launch {
                                        try {
                                            val profileResp = RetrofitClient.instance.getProfile(userEmail)
                                            userProfile = UserProfileData(
                                                nombre = profileResp.nombre ?: "",
                                                apellidos = profileResp.apellidos ?: "",
                                                telefono = profileResp.telefono ?: "",
                                                codigoPostal = profileResp.codigo_postal ?: "",
                                                direccion = profileResp.direccion ?: "",
                                                ciudad = profileResp.ciudad ?: "",
                                                fotoPerfil = profileResp.foto_perfil ?: "",
                                                contactoEmergenciaNombre = profileResp.contacto_emergencia_nombre ?: "",
                                                contactoEmergenciaTelefono = profileResp.contacto_emergencia_telefono ?: "",
                                                notifTickets = profileResp.notif_tickets,
                                                notifRecordatorios = profileResp.notif_recordatorios,
                                                notifPromociones = profileResp.notif_promociones
                                            )

                                            try {
                                                val bus = RetrofitClient.instance.getMyBusiness(userEmail)
                                                userBusiness = bus
                                            } catch (_: Exception) {}

                                            if (!profileResp.perfil_completo) {
                                                showIncompleteNotice = true
                                            } else {
                                                currentScreen = AppScreen.Home
                                            }
                                        } catch (_: Exception) {
                                            currentScreen = AppScreen.Home
                                        }
                                    }
                                },
                                onBackClick = { currentScreen = previousAuthScreen }
                            )
                        }
                        AppScreen.UserProfile -> {
                            UserProfileScreen(
                                userEmail = userEmail,
                                profileData = userProfile,
                                isFirstSetup = true,
                                onSaveProfile = { updated ->
                                    userProfile = updated
                                    currentScreen = AppScreen.Home
                                },
                                onSkipClick = { currentScreen = AppScreen.Home }
                            )
                        }
                        AppScreen.MyAccount -> {
                            UserProfileScreen(
                                userEmail = userEmail,
                                profileData = userProfile,
                                isFirstSetup = false,
                                onSaveProfile = { updated ->
                                    userProfile = updated
                                },
                                onBackClick = { currentScreen = AppScreen.Home }
                            )
                        }
                        AppScreen.PrivacyPolicy -> {
                            PrivacyPolicyScreen(
                                onBackClick = { currentScreen = AppScreen.Home }
                            )
                        }
                        AppScreen.Home -> {
                            HomeScreen(
                                userEmail = userEmail.ifBlank { "usuario@serviturno.com" },
                                userProfile = userProfile,
                                userBusiness = userBusiness,
                                onBusinessClick = { selectedBusiness ->
                                    selectedBusinessForDetail = selectedBusiness
                                    currentScreen = AppScreen.BusinessDetail
                                },
                                onMyAccountClick = { currentScreen = AppScreen.MyAccount },
                                onMyBusinessClick = {
                                    coroutineScope.launch {
                                        try {
                                            val bus = RetrofitClient.instance.getMyBusiness(userEmail.ifBlank { "jose.perez73@unach.mx" })
                                            userBusiness = bus
                                            currentScreen = AppScreen.BusinessStatus
                                        } catch (_: Exception) {
                                            if (userBusiness != null) {
                                                currentScreen = AppScreen.BusinessStatus
                                            } else {
                                                currentScreen = AppScreen.BusinessOnboarding
                                            }
                                        }
                                    }
                                },
                                onSettingsClick = {
                                    Toast.makeText(this, "Sección Ajustes (Próximamente)", Toast.LENGTH_SHORT).show()
                                },
                                onPrivacyClick = { currentScreen = AppScreen.PrivacyPolicy },
                                onLogoutClick = {
                                    // Borrar sesión guardada al cerrar sesión
                                    UserSessionManager.clearSession(this@MainActivity)
                                    userEmail = ""
                                    userProfile = UserProfileData()
                                    userBusiness = null
                                    currentTicket = null
                                    selectedBusinessForDetail = null
                                    currentScreen = AppScreen.Welcome
                                }
                            )
                        }
                        AppScreen.BusinessOnboarding -> {
                            BusinessOnboardingScreen(
                                onRegisterBusinessClick = { currentScreen = AppScreen.BusinessRegister },
                                onBackClick = { currentScreen = AppScreen.Home }
                            )
                        }
                        AppScreen.BusinessRegister -> {
                            BusinessRegisterScreen(
                                userEmail = userEmail,
                                onRegisterSuccess = {
                                    coroutineScope.launch {
                                        try {
                                            val bus = RetrofitClient.instance.getMyBusiness(userEmail.ifBlank { "jose.perez73@unach.mx" })
                                            userBusiness = bus
                                        } catch (_: Exception) {
                                            Toast.makeText(this@MainActivity, "Error al obtener datos del negocio", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            currentScreen = AppScreen.BusinessStatus
                                        }
                                    }
                                },
                                onBackClick = { currentScreen = AppScreen.BusinessOnboarding }
                            )
                        }
                        AppScreen.BusinessStatus -> {
                            userBusiness?.let { business ->
                                BusinessProfileScreen(
                                    business = business,
                                    onBackClick = { currentScreen = AppScreen.Home },
                                    onStatusToggled = { updated -> userBusiness = updated }
                                )
                            } ?: run {
                                currentScreen = AppScreen.BusinessOnboarding
                            }
                        }
                        AppScreen.BusinessDetail -> {
                            BusinessDetailScreen(
                                userEmail = userEmail,
                                business = selectedBusinessForDetail,
                                onBackClick = { currentScreen = AppScreen.Home },
                                onRequestAppointmentClick = { currentScreen = AppScreen.CreateTicket }
                            )
                        }
                        AppScreen.CreateTicket -> {
                            CreateTicketScreen(
                                userEmail = userEmail,
                                onBackClick = { currentScreen = AppScreen.BusinessDetail },
                                onTicketCreatedSuccess = { ticketResp ->
                                    currentTicket = ticketResp
                                    currentScreen = AppScreen.Tracking
                                }
                            )
                        }
                        AppScreen.Tracking -> {
                            TicketTrackingScreen(
                                ticket = currentTicket,
                                onBackClick = { currentScreen = AppScreen.Home },
                                onCancelTicket = { currentScreen = AppScreen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}