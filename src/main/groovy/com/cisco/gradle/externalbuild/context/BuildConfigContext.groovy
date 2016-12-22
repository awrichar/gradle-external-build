package com.cisco.gradle.externalbuild.context

import org.gradle.api.Task
import org.gradle.nativeplatform.NativeBinarySpec

class BuildConfigContext extends NativeBinaryContext {
    final Task buildTask

    BuildConfigContext(NativeBinarySpec binary, Task task) {
        super(binary)
        buildTask = task
    }

    List<File> getRequiredLibraries() {
        List<File> libraries = []
        binary.libs*.linkFiles.each {
            libraries.addAll(it.files)
        }
        return libraries
    }

    List<File> getRequiredIncludes() {
        List<File> includes = []
        binary.libs*.includeRoots.each {
            includes.addAll(it.files)
        }
        return includes
    }

    void lib(Object library) {
        binary.lib(library)
    }

    String getToolChainPath() {
        return getToolChainPath(true)
    }

    String getToolChainPath(boolean includeSystemPath) {
        String path = ""
        if (toolChain.hasProperty('path') && toolChain.path) {
            path += toolChain.path.join(File.pathSeparator)
        }

        if (includeSystemPath) {
            if (path) {
                path += File.pathSeparator
            }
            path += System.getenv('PATH')
        }

        return path
    }

    def methodMissing(String name, args) {
        buildTask."$name"(*args)
    }

    def propertyMissing(String name) {
        buildTask."$name"
    }

    def propertyMissing(String name, value) {
        buildTask."$name" = value
    }
}
