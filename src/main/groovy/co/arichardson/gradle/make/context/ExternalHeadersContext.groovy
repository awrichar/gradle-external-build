package co.arichardson.gradle.make.context

class ExternalHeadersContext extends NativeBinaryContext {
    private final List<Object> source = []

    ExternalHeadersContext(NativeBinaryContext parent) {
        super(parent)
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
