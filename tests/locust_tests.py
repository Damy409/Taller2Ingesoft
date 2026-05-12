"""
Locust Performance Tests para Circle Guard Microservices
Simula usuarios concurrentes realizando operaciones típicas del sistema.

Ejecución:
    locust -f locust_tests.py --host=http://localhost:8087
"""

from locust import HttpUser, task, between, TaskSet, constant_pacing
import random
import uuid
import json
from datetime import datetime, timedelta


class AuthServiceTasks(TaskSet):
    """Tareas para Auth Service (puerto 8180)"""
    
    def on_start(self):
        """Ejecutado al inicio de cada usuario"""
        self.token = None
        self.anonymous_id = None
    
    @task(3)
    def login(self):
        """Simula login de usuario - 3x más frecuente"""
        username = f"user{random.randint(1, 100)}@circleguard.edu"
        password = "password123"
        
        response = self.client.post(
            "/api/v1/auth/login",
            json={"username": username, "password": password},
            name="POST /api/v1/auth/login"
        )
        
        if response.status_code == 200:
            self.token = response.json().get("token")
            self.anonymous_id = response.json().get("anonymousId")
    
    @task(1)
    def validate_token(self):
        """Valida token JWT"""
        if self.token:
            headers = {"Authorization": f"Bearer {self.token}"}
            self.client.get(
                "/api/v1/auth/validate",
                headers=headers,
                name="GET /api/v1/auth/validate"
            )
    
    @task(2)
    def refresh_token(self):
        """Refresca token JWT"""
        if self.token:
            response = self.client.post(
                "/api/v1/auth/refresh",
                json={"token": self.token},
                name="POST /api/v1/auth/refresh"
            )


class IdentityServiceTasks(TaskSet):
    """Tareas para Identity Service (puerto 8083)"""
    
    @task(2)
    def map_identity(self):
        """Mapea una identidad real a anónima"""
        real_identity = f"user{random.randint(1, 1000)}@circleguard.edu"
        
        response = self.client.post(
            "http://localhost:8083/api/v1/identities/map",
            json={"realIdentity": real_identity},
            name="POST /api/v1/identities/map"
        )
    
    @task(1)
    def register_visitor(self):
        """Registra un visitante temporal"""
        visitor_data = {
            "name": f"Visitor {random.randint(1, 1000)}",
            "email": f"visitor{random.randint(1, 1000)}@temporary.edu"
        }
        
        self.client.post(
            "http://localhost:8083/api/v1/identities/visitor",
            json=visitor_data,
            name="POST /api/v1/identities/visitor"
        )


class DashboardServiceTasks(TaskSet):
    """Tareas para Dashboard Service (puerto 8084)"""
    
    def on_start(self):
        """Inicializa datos para el usuario"""
        self.circle_id = str(uuid.uuid4())
    
    @task(3)
    def get_analytics(self):
        """Obtiene datos analíticos del círculo"""
        self.client.get(
            f"http://localhost:8084/api/v1/analytics/{self.circle_id}",
            name="GET /api/v1/analytics/{circle_id}"
        )
    
    @task(2)
    def get_metrics(self):
        """Obtiene métricas agregadas"""
        self.client.get(
            f"http://localhost:8084/api/v1/analytics/{self.circle_id}/metrics",
            name="GET /api/v1/analytics/{circle_id}/metrics"
        )
    
    @task(1)
    def get_report(self):
        """Genera reporte de privacidad"""
        start_date = (datetime.now() - timedelta(days=30)).isoformat()
        end_date = datetime.now().isoformat()
        
        self.client.get(
            f"http://localhost:8084/api/v1/analytics/{self.circle_id}/report",
            params={"startDate": start_date, "endDate": end_date},
            name="GET /api/v1/analytics/{circle_id}/report"
        )


class FileServiceTasks(TaskSet):
    """Tareas para File Service (puerto 8085)"""
    
    def on_start(self):
        """Inicializa token de autenticación"""
        self.token = str(uuid.uuid4())
    
    @task(2)
    def upload_file(self):
        """Simula carga de archivo"""
        file_content = f"File content {random.randint(1, 10000)}".encode()
        headers = {"Authorization": f"Bearer {self.token}"}
        
        files = {
            'file': ('test_document.pdf', file_content, 'application/pdf')
        }
        
        self.client.post(
            "http://localhost:8085/api/v1/files/upload",
            files=files,
            headers=headers,
            name="POST /api/v1/files/upload"
        )
    
    @task(3)
    def list_files(self):
        """Lista archivos del usuario"""
        headers = {"Authorization": f"Bearer {self.token}"}
        
        self.client.get(
            "http://localhost:8085/api/v1/files/my-files",
            headers=headers,
            name="GET /api/v1/files/my-files"
        )
    
    @task(1)
    def download_file(self):
        """Descarga un archivo"""
        file_id = str(uuid.uuid4())
        headers = {"Authorization": f"Bearer {self.token}"}
        
        self.client.get(
            f"http://localhost:8085/api/v1/files/download/{file_id}",
            headers=headers,
            name="GET /api/v1/files/download/{file_id}"
        )


