package co.arichardson.gradle.make.tasks

import org.gradle.process.internal.ExecAction

class QMake extends GnuMake {
    String qmakeExecutable = 'qmake'
    List<String> qmakeArgs = []

    @Override
    protected void exec() {
        ExecAction qmakeAction = getExecActionFactory().newExecAction()
        qmakeAction.executable = qmakeExecutable
        qmakeAction.args = qmakeArgs
        qmakeAction.environment = environment
        qmakeAction.workingDir = workingDir
        qmakeAction.execute()

        super.exec()
    }

    @Override
    boolean equals(Object other) {
        other in QMake &&
            qmakeExecutable == other.qmakeExecutable &&
            qmakeArgs == other.qmakeArgs &&
            super.equals(other)
    }
}
