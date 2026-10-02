package com.example.serviturno_visual

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.serviturno_visual.ui.screens.CreateTicketScreen
import com.example.serviturno_visual.ui.screens.HomeScreen
import com.example.serviturno_visual.ui.screens.TicketTrackingScreen
import com.example.serviturno_visual.ui.theme.ServiTurnoVisualTheme

import com.example.serviturno_visual.ui.screens.BusinessDetailScreen

import com.example.serviturno_visual.ui.screens.LoginScreen
import com.example.serviturno_visual.ui.screens.TwoFactorScreen

enum class AppScreen { Login, TwoFactor, Home, BusinessDetail, CreateTicket, Tracking }

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
                    // Estado simple para navegar entre las pantallas
                    var currentScreen by remember { mutableStateOf(AppScreen.Login) }

                    when (currentScreen) {
                        AppScreen.Login -> {
                            LoginScreen(
                                onLoginSuccess = {
                                    // Al validar correo/contraseña, vamos al 2FA
                                    currentScreen = AppScreen.TwoFactor
                                }
                            )
                        }
                        AppScreen.TwoFactor -> {
                            TwoFactorScreen(
                                onVerifySuccess = {
                                    // Al validar el código, entramos a la app (Home)
                                    currentScreen = AppScreen.Home
                                },
                                onBackClick = {
                                    // Regresar al Login
                                    currentScreen = AppScreen.Login
                                }
                            )
                        }
                        AppScreen.Home -> {
                            HomeScreen(
                                onBusinessClick = {
                                    // Al hacer clic en un negocio, vamos a la pantalla de detalle del negocio
                                    currentScreen = AppScreen.BusinessDetail
                                }
                            )
                        }
                        AppScreen.BusinessDetail -> {
                            BusinessDetailScreen(
                                onBackClick = {
                                    // Regresar al Home
                                    currentScreen = AppScreen.Home
                                },
                                onRequestAppointmentClick = {
                                    // Al solicitar cita, vamos a crear el ticket
                                    currentScreen = AppScreen.CreateTicket
                                }
                            )
                        }
                        AppScreen.CreateTicket -> {
                            CreateTicketScreen(
                                onBackClick = {
                                    // Regresar al detalle del negocio
                                    currentScreen = AppScreen.BusinessDetail
                                },
                                onTicketCreatedSuccess = {
                                    // Al enviar el ticket exitosamente, vamos a la pantalla de seguimiento
                                    currentScreen = AppScreen.Tracking
                                }
                            )
                        }
                        AppScreen.Tracking -> {
                            TicketTrackingScreen(
                                currentStep = 2,
                                onBackClick = {
                                    // Regresar a la pantalla anterior (Home)
                                    currentScreen = AppScreen.Home
                                },
                                onCancelTicket = {
                                    // Al cancelar el ticket, regresamos al Home
                                    currentScreen = AppScreen.Home
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}