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

    @Override
    boolean equals(Object other) {
        other in BuildConfigContext && buildTask == other.buildTask
    }

    Object methodMissing(String name, args) {
        buildTask."$name"(*args)
    }

    Object propertyMissing(String name) {
        return buildTask."$name"
    }

    void propertyMissing(String name, value) {
        buildTask."$name" = value
    }
}
