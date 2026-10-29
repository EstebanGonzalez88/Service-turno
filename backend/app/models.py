from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Boolean, Table
from sqlalchemy.orm import relationship
from datetime import datetime, timezone
from app.database import Base

usuario_rol_table = Table(
    'usuario_rol', Base.metadata,
    Column('usuario_id', Integer, ForeignKey('usuarios.id')),
    Column('rol_id', Integer, ForeignKey('roles.id'))
)

rol_permiso_table = Table(
    'rol_permiso', Base.metadata,
    Column('rol_id', Integer, ForeignKey('roles.id')),
    Column('permiso_id', Integer, ForeignKey('permisos.id'))
)

class Usuario(Base):
    __tablename__ = "usuarios"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String(120))
    correo = Column(String(150), unique=True, index=True)
    telefono = Column(String(20), nullable=True)
    password_hash = Column(String(255))
    creado_en = Column(DateTime, default=lambda: datetime.now(timezone.utc))
    email_verificado = Column(Boolean, default=False)
    codigo_verificacion = Column(String(6), nullable=True)
    expiracion_codigo = Column(DateTime, nullable=True)
    activo = Column(Boolean, default=True)

    # Nuevos campos de información personal
    apellidos = Column(String(120), nullable=True)
    codigo_postal = Column(String(10), nullable=True)
    direccion = Column(String(255), nullable=True)
    ciudad = Column(String(100), nullable=True)
    foto_perfil = Column(String(500), nullable=True)

    # Campos de contacto de emergencia y notificaciones
    contacto_emergencia_nombre = Column(String(120), nullable=True)
    contacto_emergencia_telefono = Column(String(20), nullable=True)
    notif_tickets = Column(Boolean, default=True)
    notif_recordatorios = Column(Boolean, default=True)
    notif_promociones = Column(Boolean, default=False)

    roles = relationship("Rol", secondary=usuario_rol_table, back_populates="usuarios")
    logs = relationship("AuditLog", back_populates="usuario")

class Rol(Base):
    __tablename__ = "roles"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String(50), unique=True, index=True)

    usuarios = relationship("Usuario", secondary=usuario_rol_table, back_populates="roles")
    permisos = relationship("Permiso", secondary=rol_permiso_table, back_populates="roles")

class Permiso(Base):
    __tablename__ = "permisos"

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String(50), unique=True, index=True)

    roles = relationship("Rol", secondary=rol_permiso_table, back_populates="permisos")

class AuditLog(Base):
    __tablename__ = "audit_logs"

    id = Column(Integer, primary_key=True, index=True)
    usuario_id = Column(Integer, ForeignKey("usuarios.id"), nullable=True)
    fecha_hora = Column(DateTime, default=lambda: datetime.now(timezone.utc))
    direccion_ip = Column(String)
    accion = Column(String)

    usuario = relationship("Usuario", back_populates="logs")

class Content(Base):
    __tablename__ = "contents"

    id = Column(Integer, primary_key=True, index=True)
    texto = Column(String)
    autor_correo = Column(String)
    fecha_creacion = Column(DateTime, default=lambda: datetime.now(timezone.utc))

class Negocio(Base):
    __tablename__ = "negocios"

    id = Column(Integer, primary_key=True, index=True)
    usuario_id = Column(Integer, ForeignKey("usuarios.id"))
    nombre = Column(String(150))
    categoria = Column(String(100))
    descripcion = Column(String(500))
    telefono = Column(String(20))
    direccion = Column(String(255))
    ciudad = Column(String(100))
    horario = Column(String(100))
    foto_portada = Column(String(500), nullable=True)
    estado = Column(String(30), default="PENDIENTE") # PENDIENTE, APROBADO, RECHAZADO
    latitud = Column(String(50), default="16.7533")
    longitud = Column(String(50), default="-93.1165")
    abierto = Column(Boolean, default=True)
    creado_en = Column(DateTime, default=lambda: datetime.now(timezone.utc))

    usuario = relationship("Usuario")

class Ticket(Base):
    __tablename__ = "tickets"

    id = Column(Integer, primary_key=True, index=True)
    codigo_ticket = Column(String(20))
    usuario_id = Column(Integer, ForeignKey("usuarios.id"))
    negocio_id = Column(Integer, ForeignKey("negocios.id"))
    servicio = Column(String(150))
    fecha_cita = Column(String(100))
    descripcion = Column(String(500))
    foto_referencia = Column(String(500), nullable=True)
    estado_index = Column(Integer, default=0) # 0: Pendiente, 1: Confirmada, 2: En Reparación, 3: Listo, 4: Concluido, -1: Cancelado
    creado_en = Column(DateTime, default=lambda: datetime.now(timezone.utc))

    usuario = relationship("Usuario")
    negocio = relationship("Negocio")

class Resena(Base):
    __tablename__ = "resenas"

    id = Column(Integer, primary_key=True, index=True)
    negocio_id = Column(Integer, ForeignKey("negocios.id"))
    usuario_id = Column(Integer, ForeignKey("usuarios.id"))
    autor_nombre = Column(String(120))
    calificacion = Column(Integer, default=5)
    comentario = Column(String(500))
    fecha_texto = Column(String(50), default="Hoy")
    creado_en = Column(DateTime, default=lambda: datetime.now(timezone.utc))

    negocio = relationship("Negocio")
    usuario = relationship("Usuario")