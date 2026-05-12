pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
    }

    environment {
        SPRING_PROFILES_ACTIVE = 'test'
        DOCKER_REGISTRY = 'localhost:5001'
        DOCKER_IMAGE = 'cloud-task-manager'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Validate Docker') {
            steps {
                sh '''
                    docker version
                    docker info
                '''
            }
        }

        stage('Gradle Build') {
            steps {
                sh '''
                    chmod +x ./gradlew
                    ./gradlew clean assemble
                '''
            }
        }
        stage('Unit Tests') {
            steps {
                withCredentials([
                    string(credentialsId: 'cloud-task-manager-jwt-secret', variable: 'JWT_SECRET')
                ]) {
                    sh '''
                        ./gradlew test
                    '''
                }
            }
            post {
                always {
                    junit allowEmptyResults: false, testResults: '**/build/test-results/test/*.xml'
                }
            }
        }
        stage('Integration Tests') {
            steps {
                withCredentials([
                    string(credentialsId: 'cloud-task-manager-jwt-secret', variable: 'JWT_SECRET')
                ]) {
                    sh '''
                        ./gradlew integrationTest
                    '''
                }
            }
            post {
                always {
                    junit testResults: 'build/test-results/**/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Publish Docker Image') {
            steps {
                sh '''
                    IMAGE_TAG="${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}"
                    LATEST_TAG="${DOCKER_REGISTRY}/${DOCKER_IMAGE}:latest"

                    docker build -t "${IMAGE_TAG}" -t "${LATEST_TAG}" .
                    docker push "${IMAGE_TAG}"
                    docker push "${LATEST_TAG}"
                '''
            }
        }

        stage('Verify Registry Pull') {
            steps {
                sh '''
                    IMAGE_TAG="${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${BUILD_NUMBER}"

                    docker image rm "${IMAGE_TAG}" || true
                    docker pull "${IMAGE_TAG}"
                '''
            }
        }
    }

    post {
        success {
            echo 'CI pipeline passed.'
        }

        failure {
            echo 'CI pipeline failed.'
        }
    }
}
