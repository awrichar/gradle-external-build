package co.arichardson.gradle.make.tasks

import org.gradle.process.internal.ExecAction

class AutoMake extends GnuMake {
    Object configureExecutable = project.file('configure')
    List<Object> configureArgs = []
    String crossCompileHost
    Object installPrefix

    @Override
    protected void exec() {
        if (workingDir) {
            workingDir.mkdirs()
        }

        if (installPrefix) {
            project.file(installPrefix).mkdirs()
        }

        ExecAction configureAction = getExecActionFactory().newExecAction()
        configureAction.executable = configureExecutable
        configureAction.environment = environment
        configureAction.workingDir = workingDir

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
        installPrefix = prefix
    }
}
