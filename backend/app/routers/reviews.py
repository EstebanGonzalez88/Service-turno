from fastapi import APIRouter, Depends, HTTPException, status, Request
from sqlalchemy.orm import Session
from app.database import get_db
from app import schemas, models

router = APIRouter(prefix="/api/reviews", tags=["Reseñas y Calificaciones"])

@router.post("/create", response_model=schemas.ReviewResponse)
def create_review(data: schemas.ReviewCreateRequest, db: Session = Depends(get_db)):
    correo_clean = data.usuario_correo.strip().lower()
    user = db.query(models.Usuario).filter(models.Usuario.correo.ilike(correo_clean)).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    negocio = db.query(models.Negocio).filter(models.Negocio.id == data.negocio_id).first()
    if not negocio:
        raise HTTPException(status_code=404, detail="Negocio no encontrado")

    nombre_autor = f"{user.nombre or ''} {user.apellidos or ''}".strip()
    if not nombre_autor:
        nombre_autor = user.correo.split("@")[0]

    new_review = models.Resena(
        negocio_id=negocio.id,
        usuario_id=user.id,
        autor_nombre=nombre_autor,
        calificacion=data.calificacion,
        comentario=data.comentario.strip(),
        fecha_texto="Hace un momento"
    )

    db.add(new_review)
    db.commit()
    db.refresh(new_review)

    return schemas.ReviewResponse(
        id=new_review.id,
        negocio_id=new_review.negocio_id,
        autor_nombre=new_review.autor_nombre,
        calificacion=new_review.calificacion,
        comentario=new_review.comentario,
        fecha_texto=new_review.fecha_texto,
        creado_en=new_review.creado_en
    )

@router.get("/business/{business_id}", response_model=list[schemas.ReviewResponse])
def get_business_reviews(business_id: int, db: Session = Depends(get_db)):
    reviews = db.query(models.Resena).filter(models.Resena.negocio_id == business_id).order_by(models.Resena.id.desc()).all()
    res = []
    for r in reviews:
        res.append(schemas.ReviewResponse(
            id=r.id,
            negocio_id=r.negocio_id,
            autor_nombre=r.autor_nombre,
            calificacion=r.calificacion,
            comentario=r.comentario,
            fecha_texto=r.fecha_texto,
            creado_en=r.creado_en
        ))
    return res
