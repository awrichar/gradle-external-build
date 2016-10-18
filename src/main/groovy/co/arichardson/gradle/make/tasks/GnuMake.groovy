package co.arichardson.gradle.make.tasks

class GnuMake extends OutputRedirectingExec {
    Object makefile
    int jobs

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

        super.exec()
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

    void jobs(int jobs) {
        this.jobs = jobs
    }
}
