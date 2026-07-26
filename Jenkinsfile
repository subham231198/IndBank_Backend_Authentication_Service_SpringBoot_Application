pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

    environment {
        APP_NAME   = "authentication-service"
        IMAGE_NAME = "authentication-service"
        IMAGE_TAG  = "${BUILD_NUMBER}"
        PATH = "/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:${env.PATH}"
    }

    stages {

        stage('Verify Environment') {
            steps {
                sh '''
                    echo "========== JAVA =========="
                    java -version

                    echo ""
                    echo "========== MAVEN =========="
                    mvn -version

                    echo ""
                    echo "========== DOCKER =========="
                    docker --version

                    echo ""
                    echo "========== DOCKER COMPOSE =========="
                    docker compose version

                    echo ""
                    echo "========== KUBECTL =========="
                    kubectl version --client

                    echo ""
                    echo "========== KUBERNETES =========="
                    kubectl get nodes
                '''
            }
        }

        stage('Ensure MySQL & Redis Running') {
            steps {
                sh '''
                    echo "Checking MySQL..."

                    if docker ps --format '{{.Names}}' | grep -q "^auth_mysql$"; then
                        echo "MySQL is already running."
                    else
                        echo "Starting MySQL..."
                        docker compose up -d mysql
                    fi

                    echo ""

                    echo "Checking Redis..."

                    if docker ps --format '{{.Names}}' | grep -q "^auth_redis$"; then
                        echo "Redis is already running."
                    else
                        echo "Starting Redis..."
                        docker compose up -d redis
                    fi

                    echo ""
                    docker ps
                '''
            }
        }

        stage('Build Spring Boot Application') {
            steps {
                sh '''
                    mvn clean package -DskipTests
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    docker compose build authentication-service

                    docker images | grep authentication-service
                '''
            }
        }

        stage('Load Image into Docker Desktop Kubernetes') {
            steps {
                sh '''
                    echo "Loading image into Docker Desktop Kubernetes..."

                    docker save authentication-service:latest \
                        -o authentication-service.tar

                    docker desktop kubernetes images load authentication-service.tar
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    echo "Removing previous deployment..."

                    kubectl delete deployment ${APP_NAME} --ignore-not-found=true
                    kubectl delete service ${APP_NAME} --ignore-not-found=true

                    sleep 5

                    echo "Deploying latest version..."

                    kubectl apply -f authentication-deployment.yaml

                    kubectl rollout status deployment/${APP_NAME} --timeout=300s
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    echo ""
                    echo "========== DEPLOYMENTS =========="
                    kubectl get deployments

                    echo ""
                    echo "========== PODS =========="
                    kubectl get pods -o wide

                    echo ""
                    echo "========== SERVICES =========="
                    kubectl get svc

                    echo ""
                    echo "========== CURRENT IMAGE =========="
                    kubectl describe deployment ${APP_NAME} | grep Image || true

                    echo ""
                    echo "========== EVENTS =========="
                    kubectl get events --sort-by=.metadata.creationTimestamp | tail -20
                '''
            }
        }
    }

    post {

        success {
            echo "=========================================="
            echo "Application deployed successfully!"
            echo "=========================================="
        }

        failure {

            echo "=========================================="
            echo "Deployment failed!"
            echo "=========================================="

            sh '''
                echo ""
                kubectl get all

                echo ""
                kubectl describe deployment ${APP_NAME} || true

                echo ""
                kubectl describe pods -l app=${APP_NAME} || true

                echo ""
                docker ps

                echo ""
                docker compose ps
            '''
        }

        always {
            echo "Pipeline Finished."
        }
    }
}