package co.arichardson.gradle.make.tasks

import org.gradle.api.Task
import org.gradle.logging.ConsoleRenderer
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.process.internal.ExecException

class OutputRedirector {
    private static final String OUTPUT_FILENAME = 'output.txt'
    private static final String ERROR_FILENAME = 'errors.txt'
    private static final int ERROR_LOG_SIZE = 10

    private final Task task
    final File logFile
    final File errorFile

    OutputRedirector(Task task, String prefix=null) {
        this.task = task

        String logName = OUTPUT_FILENAME
        String errorName = ERROR_FILENAME
        if (prefix) {
            logName = "${prefix}-${logName}"
            errorName = "${prefix}-${errorName}"
        }

        logFile = new File(task.temporaryDir, logName)
        errorFile = new File(task.temporaryDir, errorName)
    }

    void redirect(ExecSpec spec, boolean replaceStreams, Closure<ExecResult> execAction) {
        logFile.parentFile.mkdirs()
        errorFile.parentFile.mkdirs()

        OutputStream origOutput = spec.standardOutput
        OutputStream origError = spec.errorOutput

        if (replaceStreams) {
            spec.standardOutput = logFile.newOutputStream()
            spec.errorOutput = errorFile.newOutputStream()

            spec.ignoreExitValue = true
            ExecResult result = execAction.call()

            if (result.exitValue != 0) {
                // Log up to the 10 last lines of error output
                List<String> lines = errorFile.readLines()
                int endLine = lines.size() - 1
                if (endLine > 0) {
                    int startLine = lines.size() - ERROR_LOG_SIZE
                    if (startLine < 0) startLine = 0
                    task.logger.error(lines[startLine..endLine].join('\n'))
                }

                String errorsUrl = new ConsoleRenderer().asClickableFileUrl(errorFile)
                throw new ExecException("Exec failed with code ${result.exitValue}.\nSee the full log at ${errorsUrl}")
            }
        } else {
            spec.standardOutput = new MultiOutputStream(origOutput, logFile.newOutputStream())
            spec.errorOutput = new MultiOutputStream(origError, errorFile.newOutputStream())

            execAction.call()
        }

        spec.standardOutput = origOutput
        spec.errorOutput = origError
    }
}
