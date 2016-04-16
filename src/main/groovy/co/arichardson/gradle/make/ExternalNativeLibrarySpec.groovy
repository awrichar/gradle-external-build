package co.arichardson.gradle.make

import org.gradle.nativeplatform.NativeLibrarySpec

interface ExternalNativeLibrarySpec extends NativeLibrarySpec {
    Closure<Void> getBuildInput()
    void buildInput(Closure<Void> action)

    Closure<Void> getBuildOutput()
    void buildOutput(Closure<Void> action)
}
