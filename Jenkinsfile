pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

    environment {
        APP_NAME   = "authentication-service"
        IMAGE_NAME = "authentication-service"
        IMAGE_TAG  = "${BUILD_NUMBER}"
        NAMESPACE  = "default"
        PATH = "/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:${env.PATH}"
        DOCKER_DEFAULT_PLATFORM = "linux/amd64"
    }

    stages {

        stage('Verify Environment') {
            steps {
                sh '''
                    echo "========== SYSTEM INFO =========="
                    uname -a
                    sw_vers

                    echo ""
                    echo "========== JAVA =========="
                    java -version

                    echo ""
                    echo "========== MAVEN =========="
                    mvn -version

                    echo ""
                    echo "========== DOCKER =========="
                    docker --version
                    docker info | grep -A 5 "Server Version"

                    echo ""
                    echo "========== DOCKER COMPOSE =========="
                    docker compose version

                    echo ""
                    echo "========== KUBECTL =========="
                    kubectl version --client

                    echo ""
                    echo "========== KUBERNETES CONTEXT =========="
                    kubectl config current-context
                    kubectl get nodes
                    kubectl get pods --all-namespaces | head -10

                    echo ""
                    echo "========== DOCKER DESKTOP STATUS =========="
                    if docker system info | grep -q "Kubernetes"; then
                        echo "✅ Kubernetes is enabled in Docker Desktop"
                    else
                        echo "⚠️  Kubernetes might not be enabled in Docker Desktop"
                        echo "Enable it in: Docker Desktop → Preferences → Kubernetes"
                    fi
                '''
            }
        }

        stage('Ensure Dependencies Running') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "Checking MySQL container..."
                    echo "=========================================="

                    # Check if mysql container exists and is running
                    if docker ps -a --format '{{.Names}}' | grep -q "^auth_mysql$"; then
                        if docker ps --format '{{.Names}}' | grep -q "^auth_mysql$"; then
                            echo "MySQL is already running"
                        else
                            echo "Starting existing MySQL container..."
                            docker start auth_mysql
                        fi
                    else
                        echo "Creating and starting MySQL container..."
                        docker compose up -d mysql
                        echo "Waiting for MySQL to be ready..."
                        timeout 30 sh -c 'while ! docker exec auth_mysql mysqladmin ping -h localhost --silent 2>/dev/null; do sleep 2; done'
                        echo "MySQL is ready"
                    fi

                    echo ""
                    echo "=========================================="
                    echo "Checking Redis container..."
                    echo "=========================================="

                    if docker ps -a --format '{{.Names}}' | grep -q "^auth_redis$"; then
                        if docker ps --format '{{.Names}}' | grep -q "^auth_redis$"; then
                            echo "Redis is already running"
                        else
                            echo "Starting existing Redis container..."
                            docker start auth_redis
                        fi
                    else
                        echo "Creating and starting Redis container..."
                        docker compose up -d redis
                        echo "Waiting for Redis to be ready..."
                        timeout 20 sh -c 'while ! docker exec auth_redis redis-cli ping 2>/dev/null | grep -q "PONG"; do sleep 2; done'
                        echo "Redis is ready"
                    fi

                    echo ""
                    echo "=========================================="
                    echo "Container Status:"
                    echo "=========================================="
                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
                '''
            }
        }

        stage('Build Spring Boot Application') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "Building Spring Boot Application..."
                    echo "=========================================="

                    # Clean and compile
                    mvn clean compile -DskipTests

                    # Package with tests skipped (will run tests separately)
                    mvn package -DskipTests

                    # Verify JAR was created
                    if [ -f target/*.jar ]; then
                        JAR_SIZE=$(du -h target/*.jar | cut -f1)
                        echo "JAR built successfully (size: ${JAR_SIZE})"
                    else
                        echo "ERROR: JAR file not created!"
                        exit 1
                    fi
                '''
            }
        }

        stage('Run Unit Tests') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "Running Unit Tests..."
                    echo "=========================================="

                    mvn test
                '''
            }
            post {
                always {
                    // Publish test results
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "Building Docker Image..."
                    echo "=========================================="

                    # For Apple Silicon (M1/M2/M3) compatibility
                    # Uncomment if you need x86_64 compatibility
                    # export DOCKER_DEFAULT_PLATFORM=linux/amd64

                    # Build the image using docker-compose
                    docker compose build authentication-service

                    # Tag with build number
                    docker tag ${IMAGE_NAME}:latest ${IMAGE_NAME}:${IMAGE_TAG}
                    docker tag ${IMAGE_NAME}:latest ${IMAGE_NAME}:latest

                    # Verify image was created
                    echo ""
                    echo "Image details:"
                    docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | grep ${IMAGE_NAME}

                    # For Docker Desktop on Mac, images are automatically available to Kubernetes
                    echo ""
                    echo "Image built successfully and available to Kubernetes"
                    echo "   Image: ${IMAGE_NAME}:${IMAGE_TAG}"
                '''
            }
        }

        stage('Prepare Kubernetes Manifest') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "Preparing Kubernetes Deployment..."
                    echo "=========================================="

                    # Check if deployment manifest exists
                    if [ ! -f authentication-deployment.yaml ]; then
                        echo "ERROR: authentication-deployment.yaml not found!"
                        echo "Creating a basic deployment template..."

                        cat > authentication-deployment.yaml <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${APP_NAME}
  namespace: ${NAMESPACE}
  labels:
    app: ${APP_NAME}
