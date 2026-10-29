from fastapi import APIRouter, Depends, HTTPException, status, Request
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from app.database import get_db
from app import schemas, models, security
import os
import random
import resend
from datetime import datetime, timedelta, timezone

# Configuración de Resend
resend.api_key = os.getenv("RESEND_API_KEY", "")

def generar_y_enviar_codigo(destinatario: str, asunto: str) -> tuple[str, datetime]:
    codigo = f"{random.randint(100000, 999999)}"
    expira = datetime.now(timezone.utc) + timedelta(minutes=10)

    # Modo de pruebas: Enviar el correo físico a tu casilla autorizada de Resend
    # pero asociar el código a la cuenta que se ingresó en la app
    correo_destino_real = os.getenv("TEST_EMAIL_RECIPIENT", "jose.perez73@unach.mx")

    try:
        response = resend.Emails.send({
            "from": "ServiTurno <onboarding@resend.dev>",
            "to": correo_destino_real,
            "subject": f"{asunto} - (Cuenta: {destinatario})",
            "html": f"""
                <div style="font-family: Arial, sans-serif; padding: 20px; text-align: center;">
                    <h2>Código de Verificación - ServiTurno</h2>
                    <p>Solicitud para la cuenta registrada: <b>{destinatario}</b></p>
                    <p>Usa el siguiente código de 6 dígitos para completar tu solicitud:</p>
                    <h1 style="background: #f0f4f8; padding: 10px; letter-spacing: 5px; color: #1976D2;">{codigo}</h1>
                    <p style="color: gray; font-size: 12px;">Este código vence en 10 minutos.</p>
                </div>
            """
        })
        print(f"Resend Response: {response}") # Log de la respuesta de Resend
    except Exception as e:
        print(f"Error al enviar correo (Resend): {e}")

    return codigo, expira

router = APIRouter(prefix="/api/auth", tags=["Autenticación"])

@router.post("/register")
def register(user: schemas.UserCreate, request: Request, db: Session = Depends(get_db)):
    correo_clean = user.correo.strip().lower()
    db_user = db.query(models.Usuario).filter(models.Usuario.correo.ilike(correo_clean)).first()
    if db_user:
        raise HTTPException(status_code=400, detail="El correo ya está registrado")

    hashed_password = security.get_password_hash(user.password)
    new_user = models.Usuario(
        nombre=user.nombre or "Usuario",
        correo=correo_clean,
        password_hash=hashed_password,
        email_verificado=False
    )

    default_role = db.query(models.Rol).filter(models.Rol.nombre == "Usuario Regular").first()
    if default_role:
        new_user.roles.append(default_role)

    # Generar y enviar código de verificación por correo al registrarse
    codigo, expiracion = generar_y_enviar_codigo(
        destinatario=new_user.correo,
        asunto="Verifica tu cuenta de ServiTurno"
    )

    new_user.codigo_verificacion = codigo
    new_user.expiracion_codigo = expiracion

    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    print(f"[DEBUG /register] Usuario {correo_clean} creado. Código generado: {codigo}")
    security.registrar_auditoria(db, request, f"Registro de nuevo usuario (Pendiente de verificación): {user.correo}", new_user.id)
    return {"mensaje": "Cuenta registrada. Ingrese el código enviado a su correo para activarla.", "correo": new_user.correo}

