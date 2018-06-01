package com.cisco.gradle.externalbuild.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.process.internal.ExecAction

class CMake extends GnuMake {
    @Input Object cmakeExecutable = 'cmake'
    @Input List<Object> cmakeArgs = []
    @InputDirectory @Optional File cmakeRoot

    @Override
    protected void exec() {
        if (workingDir) {
            workingDir.mkdirs()
        }

        ExecAction cmakeAction = newSubAction()
        cmakeAction.executable = cmakeExecutable
        cmakeAction.args = []
        cmakeAction.args cmakeArgs

        if (cmakeRoot) {
            cmakeAction.args cmakeRoot.path
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
        cmakeRoot = project.file(rootFolder)
    }
}
