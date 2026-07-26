pipeline {
    agent any

    environment {
        APP_NAME = "authentication-service"
        IMAGE_NAME = "authentication-service"
        IMAGE_TAG = "${BUILD_NUMBER}"
        K8S_NAMESPACE = "default"
        PATH = "/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:${env.PATH}"
    }

    tools {
        maven 'Maven3'
    }

    stages {

        stage('Verify Environment') {
            steps {
                sh '''
                    java -version
                    mvn -version
                    docker --version
                    kubectl version --client
                    kubectl config current-context
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
                sh """
                    docker build \
                        -t ${IMAGE_NAME}:${IMAGE_TAG} \
                        -t ${IMAGE_NAME}:latest .
                """
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh """
                    kubectl apply -f authentication-deployment.yaml

                    kubectl set image deployment/${APP_NAME} \
                        ${APP_NAME}=${IMAGE_NAME}:${IMAGE_TAG} \
                        -n ${K8S_NAMESPACE}
                """
            }
        }

        stage('Wait for Rollout') {
            steps {
                sh """
                    kubectl rollout status deployment/${APP_NAME} \
                        -n ${K8S_NAMESPACE}
                """
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "========== PODS =========="
                    kubectl get pods -o wide

                    echo ""
                    echo "========== DEPLOYMENTS =========="
                    kubectl get deployments

                    echo ""
                    echo "========== SERVICES =========="
                    kubectl get svc

                    echo ""
                    echo "========== IMAGE =========="
                    kubectl describe deployment authentication-service | grep Image
                '''
            }
        }
    }

    post {
        success {
            echo "Application deployed successfully to Docker Desktop Kubernetes."
        }

        failure {
            echo "Pipeline failed."
        }
    }
}