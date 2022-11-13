import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.XmlReport
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.parallelTests
import jetbrains.buildServer.configs.kotlin.buildFeatures.xmlReport
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2022.10"

project {
    vcsRoot(TCDSL)
    buildType(HelloWorld)
    buildType(HelloFoo)
    buildType(PlaywrightDSL)
    subProject(UnitTesing)
}

object UnitTesing : Project({
    name = "FRONTEND"

    buildType(Frontend_FrontendUnitTests)
    buildType(Frontend_MakeFrontEndUnitTests)

    template(UNIT_FRONTEND)
})

object HelloWorld : BuildType({
    name = "Hello world #1"
    vcs {
        root(TCDSL)
    }
    steps {
        script {
            scriptContent = "echo 'Hello world!'"
        }
    }
})

object HelloFoo : BuildType({
    name = "This is another project"
    steps {
        script {
            scriptContent = "env | sort"
        }
    }
})

object TCDSL : GitVcsRoot({
    name = "This is TCDSL Root"
    url = "https://github.com/grmmvv/TCDSL.git"
    branch = "refs/heads/master"
    branchSpec = "refs/heads/*"
    checkoutPolicy = AgentCheckoutPolicy.SHALLOW_CLONE
    authMethod = password {
        userName = ""
        password = "credentialsJSON:757b93d4-4abe-4211-a875-d15bb135d3da"
    }
})


object PlaywrightDSL : BuildType({
    id("Playwright")
    name = "playwright"

    artifactRules = "+:playwright-report => playwright-report.zip"

    params {
        param("env.PLAYWRIGHT_JUNIT_OUTPUT_NAME", "results.xml")
    }

    vcs {
        root(TCDSL)
    }

    steps {
        script {
            name = "PW"
            scriptContent = """
                set -x 
                npm i
                npx playwright test --workers=4 \
                	--project='chromium' \
                    --global-timeout=60000 \
                    --reporter='line,junit,html'
            """.trimIndent()
            dockerImage = "mcr.microsoft.com/playwright:v1.27.1-focal"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
        }
    }

    failureConditions {
        executionTimeoutMin = 5
    }

    features {
        commitStatusPublisher {
            vcsRootExtId = "${TCDSL.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = password {
                    userName = "grmmvv"
                    password = "credentialsJSON:757b93d4-4abe-4211-a875-d15bb135d3da"
                }
            }
        }
        xmlReport {
            reportType = XmlReport.XmlReportType.JUNIT
            rules = "+:results.xml"
        }
        parallelTests {
            enabled = false
            numberOfBatches = 2
        }
    }
})

object Backend : Project({
    name = "BACKEND"

    buildType(BackendUnitAnalytics)
    buildType(Backend_MakeBackendUnitTests)
    buildType(BackendSurvey)

    template(UNIT_BACKEND)
})

object BackendSurvey : BuildType({
    templates(UNIT_BACKEND)
    name = "Backend (Survey)"
})

object BackendUnitAnalytics : BuildType({
    templates(UNIT_BACKEND)
    name = "Backend unit (Analytics)"

    params {
        param("env.BACKEND_UNIT_SCOPE", "analytics")
    }
})

object Backend_MakeBackendUnitTests : BuildType({
    name = "Make backend unit-tests"

    type = BuildTypeSettings.Type.COMPOSITE

    vcs {
        showDependenciesChanges = true
    }

    dependencies {
        snapshot(BackendSurvey) {
        }
        snapshot(BackendUnitAnalytics) {
        }
    }
})

object UNIT_BACKEND : Template({
    name = "UNIT_BACKEND"

    params {
        param("env.BACKEND_UNIT_SCOPE", "survey")
    }

    vcs {
        root(AbsoluteId("HttpsGithubComGrmmvvTcdslGit"))
    }

    steps {
        script {
            name = "BACKEND_UNIT"
            id = "RUNNER_14"
            scriptContent = """echo "Backend unit-test: %env.BACKEND_UNIT_SCOPE%""""
        }
    }

    features {
        commitStatusPublisher {
            id = "BUILD_EXT_9"
            vcsRootExtId = "HttpsGithubComGrmmvvTcdslGit"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "cks242b2c2e4dc0c35b6e3906a2883c8a51PJOm6a09jlW8z1cIbUVnZOA9glnTQ9bBJeJaG73OE+wWDDN0mu5s8o54L/6Y5agG"
                }
            }
        }
    }
})


object Frontend : Project({
    name = "FRONTEND"

    buildType(Frontend_FrontendUnitTests)
    buildType(Frontend_MakeFrontEndUnitTests)

    template(UNIT_FRONTEND)
})

object Frontend_FrontendUnitTests : BuildType({
    templates(UNIT_FRONTEND)
    name = "Frontend unit-tests"
})

object Frontend_MakeFrontEndUnitTests : BuildType({
    name = "Make front-end unit-tests"

    type = BuildTypeSettings.Type.COMPOSITE

    vcs {
        showDependenciesChanges = true
    }

    dependencies {
        snapshot(Frontend_FrontendUnitTests) {
        }
    }
})

object UNIT_FRONTEND : Template({
    name = "UNIT_FRONTEND"

    vcs {
        root(AbsoluteId("HttpsGithubComGrmmvvTcdslGit"))
    }

    steps {
        script {
            id = "RUNNER_15"
            scriptContent = """echo "This is frontend unit-tests""""
        }
    }

    features {
        commitStatusPublisher {
            id = "BUILD_EXT_10"
            vcsRootExtId = "HttpsGithubComGrmmvvTcdslGit"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "cks242b2c2e4dc0c35b6e3906a2883c8a51PJOm6a09jlW8z1cIbUVnZOA9glnTQ9bBJeJaG73OE+wWDDN0mu5s8o54L/6Y5agG"
                }
            }
        }
    }
})
