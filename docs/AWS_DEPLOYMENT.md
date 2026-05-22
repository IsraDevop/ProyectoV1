# AWS Deployment Guide

This backend is ready to run in AWS with ECS or EC2 plus RDS PostgreSQL. The repository does not include real secrets; configure them as AWS Secrets Manager values, ECS task environment variables, or EC2 instance environment variables.

## Target Architecture

- **Compute:** ECS Fargate service or EC2 instance running the Docker image.
- **Database:** Amazon RDS PostgreSQL 15+.
- **Networking:** one VPC, public load balancer for the API, private subnets for RDS where possible.
- **Storage:** Amazon S3 bucket for collectible images.
- **Email:** Resend SMTP via `spring.mail`.
- **Payments:** Stripe sandbox or production keys through environment variables.

## Required Environment Variables

| Variable | Production value |
| --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://<rds-endpoint>:5432/yala_db` |
| `DB_USER` | RDS username |
| `DB_PASS` | RDS password |
| `JWT_SECRET` | random secret, minimum 32 characters |
| `CCI_ENCRYPTION_KEY` | random secret, minimum 32 characters, different from JWT when possible |
| `STRIPE_SECRET_KEY` | Stripe secret key |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |
| `AWS_REGION` | AWS region, for example `us-east-1` |
| `AWS_S3_BUCKET` | S3 bucket name for listing images |
| `AWS_S3_PUBLIC_URL_BASE` | public S3 or CloudFront base URL |
| `RESEND_API_KEY` | Resend API key used as SMTP password |
| `MAIL_HOST` | `smtp.resend.com` |
| `MAIL_PORT` | `587` |
| `MAIL_USERNAME` | `resend` |

## Security Groups

1. API service security group:
   - inbound `80` or `443` from the internet/load balancer.
   - inbound `8081` only from the load balancer if using ALB.
   - outbound PostgreSQL `5432` to the RDS security group.
2. RDS security group:
   - inbound `5432` only from the API service security group.
   - no public inbound access.

## ECS Fargate Checklist

1. Build and push the image to ECR:
   ```bash
   aws ecr create-repository --repository-name yala-api
   docker build -t yala-api .
   docker tag yala-api:latest <account-id>.dkr.ecr.<region>.amazonaws.com/yala-api:latest
   docker push <account-id>.dkr.ecr.<region>.amazonaws.com/yala-api:latest
   ```
2. Create an RDS PostgreSQL instance and database `yala_db`.
3. Create an S3 bucket for images and allow the task or EC2 instance role to run `s3:PutObject`.
4. Create an ECS task definition with container port `8081`.
5. Add all environment variables/secrets to the task definition.
6. Create an ECS service behind an Application Load Balancer.
7. Configure Stripe webhook URL:
   `https://<public-api-domain>/api/v1/payments/webhook`.
8. Validate:
   - `GET https://<public-api-domain>/swagger-ui.html`
   - `POST /api/v1/auth/register`
   - `GET /api/v1/listings`

## EC2 Alternative

Install Docker and Docker Compose on the instance, copy `.env` with production values, then run:

```bash
docker compose up --build -d yala-api
```

For EC2, prefer using RDS instead of the local `postgres` service. Set `DATABASE_URL` to the RDS endpoint and remove or ignore the compose `postgres` service in production.

Use an EC2 instance profile with S3 permissions instead of storing AWS access keys on the server.
