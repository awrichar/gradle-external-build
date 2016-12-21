package com.cisco.gradle.externalbuild.context

import org.gradle.api.Task
import org.gradle.nativeplatform.NativeBinarySpec

class BuildConfigContext extends NativeBinaryContext {
    private LibraryResolver resolver = new LibraryResolver()
    final Task buildTask

    BuildConfigContext(NativeBinarySpec binary, Task task) {
        super(binary)
        buildTask = task
    }

    @Deprecated
    List<File> getRequiredLibraries() {
        return resolver.libraries
    }

    @Deprecated
    List<File> getRequiredIncludes() {
        return resolver.headers
    }

    void lib(Object library) {
        binary.lib(library)
    }

    void withResolvedLibraries(Closure action) {
        buildTask.project.afterEvaluate {
            action.delegate = resolver
            action.resolveStrategy = Closure.DELEGATE_FIRST
            action(resolver)
        }
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

    class LibraryResolver {
        List<File> getLibraries() {
            List<File> libraries = []
            binary.libs*.linkFiles.each {
                libraries.addAll(it.files)
            }
            return libraries
        }

        List<File> getHeaders() {
            List<File> includes = []
            binary.libs*.includeRoots.each {
                includes.addAll(it.files)
            }
            return includes
        }
    }
}
