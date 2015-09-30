package downloadbuildsviaplugin.lib

import java.util.concurrent.CancellationException
import hudson.model.*
import hudson.AbortException
import hudson.console.HyperlinkNote

def executeCommand(List<String> args) {
    out("Invoking ${args}")
    ProcessBuilder pb = new ProcessBuilder(args);
    pb.redirectErrorStream(true)
    pb.directory(new File(this.binding.build.project.workspace.toURI()))

    Process process = pb.start()

    InputStream output = process.getInputStream()
    BufferedReader reader = new BufferedReader(new InputStreamReader(output))

    while ((line = reader.readLine()) != null) {
        println(line)
    }
    process.waitFor()
    out("Invoked with status ${process.exitValue()}")
    return process.exitValue()
}

def urlBuilderFile(String... args) {
    def res = ""
    for (int i = 0; i < args.length - 1; i++) {
        res += args[i] + "/"
    }
    res += args[args.length - 1]
    return res
}

def urlBuilderDir(String... args) {
    def res = ""
    for (int i = 0; i < args.length; i++) {
        res += args[i] + "/"
    }
    return res
}

def downloadArtifact(def url) {
    out("Downloading ${url}")
    List<String> download = new ArrayList<String>()
    download.add("aria2c")
    download.add("-x 3")
    download.add(url)
    if (executeCommand(download) == 0) {
        out("${url} has been downloaded successfully")
        return 1
    } else {
        out("Download failed!")
        return 0
    }
}

def deleteArtifact(def name) {
    out("Deleting ${name}")
    List<String> command = new ArrayList<String>()
    command.add("rm")
    command.add("-rf")
    command.add(name)
    if (executeCommand(command) == 0) {
        out("Successfully deleted")
        return 1
    } else {
        out("Delete command failed!")
        return 0
    }
}

def createDir(def path) {
    out("Creating directory ${path}")
    List<String> command = new ArrayList<String>()
    command.add("mkdir")
    command.add("-p")
    command.add(path)
    if (executeCommand(command) == 0) {
        out("Successfully created")
        return 1
    } else {
        out("Directory creation failed!")
        return 0
    }
}

def createFile(def path) {
    out("Creating file ${path}")
    List<String> command = new ArrayList<String>()
    command.add("touch")
    command.add(path)
    if (executeCommand(command) == 0) {
        out("Successfully created")
        return 1
    } else {
        out("File creation failed!")
        return 0
    }
}

def getBuildNumberFromFile(def path) {
    out("Opening file ${path}")
    List<String> command = new ArrayList<String>()
    command.add("head")
    command.add("-n1")
    command.add(path)
    out("Invoking ${command}")
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true)
    pb.directory(new File(this.binding.build.project.workspace.toURI()))

    Process process = pb.start()

    InputStream output = process.getInputStream()
    BufferedReader reader = new BufferedReader(new InputStreamReader(output))

    def result = ""

    while ((line = reader.readLine()) != null) {
        println(line)
        result += line
    }
    process.waitFor()
    out("Invoked with status ${process.exitValue()}")
    if (process.exitValue() == 0) {
        out("Done.")
    } else {
        out("Error!")
        changeBuildStatus(0)
    }
    return result
}

void out(def message) {
    println("[OUTPUT] +${message}")
}

void changeBuildStatus(def code) {
    switch (code) {
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

def runJob(def name, def number, def version) {
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

    if (anotherBuild.result == Result.SUCCESS) {
        return 1
    }
    if (anotherBuild.result == Result.ABORTED) {
        return -1
    } else {
        return 0
    }
}