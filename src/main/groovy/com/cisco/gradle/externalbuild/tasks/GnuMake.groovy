package com.cisco.gradle.externalbuild.tasks

import org.gradle.process.internal.ExecAction

class GnuMake extends OutputRedirectingExec {
    Object makefile
    int jobs
    List<String> targets = []

    public GnuMake() {
        executable 'make'
        jobs = project.gradle.startParameter.maxWorkerCount
    }

    @Override
    protected void exec() {
        if (workingDir) {
            workingDir.mkdirs()
        }

        if (makefile) {
            args = ['-f', project.file(makefile).path] + args
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
    }

    @Override
    boolean equals(Object other) {
        other in GnuMake &&
            makefile == other.makefile &&
            super.equals(other)
    }

    void makefile(Object makefile) {
        this.makefile = makefile
    }

    void targets(String... args) {
        this.targets.addAll(args)
    }

    void jobs(int jobs) {
        this.jobs = jobs
    }
}
