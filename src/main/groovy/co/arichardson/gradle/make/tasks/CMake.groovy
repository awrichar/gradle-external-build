package co.arichardson.gradle.make.tasks

import org.gradle.process.internal.ExecAction

class CMake extends GnuMake {
    Object cmakeExecutable = 'cmake'
    List<Object> cmakeArgs = []
    Object cmakeRoot

    @Override
    protected void exec() {
        if (workingDir) {
            workingDir.mkdirs()
        }

        ExecAction cmakeAction = getExecActionFactory().newExecAction()
        cmakeAction.executable = cmakeExecutable
        cmakeAction.environment = environment
        cmakeAction.workingDir = workingDir
        cmakeAction.args cmakeArgs

        if (cmakeRoot) {
            cmakeAction.args << project.file(cmakeRoot).path
        }

        new OutputRedirector(this, 'cmake').redirect(cmakeAction, redirectOutput) {
            cmakeAction.execute()
        }

        super.exec()
    }

    @Override
    boolean equals(Object other) {
        other in CMake &&
            cmakeExecutable == other.cmakeExecutable &&
            cmakeArgs == other.cmakeArgs &&
            cmakeRoot == other.cmakeRoot &&
            super.equals(other)
    }

    void cmakeExecutable(Object executable) {
        cmakeExecutable = executable
    }

    void cmakeArgs(Object... args) {
        cmakeArgs.addAll(args)
    }

    void cmakeRoot(Object rootFolder) {
        cmakeRoot = rootFolder
    }
}
