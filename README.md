#  Digest API

Digest is a secure and Dockerized code execution API that compiles and runs code in multiple languages — on demand.

> "Digest your code and poop the output 💩"

---

## ✨ Features

- ✅ Language support: Python, Java, C#, C++
- ✅ Version control for each language
- ✅ Supports stdin, args, and multiple files
- ✅ Docker-based sandboxing for secure execution
- ✅ Clean HTML homepage and documentation
- ✅ Simple REST API design

---

## 🚀 Getting Started

### ✅ Run with Docker (Recommended)

1. **Build the project and Docker image**

```bash
./mvnw clean package -DskipTests
docker build -t digest-api .
```

2. **Run the container**
```bash
docker run -p 8080:8080 digest-api
```

3. Open your browser:

    🌐 Home: http://localhost:8080

    📘 Docs: http://localhost:8080/docs
    

### Run Locally

```bash
./mvnw spring-boot:run
```


## 📦 API Endpoints

### ▶️ `POST /execute`

Executes user-provided code in an isolated Docker container.

#### Request

```json
{
  "language": "python",
  "version": "3.11",
  "stdin": "World",
  "args": [],
  "files": [
    {
      "name": "main.py",
      "content": "print('Hello ' + input())"
    }
  ]
}
```

#### Response

```json
{
  "stdout": "Hello World\n",
  "stderr": "",
  "exitCode": 0
}
```


### 🧠 `GET /runtimes`

Returns supported languages and available versions.

#### Example

```json
{
  "python": ["3.11", "3.10"],
  "java": ["21", "17"],
  "csharp": ["8.0"],
  "cpp": ["13.2"]
}
```
## 🛡️ Security

- Code runs inside isolated Docker containers
- Temporary directories cleaned after execution
- Suggested extensions:
  - Rate limiting
  - Timeouts/memory limits





