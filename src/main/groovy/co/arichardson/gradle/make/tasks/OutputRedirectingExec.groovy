package co.arichardson.gradle.make.tasks

import org.gradle.api.tasks.Exec

class OutputRedirectingExec extends Exec {
    boolean redirectOutput = true

    @Override
    protected void exec() {
        new OutputRedirector(this).redirect(this, redirectOutput) {
            super.exec()
            return execResult
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
