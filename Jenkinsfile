pipeline {
    agent any

    environment {
        PATH = "/usr/local/bin:${env.PATH}"
        BACKEND_DIR = 'backend'
    }

    tools {
        maven 'M3.9'
        jdk 'Java-17'
    }

    stages {
        stage('Checkout Code') {
            steps { checkout scm }
        }

        stage('Build Backend JAR') {
    		steps {
        		dir("backend") {
            		sh 'mvn clean package -DskipTests'
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

        stage('Check Docker') {
            steps {
                sh 'which docker'
                sh 'docker --version'
            }
        }

        stage('Run Infra with Docker Compose') {
            steps {
                sh 'docker compose -f backend/docker-compose-infra.yml build'
                sh 'docker compose -f backend/docker-compose-infra.yml up -d'
            }
        }

        stage('Run Apps with Docker Compose') {
            steps {
                sh 'docker compose -f backend/docker-compose-apps.yml build'
                sh 'docker compose -f backend/docker-compose-apps.yml up -d'
            }
        }
    }

    post {
        success { echo 'Pipeline Succeeded! JAR and Docker services are ready.' }
        failure { echo 'Pipeline Failed. Check logs.' }
    }
}