spec:
  replicas: 5
  selector:
    matchLabels:
      app: ${APP_NAME}
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: ${APP_NAME}
    spec:
      containers:
      - name: ${APP_NAME}
        image: ${IMAGE_NAME}:${IMAGE_TAG}
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 7079
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "docker"
        - name: MYSQL_HOST
          value: "mysql-service"
        - name: REDIS_HOST
          value: "redis-service"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 7079
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 7079
          initialDelaySeconds: 20
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: ${APP_NAME}
  namespace: ${NAMESPACE}
  labels:
    app: ${APP_NAME}
spec:
  type: LoadBalancer  # For Docker Desktop, this exposes on localhost
  ports:
  - port: 7079
    targetPort: 7079
    name: http
  selector:
    app: ${APP_NAME}
EOF
                        echo "Basic deployment manifest created"
                    else
                        # Update existing manifest with correct image tag
                        echo "Updating existing deployment manifest..."
                        cp authentication-deployment.yaml authentication-deployment.yaml.bak

                        # For Mac (sed works differently than Linux)
                        if [[ "$OSTYPE" == "darwin"* ]]; then
                            sed -i '' "s|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${IMAGE_TAG}|g" authentication-deployment.yaml
                        else
                            sed -i "s|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${IMAGE_TAG}|g" authentication-deployment.yaml
                        fi

                        echo "Deployment manifest updated with image: ${IMAGE_NAME}:${IMAGE_TAG}"
                    fi

                    echo ""
                    echo "Deployment manifest:"
                    echo "-------------------"
                    cat authentication-deployment.yaml
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "Deploying to Kubernetes (Docker Desktop)..."
                    echo "=========================================="

                    # Show current context
                    echo "Current context: $(kubectl config current-context)"

                    # Ensure namespace exists
                    kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                    # For Docker Desktop, we need to ensure the deployment uses our local image
                    # The imagePullPolicy: IfNotPresent ensures it uses local image

                    echo "Applying deployment..."
                    kubectl apply -f authentication-deployment.yaml -n ${NAMESPACE}

                    echo ""
                    echo "Waiting for rollout to complete..."
                    kubectl rollout status deployment/${APP_NAME} -n ${NAMESPACE} --timeout=300s

                    echo ""
                    echo "Deployment successful!"

                    # Get deployment details
                    echo ""
                    echo "Pod status:"
                    kubectl get pods -n ${NAMESPACE} -l app=${APP_NAME}

                    echo ""
                    echo "Service status:"
                    kubectl get svc -n ${NAMESPACE} ${APP_NAME}
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "Verifying Deployment..."
                    echo "=========================================="

                    echo "========== DEPLOYMENTS =========="
                    kubectl get deployments -n ${NAMESPACE}

                    echo ""
                    echo "========== PODS =========="
                    kubectl get pods -n ${NAMESPACE} -l app=${APP_NAME} -o wide

                    echo ""
                    echo "========== SERVICES =========="
                    kubectl get svc -n ${NAMESPACE} -l app=${APP_NAME}

                    echo ""
                    echo "========== CURRENT IMAGE =========="
                    kubectl describe deployment ${APP_NAME} -n ${NAMESPACE} | grep -A 2 "Image:"

                    echo ""
                    echo "========== POD DETAILS =========="
                    POD_NAME=$(kubectl get pods -n ${NAMESPACE} -l app=${APP_NAME} -o jsonpath='{.items[0].metadata.name}')
                    if [ -n "$POD_NAME" ]; then
                        echo "Checking pod: $POD_NAME"
                        echo ""
                        echo "Pod status:"
                        kubectl get pod $POD_NAME -n ${NAMESPACE}

                        echo ""
                        echo "Pod logs (last 20 lines):"
                        kubectl logs -n ${NAMESPACE} $POD_NAME --tail=20

                        echo ""
                        echo "Pod events:"
                        kubectl describe pod $POD_NAME -n ${NAMESPACE} | grep -A 10 "Events:"
                    else
                        echo "No pods found for ${APP_NAME}"
                    fi

                    echo ""
                    echo "========== SERVICE ACCESS =========="
                    # For Docker Desktop, LoadBalancer exposes on localhost
                    SERVICE_PORT=$(kubectl get svc ${APP_NAME} -n ${NAMESPACE} -o jsonpath='{.spec.ports[0].port}')
                    echo "Service available at: http://localhost:${SERVICE_PORT}"

                    # Try to access the service
                    echo ""
                    echo "Testing service accessibility..."
                    curl -s -o /dev/null -w "HTTP Status: %{http_code}\\n" http://localhost:${SERVICE_PORT}/actuator/health || echo "Service not yet available or no health endpoint"
                '''
            }
        }

        stage('Smoke Test') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "Running Smoke Tests..."
                    echo "=========================================="

                    SERVICE_PORT=$(kubectl get svc ${APP_NAME} -n ${NAMESPACE} -o jsonpath='{.spec.ports[0].port}')

                    echo "Testing endpoint at http://localhost:${SERVICE_PORT}"

                    # Wait for service to be ready
                    echo "Waiting for service to be ready..."
                    for i in {1..30}; do
                        if curl -s -o /dev/null -w "%{http_code}" http://localhost:${SERVICE_PORT}/actuator/health | grep -q "200"; then
                            echo "Service is ready!"
                            break
                        fi
                        echo "Waiting... ($i/30)"
                        sleep 2
                    done

                    echo ""
                    echo "Application endpoints:"
                    echo "  - Health: http://localhost:${SERVICE_PORT}/actuator/health"
                    echo "  - Info:   http://localhost:${SERVICE_PORT}/actuator/info"
                    echo "  - App:    http://localhost:${SERVICE_PORT}/api/v1"
                '''
            }
        }
    }

    post {
        success {
            echo "=========================================="
            echo "  DEPLOYMENT SUCCESSFUL!"
            echo "=========================================="
            echo "Application: ${APP_NAME}"
            echo "Version:     ${IMAGE_TAG}"
            echo "Namespace:   ${NAMESPACE}"
            echo ""
            echo "Access the application:"
            echo "  kubectl get svc ${APP_NAME} -n ${NAMESPACE}"
            echo "  http://localhost:8080"
            echo "=========================================="

            // Optional: Send notification to Slack or Teams
            // slackSend(color: 'good', message: "✅ ${APP_NAME} v${IMAGE_TAG} deployed successfully to Docker Desktop Kubernetes")
        }

        failure {
            echo "=========================================="
            echo "  DEPLOYMENT FAILED!"
            echo "=========================================="

            sh '''
                echo "========== DIAGNOSTIC INFORMATION =========="

                echo "1. Kubernetes Resources:"
                kubectl get all -n ${NAMESPACE}

                echo ""
                echo "2. Deployment Status:"
                kubectl describe deployment ${APP_NAME} -n ${NAMESPACE} || echo "Deployment not found"

                echo ""
                echo "3. Pod Status:"
                kubectl get pods -n ${NAMESPACE} -l app=${APP_NAME}
                kubectl describe pods -n ${NAMESPACE} -l app=${APP_NAME} || echo "No pods found"

                echo ""
                echo "4. Recent Events:"
                kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp' | tail -20

                echo ""
                echo "5. Docker Status:"
                docker ps -a

                echo ""
                echo "6. Docker Compose Status:"
                docker compose ps

                echo ""
                echo "7. Docker Images:"
                docker images | grep ${IMAGE_NAME}

                echo ""
                echo "8. Check if Kubernetes is running:"
                kubectl cluster-info
            '''

            // Optional: Send notification
            // slackSend(color: 'danger', message: " ${APP_NAME} deployment failed! Check Jenkins for details.")
        }

        always {
            echo "=========================================="
            echo "Pipeline completed for ${APP_NAME}"
            echo "=========================================="

            // Clean up old images to save disk space
            script {
                if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master') {
                    sh '''
                        echo "Cleaning up old Docker images (> 24h old)..."
                        docker image prune -f --filter "until=24h"
                    '''
                }
            }
        }

        cleanup {
            // Clean up workspace
            deleteDir()
        }
    }
}