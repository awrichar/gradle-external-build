package com.cisco.gradle.externalbuild.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.process.internal.ExecAction

class AutoMake extends GnuMake {
    @Input Object configureExecutable = './configure'
    @Input List<Object> configureArgs = []
    @Input @Optional String crossCompileHost
    @OutputDirectory @Optional File installPrefix

    @Override
    protected void exec() {
        if (workingDir) {
            workingDir.mkdirs()
        }

        if (installPrefix) {
            installPrefix.mkdirs()
        }

        ExecAction configureAction = newSubAction()
        configureAction.executable = configureExecutable
        configureAction.args = []

        if (crossCompileHost) {
            configureAction.args "--host=${crossCompileHost}"
        }

        if (installPrefix) {
            configureAction.args "--prefix=${installPrefix}"
        }

        configureAction.args configureArgs

        new OutputRedirector(this, 'configure').redirect(configureAction, redirectOutput) {
            configureAction.execute()
        }

        super.exec()
    }

    @Override
    boolean equals(Object other) {
        other in AutoMake &&
            configureExecutable == other.configureExecutable &&
            configureArgs == other.configureArgs &&
            crossCompileHost == other.crossCompileHost &&
            installPrefix == other.installPrefix &&
            super.equals(other)
    }

    void configureExecutable(Object executable) {
        configureExecutable = executable
    }

    void configureArgs(Object... args) {
        configureArgs.addAll(args)
    }

    void crossCompileHost(String host) {
        crossCompileHost = host
    }

    void installPrefix(Object prefix) {
        installPrefix = project.file(prefix)
    }
}
