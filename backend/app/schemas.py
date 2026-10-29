from pydantic import BaseModel, EmailStr
from typing import List, Optional
from datetime import datetime

class UserBase(BaseModel):
    nombre: str
    correo: EmailStr

class UserCreate(UserBase):
    password: str

class UserResponse(UserBase):
    id: int
    activo: bool

    class Config:
        from_attributes = True

class Token(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str

class LoginRequest(BaseModel):
    correo: EmailStr
    password: str

class VerifyRequest(BaseModel):
    correo: EmailStr
    codigo: str

class ProfileUpdate(BaseModel):
    correo: EmailStr
    nombre: Optional[str] = None
    apellidos: Optional[str] = None
    telefono: Optional[str] = None
    codigo_postal: Optional[str] = None
    direccion: Optional[str] = None
    ciudad: Optional[str] = None
    foto_perfil: Optional[str] = None
    contacto_emergencia_nombre: Optional[str] = None
    contacto_emergencia_telefono: Optional[str] = None
    notif_tickets: Optional[bool] = None
    notif_recordatorios: Optional[bool] = None
    notif_promociones: Optional[bool] = None

class UserProfileResponse(BaseModel):
    id: int
    correo: str
    nombre: Optional[str] = None
    apellidos: Optional[str] = None
    telefono: Optional[str] = None
    codigo_postal: Optional[str] = None
    direccion: Optional[str] = None
    ciudad: Optional[str] = None
    foto_perfil: Optional[str] = None
    contacto_emergencia_nombre: Optional[str] = None
    contacto_emergencia_telefono: Optional[str] = None
    notif_tickets: bool = True
    notif_recordatorios: bool = True
    notif_promociones: bool = False
    perfil_completo: bool = False

    class Config:
        from_attributes = True

class RoleCreate(BaseModel):
    nombre: str

class RoleResponse(BaseModel):
    id: int
    nombre: str

    class Config:
        from_attributes = True

class PermissionResponse(BaseModel):
    id: int
    nombre: str

    class Config:
        from_attributes = True

class AssignRoleRequest(BaseModel):
    role_id: int

class ChangePasswordRequest(BaseModel):
    old_password: str
    new_password: str

class AuditLogResponse(BaseModel):
    id: int
    usuario_id: Optional[int]
    fecha_hora: datetime
    direccion_ip: str
    accion: str

    class Config:
        from_attributes = True

class ContentCreate(BaseModel):
    texto: str

class ContentResponse(BaseModel):
    id: int
    texto: str
    autor_correo: str
    fecha_creacion: datetime

    class Config:
        from_attributes = True

class BusinessCreateRequest(BaseModel):
    usuario_correo: EmailStr
    nombre: str
    categoria: str
    descripcion: str
    telefono: str
    direccion: str
    ciudad: str
    horario: str
    foto_portada: Optional[str] = None
    latitud: Optional[str] = "16.7533"
    longitud: Optional[str] = "-93.1165"
    abierto: Optional[bool] = True

class BusinessUpdateRequest(BaseModel):
    business_id: int
    nombre: Optional[str] = None
    categoria: Optional[str] = None
    descripcion: Optional[str] = None
    telefono: Optional[str] = None
    direccion: Optional[str] = None
    horario: Optional[str] = None
    latitud: Optional[str] = None
    longitud: Optional[str] = None
    abierto: Optional[bool] = None

class BusinessResponse(BaseModel):
    id: int
    usuario_id: int
    nombre: str
    categoria: str
    descripcion: str
    telefono: str
    direccion: str
    ciudad: str
    horario: str
    foto_portada: Optional[str] = None
    estado: str
    latitud: Optional[str] = "16.7533"
    longitud: Optional[str] = "-93.1165"
    abierto: bool = True
    creado_en: datetime

    class Config:
        from_attributes = True

class TicketCreateRequest(BaseModel):
    usuario_correo: EmailStr
    negocio_id: Optional[int] = 1
    negocio_nombre: Optional[str] = "Taller Móvil Fix"
    servicio: str
    fecha_cita: str
    descripcion: str
    foto_referencia: Optional[str] = None

class TicketStatusUpdateRequest(BaseModel):
    ticket_id: int
    nuevo_estado_index: int

class TicketResponse(BaseModel):
    id: int
    codigo_ticket: str
    usuario_id: int
    usuario_correo: str
    negocio_id: int
    negocio_nombre: str
    servicio: str
    fecha_cita: str
    descripcion: str
    foto_referencia: Optional[str] = None
    estado_index: int
    creado_en: datetime

    class Config:
        from_attributes = True

class ReviewCreateRequest(BaseModel):
    negocio_id: int
    usuario_correo: EmailStr
    calificacion: int
    comentario: str

class ReviewResponse(BaseModel):
    id: int
    negocio_id: int
    autor_nombre: str
    calificacion: int
    comentario: str
    fecha_texto: str
    creado_en: datetime

    class Config:
        from_attributes = True