pipeline {
    agent any

    environment {
        APP_NAME = "authentication-service"
        IMAGE_NAME = "authentication-service"
        IMAGE_TAG = "${BUILD_NUMBER}"
        K8S_NAMESPACE = "default"
    }

    tools {
        maven 'Maven3'
    }

    stages {

        stage('Verify Environment') {
            steps {
                sh '''
                    export PATH=/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH

                    echo "===== JAVA ====="
                    java -version

                    echo ""
                    echo "===== MAVEN ====="
                    mvn -version

                    echo ""
                    echo "===== DOCKER ====="
                    docker --version

                    echo ""
                    echo "===== KUBECTL ====="
                    kubectl version --client

                    echo ""
                    echo "===== WORKSPACE ====="
                    pwd
                    ls -la
                '''
            }
        }

        stage('Build Maven Package') {
            steps {
                sh '''
                    export PATH=/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH

                    mvn clean package -DskipTests
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    export PATH=/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:\$PATH

                    docker build \
                        -t ${IMAGE_NAME}:${IMAGE_TAG} \
                        -t ${IMAGE_NAME}:latest .
                """
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh """
                    export PATH=/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:\$PATH

                    kubectl apply -f k8s/

                    kubectl set image deployment/${APP_NAME} \
                        ${APP_NAME}=${IMAGE_NAME}:${IMAGE_TAG} \
                        -n ${K8S_NAMESPACE}
                """
            }
        }

        stage('Wait for Rollout') {
            steps {
                sh """
                    export PATH=/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:\$PATH

                    kubectl rollout status deployment/${APP_NAME} \
                        -n ${K8S_NAMESPACE}
                """
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    export PATH=/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH

                    echo "===== PODS ====="
                    kubectl get pods

                    echo ""
                    echo "===== DEPLOYMENTS ====="
                    kubectl get deployments

                    echo ""
                    echo "===== SERVICES ====="
                    kubectl get svc
                '''
            }
        }
    }

    post {

        success {
            echo "Pipeline completed successfully."
        }

        failure {
            echo "Pipeline failed."
        }
    }
}