package co.arichardson.gradle.make.tasks

class MakeTask extends OutputRedirectingExec {
    File makefile

    public MakeTask() {
        executable 'make'
        args '-j', project.gradle.startParameter.maxWorkerCount
    }

    @Override
    protected void exec() {
        if (makefile) {
            args = ['-f', makefile.path] + args
        }

        super.exec()
    }
}

