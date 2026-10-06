from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from app.database import engine, Base, SessionLocal
from app.routers import auth, admin, content
from app.models import Role, Permission, User
from app.security import get_password_hash

# Inicializar Base de Datos (crea las tablas si no existen)
Base.metadata.create_all(bind=engine)

# Inyección de roles iniciales (Seed)
def init_db():
    db = SessionLocal()
    if not db.query(Role).first():
        admin_role = Role(nombre="Administrador")
        editor_role = Role(nombre="Editor")
        user_role = Role(nombre="Usuario Regular")

        perm_read = Permission(nombre="Lectura")
        perm_write = Permission(nombre="Escritura")
        perm_delete = Permission(nombre="Eliminacion")

        editor_role.permissions.extend([perm_read, perm_write, perm_delete])
        user_role.permissions.append(perm_read)

        db.add_all([admin_role, editor_role, user_role, perm_read, perm_write, perm_delete])

        # Crear un admin por defecto
        admin_user = User(nombre="Admin Default", email="admin@serviturno.com", password_hash=get_password_hash("Admin123!"))
        admin_user.roles.append(admin_role)
        db.add(admin_user)

        db.commit()
    db.close()

init_db()

# Rate limiting
limiter = Limiter(key_func=get_remote_address)

app = FastAPI(title="ServiTurno API (Auth Roles & Permissions)")
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# CORS Seguro
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:8000", "http://127.0.0.1:8000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Exception handlers para no fugar información
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={"detail": "Error interno del servidor. Por favor, contacte al administrador."},
    )

app.include_router(auth.router)
app.include_router(admin.router)
app.include_router(content.router)

from fastapi.staticfiles import StaticFiles
import os

# Frontend de prueba
frontend_path = os.path.join(os.path.dirname(__file__), "../frontend")
if os.path.exists(frontend_path):
    app.mount("/", StaticFiles(directory=frontend_path, html=True), name="frontend")
