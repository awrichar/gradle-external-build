package com.cisco.gradle.externalbuild.context

import com.cisco.gradle.externalbuild.ExternalBuildPlugin
import org.gradle.language.cpp.CppSourceSet
import org.gradle.nativeplatform.NativeBinarySpec

class BuildOutputContext extends NativeBinaryContext {
    File outputFile

    BuildOutputContext(NativeBinarySpec parent) {
        super(parent)
    }

    void exportedHeaders(Closure<Void> config) {
        CppSourceSet externalSource = binary.sources.get(ExternalBuildPlugin.EXTERNAL_SOURCE) as CppSourceSet
        externalSource.exportedHeaders(config)
    }
}
