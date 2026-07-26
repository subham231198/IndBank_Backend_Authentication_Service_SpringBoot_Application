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
                        -t ${IMAGE_NAME}:${IMAGE_TAG} \
                        -t ${IMAGE_NAME}:latest .

                    docker images | grep ${IMAGE_NAME}
                '''
            }
        }

        stage('Start NGINX Pod') {
            steps {
                sh '''
                    if ! kubectl get pod nginx >/dev/null 2>&1; then
                        echo "Starting NGINX Pod..."
                        kubectl run nginx --image=nginx --restart=Never
                    else
                        echo "NGINX Pod already exists."
                    fi

                    kubectl wait \
                        --for=condition=Ready \
                        pod/nginx \
                        --timeout=120s || true
                '''
            }
        }

        stage('Deploy Application') {
            steps {
                sh '''
                    echo "Removing previous deployment..."

                    kubectl delete deployment ${APP_NAME} --ignore-not-found=true
                    kubectl delete service ${APP_NAME} --ignore-not-found=true

                    sleep 5

                    echo "Deploying application..."

                    kubectl apply -f authentication-deployment.yaml

                    kubectl rollout status deployment/${APP_NAME} --timeout=300s
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

                    echo "========== DEPLOYMENT =========="
                    kubectl describe deployment ${APP_NAME}

                    echo ""

                    echo "========== EVENTS =========="
                    kubectl get events --sort-by=.metadata.creationTimestamp | tail -20
                '''
            }
        }
    }

    post {

        success {
            echo "======================================="
            echo "Application deployed successfully!"
            echo "======================================="
        }

        failure {
            echo "======================================="
            echo "Deployment failed!"
            echo "======================================="

            sh '''
                kubectl get all

                echo ""
                kubectl describe deployment ${APP_NAME} || true

                echo ""
                kubectl describe pods -l app=${APP_NAME} || true
            '''
        }

        always {
            echo "Pipeline Finished."
        }
    }
}