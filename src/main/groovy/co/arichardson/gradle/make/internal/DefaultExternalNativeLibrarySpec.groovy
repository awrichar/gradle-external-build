package co.arichardson.gradle.make.internal

import co.arichardson.gradle.make.ExternalNativeLibrarySpec
import org.gradle.nativeplatform.internal.DefaultNativeLibrarySpec

class DefaultExternalNativeLibrarySpec extends DefaultNativeLibrarySpec implements ExternalNativeLibrarySpec {
    private Closure<Void> configureBuildAction
    private Closure<File> externalOutputsAction

    @Override
    Closure<Void> getConfigureBuild() {
        return configureBuildAction
    }

    @Override
    void configureBuild(Closure<Void> action) {
        configureBuildAction = action
    }

    @Override
    Closure<Void> getExternalOutputs() {
        return externalOutputsAction
    }

    @Override
    void externalOutputs(Closure<Void> action) {
        externalOutputsAction = action
    }
}
