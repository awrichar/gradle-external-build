package co.arichardson.gradle.make

import org.gradle.nativeplatform.NativeLibrarySpec

interface ExternalNativeLibrarySpec extends NativeLibrarySpec {
    Closure<Void> getConfigureBuild()
    void configureBuild(Closure<Void> action)

    Closure<Void> getExternalOutputs()
    void externalOutputs(Closure<Void> action)
}
