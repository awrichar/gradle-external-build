package co.arichardson.gradle.make.context

import co.arichardson.gradle.make.Utils

class ExternalOutputsContext extends NativeBinaryContext {
    final ExternalHeadersContext headersContext
    final File outputDir
    File outputFile

    ExternalOutputsContext(BuildTaskContext parent) {
        super(parent)
        headersContext = new ExternalHeadersContext(parent)
        outputDir = parent.outputDir
    }

    void exportedHeaders(Closure<Void> action) {
        Utils.invokeWithContext(action, headersContext)
    }
}
