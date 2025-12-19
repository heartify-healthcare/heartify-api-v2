Here is the updated `README.md` for **Heartify API v2**.

I have revised the **Architecture**, **AI Integration**, and **Related Repositories** sections to accurately reflect the use of your two distinct Python AI microservices (`heartify-denoised-model` and `heartify-dl-model`) and their specific architectures (Attention U-Net and Wav2Vec2/ECG-FM), replacing the outdated v1 information.

---

# Heartify Healthcare Platform API (v2)

**A Microservices-based AIoT Backend for Cardiovascular Disease Prediction & Monitoring**

This repository contains the **version 2 (Java Spring Boot Microservices)** backend of the Heartify ecosystem. It serves as the core orchestration layer for the Heartify mobile application, managing user identities, processing ECG data from IoT devices, and coordinating a pipeline of specialized AI services for heart health analysis.

## 📚 Academic Context

This project is an evolution of a university **capstone project** developed under the topic:

> **"Heart disease risk prediction using ECG signals with deep learning and large language models."**

While v1 was a monolithic Flask application, **v2** re-architects the system into a scalable microservices infrastructure. It integrates State-of-the-Art (SOTA) deep learning models—moving from simple CNN-LSTMs to **Foundation Models (Wav2Vec2)** and **Attention U-Nets**—and adds Generative AI for patient-friendly explanations.

## 🏗️ Architecture

The system is built using **Java 21** and **Spring Boot 3.5.8**, employing a microservices architecture managed by **Spring Cloud**. The `ai-service` acts as a central orchestrator, delegating complex signal processing tasks to specialized Python/Flask microservices.

### Microservices Overview

| Service | Tech Stack | Description |
| --- | --- | --- |
| **Config Server** | Spring Cloud Config | Centralized configuration management for all microservices. |
| **Eureka Server** | Netflix Eureka | Service discovery registry. |
| **API Gateway** | Spring Cloud Gateway | Entry point for mobile app requests; handles routing and security. |
| **User Service** | Spring Boot, PostgreSQL | Manages authentication (JWT), user profiles, and OTP verification via Email. |
| **AI Service** | Spring Boot, MongoDB, Qdrant | **The Brain.** Orchestrates the AI pipeline: calls the Denoising API, then the Prediction API, and finally Google Gemini (LLM) with RAG for medical explanations. |

### 🧠 The AI Pipeline

The system utilizes a multi-stage AI workflow:

1. **Preprocessing (Denoising):**
* **Service:** `heartify-denoised-model` (Python/Flask)
* **Model:** **Attention U-Net**
* **Function:** Removes noise and artifacts from raw 130Hz ECG signals (from devices like Polar H10) to ensure high-quality input for analysis.


2. **Analysis (Prediction):**
* **Service:** `heartify-dl-model` (Python/Flask)
* **Model:** **ECG Foundation Model (ECG-FM)** based on **Wav2Vec2 CMSC-RLM** architecture.
* **Function:** Classifies the cleaned signal into 12 categories (e.g., AFIB, Normal, Bradycardia, LBBB/RBBB).
* **Feature Extraction:** Calculates physiological metrics (HR, HRV, QRS duration) and generates visualization images.


3. **Interpretation (GenAI):**
* **Service:** `ai-service` (Java/Spring)
* **Model:** **Google Gemini 1.5 Flash** (Multimodal)
* **RAG:** Uses **Qdrant** vector database to retrieve medical guidelines (ACC/AHA/HRS) to ground the LLM's response, providing patients with accurate, easy-to-understand explanations in Vietnamese.



## 🛠️ Technologies Used

* **Core:** Java 21, Maven 3.9.11
* **Frameworks:** Spring Boot 3.5.8, Spring Cloud 2025.0.0
* **Databases:**
  * **PostgreSQL 15:** User identity and relational data.
  * **MongoDB 7:** Unstructured data (ECG sessions, prediction logs).
  * **Qdrant:** Vector search engine for RAG.


* **AI & Data Science (External Services):**
  * **Python 3.10+**, **Flask**
  * **PyTorch**, **Fairseq**
  * **NumPy**, **SciPy**


* **Infrastructure:** Docker, Docker Compose.

## 🚀 Getting Started

### Prerequisites

* Java Development Kit (JDK) 21
* Maven 3.9+
* Docker & Docker Compose
* Git

### 1. Clone the Repository

```bash
git clone https://github.com/heartify-healthcare/heartify-api-v2
cd heartify-api-v2
# Initialize submodules (essential for the config-repo)
git submodule update --init --recursive
```

### 2. Start Infrastructure (Databases)

Use Docker Compose to spin up PostgreSQL, MongoDB, and Qdrant.

```bash
docker-compose up -d
```

### 3. Run the Microservices

The services must be started in the following specific order to ensure dependencies (config, discovery) are met.

Open separate terminal tabs or use a script to run:

**1. Config Server** (Must be first)

```bash
cd config-server
mvn clean compile
mvn spring-boot:run
```

**2. Eureka Server**

```bash
cd eureka-server
mvn clean compile
mvn spring-boot:run
```

**3. API Gateway**

```bash
cd api-gateway
mvn clean compile
mvn spring-boot:run
```

**4. User Service**

```bash
cd user-service
mvn clean compile
mvn spring-boot:run
```

**5. AI Service**

```bash
cd ai-service
mvn clean compile
mvn spring-boot:run
```

> **Note:** The `AI Service` requires the Python backend services (`heartify-dl-model` and `heartify-denoised-model`) to be running separately to perform predictions. Ensure you configure their URLs in your configuration.

## 🔗 Related Repositories

* **Mobile App:** [heartify-healthcare/heartify-v2](https://github.com/heartify-healthcare/heartify-v2) (React Native)
* **ECG Prediction Model API:** [heartify-healthcare/heartify-dl-model](https://github.com/heartify-healthcare/heartify-dl-model) (Flask, PyTorch, Wav2Vec2)
* **ECG Denoising Model API:** [heartify-healthcare/heartify-denoised-model](https://github.com/heartify-healthcare/heartify-denoised-model) (Flask, PyTorch, Attention U-Net)

## ✍️ Authors

* **Vo Tran Phi** ([votranphi](https://github.com/votranphi))
* **Le Duong Minh Phuc** ([minhphuc2544](https://github.com/minhphuc2544))

## 📄 License

This project is available under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) license.