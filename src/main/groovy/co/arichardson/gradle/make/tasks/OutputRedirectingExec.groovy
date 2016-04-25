package co.arichardson.gradle.make.tasks

import org.gradle.api.tasks.Exec
import org.gradle.logging.ConsoleRenderer
import org.gradle.process.internal.ExecException

class OutputRedirectingExec extends Exec {
    boolean redirectOutput = true

    final File logFile
    final File errorFile

    public OutputRedirectingExec() {
        logFile = project.file("${temporaryDir}/output.txt")
        errorFile = project.file("${temporaryDir}/errors.txt")
    }

    @Override
    protected void exec() {
        logFile.parentFile.mkdirs()
        errorFile.parentFile.mkdirs()

        if (redirectOutput) {
            standardOutput = logFile.newOutputStream()
            errorOutput = errorFile.newOutputStream()

            ignoreExitValue = true
            super.exec()

            if (execResult.exitValue != 0) {
                // Log up to the 10 last lines of error output
                List<String> lines = errorFile.readLines()
                int endLine = lines.size() - 1
                if (endLine > 0) {
                    int startLine = lines.size() - 10
                    if (startLine < 0) startLine = 0
                    logger.error(lines[startLine..endLine].join('\n'))
                }

                String errorsUrl = new ConsoleRenderer().asClickableFileUrl(errorFile)
                throw new ExecException("Exec failed with code ${execResult.exitValue}.\nSee the full log at ${errorsUrl}")
            }
        } else {
            standardOutput = new MultiOutputStream(standardOutput, logFile.newOutputStream())
            errorOutput = new MultiOutputStream(errorOutput, errorFile.newOutputStream())

            super.exec()
        }
    }

    @Override
    boolean equals(Object other) {
        return other in OutputRedirectingExec &&
            executable == other.executable &&
            args == other.args &&
            environment == other.environment
    }
}
