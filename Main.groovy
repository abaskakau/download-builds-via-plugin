import hudson.model.*
import hudson.AbortException
import hudson.console.HyperlinkNote
import java.util.concurrent.CancellationException

String [] artifactsList = [
        "pentaho-business-analytics-x64.bin",
        "pentaho-business-analytics-x64.exe",
        "pentaho-business-analytics-x64.app.tar.gz",
        "biserver-ee.zip",
        "pdi-ee.zip"
]
def buildVersion = build.buildVariableResolver.resolve("buildVersion")
def buildDirectory = build.getEnvironment(listener).get('PENTAHO_BUILDS_PATH')
def hostedLocation = "http://10.177.176.213/hosted"
def hostedVersion = "latest"
def BUILD_NUMBER = build.getEnvironment(listener).get('BUILD_NUMBER')
def buildArchive = urlBuilderDir("/media/storage/jenkins/jobs",this.binding.build.project.name,"builds",BUILD_NUMBER,"archive")
def buildNumber = build.buildVariableResolver.resolve("buildNumber")
def lastBuildFile = urlBuilderFile(buildDirectory,"DMZ-TEST",buildVersion,".lastBuild")

def script = new GroovyScriptEngine(this.binding.build.project.workspace.toString()).with {
    loadScriptByName('Library.groovy')
}
this.metaClass.mixin script

try
{
    //Initial info
    out("==========================INITIAL INFORMATION========================")
    out("JOB NAME = ${this.binding.build.project.name}")
    out("BUILD VERSION = ${buildVersion}")
    out("BUILD NUMBER LEGACY = ${buildNumber}")
    out("BUILD NUMBER JENKINS = ${BUILD_NUMBER}")
    out("HOSTED LOCATION = ${hostedLocation}")
    out("WORKSPACE = ${this.binding.build.project.workspace}")
    out("LAST BUILD FILE = ${lastBuildFile}")
    out("PENTAHO BUILDS PATH = ${buildDirectory}")
    out("artifactsList = ")
    artifactsList.each { print(it+", ") }
    println()
    out("===================END OF INITIAL INFORMATION========================")

    //Startup
    createDir(urlBuilderDir(buildDirectory,"DMZ-TEST",buildVersion))
    createFile(lastBuildFile)
    if ((!buildNumber) || (!buildVersion))
    {
        out("Wrong parameter!")
        out("Please use control job to run!")
        changeBuildStatus(0)
    }

    //Main body
    artifactsList.each {
        out("Triggering download for ${it}")
        changeBuildStatus(downloadArtifact(urlBuilderFile(hostedLocation, buildVersion, hostedVersion, it)))
        changeBuildStatus(renameArtifact(urlBuilderFile(this.binding.build.project.workspace.toString(), it), urlBuilderFile(this.binding.build.project.workspace.toString(), "${buildVersion}-${buildNumber}-${it}")))
        changeBuildStatus(createLink(buildArchive, urlBuilderFile(buildDirectory,"DMZ-TEST",buildVersion,buildNumber)))
        changeBuildStatus(createLink(buildArchive, urlBuilderFile(buildDirectory,"DMZ-TEST",buildVersion,"LATEST")))
        changeBuildStatus(setBuildNumberFromFile(buildNumber,lastBuildFile))
    }
}
catch(CancellationException x)
{
    throw new AbortException("${this.binding.build.project.name} aborted.")
}