from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy.orm import Session
from typing import List
from app.database import get_db
from app import schemas, models, security

# Rutas protegidas solo para roles con permiso específico o Administradores
router = APIRouter(prefix="/api/admin", tags=["Administración"])

# Dependencia para verificar si es Admin (o si tiene permiso global, aquí forzamos Admin explícitamente como requerido)
def require_admin(current_user: models.User = Depends(security.get_current_user)):
    if not any(r.nombre == "Administrador" for r in current_user.roles):
        raise HTTPException(status_code=403, detail="Acceso denegado. Requiere rol Administrador.")
    return current_user

@router.get("/users", response_model=List[schemas.UserResponse])
def get_users(db: Session = Depends(get_db), admin: models.User = Depends(require_admin)):
    return db.query(models.User).all()

@router.get("/roles", response_model=List[schemas.RoleResponse])
def get_roles(db: Session = Depends(get_db), admin: models.User = Depends(require_admin)):
    return db.query(models.Role).all()

@router.post("/roles")
def create_role(role: schemas.RoleCreate, request: Request, db: Session = Depends(get_db), admin: models.User = Depends(require_admin)):
    db_role = db.query(models.Role).filter(models.Role.nombre == role.nombre).first()
    if db_role:
        raise HTTPException(status_code=400, detail="El rol ya existe")

    new_role = models.Role(nombre=role.nombre)
    db.add(new_role)
    db.commit()
    security.registrar_auditoria(db, request, f"Creó el rol: {role.nombre}", admin.id)
    return {"mensaje": "Rol creado exitosamente"}

@router.post("/users/{user_id}/roles/{role_id}")
def assign_role(user_id: int, role_id: int, request: Request, db: Session = Depends(get_db), admin: models.User = Depends(require_admin)):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    role = db.query(models.Role).filter(models.Role.id == role_id).first()
    if not user or not role:
        raise HTTPException(status_code=404, detail="Usuario o Rol no encontrado")

    if role not in user.roles:
        user.roles.append(role)
        db.commit()
        security.registrar_auditoria(db, request, f"Asignó rol {role.nombre} al usuario {user.email}", admin.id)

    return {"mensaje": "Rol asignado correctamente"}

@router.delete("/users/{user_id}/roles/{role_id}")
def revoke_role(user_id: int, role_id: int, request: Request, db: Session = Depends(get_db), admin: models.User = Depends(require_admin)):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    role = db.query(models.Role).filter(models.Role.id == role_id).first()
    if not user or not role:
        raise HTTPException(status_code=404, detail="Usuario o Rol no encontrado")

    if role in user.roles:
        user.roles.remove(role)
        db.commit()
        security.registrar_auditoria(db, request, f"Revocó rol {role.nombre} del usuario {user.email}", admin.id)

    return {"mensaje": "Rol revocado correctamente"}

@router.get("/audit", response_model=List[schemas.AuditLogResponse])
def get_audit_logs(db: Session = Depends(get_db), admin: models.User = Depends(require_admin)):
    return db.query(models.AuditLog).order_by(models.AuditLog.fecha_hora.desc()).all()
