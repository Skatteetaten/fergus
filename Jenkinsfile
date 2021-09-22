#!/usr/bin/env groovy
def jenkinsfile

def config = [
    scriptVersion              : 'feature/AOS-5907'
    iqOrganizationName         : 'Team AOS',
    compilePropertiesIq        : "-x test",
    pipelineScript             : 'https://git.aurora.skead.no/scm/ao/aurora-pipeline-scripts.git',
    credentialsId              : "github",
    javaVersion                : 11,
    jiraFiksetIKomponentversjon: true,
    chatRoom                   : "#aos-notifications",
    openShiftBuilderVersion    : "feature_AOS_5907_release_binary_build-SNAPSHOT",
    sonarQube                  : false,
    disableAllReports          : true,
    iq                         : false,
    uploadLeveransepakke       : false,
    versionStrategy            : [
        [branch: 'master', versionHint: '0']
    ]
]
fileLoader.withGit(config.pipelineScript, config.scriptVersion) {
  jenkinsfile = fileLoader.load('templates/leveransepakke')
}
jenkinsfile.gradle(config.scriptVersion, config)