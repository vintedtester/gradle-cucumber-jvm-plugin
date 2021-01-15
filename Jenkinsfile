pipeline {
    agent any
    options {
        ansiColor('xterm')
        timestamps()
    }
    triggers {
        pollSCM('')
        issueCommentTrigger('.*test this please.*')
    }
    stages {
        stage('Test') {
            steps {
                sh './gradlew build'
            }
        }
        stage('Publish') {
            when {
                branch 'master'
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus_oom_user', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                    sh "sed -i \"s/^version=.*\$/version=\$(git describe --tags)/\" gradle.properties"
                    sh './gradlew publish'
                }
            }
        }
    }
}
