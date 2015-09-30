import hudson.model.*
import hudson.AbortException
import hudson.console.HyperlinkNote
import java.util.concurrent.CancellationException

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
def runJob(def name, def number, def version)
{
    def job = Hudson.instance.getJob(name)
    def anotherJob
    try {
        def params = [
                new StringParameterValue('buildNumber', number),
                new StringParameterValue('buildVersion', version),
        ]
        def future = job.scheduleBuild2(0, new Cause.UpstreamCause(build), new ParametersAction(params))
        out("Waiting for the completion of " + HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName))
        anotherBuild = future.get()
    } catch (CancellationException x) {
        throw new AbortException("${job.fullDisplayName} aborted.")
    }
    out(HyperlinkNote.encodeTo('/' + anotherBuild.url, anotherBuild.fullDisplayName) + " completed. Result was " + anotherBuild.result)

    if (anotherBuild.result == Result.SUCCESS)
    {
        return 1
    }
    if (anotherBuild.result == Result.ABORTED)
    {
        return -1
    }
    else
    {
        return 0
    }
}
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