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
}

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
