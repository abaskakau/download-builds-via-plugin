import hudson.model.*
import hudson.AbortException
import hudson.console.HyperlinkNote
import java.util.concurrent.CancellationException

def script = new GroovyScriptEngine(this.binding.build.project.workspace.toString()).with {
    loadScriptByName('Library.groovy')
}
this.metaClass.mixin script

String [] versionsList = [
        "6.0-NIGHTLY",
        "5.4-NIGHTLY",
        "6.0.0.0",
        "MASTER-NIGHTLY"
]
def buildDirectory = build.getEnvironment(listener).get('PENTAHO_BUILDS_PATH')
def mainJob = build.buildVariableResolver.resolve("mainJob")
def versionSpecified = build.buildVariableResolver.resolve("versionSpecified")
def hostedLocation = "http://10.177.176.213/hosted"
def hostedVersion = "latest"

try
{
    //Initial info
    out("==========================INITIAL INFORMATION========================")
    out("JOB NAME = ${this.binding.build.project.name}")
    out("MAIN JOB = ${mainJob}")
    out("VERSION SPECIFIED = ${versionSpecified}")
    out("HOSTED LOCATION = ${hostedLocation}")
    out("WORKSPACE = ${this.binding.build.project.workspace}")
    out("PENTAHO BUILDS PATH = ${buildDirectory}")
    out("versionsList = ")
    versionsList.each { print(it+", ") }
    println()
    out("===================END OF INITIAL INFORMATION========================")

    //Startup

    if (versionSpecified == "None")
    {
        versionsList.each {
            def lastBuildFile = urlBuilderFile(buildDirectory,"DMZ-TEST",it,".lastBuild")
            createDir(urlBuilderDir(buildDirectory,"DMZ-TEST",it))
            createFile(lastBuildFile)

            out("Getting actual build number")

            downloadArtifact(urlBuilderFile(hostedLocation,it,hostedVersion,"build.info"))

            def actualVersion = getBuildNumberFromFile(urlBuilderFile(this.binding.build.project.workspace.toString(),"build.info"))
            def storedVersion = getBuildNumberFromFile(lastBuildFile)

            deleteArtifact(urlBuilderFile(this.binding.build.project.workspace.toString(),"build.info"))

            out("Actual build version = ${actualVersion}")
            out("Actual version is ${actualVersion}. Last stored version is ${storedVersion}")
            if (storedVersion != "")
            {
                if (storedVersion < actualVersion)
                {
                    out("New version detected. Downloading")
                    changeBuildStatus(runJob(mainJob,actualVersion,it))
                    return
                }
                else
                {
                    out("Stored version is ${storedVersion} not less than ${actualVersion}")
                    return
                }
            }
            else
            {
                out ("No stored data. Downloading actual version")
                changeBuildStatus(runJob(mainJob,actualVersion,it))
                return
            }
        }
    }
    else
    {
        def lastBuildFile = urlBuilderFile(buildDirectory,"DMZ-TEST",versionSpecified,".lastBuild")
        createDir(urlBuilderDir(buildDirectory,"DMZ-TEST",versionSpecified))
        createFile(lastBuildFile)

        out("Getting actual build number")

        downloadArtifact(urlBuilderFile(hostedLocation,versionSpecified,hostedVersion,"build.info"))

        def actualVersion = getBuildNumberFromFile(urlBuilderFile(this.binding.build.project.workspace.toString(),"build.info"))
        def storedVersion = getBuildNumberFromFile(lastBuildFile)

        deleteArtifact(urlBuilderFile(this.binding.build.project.workspace.toString(),"build.info"))

        out("Actual build version = ${actualVersion}")
        out("Actual version is ${actualVersion}. Last stored version is ${storedVersion}")
        if (storedVersion != "")
        {
            if (storedVersion < actualVersion)
            {
                out("New version detected. Downloading")
                changeBuildStatus(runJob(mainJob,actualVersion,versionSpecified))
                return
            }
            else
            {
                out("Stored version is ${storedVersion} not less than ${actualVersion}")
                return
            }
        }
        else
        {
            out ("No stored data. Downloading actual version")
            changeBuildStatus(runJob(mainJob,actualVersion,it))
            return
        }
    }
}
catch(CancellationException x)
{
    throw new AbortException("${this.binding.build.project.name} aborted.")
}