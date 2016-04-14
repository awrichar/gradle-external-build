package co.arichardson.gradle.make.context

class ExternalHeadersContext extends NativeBinaryContext {
    final File outputDir
    private final List<Object> source = []

    ExternalHeadersContext(BuildTaskContext parent) {
        super(parent)
        outputDir = parent.outputDir
    }

    List<Object> getSrcDirs() {
        return source
    }

    void srcDir(Object srcDir) {
        source << srcDir;
    }

    void srcDirs(Object... srcDirs) {
        srcDirs.each { source << it }
    }
}
