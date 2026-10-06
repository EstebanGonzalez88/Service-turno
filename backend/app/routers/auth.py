from fastapi import APIRouter, Depends, HTTPException, status, Request
from sqlalchemy.orm import Session
from app.database import get_db
from app import schemas, models, security

router = APIRouter(prefix="/api/auth", tags=["Autenticación"])

@router.post("/register", response_model=schemas.UserResponse)
def register(user: schemas.UserCreate, request: Request, db: Session = Depends(get_db)):
    db_user = db.query(models.User).filter(models.User.email == user.email).first()
    if db_user:
        raise HTTPException(status_code=400, detail="El correo ya está registrado")

    hashed_password = security.get_password_hash(user.password)
    new_user = models.User(nombre=user.nombre, email=user.email, password_hash=hashed_password)

    # Asignar rol Usuario Regular por defecto
    default_role = db.query(models.Role).filter(models.Role.nombre == "Usuario Regular").first()
    if default_role:
        new_user.roles.append(default_role)

    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    security.registrar_auditoria(db, request, f"Registro de nuevo usuario: {user.email}", new_user.id)
    return new_user

from fastapi.security import OAuth2PasswordRequestForm

@router.post("/login", response_model=schemas.Token)
def login(request: Request, login_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    # Protección simple evitando fuga de info (error génerico)
    user = db.query(models.User).filter(models.User.email == login_data.username).first()
    if not user or not security.verify_password(login_data.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Correo o contraseña incorrectos",
            headers={"WWW-Authenticate": "Bearer"},
        )
    if not user.activo:
        raise HTTPException(status_code=400, detail="Usuario inactivo")

    # Tokens
    access_token = security.create_access_token(data={"sub": user.email})
    refresh_token = security.create_refresh_token(data={"sub": user.email})

    security.registrar_auditoria(db, request, f"Inicio de sesión exitoso", user.id)

    return {"access_token": access_token, "refresh_token": refresh_token, "token_type": "bearer"}

@router.post("/change-password")
def change_password(data: schemas.ChangePasswordRequest, request: Request, current_user: models.User = Depends(security.get_current_user), db: Session = Depends(get_db)):
    if not security.verify_password(data.old_password, current_user.password_hash):
        raise HTTPException(status_code=400, detail="La contraseña actual es incorrecta")

    current_user.password_hash = security.get_password_hash(data.new_password)
    db.commit()
    security.registrar_auditoria(db, request, "Cambio de contraseña", current_user.id)

    return {"mensaje": "Contraseña actualizada exitosamente"}
