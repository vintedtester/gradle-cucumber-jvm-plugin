package com.commercehub.gradle.cucumber

import groovy.util.logging.Slf4j
import org.apache.tools.ant.util.TeeOutputStream
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.zeroturnaround.exec.ProcessExecutor

/**
 * Created by jgelais on 6/17/15.
 */
@Slf4j
class JavaProcessLauncher {
    String mainClassName
    List<File> classpath
    List<String> args = []
    List<String> jvmArgs = []
    File consoleOutLogFile
    File consoleErrLogFile
    Map<String, String> systemProperties = [:]
    Logger gradleLogger

    JavaProcessLauncher(String mainClassName, List<File> classpath) {
        this.mainClassName = mainClassName
        this.classpath = classpath
    }

    JavaProcessLauncher setArgs(List<String> args) {
        this.args = args*.toString()
        return this
    }

    JavaProcessLauncher setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs*.toString()
        return this
    }

    JavaProcessLauncher setConsoleOutLogFile(File consoleOutLogFile) {
        this.consoleOutLogFile = consoleOutLogFile
        return this
    }

    JavaProcessLauncher setConsoleErrLogFile(File consoleErrLogFile) {
        this.consoleErrLogFile = consoleErrLogFile
        return this
    }

    JavaProcessLauncher setSystemProperties(Map<String, String> systemProperties) {
        this.systemProperties = systemProperties
        return this
    }

    JavaProcessLauncher setGradleLogger(Logger gradleLogger) {
        this.gradleLogger = gradleLogger
        return this
    }

    int execute() {
        List<String> command = []
        command << javaCommand
        command << '-cp'
        command << classPathAsString
        if (!systemProperties.isEmpty()) {
            systemProperties.each { key, value ->
                command << "-D${key}=${value}".toString()
            }
        }
        if(!jvmArgs.empty) {
            jvmArgs.each {
                command << it
            }
        }
        command << mainClassName
        command.addAll(args)

        ProcessExecutor processExecutor = new ProcessExecutor().command(command)

        LoggingOutputStream infoOutputStream = gradleLogger ?
                new LoggingOutputStream(gradleLogger, LogLevel.INFO) : null
        OutputStream consoleOutputStream = consoleOutLogFile ?
                consoleOutLogFile.newOutputStream() : null
        OutputStream stdout = combineOutputStreams(infoOutputStream, consoleOutputStream)
        if (stdout) {
            processExecutor.redirectOutput(stdout)
        }

        LoggingOutputStream errorOutputStream = gradleLogger ?
                new LoggingOutputStream(gradleLogger, LogLevel.ERROR) : null
        OutputStream consoleErrorStream = consoleErrLogFile ?
                consoleErrLogFile.newOutputStream() : null
        OutputStream stderr = combineOutputStreams(errorOutputStream, consoleErrorStream)
        if (stderr) {
            processExecutor.redirectError(stderr)
        }

        try {
            log.debug("Running command [${command.join(' ')}]")
            return processExecutor.destroyOnExit().execute().exitValue
        } finally {
            stdout?.close()
            stderr?.close()
        }
    }

    /**
     * Returns an output stream that sends output to both streams. If either stream is null, the other is
     * returned. If both are null, null is returned.
     * @return OutputStream, may be null.
     */
    private static OutputStream combineOutputStreams(OutputStream left, OutputStream right) {
        if (left == null) {
            return right
        }
        if (right == null) {
            return left
        }
        return new TeeOutputStream(left, right)
    }

    String getClassPathAsString() {
        return classpath*.absolutePath.join(System.getProperty('path.separator'))
    }

    static String getJavaCommand() {
        File javaHome = new File(System.getProperty('java.home'))
        return new File(new File(javaHome, 'bin'), javaExecutable).absolutePath
    }

    static String getJavaExecutable() {
        return System.getProperty('os.name').contains('win') ? 'java.exe' : 'java'
    }
}
