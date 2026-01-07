# Utilix – Reactive Utility Billing System

**Frontend deployment:**  
[Utilix](https://utilixxxx.netlify.app/home)

**Tech Stack:**  
Java 17 | Spring Boot 3.4.1 | WebFlux | Docker | Kafka | MongoDB | Angular 17 | Jenkins

---

## Overview
Utilix is a fully reactive, event-driven, and secure full-stack ecosystem designed to manage modern urban infrastructure (Electricity, Water, Gas, and Internet).  

This project **implemented** Spring Boot WebFlux for high-throughput non-blocking I/O, Apache Kafka for asynchronous notifications, and a Jenkins CI/CD pipeline for automated delivery.  

All backend services were independently deployable, communicated via Reactive MongoDB, and were accessed through a unified Spring Cloud Gateway.

---

## Utilix Dashboard
Below image is the **Home Page** of the Utilix application.<br>
For remaining UI screens, refer **`Utilix Outputs.pdf`** included in this repository.

<div align="center">
  <img 
    alt="Home Page"
    src="https://github.com/user-attachments/assets/2ac17dd3-4e9b-4abc-85e0-3a3b6fd6b728" />
</div>

---

## System Overview
The ecosystem consists of **11 core components**: 10 backend microservices + 1 frontend application.

Each service was:

- **Containerized:** Orchestrated via Docker Compose.  
- **Reactive:** Built on Project Reactor (Mono, Flux).  
- **Service Discovery:** Registered with Netflix Eureka.  
- **Centralized Config:** Managed via Spring Cloud Config Server.  
- **Automated:** Integrated with a Jenkins pipeline for CI/CD.  

---

## Technical Architecture

### 1. Frontend Application (Angular Client `:4200`)
- **Responsibility:** Multi-role dashboard for Admin, Officers, and Consumers.  
- **Tech:** Angular 17, RxJS, Bootstrap 5.  
- **Key Feature:** Self-Healing Profile Pattern — detected skeleton profiles and **implemented** a "Silent Sync" to update data from Auth session storage automatically.  

### 2. API Gateway (`:8080`)
- **Responsibility:** Entry point for all traffic.  
- **Tech:** Spring Cloud Gateway.  
- **Features:** JWT validation, dynamic routing via Eureka, and CORS configuration.  

### 3. Auth Service (`:8081`)
- **Responsibility:** Security, RBAC, and Identity.  
- **Tech:** Spring Security Reactive, JWT, BCrypt.  
- **DB:** utility_auth_db.  

### 4. Consumer Service (`:8082`)
- **Responsibility:** Profile management and utility connection requests.  
- **Tech:** Spring WebFlux, Reactive MongoDB.  
- **Feature:** **Implemented** automated governance for utility connections.

### 5. Meter Service (`:8083`)
- **Responsibility:** Manages physical meter data and usage readings.  
- **Tech:** Spring WebFlux, Reactive MongoDB.  
- **Features:** Implemented validation of meter connection status, processing of monthly meter readings submitted by Billing Officers, triggers the Billing Service workflow upon successful reading submission..  

### 6. Billing Service (`:8084`)
- **Responsibility:** The core calculation engine for utility costs.  
- **Tech:** Spring WebFlux, Reactive MongoDB.  
- **Feature:** Dynamically calculates bill amounts based on consumption units and tariff categories.

### 7. Payment Service (`:8085`)
- **Responsibility:** Handles financial transactions and revenue reporting.  
- **Tech:** Spring WebFlux, Reactive MongoDB.  
- **Feature:** Processing of bill payments via Transaction ID generation (TXN-...).

### 8. Messaging & Infrastructure
- **Kafka (`:9092`):** Handled "Connection Approved" email notifications.  
- **Eureka (`:8761`):** Service Discovery.  
- **Config Server (`:8888`):** Centralized properties management.  

---

## CI/CD Pipeline (Jenkins `:9090`)
Implemented Jenkins pipeline and optimized for Docker.

### Pipeline Stages
- **Checkout:** Pulled source code from GitHub.  
- **Build Backend:** Navigated to `/backend/` and ran Maven build.  
- **Verify JAR:** Validated successful artifact generation in sub-module target folders.  
- **Docker Build:** Built ARM64-compatible Docker images for each service.  
- **Cleanup:** Pruned dangling images to optimize local storage.  

---

## Business Rules & Logic
- **RBAC Governance:** Only Admins or Billing Officers could assign meter numbers and activate pending connection requests.  
- **Dynamic Billing:** **Implemented** a Slab-Rate Engine for consumption-based cost calculation.  
- **Payment Integrity:** Bill status remained `UNPAID` until a unique Transaction ID was generated and synced.  
- **Data Uniqueness:** Every utility connection was uniquely mapped to one active Meter Number.  
- **Automated Security:** Account status `INACTIVE` in the Auth Service immediately revoked all system access.  

---

## Architrecture Diagram
<div align="center">
  <img 
    alt="Architrecture Diagram"
    src="https://github.com/user-attachments/assets/56575b92-cab7-492d-acf2-6d0f75d42fb0" />
</div>
