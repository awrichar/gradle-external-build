package co.arichardson.gradle.make.context

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
