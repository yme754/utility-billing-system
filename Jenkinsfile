pipeline {
    agent any

    tools {
        // MUST match the names you gave in "Global Tool Configuration"
        maven 'Maven-3.9' 
        jdk 'Java-17'
    }

    environment {
        // Defines where the backend is located relative to root
        BACKEND_DIR = 'backend' 
    }

    stages {
        stage('Checkout Code') {
            steps {
                // Pulls code from your GitHub repo
                checkout scm
            }
        }

        stage('Build Backend JAR') {
            steps {
                script {
                    echo '--- Building Backend JAR ---'
                    // Switch to backend directory and run maven build
                    dir("${BACKEND_DIR}") {
                        // -DskipTests speeds it up; remove if you want to run tests
                        sh 'mvn clean package -DskipTests' 
                    }
                }
            }
        }

        stage('Verify JAR') {
            steps {
                dir("${BACKEND_DIR}/target") {
                    sh 'ls -l *.jar'
                    echo '--- JAR File Successfully Built! ---'
                }
            }
        }
    }
    
    post {
        success {
            echo 'Pipeline Succeeded! JAR is ready.'
        }
        failure {
            echo 'Pipeline Failed. Check logs.'
        }
    }
}