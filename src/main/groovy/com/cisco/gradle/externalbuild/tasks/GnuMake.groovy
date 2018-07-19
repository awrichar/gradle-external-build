package com.cisco.gradle.externalbuild.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.process.internal.ExecAction

class GnuMake extends OutputRedirectingExec {
    @InputFile @Optional File makefile
    @Internal int jobs
    @Input @Optional List<String> targets = []

    public GnuMake() {
        executable 'make'
        jobs = project.gradle.startParameter.maxWorkerCount
    }

    @Override
    protected void exec() {
        if (workingDir) {
            workingDir.mkdirs()
        }

        List<String> origArgs = []
        origArgs.addAll(args)

        if (makefile) {
            args = ['-f', makefile.path] + args
        }

        args '-j', jobs

        if (targets) {
            targets.each { String target ->
                ExecAction action = newSubAction()
                action.args target

                new OutputRedirector(this, "make${target.capitalize()}").redirect(action, redirectOutput) {
                    action.execute()
                }
            }
        } else {
            super.exec()
        }

        args = origArgs
    }

    @Override
    boolean equals(Object other) {
        other in GnuMake &&
            makefile == other.makefile &&
            super.equals(other)
    }

    void makefile(Object makefile) {
        this.makefile = project.file(makefile)
    }

    void targets(String... args) {
        this.targets.addAll(args)
    }

    void jobs(int jobs) {
        this.jobs = jobs
    }
}
