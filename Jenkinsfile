pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

    environment {
        APP_NAME = "authentication-service"
        IMAGE_NAME = "authentication-service"
        IMAGE_TAG = "${BUILD_NUMBER}"
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

        stage('Start NGINX Pod') {
            steps {
                sh '''
                    if ! kubectl get pod nginx >/dev/null 2>&1; then
                        echo "Starting NGINX pod..."
                        kubectl run nginx --image=nginx --restart=Never
                    else
                        echo "NGINX pod already running."
                    fi

                    kubectl get pods
                '''
            }
        }

        stage('Deploy Application') {
            steps {
                sh '''
                    kubectl apply -f authentication-deployment.yaml

                    kubectl set image deployment/authentication-service \
                        authentication-service=authentication-service:${BUILD_NUMBER}

                    kubectl rollout status deployment/authentication-service --timeout=300s
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
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
                    kubectl describe deployment authentication-service | grep Image || true

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
                kubectl get all
                kubectl describe deployment authentication-service || true
            '''
        }

        always {
            echo "Pipeline Finished."
        }
    }
}