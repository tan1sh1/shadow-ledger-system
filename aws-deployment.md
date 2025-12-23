# AWS Deployment Guide

This guide describes how to deploy the Shadow Ledger System on AWS.

## 1. Prerequisites
- AWS account with permissions for ECS, ECR, RDS, IAM, and VPC.
- Docker installed locally for image builds.
- AWS CLI and ECS CLI configured.

## 2. Build and Push Docker Images
- Build images for each service:
  ```bash
  docker build -t <your-ecr-repo>/api-gateway:latest ./api-gateway
  docker build -t <your-ecr-repo>/event-service:latest ./event-service
  docker build -t <your-ecr-repo>/shadow-ledger-service:latest ./shadow-ledger-service
  docker build -t <your-ecr-repo>/drift-correction-service:latest ./drift-correction-service
  ```
- Push images to ECR:
  ```bash
  aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <your-ecr-repo>
  docker push <your-ecr-repo>/api-gateway:latest
  # Repeat for other services
  ```

## 3. Database Setup
- Use Amazon RDS for PostgreSQL or MySQL.
- Create databases for each service as needed.
- Store DB credentials in AWS Secrets Manager.

## 4. ECS Cluster Setup
- Create an ECS cluster (Fargate or EC2 launch type).
- Define task definitions for each service, referencing the ECR images.
- Set environment variables and secrets for each container.
- Configure service discovery if needed.

## 5. Networking
- Set up a VPC with public/private subnets.
- Use an Application Load Balancer (ALB) to route traffic to the API Gateway.
- Open required ports (8080-8083) in security groups.

## 6. Deployment
- Deploy services using ECS Service.
- Monitor health via ECS and CloudWatch.

## 7. Monitoring & Logging
- Enable CloudWatch Logs for all containers.
- Set up CloudWatch Alarms for health and performance.
- Optionally, integrate with AWS X-Ray for tracing.

## 8. Scaling
- Configure auto-scaling policies for ECS services.
- Use ALB health checks for service availability.

---

**Note:** For production, enable HTTPS, use secure secrets management, and restrict security groups appropriately.
