resource "aws_db_subnet_group" "this" {
  name       = "${var.name}-db-subnets"
  subnet_ids = var.private_subnet_ids
  tags       = { Name = "${var.name}-db-subnets" }
}

resource "aws_security_group" "db" {
  name        = "${var.name}-db-sg"
  description = "Allow Postgres only from the EKS node security group"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Postgres from EKS nodes"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.eks_node_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.name}-db-sg" }
}

resource "random_password" "db" {
  length           = 24
  special          = true
  override_special = "!#$%^&*()-_=+"
}

# The generated password is written to Secrets Manager, never to Terraform state output
# or a .tfvars file. Read it with: aws secretsmanager get-secret-value --secret-id <name>
resource "aws_secretsmanager_secret" "db" {
  name = "${var.name}-db-credentials"
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id
  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db.result
    dbname   = var.db_name
    host     = aws_db_instance.this.address
    port     = 5432
  })
}

resource "aws_db_instance" "this" {
  identifier     = "${var.name}-db"
  engine         = "postgres"
  engine_version = "17.2"
  instance_class = var.instance_class

  allocated_storage     = var.allocated_storage_gb
  max_allocated_storage = var.max_allocated_storage_gb
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.db_name
  username = var.db_username
  password = random_password.db.result

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.db.id]
  multi_az                = var.multi_az
  publicly_accessible      = false
  deletion_protection      = var.deletion_protection
  backup_retention_period  = var.backup_retention_days
  skip_final_snapshot      = !var.deletion_protection
  final_snapshot_identifier = var.deletion_protection ? "${var.name}-db-final" : null

  # PostGIS ships as an extension, enabled via `CREATE EXTENSION postgis;`
  # in the V1 Flyway migration — no special parameter group needed on modern RDS Postgres.

  tags = { Name = "${var.name}-db" }
}
