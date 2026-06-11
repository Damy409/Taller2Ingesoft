terraform {
  required_version = ">= 1.6.0"
  backend "s3" {
    bucket         = "circleguard-terraform-state-prod"
    key            = "circle-guard/prod/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "circleguard-terraform-locks-prod"
    encrypt        = true
  }
  required_providers {
    kubernetes = { source = "hashicorp/kubernetes", version = "~> 2.30" }
  }
}

provider "kubernetes" {
  config_path = var.kubeconfig_path
}

variable "kubeconfig_path" { type = string, default = "~/.kube/config" }
variable "registry" { type = string, default = "local" }

module "namespace" {
  source = "../../modules/namespace"
  name   = "circleguard-prod"
  labels = { environment = "prod", app = "circleguard" }
}

module "auth_service" {
  source    = "../../modules/microservice"
  namespace = module.namespace.name
  name      = "circleguard-auth-service"
  image     = "${var.registry}/circleguard-auth-service:latest"
  port      = 8180
  replicas  = 3
  env = {
    SPRING_PROFILES_ACTIVE = "prod"
  }
}

module "observability" {
  source    = "../../modules/observability"
  namespace = module.namespace.name
}
