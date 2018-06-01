package com.cisco.gradle.externalbuild.tasks

import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Exec
import org.gradle.process.internal.ExecAction

class OutputRedirectingExec extends Exec {
    @Console boolean redirectOutput = true

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

    protected ExecAction newSubAction() {
        ExecAction action = execActionFactory.newExecAction()
        action.executable = executable
        action.args = args
        action.workingDir = workingDir
        action.environment = environment
        action.standardInput = standardInput
        action.standardOutput = standardOutput
        action.errorOutput = errorOutput
        return action
    }
}
