package co.arichardson.gradle.make.context

import co.arichardson.gradle.make.Utils
import org.gradle.nativeplatform.NativeBinarySpec

class ExternalOutputsContext extends NativeBinaryContext {
    final ExternalHeadersContext headersContext
    File outputFile

    ExternalOutputsContext(NativeBinarySpec parent) {
        super(parent)
        headersContext = new ExternalHeadersContext(parent)
    }

    void exportedHeaders(Closure<Void> action) {
        Utils.invokeWithContext(action, headersContext)
    }
}
