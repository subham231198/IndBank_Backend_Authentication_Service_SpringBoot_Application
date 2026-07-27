pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

    environment {
        APP_NAME = "authentication-service"
        IMAGE_NAME = "authentication-service"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        VERSION_LABEL = "v${env.BUILD_NUMBER}"
        APP_PORT = "7079"
        HOSTNAME = "app.indbank.security.auth"
        K8S_YAML = "${WORKSPACE}/authentication-deployment.yaml"
    }

    stages {
        stage('Verify Tools') {
            steps {
                sh '''
                    echo "Verifying tools..."
                    java -version
                    mvn -version
                    docker --version
                    kubectl version --client
                    echo "Workspace: ${WORKSPACE}"
                    echo "YAML file path: ${K8S_YAML}"
                    ls -la ${K8S_YAML} || echo "YAML file not found"
                '''
            }
        }

        stage('Build & Test') {
            steps {
                sh '''
                    echo "Building application..."
                    mvn clean test
                    mvn package -DskipTests
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
                    echo "Building Docker image with tag: ${IMAGE_TAG}"

                    # Build with version tag
                    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .

                    # Tag as latest
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest

                    echo "Image built successfully"
                    docker images | grep ${IMAGE_NAME}
                '''
            }
        }

        stage('Deploy MySQL') {
            steps {
                sh '''
                    echo "Checking if MySQL is already deployed..."

                    if kubectl get deployment mysql &>/dev/null; then
                        echo "MySQL deployment already exists, skipping creation"
                        kubectl rollout status deployment/mysql --timeout=30s
                    else
                        echo "MySQL deployment not found, creating..."

                        # Create MySQL deployment
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
                    fi

                    echo "MySQL Service Status:"
                    kubectl get svc mysql-service
                    kubectl get pods -l app=mysql
                '''
            }
        }

        stage('Deploy Application') {
            steps {
                sh '''
                    echo "Deploying application with version: ${VERSION_LABEL}"

                    # Verify YAML file exists
                    if [ ! -f "${K8S_YAML}" ]; then
                        echo "ERROR: YAML file not found at ${K8S_YAML}"
                        ls -la ${WORKSPACE}/
                        exit 1
                    fi

                    echo "Using YAML file: ${K8S_YAML}"

                    # Ensure we use latest image (YAML already has :latest)
                    echo "Using image: ${IMAGE_NAME}:latest"

                    # Apply the YAML
                    kubectl apply -f ${K8S_YAML}

                    # Force restart to pick up latest image
                    kubectl rollout restart deployment/${APP_NAME}

                    # Wait for deployment
                    kubectl rollout status deployment/${APP_NAME} --timeout=300s

                    echo "Deployment completed"
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "Verifying deployment..."

                    # Get pod details
                    POD_NAME=$(kubectl get pods -l app=${APP_NAME} -o jsonpath='{.items[0].metadata.name}')

                    if [ -z "$POD_NAME" ]; then
                        echo "ERROR: No pods found for application"
                        kubectl get pods -l app=${APP_NAME}
                        exit 1
                    fi

                    echo "Pod Name: $POD_NAME"
                    echo ""

                    # Check the image being used
                    echo "Image being used:"
                    kubectl describe pod $POD_NAME | grep "Image:"
                    echo ""

                    # Check imagePullPolicy
                    echo "Image Pull Policy:"
                    kubectl get pod $POD_NAME -o jsonpath='{.spec.containers[0].imagePullPolicy}'
                    echo ""
                    echo ""

                    # Show pod status
                    kubectl get pods -l app=${APP_NAME}
                    kubectl get svc ${APP_NAME}
                    kubectl get ingress ${APP_NAME}-ingress

                    # Show logs
                    echo ""
                    echo "Recent logs:"
                    kubectl logs $POD_NAME --tail=20
                '''
            }
        }

        stage('Smoke Test') {
            steps {
                sh '''
                    echo "Running smoke tests..."
                    RUNNING_PODS=$(kubectl get pods -l app=${APP_NAME} --field-selector=status.phase=Running -o name | wc -l | tr -d ' ')

                    if [ "$RUNNING_PODS" = "1" ]; then
                        echo "Application is running successfully"
                        echo "URL: http://${HOSTNAME}"

                        # Optional: Test the endpoint
                        echo "Testing application endpoint..."
                        curl -s -o /dev/null -w "HTTP Status: %{http_code}\\n" http://${HOSTNAME}/actuator/health 2>/dev/null || echo "Health endpoint not available (might be normal)"
                    else
                        echo "Application is not running properly"
                        kubectl get pods -l app=${APP_NAME}
                        exit 1
                    fi
                '''
            }
        }
    }

    post {
        success {
            sh '''
                echo "=========================================="
                echo "Deployment Successful"
                echo "=========================================="
                echo "Application: ${APP_NAME}"
                echo "Version: ${VERSION_LABEL}"
                echo "Image Tag: ${IMAGE_TAG}"
                echo "URL: http://${HOSTNAME}"
                echo "=========================================="
            '''
        }

        failure {
            sh '''
                echo "=========================================="
                echo "Deployment Failed"
                echo "=========================================="
                echo "Debug Information:"
                echo ""
                echo "1. Application Pods:"
                kubectl get pods -l app=${APP_NAME}
                echo ""
                echo "2. Application Description:"
                kubectl describe pods -l app=${APP_NAME} | tail -50
                echo ""
                echo "3. MySQL Pods:"
                kubectl get pods -l app=mysql
                echo ""
                echo "4. All Services:"
                kubectl get svc
                echo ""
                echo "5. All Ingress:"
                kubectl get ingress
                echo ""
                echo "6. Recent Events:"
                kubectl get events --sort-by='.lastTimestamp' | tail -20
                echo ""
                echo "7. Docker Images:"
                docker images | grep ${IMAGE_NAME}
            '''
        }

        always {
            cleanWs()
        }
    }
}