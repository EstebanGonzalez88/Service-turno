from fastapi import APIRouter, Depends, HTTPException, status, Request
from sqlalchemy.orm import Session
from app.database import get_db
from app import schemas, models
import random

router = APIRouter(prefix="/api/tickets", tags=["Tickets y Citas"])

@router.post("/create", response_model=schemas.TicketResponse)
def create_ticket(data: schemas.TicketCreateRequest, db: Session = Depends(get_db)):
    correo_clean = data.usuario_correo.strip().lower()
    user = db.query(models.Usuario).filter(models.Usuario.correo.ilike(correo_clean)).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario cliente no encontrado")

    # Buscar el negocio por ID o tomar el primer negocio disponible
    negocio = db.query(models.Negocio).filter(models.Negocio.id == data.negocio_id).first()
    if not negocio:
        negocio = db.query(models.Negocio).first()
        if not negocio:
            # Si no hay ningún negocio en BD, creamos uno de prueba por defecto
            negocio = models.Negocio(
                usuario_id=user.id,
                nombre=data.negocio_nombre or "Taller Móvil Fix",
                categoria="Reparación y Servicio Técnico",
                descripcion="Taller especialista de servicios",
                telefono="9611234567",
                direccion="Calle Central #45",
                ciudad="Tuxtla Gutiérrez",
                horario="Lunes a Sábado: 9:00 AM - 7:00 PM",
                estado="APROBADO"
            )
            db.add(negocio)
            db.commit()
            db.refresh(negocio)

    # Generar código consecutivo de ticket (Ej: #1042)
    count = db.query(models.Ticket).count()
    codigo_ticket = f"#{1040 + count + 1}"

    new_ticket = models.Ticket(
        codigo_ticket=codigo_ticket,
        usuario_id=user.id,
        negocio_id=negocio.id,
        servicio=data.servicio.strip(),
        fecha_cita=data.fecha_cita.strip(),
        descripcion=data.descripcion.strip(),
        foto_referencia=data.foto_referencia,
        estado_index=0 # 0: Solicitud Enviada / Pendiente
    )

    db.add(new_ticket)
    db.commit()
    db.refresh(new_ticket)

    return schemas.TicketResponse(
        id=new_ticket.id,
        codigo_ticket=new_ticket.codigo_ticket,
        usuario_id=new_ticket.usuario_id,
        usuario_correo=user.correo,
        negocio_id=negocio.id,
        negocio_nombre=negocio.nombre,
        servicio=new_ticket.servicio,
        fecha_cita=new_ticket.fecha_cita,
        descripcion=new_ticket.descripcion,
        foto_referencia=new_ticket.foto_referencia,
        estado_index=new_ticket.estado_index,
        creado_en=new_ticket.creado_en
    )

@router.get("/my-tickets/{correo}", response_model=list[schemas.TicketResponse])
def get_my_tickets(correo: str, db: Session = Depends(get_db)):
    correo_clean = correo.strip().lower()
    user = db.query(models.Usuario).filter(models.Usuario.correo.ilike(correo_clean)).first()
    if not user:
        return []

    tickets = db.query(models.Ticket).filter(models.Ticket.usuario_id == user.id).all()
    res = []
    for t in tickets:
        negocio_nombre = t.negocio.nombre if t.negocio else "Taller Móvil Fix"
        res.append(schemas.TicketResponse(
            id=t.id,
            codigo_ticket=t.codigo_ticket,
            usuario_id=t.usuario_id,
            usuario_correo=user.correo,
            negocio_id=t.negocio_id,
            negocio_nombre=negocio_nombre,
            servicio=t.servicio,
            fecha_cita=t.fecha_cita,
            descripcion=t.descripcion,
            foto_referencia=t.foto_referencia,
            estado_index=t.estado_index,
            creado_en=t.creado_en
        ))
    return res

@router.get("/business-tickets/{business_id}", response_model=list[schemas.TicketResponse])
def get_business_tickets(business_id: int, db: Session = Depends(get_db)):
    tickets = db.query(models.Ticket).filter(models.Ticket.negocio_id == business_id).all()
    res = []
    for t in tickets:
        usuario_correo = t.usuario.correo if t.usuario else "cliente@correo.com"
        negocio_nombre = t.negocio.nombre if t.negocio else "Mi Negocio"
        res.append(schemas.TicketResponse(
            id=t.id,
            codigo_ticket=t.codigo_ticket,
            usuario_id=t.usuario_id,
            usuario_correo=usuario_correo,
            negocio_id=t.negocio_id,
            negocio_nombre=negocio_nombre,
            servicio=t.servicio,
            fecha_cita=t.fecha_cita,
            descripcion=t.descripcion,
            foto_referencia=t.foto_referencia,
            estado_index=t.estado_index,
            creado_en=t.creado_en
        ))
    return res

@router.put("/update-status", response_model=schemas.TicketResponse)
def update_ticket_status(data: schemas.TicketStatusUpdateRequest, db: Session = Depends(get_db)):
    ticket = db.query(models.Ticket).filter(models.Ticket.id == data.ticket_id).first()
    if not ticket:
        raise HTTPException(status_code=404, detail="Ticket no encontrado")

    ticket.estado_index = data.nuevo_estado_index
    db.commit()
    db.refresh(ticket)

    usuario_correo = ticket.usuario.correo if ticket.usuario else "cliente@correo.com"
    negocio_nombre = ticket.negocio.nombre if ticket.negocio else "Taller Móvil Fix"

    return schemas.TicketResponse(
        id=ticket.id,
        codigo_ticket=ticket.codigo_ticket,
        usuario_id=ticket.usuario_id,
        usuario_correo=usuario_correo,
        negocio_id=ticket.negocio_id,
        negocio_nombre=negocio_nombre,
        servicio=ticket.servicio,
        fecha_cita=ticket.fecha_cita,
        descripcion=ticket.descripcion,
        foto_referencia=ticket.foto_referencia,
        estado_index=ticket.estado_index,
        creado_en=ticket.creado_en
    )
