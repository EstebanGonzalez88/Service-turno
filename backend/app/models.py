from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Boolean, Table
from sqlalchemy.orm import relationship
from datetime import datetime, timezone
from app.database import Base

# Tablas intermedias para la relación muchos a muchos
user_role_table = Table(
    'user_role', Base.metadata,
    Column('user_id', Integer, ForeignKey('users.id')),
    Column('role_id', Integer, ForeignKey('roles.id'))
)

role_permission_table = Table(
    'role_permission', Base.metadata,
    Column('role_id', Integer, ForeignKey('roles.id')),
    Column('permission_id', Integer, ForeignKey('permissions.id'))
)

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String, index=True)
    email = Column(String, unique=True, index=True)
    password_hash = Column(String)
    activo = Column(Boolean, default=True)

    roles = relationship("Role", secondary=user_role_table, back_populates="users")
    logs = relationship("AuditLog", back_populates="user")

class Role(Base):
    __tablename__ = "roles"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String, unique=True, index=True)

    users = relationship("User", secondary=user_role_table, back_populates="roles")
    permissions = relationship("Permission", secondary=role_permission_table, back_populates="roles")

class Permission(Base):
    __tablename__ = "permissions"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String, unique=True, index=True) # ej. "Lectura", "Escritura", "Eliminacion"

    roles = relationship("Role", secondary=role_permission_table, back_populates="permissions")

class AuditLog(Base):
    __tablename__ = "audit_logs"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    fecha_hora = Column(DateTime, default=lambda: datetime.now(timezone.utc))
    direccion_ip = Column(String)
    accion = Column(String)

    user = relationship("User", back_populates="logs")

class Content(Base):
    __tablename__ = "contents"

    id = Column(Integer, primary_key=True, index=True)
    texto = Column(String)
    autor_email = Column(String)
    fecha_creacion = Column(DateTime, default=lambda: datetime.now(timezone.utc))

