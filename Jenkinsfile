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
                            sh './gradlew clean test'
                        // echo "Checking test report files..."
                        // find . -path "*test-results*" -type f
                        // find . -path "*build/reports/tests*" -type f
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
                    sh './gradlew clean test integrationTest'
                }
            }
            post {
                always {
                     sh 'find build/test-results -name "*.xml" -type f -print'
                     junit testResults: 'build/test-results/**/*.xml', allowEmptyResults: false               }
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