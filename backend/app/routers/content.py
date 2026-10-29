from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy.orm import Session
from typing import List
from app.database import get_db
from app import models, schemas, security

router = APIRouter(prefix="/api/content", tags=["Contenido"])

@router.get("/", response_model=List[schemas.ContentResponse])
def read_content(
    db: Session = Depends(get_db),
    current_user: models.Usuario = Depends(security.RequirePermissions(["Lectura"]))
):
    return db.query(models.Content).order_by(models.Content.fecha_creacion.desc()).all()

@router.post("/", response_model=schemas.ContentResponse)
def create_content(
    content_data: schemas.ContentCreate,
    request: Request,
    db: Session = Depends(get_db),
    current_user: models.Usuario = Depends(security.RequirePermissions(["Escritura"]))
):
    new_content = models.Content(texto=content_data.texto, autor_correo=current_user.correo)
    db.add(new_content)
    db.commit()
    db.refresh(new_content)

    security.registrar_auditoria(db, request, f"Creó el contenido ID {new_content.id}", current_user.id)
    return new_content

@router.delete("/{content_id}")
def delete_content(
    content_id: int,
    request: Request,
    db: Session = Depends(get_db),
    current_user: models.Usuario = Depends(security.RequirePermissions(["Eliminacion"]))
):
    content = db.query(models.Content).filter(models.Content.id == content_id).first()
    if not content:
        raise HTTPException(status_code=404, detail="Contenido no encontrado")

    db.delete(content)
    db.commit()

    security.registrar_auditoria(db, request, f"Eliminó el contenido ID {content_id}", current_user.id)
    return {"mensaje": f"Contenido eliminado con éxito"}