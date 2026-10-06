from pydantic import BaseModel, EmailStr
from typing import List, Optional
from datetime import datetime

class UserBase(BaseModel):
    nombre: str
    email: EmailStr

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
    email: EmailStr
    password: str

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
    user_id: Optional[int]
    fecha_hora: datetime
    direccion_ip: str
    accion: str

    class Config:
        from_attributes = True

# --- Nuevos esquemas para el Contenido Real ---

class ContentCreate(BaseModel):
    texto: str

class ContentResponse(BaseModel):
    id: int
    texto: str
    autor_email: str
    fecha_creacion: datetime

    class Config:
        from_attributes = True
