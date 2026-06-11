terraform {
  required_version = ">= 1.6.0"
  backend "s3" {
    bucket         = "circleguard-terraform-state-stage"
    key            = "circle-guard/stage/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "circleguard-terraform-locks-stage"
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
  name   = "circleguard-stage"
  labels = { environment = "stage", app = "circleguard" }
}

module "auth_service" {
  source    = "../../modules/microservice"
  namespace = module.namespace.name
  name      = "circleguard-auth-service"
  image     = "${var.registry}/circleguard-auth-service:latest"
  port      = 8180
  replicas  = 2
  env = {
    SPRING_PROFILES_ACTIVE = "stage"
  }
}

module "observability" {
  source    = "../../modules/observability"
  namespace = module.namespace.name
}
