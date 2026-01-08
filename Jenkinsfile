pipeline {
    agent any

    tools {
        maven 'M3.9'
        jdk 'Java-17'
    }

    environment {
        BACKEND_DIR = 'backend'
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend JAR') {
            steps {
                script {
                    echo '--- Building Backend JAR ---'
                    dir("${BACKEND_DIR}") {
                        sh 'mvn clean package -DskipTests'
                    }
                }
            }
        }

        stage('Verify JARs') {
            steps {
                dir("backend") {
                    sh 'find . -name "*.jar" -ls'
                }
            }
        }

        stage('Run Infra with Docker Compose') {
            steps {
                script {
                    sh 'docker-compose -f docker-compose-infra.yml build'
                    sh 'docker-compose -f docker-compose-infra.yml up -d'
                }
            }
        }

        stage('Run Apps with Docker Compose') {
            steps {
                script {
                    sh 'docker-compose -f docker-compose-apps.yml build'
                    sh 'docker-compose -f docker-compose-apps.yml up -d'
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline Succeeded! JAR and Docker services are ready.'
        }
        failure {
            echo 'Pipeline Failed. Check logs.'
        }
    }
}
