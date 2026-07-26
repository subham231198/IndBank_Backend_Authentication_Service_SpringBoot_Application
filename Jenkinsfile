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
        APP_PORT   = "7079"
        HOSTNAME   = "app.indbank.security.auth"
        PATH = "/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:${env.PATH}"
    }

    stages {

        stage('Verify Environment') {
            steps {
                sh '''
                    echo "========== SYSTEM INFO =========="
                    uname -a
                    sw_vers

                    echo "========== JAVA =========="
                    java -version

                    echo "========== MAVEN =========="
                    mvn -version

                    echo "========== DOCKER =========="
                    docker --version

                    echo "========== DOCKER COMPOSE =========="
                    docker compose version

                    echo "========== KUBECTL =========="
                    kubectl version --client

                    echo "========== KUBERNETES CONTEXT =========="
                    kubectl config current-context
                    kubectl get nodes
                '''
            }
        }

        stage('Ensure MySQL Running') {
            steps {
                sh '''
                    echo "Checking MySQL container..."
                    if docker ps -a --format '{{.Names}}' | grep -q "^auth_mysql$"; then
                        if docker ps --format '{{.Names}}' | grep -q "^auth_mysql$"; then
                            echo "MySQL container is already running"
                        else
                            echo "Starting MySQL container..."
                            docker start auth_mysql
                        fi
                    else
                        echo "Creating and starting MySQL container..."
                        docker compose up -d mysql
                        echo "Waiting for MySQL to be ready..."
                        timeout 60 sh -c 'while ! docker exec auth_mysql mysqladmin ping -h localhost -u root -pSM231198 --silent 2>/dev/null; do sleep 2; done'
                        echo "MySQL is ready"
                    fi

                    echo "Container Status:"
                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
                '''
            }
        }

        stage('Deploy MySQL to Kubernetes') {
            steps {
                sh '''
                    echo "Checking if MySQL deployment exists in Kubernetes..."

                    if kubectl get deployment mysql &>/dev/null; then
                        echo "MySQL deployment already exists, checking status..."
                        kubectl rollout status deployment/mysql --timeout=60s
                        echo "MySQL is already running in Kubernetes"
                    else
                        echo "Creating MySQL deployment and service..."
                        cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
  labels:
    app: mysql
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        env:
        - name: MYSQL_ROOT_PASSWORD
          value: "SM231198"
        - name: MYSQL_DATABASE
          value: "AuthenticationDB"
        - name: MYSQL_ROOT_HOST
          value: "%"
        ports:
        - containerPort: 3306
        args:
        - --default-authentication-plugin=mysql_native_password
        - --character-set-server=utf8mb4
        - --collation-server=utf8mb4_unicode_ci
---
apiVersion: v1
kind: Service
metadata:
  name: mysql-service
spec:
  selector:
    app: mysql
  ports:
  - port: 3306
    targetPort: 3306
EOF
                        echo "Waiting for MySQL to be ready..."
                        kubectl rollout status deployment/mysql --timeout=120s
                        echo "MySQL deployment completed"
                    fi

                    echo ""
                    echo "Verifying Kubernetes services..."
                    kubectl get svc mysql-service
                    kubectl get pods -l app=mysql
                '''
            }
        }

        stage('Build Spring Boot Application') {
            steps {
                sh '''
                    echo "Building Spring Boot application..."
                    mvn clean compile -DskipTests
                    mvn package -DskipTests

                    if [ -f target/*.jar ]; then
                        echo "JAR built successfully"
                        ls -lh target/*.jar
                    else
                        echo "ERROR: JAR file not created"
                        exit 1
                    fi
                '''
            }
        }

        stage('Run Unit Tests') {
            steps {
                sh '''
                    echo "Running unit tests..."
                    mvn test
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    echo "Building Docker image..."
                    docker build -t ${IMAGE_NAME}:latest .
                    docker tag ${IMAGE_NAME}:latest ${IMAGE_NAME}:${IMAGE_TAG}

                    echo "Image details:"
                    docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | grep ${IMAGE_NAME}

                    echo "Image built successfully: ${IMAGE_NAME}:${IMAGE_TAG}"
                '''
            }
        }

        stage('Install Ingress Controller') {
            steps {
                sh '''
                    echo "Checking if Ingress Controller is installed..."
                    if ! kubectl get namespace ingress-nginx &>/dev/null; then
                        echo "Installing NGINX Ingress Controller..."
                        kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml
                        echo "Waiting for Ingress Controller to be ready..."
                        kubectl wait --namespace ingress-nginx \
                          --for=condition=ready pod \
                          --selector=app.kubernetes.io/component=controller \
                          --timeout=120s
                    else
                        echo "Ingress Controller already installed"
                    fi
                '''
            }
        }

        stage('Deploy Application to Kubernetes') {
            steps {
                sh '''
                    echo "Deploying application to Kubernetes..."

                    kubectl delete deployment ${APP_NAME} --ignore-not-found=true
                    kubectl delete service ${APP_NAME} --ignore-not-found=true
                    kubectl delete ingress ${APP_NAME}-ingress --ignore-not-found=true

                    sleep 3

                    echo "Verifying image exists locally..."
                    if ! docker images --format '{{.Repository}}:{{.Tag}}' | grep -q "^${IMAGE_NAME}:latest$"; then
                        echo "Image not found, building..."
                        docker build -t ${IMAGE_NAME}:latest .
                        docker tag ${IMAGE_NAME}:latest ${IMAGE_NAME}:${IMAGE_TAG}
                    else
                        echo "Image ${IMAGE_NAME}:latest exists"
                        docker tag ${IMAGE_NAME}:latest ${IMAGE_NAME}:${IMAGE_TAG} 2>/dev/null || true
                    fi

                    echo "Creating application deployment..."
                    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${APP_NAME}
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
        image: ${IMAGE_NAME}:latest
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: ${APP_PORT}
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:mysql://mysql-service:3306/AuthenticationDB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        - name: SPRING_DATASOURCE_USERNAME
          value: "root"
        - name: SPRING_DATASOURCE_PASSWORD
          value: "SM231198"
        - name: SPRING_JPA_HIBERNATE_DDL_AUTO
          value: "update"
        - name: SPRING_JPA_SHOW_SQL
          value: "true"
        - name: SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT
          value: "60000"
        - name: SERVER_PORT
          value: "${APP_PORT}"
        - name: SPRING_AUTOCONFIGURE_EXCLUDE
          value: "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.session.SessionAutoConfiguration"
        - name: SPRING_DATA_REDIS_REPOSITORIES_ENABLED
          value: "false"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          tcpSocket:
            port: ${APP_PORT}
          initialDelaySeconds: 120
          periodSeconds: 10
          failureThreshold: 10
        readinessProbe:
          tcpSocket:
            port: ${APP_PORT}
          initialDelaySeconds: 90
          periodSeconds: 5
          failureThreshold: 10
---
apiVersion: v1
kind: Service
metadata:
  name: ${APP_NAME}
  labels:
    app: ${APP_NAME}
spec:
  type: ClusterIP
  ports:
  - port: ${APP_PORT}
    targetPort: ${APP_PORT}
    name: http
  selector:
    app: ${APP_NAME}
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ${APP_NAME}-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  ingressClassName: nginx
  rules:
  - host: ${HOSTNAME}
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: ${APP_NAME}
            port:
              number: ${APP_PORT}
EOF

                    echo "Waiting for rollout to complete..."
                    kubectl rollout status deployment/${APP_NAME} --timeout=300s

                    echo "Deployment Status:"
                    kubectl get pods -l app=${APP_NAME}
                    kubectl get svc ${APP_NAME}
                    kubectl get ingress

                    echo "Application deployment successful!"
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "Verifying deployment..."

                    POD_NAME=$(kubectl get pods -l app=${APP_NAME} -o jsonpath='{.items[0].metadata.name}')

                    if [ -n "$POD_NAME" ]; then
                        echo "Pod Status:"
                        kubectl get pod $POD_NAME

                        echo "Pod Logs:"
                        kubectl logs $POD_NAME --tail=50

                        echo "Service Details:"
                        kubectl get svc ${APP_NAME}

                        echo "Ingress Details:"
                        kubectl get ingress ${APP_NAME}-ingress
                    else
                        echo "No pods found for ${APP_NAME}"
                        exit 1
                    fi
                '''
            }
        }

        stage('Smoke Test') {
            steps {
                sh '''
                    echo "Running smoke tests..."

                    echo "Checking pod status..."
                    RUNNING_PODS=$(kubectl get pods -l app=${APP_NAME} --field-selector=status.phase=Running -o name | wc -l | tr -d ' ')

                    if [ "$RUNNING_PODS" = "5" ]; then
                        echo "All 5 pods are running successfully!"
                        echo "Application is deployed and running."

                        echo ""
                        echo "Pod details:"
                        kubectl get pods -l app=${APP_NAME} -o wide

                        echo ""
                        echo "Service details:"
                        kubectl get svc ${APP_NAME}

                        echo ""
                        echo "Ingress details:"
                        kubectl get ingress ${APP_NAME}-ingress

                        echo ""
                        echo "Application URL: http://${HOSTNAME}"
                        echo ""
                        echo "Smoke tests passed!"
                    else
                        echo "ERROR: Not all pods are running"
                        echo "Expected: 5, Running: $RUNNING_PODS"
                        kubectl get pods -l app=${APP_NAME}
                        exit 1
                    fi
                '''
            }
        }
    }

    post {
        success {
            echo "=========================================="
            echo "Deployment Successful!"
            echo "=========================================="
            echo "Application: ${APP_NAME}"
            echo "Version: ${IMAGE_TAG}"
            echo "Port: ${APP_PORT}"
            echo ""
            echo "Access the application:"
            echo "  http://${HOSTNAME}"
            echo ""
            echo "=========================================="
            echo "Configuring /etc/hosts..."
            echo "=========================================="

            sh '''
                echo "Checking /etc/hosts entry..."

                if grep -q "${HOSTNAME}" /etc/hosts; then
                    echo "Hostname ${HOSTNAME} already exists in /etc/hosts"
                else
                    echo "Adding ${HOSTNAME} to /etc/hosts..."
                    sudo sh -c 'echo "127.0.0.1 ${HOSTNAME}" >> /etc/hosts'
                    echo "Hostname added successfully!"
                fi

                echo ""
                echo "Verifying entry:"
                grep "${HOSTNAME}" /etc/hosts

                echo ""
                echo "Application URL: http://${HOSTNAME}"
                echo ""
                echo "=========================================="
                echo "Postman Configuration:"
                echo "  Base URL: http://${HOSTNAME}"
                echo "=========================================="
            '''
        }

        failure {
            echo "=========================================="
            echo "Deployment Failed!"
            echo "=========================================="

            sh '''
                echo "Diagnostic Information:"
                echo "Kubernetes Resources:"
                kubectl get all

                echo "Deployment Status:"
                kubectl describe deployment ${APP_NAME} || echo "Deployment not found"

                echo "Pod Status:"
                kubectl get pods -l app=${APP_NAME}
                kubectl describe pods -l app=${APP_NAME} || echo "No pods found"

                echo "MySQL Status:"
                kubectl get pods -l app=mysql
                kubectl describe pods -l app=mysql || echo "MySQL not found"

                echo "Ingress Status:"
                kubectl describe ingress ${APP_NAME}-ingress || echo "Ingress not found"

                echo "Recent Events:"
                kubectl get events --sort-by='.lastTimestamp' | tail -20

                echo "Docker Status:"
                docker ps -a
                docker images | grep ${IMAGE_NAME}

                echo "Docker Compose Status:"
                docker compose ps 2>/dev/null || echo "No docker-compose services"
            '''
        }

        always {
            echo "Pipeline finished"

            script {
                if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master') {
                    sh '''
                        echo "Cleaning up old Docker images..."
                        docker image prune -f --filter "until=24h" || true
                    '''
                }
            }
        }

        cleanup {
            deleteDir()

            sh '''
                kubectl delete pod test-curl --ignore-not-found=true 2>/dev/null || true
            '''
        }
    }
}