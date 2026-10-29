from fastapi import APIRouter, Depends, HTTPException, status, Request
from sqlalchemy.orm import Session
from app.database import get_db
from app import schemas, models

router = APIRouter(prefix="/api/business", tags=["Negocios"])

@router.post("/register", response_model=schemas.BusinessResponse)
def register_business(data: schemas.BusinessCreateRequest, db: Session = Depends(get_db)):
    correo_clean = data.usuario_correo.strip().lower()
    user = db.query(models.Usuario).filter(models.Usuario.correo.ilike(correo_clean)).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    # Verificar si el usuario ya tiene un negocio registrado
    existing_business = db.query(models.Negocio).filter(models.Negocio.usuario_id == user.id).first()
    if existing_business:
        # Si ya existe, actualizamos los datos y lo dejamos en estado PENDIENTE
        existing_business.nombre = data.nombre.strip()
        existing_business.categoria = data.categoria.strip()
        existing_business.descripcion = data.descripcion.strip()
        existing_business.telefono = data.telefono.strip()
        existing_business.direccion = data.direccion.strip()
        existing_business.ciudad = data.ciudad.strip()
        existing_business.horario = data.horario.strip()
        if data.foto_portada:
            existing_business.foto_portada = data.foto_portada
        existing_business.estado = "PENDIENTE"
        db.commit()
        db.refresh(existing_business)
        return existing_business

    new_business = models.Negocio(
        usuario_id=user.id,
        nombre=data.nombre.strip(),
        categoria=data.categoria.strip(),
        descripcion=data.descripcion.strip(),
        telefono=data.telefono.strip(),
        direccion=data.direccion.strip(),
        ciudad=data.ciudad.strip(),
        horario=data.horario.strip(),
        foto_portada=data.foto_portada,
        estado="PENDIENTE"
    )

    db.add(new_business)
    db.commit()
    db.refresh(new_business)
    return new_business

@router.get("/my-business/{correo}", response_model=schemas.BusinessResponse)
def get_my_business(correo: str, db: Session = Depends(get_db)):
    correo_clean = correo.strip().lower()
    user = db.query(models.Usuario).filter(models.Usuario.correo.ilike(correo_clean)).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    business = db.query(models.Negocio).filter(models.Negocio.usuario_id == user.id).first()
    if not business:
        raise HTTPException(status_code=404, detail="El usuario no tiene ningún negocio registrado")

    return business

@router.get("/all", response_model=list[schemas.BusinessResponse])
def get_all_businesses(db: Session = Depends(get_db)):
    return db.query(models.Negocio).all()

@router.post("/toggle-status/{business_id}", response_model=schemas.BusinessResponse)
def toggle_business_status(business_id: int, db: Session = Depends(get_db)):
    business = db.query(models.Negocio).filter(models.Negocio.id == business_id).first()
    if not business:
        raise HTTPException(status_code=404, detail="Negocio no encontrado")

    # Alternar entre PENDIENTE y APROBADO para facilidad de pruebas
    if business.estado == "PENDIENTE":
        business.estado = "APROBADO"
    else:
        business.estado = "PENDIENTE"

    db.commit()
    db.refresh(business)
    return business

@router.put("/update", response_model=schemas.BusinessResponse)
def update_business(data: schemas.BusinessUpdateRequest, db: Session = Depends(get_db)):
    business = db.query(models.Negocio).filter(models.Negocio.id == data.business_id).first()
    if not business:
        raise HTTPException(status_code=404, detail="Negocio no encontrado")

    if data.nombre is not None: business.nombre = data.nombre.strip()
    if data.categoria is not None: business.categoria = data.categoria.strip()
    if data.descripcion is not None: business.descripcion = data.descripcion.strip()
    if data.telefono is not None: business.telefono = data.telefono.strip()
    if data.direccion is not None: business.direccion = data.direccion.strip()
    if data.horario is not None: business.horario = data.horario.strip()
    if data.latitud is not None: business.latitud = data.latitud.strip()
    if data.longitud is not None: business.longitud = data.longitud.strip()
    if data.abierto is not None: business.abierto = data.abierto

    db.commit()
    db.refresh(business)
    return business
