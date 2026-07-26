pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

    environment {
        APP_NAME = "authentication-service"
        IMAGE_NAME = "authentication-service"
        IMAGE_TAG = "${BUILD_NUMBER}"
        NAMESPACE = "default"
        PATH = "/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:${env.PATH}"
    }

    stages {

        stage('Verify Environment') {
            steps {
                sh '''
                    echo "========== JAVA =========="
                    java -version

                    echo "========== MAVEN =========="
                    mvn -version

                    echo "========== DOCKER =========="
                    docker --version

                    echo "========== KUBECTL =========="
                    kubectl version --client

                    echo "========== CURRENT CONTEXT =========="
                    kubectl config current-context

                    echo "========== CLUSTER =========="
                    kubectl get nodes
                '''
            }
        }

        stage('Build Maven Package') {
            steps {
                sh '''
                    mvn clean package -DskipTests
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    docker build \
                        -t authentication-service:${BUILD_NUMBER} \
                        -t authentication-service:latest .
                '''
            }
        }

        stage('Verify Docker Image') {
            steps {
                sh '''
                    docker images | grep authentication-service
                '''
            }
        }

        stage('Start NGINX Ingress (if required)') {
            steps {
                sh '''
                    if ! kubectl get namespace ingress-nginx >/dev/null 2>&1; then
                        echo "Installing NGINX Ingress Controller..."

                        kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
                        kubectl run nginx --image nginx
                        kubectl wait \
                          --namespace ingress-nginx \
                          --for=condition=Ready pod \
                          --selector=app.kubernetes.io/component=controller \
                          --timeout=300s
                    else
                        echo "NGINX Ingress already installed."
                    fi
                '''
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    echo "Starting NGINX Pod..."
                    kubectl run nginx --image=nginx --restart=Never || true

                    echo "Creating Authentication Deployment..."
                    kubectl create deployment authentication-service --image=nginx || true

                    echo "Waiting for deployment..."
                    kubectl rollout status deployment/authentication-service --timeout=300s

                    echo "Current Resources"
                    kubectl get pods
                    kubectl get deployments
                    kubectl get svc
                '''
            }
        }
        stage('Deploy Application') {
            steps {
                sh '''
                    kubectl apply -f authentication-deployment.yaml
                '''
            }
        }

        stage('Update Docker Image') {
            steps {
                sh '''
                    kubectl set image deployment/authentication-service \
                    authentication-service=authentication-service:${BUILD_NUMBER} \
                    --record || true
                '''
            }
        }

        stage('Wait for Rollout') {
            steps {
                sh '''
                    kubectl rollout status deployment/authentication-service --timeout=300s
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
                    kubectl describe deployment authentication-service | grep Image

                    echo ""
                    echo "========== EVENTS =========="
                    kubectl get events --sort-by=.metadata.creationTimestamp | tail -20
                '''
            }
        }

    }

    post {

        success {
            echo "=================================="
            echo "Application deployed successfully!"
            echo "=================================="
        }

        failure {
            echo "=================================="
            echo "Deployment failed."
            echo "=================================="

            sh '''
                kubectl get pods -A || true
                kubectl describe deployment authentication-service || true
            '''
        }

        always {
            echo "Pipeline Finished."
        }
    }
}