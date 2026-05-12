"""
Configuración y guía de ejecución de Locust para pruebas de rendimiento

Instalación de Locust:
    pip install locust

Ejecución básica en modo web:
    locust -f locust_tests.py --host=http://localhost:8087

Ejecución sin interfaz gráfica:
    locust -f locust_tests.py --host=http://localhost:8087 --headless -u 100 -r 10 -t 5m

Parámetros:
    -f: Archivo con las pruebas
    --host: URL base del servicio
    --headless: Ejecutar sin interfaz web
    -u: Número total de usuarios
    -r: Tasa de spawn (usuarios por segundo)
    -t: Duración total de la prueba
"""

import os
import json
from datetime import datetime
from pathlib import Path


class PerformanceTestReport:
    """Generador de reportes de pruebas de rendimiento"""
    
    def __init__(self, test_name: str):
        self.test_name = test_name
        self.timestamp = datetime.now().isoformat()
        self.results = {}
    
    def add_result(self, endpoint: str, response_time: float, status_code: int, success: bool):
        """Agrega resultado de una prueba"""
        if endpoint not in self.results:
            self.results[endpoint] = {
                "response_times": [],
                "status_codes": [],
                "success_count": 0,
                "failure_count": 0
            }
        
        self.results[endpoint]["response_times"].append(response_time)
        self.results[endpoint]["status_codes"].append(status_code)
        
        if success:
            self.results[endpoint]["success_count"] += 1
        else:
            self.results[endpoint]["failure_count"] += 1
    
    def generate_report(self) -> dict:
        """Genera reporte con estadísticas"""
        report = {
            "test_name": self.test_name,
            "timestamp": self.timestamp,
            "endpoints": {}
        }
        
        for endpoint, data in self.results.items():
            response_times = data["response_times"]
            if response_times:
                report["endpoints"][endpoint] = {
                    "avg_response_time_ms": sum(response_times) / len(response_times),
                    "min_response_time_ms": min(response_times),
                    "max_response_time_ms": max(response_times),
                    "p95_response_time_ms": self._percentile(response_times, 95),
                    "p99_response_time_ms": self._percentile(response_times, 99),
                    "total_requests": len(response_times),
                    "success_count": data["success_count"],
                    "failure_count": data["failure_count"],
                    "success_rate": (data["success_count"] / len(response_times) * 100) if response_times else 0
                }
        
        return report
    
    @staticmethod
    def _percentile(data: list, percentile: int) -> float:
        """Calcula percentil de una lista"""
        if not data:
            return 0
        sorted_data = sorted(data)
        index = int(len(sorted_data) * percentile / 100)
        return sorted_data[min(index, len(sorted_data) - 1)]


class PerformanceAnalyzer:
    """Analiza resultados de pruebas de rendimiento"""
    
    @staticmethod
    def is_acceptable(response_time_ms: float, threshold_ms: float = 1000) -> bool:
        """Verifica si el tiempo de respuesta es aceptable"""
        return response_time_ms < threshold_ms
    
    @staticmethod
    def categorize_performance(avg_response_time: float) -> str:
        """Categoriza el rendimiento"""
        if avg_response_time < 100:
            return "EXCELLENT"
        elif avg_response_time < 300:
            return "GOOD"
        elif avg_response_time < 1000:
            return "ACCEPTABLE"
        elif avg_response_time < 3000:
            return "POOR"
        else:
            return "UNACCEPTABLE"
    
    @staticmethod
    def detect_bottleneck(endpoints_data: dict) -> list:
        """Detecta posibles cuellos de botella"""
        bottlenecks = []
        
        for endpoint, data in endpoints_data.items():
            avg_time = data.get("avg_response_time_ms", 0)
            failure_rate = 100 - data.get("success_rate", 100)
            
            if avg_time > 2000:
                bottlenecks.append({
                    "endpoint": endpoint,
                    "issue": f"High response time: {avg_time:.2f}ms",
                    "severity": "HIGH" if avg_time > 5000 else "MEDIUM"
                })
            
            if failure_rate > 5:
                bottlenecks.append({
                    "endpoint": endpoint,
                    "issue": f"High failure rate: {failure_rate:.2f}%",
                    "severity": "CRITICAL" if failure_rate > 10 else "HIGH"
                })
        
        return bottlenecks


# Configuraciones de pruebas predefinidas
LOAD_TEST_CONFIG = {
    "name": "Load Test",
    "users": 50,
    "spawn_rate": 2,
    "duration_seconds": 300,
    "description": "Prueba de carga básica con 50 usuarios concurrentes"
}

STRESS_TEST_CONFIG = {
    "name": "Stress Test",
    "users": 200,
    "spawn_rate": 5,
    "duration_seconds": 600,
    "description": "Prueba de estrés para encontrar límites del sistema"
}

SPIKE_TEST_CONFIG = {
    "name": "Spike Test",
    "users": 300,
    "spawn_rate": 20,
    "duration_seconds": 900,
    "description": "Prueba de picos para verificar recuperación"
}

ENDURANCE_TEST_CONFIG = {
    "name": "Endurance Test",
    "users": 100,
    "spawn_rate": 2,
    "duration_seconds": 1800,
    "description": "Prueba de resistencia para detectar problemas de larga duración"
}


if __name__ == "__main__":
    # Ejemplo de uso
    report = PerformanceTestReport("Sample Performance Test")
    
    # Simular resultados
    report.add_result("/api/v1/auth/login", 150.5, 200, True)
    report.add_result("/api/v1/auth/login", 120.3, 200, True)
    report.add_result("/api/v1/auth/login", 200.1, 200, True)
    
    report.add_result("/api/v1/identities/map", 80.2, 200, True)
    report.add_result("/api/v1/identities/map", 95.1, 200, True)
    
    # Generar reporte
    final_report = report.generate_report()
    
    # Analizar resultados
    analyzer = PerformanceAnalyzer()
    bottlenecks = analyzer.detect_bottleneck(final_report["endpoints"])
    
    print(json.dumps(final_report, indent=2))
    print("\nBottlenecks detected:")
    for bottleneck in bottlenecks:
        print(f"  - {bottleneck['endpoint']}: {bottleneck['issue']} ({bottleneck['severity']})")