class FormServiceTasks(TaskSet):
    """Tareas para Form Service (puerto 8086)"""
    
    def on_start(self):
        """Inicializa token"""
        self.token = str(uuid.uuid4())
    
    @task(4)
    def submit_form(self):
        """Envía un formulario de síntomas"""
        symptoms = random.choice([
            ["fever", "cough"],
            ["fever"],
            ["cough", "fatigue"],
            ["headache", "fever", "cough"],
            ["fatigue"],
            []
        ])
        
        form_data = {
            "symptoms": symptoms,
            "intensity": random.choice(["mild", "moderate", "severe"]),
            "notes": f"Health form submission {datetime.now()}"
        }
        
        headers = {"Authorization": f"Bearer {self.token}"}
        
        self.client.post(
            "http://localhost:8086/api/v1/questionnaire/submit",
            json=form_data,
            headers=headers,
            name="POST /api/v1/questionnaire/submit"
        )
    
    @task(2)
    def get_submissions(self):
        """Obtiene envíos del usuario"""
        headers = {"Authorization": f"Bearer {self.token}"}
        
        self.client.get(
            "http://localhost:8086/api/v1/questionnaire/my-submissions",
            headers=headers,
            name="GET /api/v1/questionnaire/my-submissions"
        )
    
    @task(1)
    def get_form_history(self):
        """Obtiene historial de formularios"""
        headers = {"Authorization": f"Bearer {self.token}"}
        days = random.randint(1, 30)
        
        self.client.get(
            "http://localhost:8086/api/v1/questionnaire/history",
            params={"days": days},
            headers=headers,
            name="GET /api/v1/questionnaire/history"
        )


class GatewayServiceTasks(TaskSet):
    """Tareas para Gateway Service (puerto 8087)"""
    
    @task(2)
    def health_check(self):
        """Verifica salud del gateway"""
        self.client.get(
            "/health",
            name="GET /health"
        )
    
    @task(1)
    def service_discovery(self):
        """Descubre servicios disponibles"""
        self.client.get(
            "/api/v1/services",
            name="GET /api/v1/services"
        )


class AuthUser(HttpUser):
    """Usuario que realiza operaciones de autenticación"""
    tasks = [AuthServiceTasks]
    wait_time = between(1, 3)


class IdentityUser(HttpUser):
    """Usuario que realiza operaciones de identidad"""
    tasks = [IdentityServiceTasks]
    wait_time = between(1, 4)


class DashboardUser(HttpUser):
    """Usuario que accede al dashboard"""
    tasks = [DashboardServiceTasks]
    wait_time = between(2, 5)


class FileUser(HttpUser):
    """Usuario que carga/descarga archivos"""
    tasks = [FileServiceTasks]
    wait_time = between(2, 6)


class FormUser(HttpUser):
    """Usuario que envía formularios"""
    tasks = [FormServiceTasks]
    wait_time = between(1, 3)


class GatewayUser(HttpUser):
    """Usuario que interactúa con el gateway"""
    tasks = [GatewayServiceTasks]
    wait_time = constant_pacing(1)


"""
Configuración recomendada de pruebas:

1. Prueba de Carga Básica (5 minutos):
   - 10 usuarios Auth + 10 Dashboard + 5 File + 5 Form
   - Ramp-up: 30 segundos
   - Objetivo: Validar capacidad base

2. Prueba de Estrés (10 minutos):
   - 50 usuarios Auth + 30 Dashboard + 20 File + 20 Form
   - Ramp-up: 2 minutos
   - Objetivo: Encontrar punto de ruptura

3. Prueba de Picos (15 minutos):
   - Comenzar con 5 usuarios
   - Aumentar a 100 usuarios cada 3 minutos
   - Objetivo: Verificar recuperación bajo picos

4. Prueba de Resistencia (30 minutos):
   - 30 usuarios uniformes
   - Ramp-up: 2 minutos
   - Objetivo: Detectar memory leaks y problemas de larga duración
"""
