package com.cisco.gradle.externalbuild.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.process.internal.ExecAction

class QMake extends GnuMake {
    @Input String qmakeExecutable = 'qmake'
    @Input List<Object> qmakeArgs = []
    @InputFile @Optional File qmakeProject

    @Override
    protected void exec() {
        if (workingDir) {
            workingDir.mkdirs()
        }

        ExecAction qmakeAction = getExecActionFactory().newExecAction()
        qmakeAction.executable = qmakeExecutable
        qmakeAction.args = qmakeArgs
        qmakeAction.environment = environment
        qmakeAction.workingDir = workingDir

        if (qmakeProject) {
            qmakeAction.args(qmakeProject.path)
        }

        new OutputRedirector(this, 'qmake').redirect(qmakeAction, redirectOutput) {
            qmakeAction.execute()
        }

        super.exec()
    }

    @Override
    boolean equals(Object other) {
        other in QMake &&
            qmakeExecutable == other.qmakeExecutable &&
            qmakeArgs == other.qmakeArgs &&
            qmakeProject == other.qmakeProject &&
            super.equals(other)
    }

    void qmakeExecutable(String executable) {
        qmakeExecutable = executable
    }

    void qmakeArgs(Object... args) {
        qmakeArgs.addAll(args)
    }

    void qmakeProject(Object projectFile) {
        qmakeProject = project.file(projectFile)
    }
}
