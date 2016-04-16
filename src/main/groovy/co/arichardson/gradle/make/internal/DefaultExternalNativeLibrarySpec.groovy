package co.arichardson.gradle.make.internal

import co.arichardson.gradle.make.ExternalNativeLibrarySpec
import org.gradle.nativeplatform.internal.DefaultNativeLibrarySpec

class DefaultExternalNativeLibrarySpec extends DefaultNativeLibrarySpec implements ExternalNativeLibrarySpec {
    private Closure<Void> buildInputAction
    private Closure<Void> buildOutputAction

    @Override
    Closure<Void> getBuildInput() {
        return buildInputAction
    }

    @Override
    void buildInput(Closure<Void> action) {
        buildInputAction = action
    }

    @Override
    Closure<Void> getBuildOutput() {
        return buildOutputAction
    }

    @Override
    void buildOutput(Closure<Void> action) {
        buildOutputAction = action
    }
}