@router.post("/login")
def login(request: Request, login_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    correo_clean = login_data.username.strip().lower()
    user = db.query(models.Usuario).filter(models.Usuario.correo.ilike(correo_clean)).first()
    if not user or not security.verify_password(login_data.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Correo o contraseña incorrectos",
            headers={"WWW-Authenticate": "Bearer"},
        )
    if not user.activo:
        raise HTTPException(status_code=400, detail="Usuario inactivo")

    # Si el usuario no ha verificado su correo al registrarse, bloqueamos el acceso
    if not user.email_verificado:
        codigo, expiracion = generar_y_enviar_codigo(
            destinatario=user.correo,
            asunto="Código de activación de cuenta ServiTurno"
        )
        user.codigo_verificacion = codigo
        user.expiracion_codigo = expiracion
        db.commit()
        print(f"[DEBUG /login] Usuario no verificado. Nuevo código enviado a {user.correo}: {codigo}")
        raise HTTPException(
            status_code=403,
            detail="Debes verificar tu correo antes de iniciar sesión. Te hemos enviado un nuevo código."
        )

    # Generar y enviar código 2FA
    codigo, expiracion = generar_y_enviar_codigo(
        destinatario=user.correo,
        asunto="Tu código de acceso a ServiTurno"
    )

    # Guardar código en BD
    user.codigo_verificacion = codigo
    user.expiracion_codigo = expiracion
    db.commit()

    print(f"[DEBUG /login] Código 2FA generado y guardado para {user.correo}: {codigo}")
    return {"mensaje": "Credenciales válidas. Ingresa el código enviado a tu correo.", "correo": user.correo}

@router.post("/login-verify", response_model=schemas.Token)
def login_verify(data: schemas.VerifyRequest, request: Request, db: Session = Depends(get_db)):
    correo_clean = data.correo.strip().lower()
    codigo_clean = data.codigo.strip()

    print(f"[DEBUG /login-verify] Verificando correo: '{correo_clean}', Código ingresado: '{codigo_clean}'")

    user = db.query(models.Usuario).filter(models.Usuario.correo.ilike(correo_clean)).first()
    if not user:
        print(f"[DEBUG /login-verify] Usuario no encontrado para: {correo_clean}")
        raise HTTPException(status_code=400, detail="Usuario no encontrado")

    print(f"[DEBUG /login-verify] Código en BD: '{user.codigo_verificacion}' vs Código ingresado: '{codigo_clean}'")

    if not user.codigo_verificacion or str(user.codigo_verificacion).strip() != codigo_clean:
        print(f"[DEBUG /login-verify] ERROR: El código no coincide")
        raise HTTPException(status_code=400, detail="Código inválido")

    # Verificar expiración
    if user.expiracion_codigo and user.expiracion_codigo.replace(tzinfo=timezone.utc) < datetime.now(timezone.utc):
        print(f"[DEBUG /login-verify] ERROR: El código ha expirado")
        raise HTTPException(status_code=400, detail="El código ha expirado")

    # ACTIVAMOS Y CONFIRMAMOS EL CORREO DEL USUARIO
    user.email_verificado = True
    user.codigo_verificacion = None
    user.expiracion_codigo = None
    db.commit()

    access_token = security.create_access_token(data={"sub": user.correo})
    refresh_token = security.create_refresh_token(data={"sub": user.correo})

    security.registrar_auditoria(db, request, f"Verificación de código exitosa (Cuenta activada)", user.id)
    print(f"[DEBUG /login-verify] ¡ÉXITO! Usuario {user.correo} verificado e ingresado correctamente.")
    return {"access_token": access_token, "refresh_token": refresh_token, "token_type": "bearer"}

def check_perfil_completo(user: models.Usuario) -> bool:
    return bool(
        user.nombre and user.apellidos and user.telefono and
        user.codigo_postal and user.direccion and user.ciudad
    )

@router.put("/profile", response_model=schemas.UserProfileResponse)
def update_profile(data: schemas.ProfileUpdate, db: Session = Depends(get_db)):
    correo_clean = data.correo.strip().lower()
    user = db.query(models.Usuario).filter(models.Usuario.correo.ilike(correo_clean)).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    if data.nombre is not None: user.nombre = data.nombre.strip()
    if data.apellidos is not None: user.apellidos = data.apellidos.strip()
    if data.telefono is not None: user.telefono = data.telefono.strip()
    if data.codigo_postal is not None: user.codigo_postal = data.codigo_postal.strip()
    if data.direccion is not None: user.direccion = data.direccion.strip()
    if data.ciudad is not None: user.ciudad = data.ciudad.strip()
    if data.foto_perfil is not None: user.foto_perfil = data.foto_perfil.strip()
    if data.contacto_emergencia_nombre is not None: user.contacto_emergencia_nombre = data.contacto_emergencia_nombre.strip()
    if data.contacto_emergencia_telefono is not None: user.contacto_emergencia_telefono = data.contacto_emergencia_telefono.strip()
    if data.notif_tickets is not None: user.notif_tickets = data.notif_tickets
    if data.notif_recordatorios is not None: user.notif_recordatorios = data.notif_recordatorios
    if data.notif_promociones is not None: user.notif_promociones = data.notif_promociones

    db.commit()
    db.refresh(user)

    is_complete = check_perfil_completo(user)

    return schemas.UserProfileResponse(
        id=user.id,
        correo=user.correo,
        nombre=user.nombre,
        apellidos=user.apellidos,
        telefono=user.telefono,
        codigo_postal=user.codigo_postal,
        direccion=user.direccion,
        ciudad=user.ciudad,
        foto_perfil=user.foto_perfil,
        contacto_emergencia_nombre=user.contacto_emergencia_nombre,
        contacto_emergencia_telefono=user.contacto_emergencia_telefono,
        notif_tickets=user.notif_tickets if user.notif_tickets is not None else True,
        notif_recordatorios=user.notif_recordatorios if user.notif_recordatorios is not None else True,
        notif_promociones=user.notif_promociones if user.notif_promociones is not None else False,
        perfil_completo=is_complete
    )

@router.get("/profile/{correo}", response_model=schemas.UserProfileResponse)
def get_profile(correo: str, db: Session = Depends(get_db)):
    correo_clean = correo.strip().lower()
    user = db.query(models.Usuario).filter(models.Usuario.correo.ilike(correo_clean)).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    is_complete = check_perfil_completo(user)

    return schemas.UserProfileResponse(
        id=user.id,
        correo=user.correo,
        nombre=user.nombre,
        apellidos=user.apellidos,
        telefono=user.telefono,
        codigo_postal=user.codigo_postal,
        direccion=user.direccion,
        ciudad=user.ciudad,
        foto_perfil=user.foto_perfil,
        contacto_emergencia_nombre=user.contacto_emergencia_nombre,
        contacto_emergencia_telefono=user.contacto_emergencia_telefono,
        notif_tickets=user.notif_tickets if user.notif_tickets is not None else True,
        notif_recordatorios=user.notif_recordatorios if user.notif_recordatorios is not None else True,
        notif_promociones=user.notif_promociones if user.notif_promociones is not None else False,
        perfil_completo=is_complete
    )

@router.post("/change-password")
def change_password(data: schemas.ChangePasswordRequest, request: Request, current_user: models.Usuario = Depends(security.get_current_user), db: Session = Depends(get_db)):
    if not security.verify_password(data.old_password, current_user.password_hash):
        raise HTTPException(status_code=400, detail="La contraseña actual es incorrecta")

    current_user.password_hash = security.get_password_hash(data.new_password)
    db.commit()
    security.registrar_auditoria(db, request, "Cambio de contraseña", current_user.id)
    return {"mensaje": "Contraseña actualizada exitosamente"}