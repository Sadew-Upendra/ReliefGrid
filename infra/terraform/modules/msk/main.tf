# MSK Serverless: no broker sizing/scaling to manage, billed per throughput —
# the right fit for a project at ReliefGrid's current traffic level. Switch to
# provisioned MSK (aws_msk_cluster) later if steady, predictable throughput
# makes it cheaper.

resource "aws_security_group" "msk" {
  name        = "${var.name}-msk-sg"
  description = "Allow Kafka (TLS) only from the EKS node security group"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Kafka TLS from EKS nodes"
    from_port       = 9098
    to_port         = 9098
    protocol        = "tcp"
    security_groups = [var.eks_node_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.name}-msk-sg" }
}

resource "aws_msk_serverless_cluster" "this" {
  cluster_name = "${var.name}-kafka"

  vpc_config {
    subnet_ids         = var.private_subnet_ids
    security_group_ids = [aws_security_group.msk.id]
  }

  client_authentication {
    sasl {
      iam {
        enabled = true
      }
    }
  }

  tags = { Name = "${var.name}-kafka" }
}
