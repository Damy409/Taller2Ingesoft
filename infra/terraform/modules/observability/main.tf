variable "namespace" { type = string }

resource "kubernetes_config_map" "prometheus_rules" {
  metadata { name = "circleguard-alert-rules", namespace = var.namespace }
  data = {
    "alerts.yml" = <<-YAML
    groups:
    - name: circleguard
      rules:
      - alert: HighErrorRate
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) > 1
        for: 2m
        labels: { severity: critical }
        annotations: { summary: "High 5xx rate in CircleGuard" }
    YAML
  }
}
