package co.arichardson.gradle.make.context

import co.arichardson.gradle.make.Utils

class ExternalOutputsContext extends NativeBinaryContext {
    final ExternalHeadersContext headersContext
    File outputFile

    ExternalOutputsContext(NativeBinaryContext parent) {
        super(parent)
        headersContext = new ExternalHeadersContext(parent)
    }

    void exportedHeaders(Closure<Void> action) {
        Utils.invokeWithContext(action, headersContext)
    }
}
