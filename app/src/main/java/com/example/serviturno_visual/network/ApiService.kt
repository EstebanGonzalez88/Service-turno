package com.example.serviturno_visual.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// Data classes para las respuestas
data class LoginChallengeResponse(
    val mensaje: String,
    val correo: String
)

data class VerifyRequest(
    val correo: String,
    val codigo: String
)

data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String
)

data class RegisterRequest(
    val nombre: String = "Usuario",
    val correo: String,
    val password: String
)

data class RegisterResponse(
    val mensaje: String,
    val correo: String
)

data class ProfileUpdateRequest(
    val correo: String,
    val nombre: String? = null,
    val apellidos: String? = null,
    val telefono: String? = null,
    val codigo_postal: String? = null,
    val direccion: String? = null,
    val ciudad: String? = null,
    val foto_perfil: String? = null,
    val contacto_emergencia_nombre: String? = null,
    val contacto_emergencia_telefono: String? = null,
    val notif_tickets: Boolean? = null,
    val notif_recordatorios: Boolean? = null,
    val notif_promociones: Boolean? = null
)

data class UserProfileResponse(
    val id: Int,
    val correo: String,
    val nombre: String? = null,
    val apellidos: String? = null,
    val telefono: String? = null,
    val codigo_postal: String? = null,
    val direccion: String? = null,
    val ciudad: String? = null,
    val foto_perfil: String? = null,
    val contacto_emergencia_nombre: String? = null,
    val contacto_emergencia_telefono: String? = null,
    val notif_tickets: Boolean = true,
    val notif_recordatorios: Boolean = true,
    val notif_promociones: Boolean = false,
    val perfil_completo: Boolean = false
)

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)

data class ChangePasswordResponse(
    val mensaje: String
)

data class BusinessCreateRequest(
    val usuario_correo: String,
    val nombre: String,
    val categoria: String,
    val descripcion: String,
    val telefono: String,
    val direccion: String,
    val ciudad: String,
    val horario: String,
    val foto_portada: String? = null
)

data class BusinessResponse(
    val id: Int,
    val usuario_id: Int,
    val nombre: String,
    val categoria: String,
    val descripcion: String,
    val telefono: String,
    val direccion: String,
    val ciudad: String,
    val horario: String,
    val foto_portada: String? = null,
    val estado: String,
    val latitud: String = "16.7533",
    val longitud: String = "-93.1165",
    val abierto: Boolean = true,
    val creado_en: String
)

data class BusinessUpdateRequest(
    val business_id: Int,
    val nombre: String? = null,
    val categoria: String? = null,
    val descripcion: String? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    val horario: String? = null,
    val latitud: String? = null,
    val longitud: String? = null,
    val abierto: Boolean? = null
)

data class TicketCreateRequest(
    val usuario_correo: String,
    val negocio_id: Int = 1,
    val negocio_nombre: String = "Taller Móvil Fix",
    val servicio: String,
    val fecha_cita: String,
    val descripcion: String,
    val foto_referencia: String? = null
)

data class TicketStatusUpdateRequest(
    val ticket_id: Int,
    val nuevo_estado_index: Int
)

data class TicketResponse(
    val id: Int,
    val codigo_ticket: String,
    val usuario_id: Int,
    val usuario_correo: String,
    val negocio_id: Int,
    val negocio_nombre: String,
    val servicio: String,
    val fecha_cita: String,
    val descripcion: String,
    val foto_referencia: String? = null,
    val estado_index: Int,
    val creado_en: String
)

data class ReviewCreateRequest(
    val negocio_id: Int,
    val usuario_correo: String,
    val calificacion: Int,
    val comentario: String
)

data class ReviewResponse(
    val id: Int,
    val negocio_id: Int,
    val autor_nombre: String,
    val calificacion: Int,
    val comentario: String,
    val fecha_texto: String,
    val creado_en: String
)

interface ApiService {
    
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResponse
    
    @FormUrlEncoded
    @POST("api/auth/login")
    suspend fun loginChallenge(
        @Field("username") correo: String,
        @Field("password") contrasena: String
    ): LoginChallengeResponse

    @POST("api/auth/login-verify")
    suspend fun loginVerify(
        @Body request: VerifyRequest
    ): TokenResponse

    @PUT("api/auth/profile")
    suspend fun updateProfile(
        @Body request: ProfileUpdateRequest
    ): UserProfileResponse

    @GET("api/auth/profile/{correo}")
    suspend fun getProfile(
        @Path("correo") correo: String
    ): UserProfileResponse

    @POST("api/auth/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): ChangePasswordResponse

    @POST("api/business/register")
    suspend fun registerBusiness(
        @Body request: BusinessCreateRequest
    ): BusinessResponse

    @GET("api/business/my-business/{correo}")
    suspend fun getMyBusiness(
        @Path("correo") correo: String
    ): BusinessResponse

    @GET("api/business/all")
    suspend fun getAllBusinesses(): List<BusinessResponse>

    @POST("api/business/toggle-status/{business_id}")
    suspend fun toggleBusinessStatus(
        @Path("business_id") businessId: Int
    ): BusinessResponse

    @PUT("api/business/update")
    suspend fun updateBusiness(
        @Body request: BusinessUpdateRequest
    ): BusinessResponse

    @POST("api/tickets/create")
    suspend fun createTicket(
        @Body request: TicketCreateRequest
    ): TicketResponse

    @GET("api/tickets/my-tickets/{correo}")
    suspend fun getMyTickets(
        @Path("correo") correo: String
    ): List<TicketResponse>

    @GET("api/tickets/business-tickets/{business_id}")
    suspend fun getBusinessTickets(
        @Path("business_id") businessId: Int
    ): List<TicketResponse>

    @PUT("api/tickets/update-status")
    suspend fun updateTicketStatus(
        @Body request: TicketStatusUpdateRequest
    ): TicketResponse

    @POST("api/reviews/create")
    suspend fun createReview(
        @Body request: ReviewCreateRequest
    ): ReviewResponse

    @GET("api/reviews/business/{business_id}")
    suspend fun getBusinessReviews(
        @Path("business_id") businessId: Int
    ): List<ReviewResponse>
}

// Cliente de Retrofit con Sistema Indestructible de Doble Respaldo (Fallback de Red)
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            try {
                // Intento 1: IP nativa del emulador (10.0.2.2)
                chain.proceed(request)
            } catch (_: Exception) {
                // Intento 2 (Fallback): Si 10.0.2.2 falla (bloqueado por Firewall de red pública),
                // cambia automáticamente la URL a 127.0.0.1 (túnel ADB) y reintenta de inmediato sin marcar error
                val fallbackUrl = request.url.newBuilder()
                    .host("127.0.0.1")
                    .build()
                val fallbackRequest = request.newBuilder().url(fallbackUrl).build()
                chain.proceed(fallbackRequest)
            }
        }
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}