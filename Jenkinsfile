#!groovy
pipeline {
  agent any

  tools {
    jdk "11.0.8"
  }

  options {
    buildDiscarder logRotator(numToKeepStr: '10')
  }

  stages {
    stage('Clean') {
      steps {
        sh 'mvn clean';
      }
    }

    stage('Build') {
      steps {
        sh 'mvn package';
      }
    }

    stage('Verify') {
      steps {
        sh 'mvn verify';
      }
    }

    stage('Archive') {
      steps {
        archiveArtifacts artifacts: 'target/reformcloud-versions-updater-1.0-SNAPSHOT.jar', fingerprint: true
      }
    }
  }

  post {
    always {
      withCredentials([string(credentialsId: 'discord-webhook', variable: 'url')]) {
        discordSend description: 'New build of Version-Updater', footer: 'Update', link: BUILD_URL, successful: currentBuild.resultIsBetterOrEqualTo('SUCCESS'), title: JOB_NAME, webhookURL: url
      }
    }
  }
}
