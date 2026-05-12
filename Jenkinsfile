pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
    }

    environment {
        SPRING_PROFILES_ACTIVE = 'test'
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
                    junit 'build/test-results/test/*.xml'
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
                    junit 'build/test-results/integrationTest/*.xml'
                }
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