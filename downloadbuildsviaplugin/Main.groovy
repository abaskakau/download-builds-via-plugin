package downloadbuildsviaplugin

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

def executeCommand(List<String> args)
{
    out("Invoking ${args}")
    ProcessBuilder pb=new ProcessBuilder(args);
    pb.redirectErrorStream(true)
    pb.directory(new File(this.binding.build.project.workspace.toURI()))

    Process process = pb.start()

    InputStream output = process.getInputStream()
    BufferedReader reader = new BufferedReader(new InputStreamReader(output))

    while ((line = reader.readLine()) != null)
    {
        println(line)
    }
    process.waitFor()
    out("Invoked with status ${process.exitValue()}")
    return process.exitValue()
}
def urlBuilderFile(String... args)
{
    def res=""
    for (int i=0;i<args.length-1;i++)
    {
        res+=args[i]+"/"
    }
    res+=args[args.length-1]
    return res
}
def urlBuilderDir(String... args)
{
    def res=""
    for (int i=0;i<args.length;i++)
    {
        res+=args[i]+"/"
    }
    return res
}
def downloadArtifact(def url)
{
    out("Downloading ${url}")
    List<String> download = new ArrayList<String>()
    download.add("aria2c")
    download.add("-x 3")
    download.add(url)
    if (executeCommand(download) == 0)
    {
        out("${url} has been downloaded successfully")
        return 1
    }
    else
    {
        out("Download failed!")
        return 0
    }
}
def renameArtifact(def oldName, def newName)
{
    out("Renaming ${oldName} to ${newName}")
    List<String> command = new ArrayList<String>()
    command.add("mv")
    command.add(oldName)
    command.add(newName)
    if (executeCommand(command) == 0)
    {
        out("Renamed successfully")
        return 1
    }
    else
    {
        out("Renaming failed!")
        return 0
    }
}
def deleteArtifact(def name)
{
    out("Deleting ${name}")
    List<String> command = new ArrayList<String>()
    command.add("rm")
    command.add("-rf")
    command.add(name)
    if (executeCommand(command) == 0)
    {
        out("Successfully deleted")
        return 1
    }
    else
    {
        out("Delete command failed!")
        return 0
    }
}
def createLink(def linkWhat, def linkWhere, def deleteOldLink = 1)
{
    out("Creating link ${linkWhere}")
    List<String> command = new ArrayList<String>()
    command.add("ln")
    command.add("-s")
    command.add(linkWhat)
    command.add(linkWhere)
    if (deleteOldLink)
    {
        deleteArtifact(linkWhere)
    }
    if (executeCommand(command) == 0)
    {
        out("Successfully created")
        return 1
    }
    else
    {
        out("Link creation failed!")
        return 0
    }
}
def createDir(def path)
{
    out("Creating directory ${path}")
    List<String> command = new ArrayList<String>()
    command.add("mkdir")
    command.add("-p")
    command.add(path)
    if (executeCommand(command) == 0)
    {
        out("Successfully created")
        return 1
    }
    else
    {
        out("Directory creation failed!")
        return 0
    }
}
def createFile(def path)
{
    out("Creating file ${path}")
    List<String> command = new ArrayList<String>()
    command.add("touch")
    command.add(path)
    if (executeCommand(command) == 0)
    {
        out("Successfully created")
        return 1
    }
    else
    {
        out("File creation failed!")
        return 0
    }
}
def getBuildNumberFromFile(def path)
{
    out("Opening file ${path}")
    List<String> command = new ArrayList<String>()
    command.add("head")
    command.add("-n1")
    command.add(path)
    out("Invoking ${command}")
    ProcessBuilder pb=new ProcessBuilder(command);
    pb.redirectErrorStream(true)
    pb.directory(new File(this.binding.build.project.workspace.toURI()))

    Process process = pb.start()

    InputStream output = process.getInputStream()
    BufferedReader reader = new BufferedReader(new InputStreamReader(output))

    def result=""

    while ((line = reader.readLine()) != null)
    {
        println(line)
        result+=line
    }
    process.waitFor()
    out("Invoked with status ${process.exitValue()}")
    if (process.exitValue() == 0)
    {
        out("Done.")
    }
    else
    {
        out("Error!")
        changeBuildStatus(0)
    }
    return result
}
def setBuildNumberFromFile(def number, def path)
{
    out("Writing ${number} to ${path}")
    File file = new File(path)
    file.write(number)
    out("Done.")
}
void out(def message)
{
    println("[OUTPUT] +${message}")
}
void changeBuildStatus(def code)
{
    switch (code)
    {
        case 1:
            build.result = Result.SUCCESS
            out("Build marked as successfull")
            break
        case 0:
            build.result = Result.FAILURE
            out("Build marked as failed")
            throw new AbortException("Build failed!")
            break
        case -1:
            build.result = Result.ABORTED
            out("Build marked as aborted")
            break
    }
}

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