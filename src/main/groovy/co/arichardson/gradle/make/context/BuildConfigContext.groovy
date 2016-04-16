package co.arichardson.gradle.make.context

import org.gradle.api.Task
import org.gradle.nativeplatform.NativeBinarySpec

class BuildConfigContext extends NativeBinaryContext {
    final List<File> linkedLibraries
    final Task buildTask

    BuildConfigContext(NativeBinarySpec binary, Task task) {
        super(binary)

        linkedLibraries = []
        binary.libs*.linkFiles.each {
            linkedLibraries.addAll(it.files)
        }

        buildTask = task
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
