variable "namespace" { type = string }
variable "name" { type = string }
variable "image" { type = string }
variable "port" { type = number }
variable "replicas" { type = number, default = 1 }
variable "env" { type = map(string), default = {} }

resource "kubernetes_deployment" "this" {
  metadata {
    name      = var.name
    namespace = var.namespace
    labels    = { app = var.name }
  }

  spec {
    replicas = var.replicas
    selector { match_labels = { app = var.name } }
    template {
      metadata { labels = { app = var.name } }
      spec {
        container {
          name  = var.name
          image = var.image
          port { container_port = var.port }
          dynamic "env" {
            for_each = var.env
            content { name = env.key, value = env.value }
          }
          liveness_probe { http_get { path = "/actuator/health/liveness", port = var.port } initial_delay_seconds = 40 period_seconds = 20 }
          readiness_probe { http_get { path = "/actuator/health/readiness", port = var.port } initial_delay_seconds = 30 period_seconds = 10 }
        }
      }
    }
  }
}

resource "kubernetes_service" "this" {
  metadata { name = var.name, namespace = var.namespace }
  spec {
    selector = { app = var.name }
    port { port = var.port, target_port = var.port }
    type = "ClusterIP"
  }
}
