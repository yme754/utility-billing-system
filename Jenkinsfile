pipeline {
    agent any

    tools {
        maven 'Maven-3.9' 
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
